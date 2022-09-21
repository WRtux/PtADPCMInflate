import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileLock;
import java.nio.file.AccessDeniedException;

public final class Inflator {

	static final class PtADPCMDecoder {

		static final int INDEX_COUNT = 12;

		static final int[] SAMPLE_VAR_LIST = {
			-28, -20, -14, -10, -7, -5, -3, -1,
			+1, +3, +5, +7, +10, +14, +20, +28
		};
		static final int[] INDEX_VAR_LIST = {
			+2, +2, +1, +1, 0, 0, 0, -1,
			-1, 0, 0, 0, +1, +1, +2, +2
		};

		static final int[][] SAMPLE_VAR_TABLE;
		static {
			SAMPLE_VAR_TABLE = new int[INDEX_COUNT][0xF + 1];
			for (int i = 0; i < INDEX_COUNT; i++) {
				for (int j = 0x0; j <= 0xF; j++) {
					SAMPLE_VAR_TABLE[i][j] = (SAMPLE_VAR_LIST[j] << i) / 2;
				}
			}
		}

		static final void decodeData(WaveInfo ininf, DataInput din, DataOutput dout) throws IOException {
			int fcnt = ininf.getFrameCount(), fs = ininf.getFrameSize() / ininf.channelCount;
			for (int i = 0; i < fcnt; i++) {
				int[][] sbuf = new int[ininf.channelCount][ininf.frameSampleCount];
				for (int j = 0; j < ininf.channelCount; j++) {
					sbuf[j][0] = IOHelper.readShort(din);
					sbuf[j][1] = IOHelper.readShort(din);
					int cur = IOHelper.readByteU(din);
					byte[] buf = IOHelper.readBytes(din, fs - 5);
					for (int k = 2; k < ininf.frameSampleCount; k++) {
						int d = (buf[(k - 2) / 2] >>> k % 2 * 4) & 0xF;
						int lvl = (sbuf[j][k - 1] * 2 - sbuf[j][k - 2]) + SAMPLE_VAR_TABLE[cur][d];
						sbuf[j][k] = Math.min(Math.max(lvl, -0x8000), 0x7FFF);
						cur = Math.min(Math.max(cur + INDEX_VAR_LIST[d], 0), INDEX_COUNT - 1);
					}
				}
				for (int j = 0; j < ininf.frameSampleCount; j++) {
					for (int k = 0; k < ininf.channelCount; k++) {
						IOHelper.writeShort(dout, sbuf[k][j]);
					}
				}
			}
		}

		@Deprecated
		private PtADPCMDecoder() {
			throw new IllegalStateException();
		}

	}

	static WaveInfo prepareInputInfo(DataInput din, int lim) throws ApplicationException, IOException {
		int pos = 0;
		{
			String tag = IOHelper.readString(din, "ISO-8859-1", 4);
			if (!tag.equals(WaveInfo.HEADER_MAGIC))
				throw new ApplicationException(-2, "Unrecognized file header: " + tag);
			if (8 + IOHelper.readInt(din) != lim)
				System.err.println("Warning: File size may be incorrect.");
			tag = IOHelper.readString(din, "ISO-8859-1", 4);
			if (!tag.equals(WaveInfo.HEADER_TYPE))
				throw new ApplicationException(-2, "Unrecognized file type: " + tag);
			pos += 12;
		}
		WaveInfo inf = null;
		while (true) {
			String tag = IOHelper.readString(din, "ISO-8859-1", 4);
			int s = IOHelper.readInt(din);
			switch (tag) {
			case "fmt ": {
				if (!(s == 16 || s >= 18))
					throw new ApplicationException(-2, "Invalid header size: " + s);
				inf = new WaveInfo();
				inf.encoding = WaveInfo.Encoding.get(IOHelper.readShort(din));
				inf.channelCount = IOHelper.readShortU(din);
				inf.sampleRate = IOHelper.readInt(din);
				IOHelper.readInt(din);
				int fs = IOHelper.readShortU(din);
				inf.sampleDepth = IOHelper.readShortU(din);
				if (s >= 18) {
					if (IOHelper.readShortU(din) != s - 18)
						System.err.println("Warning: Extended header size may be incorrect.");
					IOHelper.readBytes(din, s - 18);
				}
				if (inf.encoding != WaveInfo.Encoding.PTADPCM)
					throw new ApplicationException(1, "Unexpected encoding: " + inf.encoding.name);
				if (inf.sampleDepth != 4 && inf.sampleDepth != 16)
					throw new ApplicationException(-2, "Invalid sample depth: " + inf.sampleDepth);
				if (fs % inf.channelCount != 0 || fs / inf.channelCount <= 5)
					throw new ApplicationException(-2, "Invalid frame size: " + fs);
				inf.frameSampleCount = 2 + (fs / inf.channelCount - 5) * 2;
				break;
			}
			case "data":
				if (8 + s != lim - pos)
					System.err.println("Warning: Data size may be incorrect.");
				inf.setDataSize(s);
				return inf;
			default:
				System.err.println("Warning: Unrecognized tag: " + tag);
			case "JUNK":
				if (din.skipBytes(s) != s)
					throw new EOFException();
			}
			pos += 8 + s;
		}
	}

	static WaveInfo prepareOutputInfo(DataOutput dout, WaveInfo ininf) throws ApplicationException, IOException {
		WaveInfo outinf = new WaveInfo(ininf.channelCount, ininf.sampleRate, 16);
		outinf.sampleCount = ininf.sampleCount;
		int s = outinf.getDataSize();
		IOHelper.writeString(dout, WaveInfo.HEADER_MAGIC, "ISO-8859-1");
		IOHelper.writeInt(dout, 4 + (8 + 16) + 8 + s);
		IOHelper.writeString(dout, WaveInfo.HEADER_TYPE, "ISO-8859-1");
		IOHelper.writeString(dout, "fmt ", "ISO-8859-1");
		IOHelper.writeInt(dout, 16);
		IOHelper.writeShort(dout, outinf.encoding.ID);
		IOHelper.writeShort(dout, outinf.channelCount);
		IOHelper.writeInt(dout, outinf.sampleRate);
		IOHelper.writeInt(dout, outinf.getByteRate());
		IOHelper.writeShort(dout, outinf.getFrameSize());
		IOHelper.writeShort(dout, outinf.sampleDepth);
		IOHelper.writeString(dout, "data", "ISO-8859-1");
		IOHelper.writeInt(dout, s);
		return outinf;
	}

	public static void inflate(File fin, File fout) throws ApplicationException, IOException {
		if (fin.equals(fout))
			throw new ApplicationException(5, "Writing to source file.");
		try (
			InputStream in = new BufferedInputStream(new FileInputStream(fin));
			DataInputStream din = new DataInputStream(in);
		) {
			System.err.println("Preparing header...");
			WaveInfo ininf = prepareInputInfo(din, (int) fin.length());
			WaveInfo outinf;
			try (
				FileOutputStream fsout = new FileOutputStream(fout);
				OutputStream out = new BufferedOutputStream(fsout);
				DataOutputStream dout = new DataOutputStream(out);
				FileLock lck = fsout.getChannel().tryLock();
			) {
				if (lck == null)
					throw new AccessDeniedException("File already in use.");
				outinf = prepareOutputInfo(dout, ininf);
				System.err.println("Decoding data...");
				PtADPCMDecoder.decodeData(ininf, din, dout);
			} catch (FileNotFoundException ex) {
				throw new ApplicationException(2, "Cannot open output file.");
			}
			System.err.println("Inflation complete.");
			System.err.printf("Output length: %.3fs, %dch, %dHz, %dkbps%n",
				outinf.getLength(), outinf.channelCount, outinf.sampleRate, outinf.getByteRate() * 8 / 1000);
		} catch (FileNotFoundException ex) {
			throw new ApplicationException(2, "File does not exist.");
		}
	}

	@Deprecated
	private Inflator() {
		throw new IllegalStateException();
	}

}

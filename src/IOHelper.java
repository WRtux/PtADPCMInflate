import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class IOHelper {

	public static boolean bitOfBytes(byte[] bs, int i) {
		return (bs[i / 8] >>> i % 8 & 1) == 1 ? true : false;
	}

	public static byte readByte(DataInput din) throws IOException {
		return din.readByte();
	}

	public static int readByteU(DataInput din) throws IOException {
		return din.readUnsignedByte();
	}

	public static short readShort(DataInput din) throws IOException {
		return Short.reverseBytes(din.readShort());
	}

	public static int readShortU(DataInput din) throws IOException {
		return Short.toUnsignedInt(Short.reverseBytes(din.readShort()));
	}

	public static int readInt(DataInput din) throws IOException {
		return Integer.reverseBytes(din.readInt());
	}

	//ADV
	public static int readIndex(DataInput din, int s) throws IOException {
		switch (s) {
		case 1: {
			int i = din.readUnsignedByte();
			return i != 0xFF ? i : -1;
		}
		case 2: {
			int i = Short.toUnsignedInt(Short.reverseBytes(din.readShort()));
			return i != 0xFFFF ? i : -1;
		}
		case 4:
			return Integer.reverseBytes(din.readInt());
		default:
			throw new IllegalArgumentException("Invalid index size.");
		}
	}

	public static float readFloat(DataInput din) throws IOException {
		return Float.intBitsToFloat(Integer.reverseBytes(din.readInt()));
	}

	public static byte[] readBytes(DataInput din, int s) throws IOException {
		byte[] bs = new byte[s];
		din.readFully(bs);
		return bs;
	}

	//ADV
	public static int[] readIndices(DataInput din, int s, int cnt) throws IOException {
		int[] is = new int[cnt];
		switch (s) {
		case 1:
			for (int i = 0; i < cnt; i++) {
				int j = din.readUnsignedByte();
				is[i] = (j != 0xFF ? j : -1);
			}
			break;
		case 2:
			for (int i = 0; i < cnt; i++) {
				int j = Short.toUnsignedInt(Short.reverseBytes(din.readShort()));
				is[i] = (j != 0xFFFF ? j : -1);
			}
			break;
		case 4:
			for (int i = 0; i < cnt; i++) {
				is[i] = Integer.reverseBytes(din.readInt());
			}
			break;
		default:
			throw new IllegalArgumentException("Invalid index size.");
		}
		return is;
	}

	public static float[] readVector(DataInput din, int dim) throws IOException {
		float[] v = new float[dim];
		for (int i = 0; i < dim; i++) {
			v[i] = readFloat(din);
		}
		return v;
	}

	public static String readString(DataInput din, String enc, int s) throws IOException {
		return new String(readBytes(din, s), enc);
	}

	public static String readStringIndexed(DataInput din, String enc, int is) throws IOException {
		int s = readIndex(din, is);
		return readString(din, enc, s);
	}
	public static String readStringNormal(DataInput din, String enc) throws IOException {
		return readStringIndexed(din, enc, 4);
	}
	public static String readStringShort(DataInput din, String enc) throws IOException {
		return readStringIndexed(din, enc, 2);
	}

	public static void writeByte(DataOutput dout, int num) throws IOException {
		dout.writeByte(num);
	}

	public static void writeShort(DataOutput dout, int num) throws IOException {
		dout.writeShort(Short.reverseBytes((short) num));
	}

	public static void writeInt(DataOutput dout, int num) throws IOException {
		dout.writeInt(Integer.reverseBytes(num));
	}

	public static void writeFloat(DataOutput dout, float num) throws IOException {
		dout.writeInt(Integer.reverseBytes(Float.floatToRawIntBits(num)));
	}

	public static void writeBytes(DataOutput dout, byte[] bs) throws IOException {
		dout.write(bs);
	}

	public static void writeVector(DataOutput dout, float[] v) throws IOException {
		for (float num : v) {
			writeFloat(dout, num);
		}
	}

	public static int writeString(DataOutput dout, String str, String enc) throws IOException {
		byte[] bs = str.getBytes(enc);
		dout.write(bs);
		return bs.length;
	}

	public static int writeStringNormal(DataOutput dout, String str, String enc) throws IOException {
		byte[] bs = str.getBytes(enc);
		writeInt(dout, bs.length);
		dout.write(bs);
		return bs.length;
	}
	public static int writeStringShort(DataOutput dout, String str, String enc) throws IOException {
		byte[] bs = str.getBytes(enc);
		int s = Math.min(bs.length, 0xFFFF);
		if (bs.length > 0xFFFF)
			System.err.println("Warning: Truncating output to fit short string.");
		writeShort(dout, s);
		dout.write(bs, 0, s);
		return s;
	}

	@Deprecated
	private IOHelper() {
		throw new IllegalStateException();
	}

}

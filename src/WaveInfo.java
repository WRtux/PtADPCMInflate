public final class WaveInfo {

	public static final String HEADER_MAGIC = "RIFF";
	public static final String HEADER_TYPE = "WAVE";

	public static enum Encoding {
		PCM((short) 0x0001), MSADPCM((short) 0x0002, "Microsoft ADPCM"), PTADPCM((short) 0x8311, "Platinum ADPCM"),
		VORBIS((short) 0x6771, "Vorbis"), WWVORBIS((short) 0xFFFF, "Wwise Vorbis");

		public static Encoding get(short id) {
			for (Encoding code : Encoding.values()) {
				if (id == code.ID)
					return code;
			}
			throw new IllegalArgumentException(String.format("Unsupported audio encoding: %04X",
				Short.toUnsignedInt(id)));
		}

		public final short ID;

		public final String name;

		private Encoding(short id, String n) {
			this.ID = id;
			this.name = n;
		}
		private Encoding(short id) {
			this.ID = id;
			this.name = this.name();
		}

	}

	public Encoding encoding;

	public int channelCount;

	public int sampleRate;

	public int sampleDepth;

	public int frameSampleCount;

	public int sampleCount;

	public WaveInfo() {}

	public WaveInfo(Encoding code) {
		this.encoding = code;
	}

	public WaveInfo(Encoding code, int chcnt, int sr, int sd, int fscnt) {
		this(code);
		this.channelCount = chcnt;
		this.sampleRate = sr;
		this.sampleDepth = sd;
		this.frameSampleCount = fscnt;
	}
	public WaveInfo(int chcnt, int sr, int sd) {
		this(Encoding.PCM, chcnt, sr, sd, 1);
	}

	public int getFrameCount() {
		switch (this.encoding) {
		case PCM:
		case MSADPCM:
		case PTADPCM:
			return Math.floorDiv(this.sampleCount - 1, this.frameSampleCount) + 1;
		default:
			throw new UnsupportedOperationException();
		}
	}

	public double getLength() {
		return (double) this.sampleCount / this.sampleRate;
	}

	int getSampleSize() {
		if (this.encoding != Encoding.PCM)
			throw new IllegalStateException();
		return Math.floorDiv(this.sampleDepth - 1, 8) + 1;
	}

	public int getFrameSize() {
		switch (this.encoding) {
		case PCM:
			return this.channelCount * this.frameSampleCount * this.getSampleSize();
		case MSADPCM:
			if (this.channelCount > 2)
				throw new IllegalArgumentException("Too many channels.");
			if (this.sampleDepth != 4 && this.sampleDepth != 16)
				throw new IllegalArgumentException("Invalid sample depth.");
			if (this.frameSampleCount <= 2)
				throw new IllegalArgumentException("Frame sample count too small.");
			return this.channelCount * 7 + (this.channelCount * (this.frameSampleCount - 2) + 1) / 2;
		case PTADPCM:
			if (this.sampleDepth != 4 && this.sampleDepth != 16)
				throw new IllegalArgumentException("Invalid sample depth.");
			if (this.frameSampleCount <= 2)
				throw new IllegalArgumentException("Frame sample count too small.");
			return this.channelCount * (5 + (this.frameSampleCount - 2 + 1) / 2);
		default:
			throw new UnsupportedOperationException();
		}
	}

	public int getDataSize() {
		switch (this.encoding) {
		case PCM:
			return this.channelCount * this.sampleCount * this.getSampleSize();
		case MSADPCM:
		case PTADPCM:
			return this.getFrameCount() * this.getFrameSize();
		default:
			throw new UnsupportedOperationException();
		}
	}

	public int getByteRate() {
		return (int) Math.round(this.getDataSize() / this.getLength());
	}

	public int setLength(double t) {
		this.sampleCount = (int) Math.ceil(t * this.sampleRate);
		return this.sampleCount;
	}

	public int setDataSize(int s) {
		int fs = this.getFrameSize();
		if (s % fs != 0)
			System.err.println("Warning: Data may be malformed.");
		this.sampleCount = (s / fs) * this.frameSampleCount;
		return this.sampleCount;
	}

}

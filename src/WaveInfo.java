public final class WaveInfo {

	public static final String HEADER_MAGIC = "RIFF";
	public static final String HEADER_TYPE = "WAVE";

	public static enum Coding {
		PCM((short) 0x0001), PTADPCM((short) 0x8311);

		public static Coding get(short id) {
			for (Coding code : Coding.values()) {
				if (id == code.ID)
					return code;
			}
			throw new IllegalArgumentException(String.format("Unsupported audio coding: %04X",
				Short.toUnsignedInt(id)));
		}

		public final short ID;

		private Coding(short id) {
			this.ID = id;
		}

	}

	public Coding coding;

	public int channelCount;

	public int sampleRate;

	public int sampleDepth;

	public int frameSampleCount;

	public int sampleCount;

	public WaveInfo() {}

	public WaveInfo(Coding code) {
		this.coding = code;
	}

	public WaveInfo(Coding code, int chcnt, int sr, int sd, int fscnt) {
		this(code);
		this.channelCount = chcnt;
		this.sampleRate = sr;
		this.sampleDepth = sd;
		this.frameSampleCount = fscnt;
	}
	public WaveInfo(int chcnt, int sr, int sd) {
		this(Coding.PCM, chcnt, sr, sd, 1);
	}

	public int getFrameCount() {
		return Math.floorDiv(this.sampleCount - 1, this.frameSampleCount) + 1;
	}

	public double getLength() {
		return (double) this.sampleCount / this.sampleRate;
	}

	int getSampleSize() {
		if (this.coding != Coding.PCM)
			throw new IllegalStateException();
		return Math.floorDiv(this.sampleDepth - 1, 8) + 1;
	}

	public int getFrameSize() {
		switch (this.coding) {
		case PCM:
			return this.channelCount * this.frameSampleCount * this.getSampleSize();
		case PTADPCM:
			if (this.sampleDepth != 4 && this.sampleDepth != 16)
				throw new IllegalArgumentException("Invalid sample depth.");
			if (this.frameSampleCount <= 2)
				throw new IllegalArgumentException("Frame sample count too small.");
			return this.channelCount * (5 + (this.frameSampleCount - 2 + 1) / 2);
		default:
			throw new IllegalStateException();
		}
	}

	public int getDataSize() {
		switch (this.coding) {
		case PCM:
			return this.channelCount * this.sampleCount * this.getSampleSize();
		case PTADPCM:
			return this.getFrameCount() * this.getFrameSize();
		default:
			throw new IllegalStateException();
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

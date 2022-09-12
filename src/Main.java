import java.io.File;

public final class Main {

	public static void main(String[] args) {
		try {
			if (args.length != 2)
				throw new ApplicationException(22, "Wrong arugments.");
			Inflator.inflate(new File(args[0]), new File(args[1]));
		} catch (ApplicationException ex) {
			System.err.println(ex.getMessage());
			System.exit(ex.signal);
		} catch (Exception ex) {
			System.err.println("Uncaught exception:");
			ex.printStackTrace(System.err);
			System.exit(-1);
		}
	}

	@Deprecated
	private Main() {
		throw new IllegalStateException();
	}

}

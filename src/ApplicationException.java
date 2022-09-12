@SuppressWarnings("serial")
public class ApplicationException extends Exception {

	public final int signal;

	public ApplicationException(int sig) {
		super();
		this.signal = sig;
	}

	public ApplicationException(int sig, String msg) {
		super(msg);
		this.signal = sig;
	}

}

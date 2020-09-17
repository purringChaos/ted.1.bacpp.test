package tv.blackarrow.cpp.exeptions;

public class FormatException extends Exception {

	private static final long serialVersionUID = -7203026627848541445L;

	public FormatException() {
		super();
	}

	public FormatException(String message, Throwable cause) {
		super(message, cause);
	}

	public FormatException(String message) {
		super(message);
	}

	public FormatException(Throwable cause) {
		super(cause);
	}

}

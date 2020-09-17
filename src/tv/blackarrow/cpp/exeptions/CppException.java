package tv.blackarrow.cpp.exeptions;

public class CppException extends Exception {

	private static final long serialVersionUID = -7203026627848541444L;

	public CppException() {
		super();
	}

	public CppException(String message, Throwable cause) {
		super(message, cause);
	}

	public CppException(String message) {
		super(message);
	}

	public CppException(Throwable cause) {
		super(cause);
	}

}

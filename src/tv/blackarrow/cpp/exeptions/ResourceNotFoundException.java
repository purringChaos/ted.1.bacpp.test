package tv.blackarrow.cpp.exeptions;

public class ResourceNotFoundException extends Exception {

	private static final long serialVersionUID = -7203026627848541444L;
	private int errorCode = 2; //default
	
	public ResourceNotFoundException() {
		super();
	}

	public ResourceNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResourceNotFoundException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public ResourceNotFoundException(Throwable cause) {
		super(cause);
	}
	
	public int getErrorCode() {
		return this.errorCode;
	}

}

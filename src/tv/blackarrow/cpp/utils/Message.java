/**
 * 
 */
package tv.blackarrow.cpp.utils;


/**
 * @author amit
 *
 */
public class Message {
	
	private static final String EMPTY_STRING = "";
	private String errorMessage = null;
	private String action = null;
	private String message = null;

	public Message() {
	}

	public Message(String errorMessage, String action, String message) {
		super();
		this.errorMessage = errorMessage;
		this.action = action;
		this.message = message;
	}

	/**
	 * @return the errorMessage
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @param errorMessage the errorMessage to set
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage == null ? EMPTY_STRING : errorMessage;
	}

	/**
	 * @return the action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * @param action the action to set
	 */
	public void setAction(String action) {
		this.action = action == null ? EMPTY_STRING : action;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message == null ? EMPTY_STRING : message;
	}
}

package tv.blackarrow.cpp.signaling.ext;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Duration implements java.io.Serializable  {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5524778528308480218L;
	private String value;

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}

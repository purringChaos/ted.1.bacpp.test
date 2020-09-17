package tv.blackarrow.cpp.signaling.ext;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SpliceType implements java.io.Serializable  {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7509487345128606701L;
	private String value;

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	
}

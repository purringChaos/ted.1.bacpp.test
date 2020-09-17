package tv.blackarrow.cpp.model.scte224.asserts;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;

/**
 * 
 * @author nyadav
 *
 */
public class SegmentationUpidValueAssert extends BaseAssert {

	@Expose
	private String text;

	public SegmentationUpidValueAssert() {
		super();
	}
	

	public SegmentationUpidValueAssert(String text) {
		super();
		this.text = text;
	}


	/**
	 * @param assertOperation
	 */
	public SegmentationUpidValueAssert(AssertOperation assertOperation) {
		super(assertOperation);
	}

	@Override
	public boolean isTrue(SegmentationDescriptorType segmentationDescriptorType) {
		byte[] upidBinary = segmentationDescriptorType.getUpid();
		String upidHex = new HexBinaryAdapter().marshal(upidBinary);
		String upidText = Scte35BinaryUtil.hexToString(upidHex);
		String signalIdInUpidHex = ESAMHelper.getSignalIdFromUPIDHexString(upidHex);
		return isTrue(upidText, getText()) || isTrue(signalIdInUpidHex, getText());
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public static SegmentationUpidValueAssert getNewInstance(boolean matchStrValue) {
		if(matchStrValue) {
			return new SegmentationUpidValueAssert();
		} else {
			return new SegmentationUpidValueAssert(AssertOperation.NOTEQUALS);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("SegmentationUpidValueAssert [text=%s]", text);
	}

}

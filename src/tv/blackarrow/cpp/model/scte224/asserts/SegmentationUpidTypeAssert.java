package tv.blackarrow.cpp.model.scte224.asserts;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;

/**
 * 
 * @author nyadav
 *
 */
public class SegmentationUpidTypeAssert extends BaseAssert {
	@Expose
	private Short segmentationUpidType;

	public SegmentationUpidTypeAssert() {
		super();
	}

	public SegmentationUpidTypeAssert(Short segmentationUpidType) {
		super();
		this.segmentationUpidType = segmentationUpidType;
	}

	/**
	 * @param assertOperation
	 */
	public SegmentationUpidTypeAssert(AssertOperation assertOperation) {
		super(assertOperation);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean isTrue(SegmentationDescriptorType segmentationDescriptorType) {
		return isTrue(segmentationDescriptorType.getUpidType(),getSegmentationUpidType());
	}

	public Short getSegmentationUpidType() {
		return segmentationUpidType;
	}

	public void setSegmentationUpidType(Short segmentationUpidType) {
		this.segmentationUpidType = segmentationUpidType;
	}

	public static SegmentationUpidTypeAssert getNewInstance() {
		return new SegmentationUpidTypeAssert();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("SegmentationUpidAssert [segmentationUpidType=%s]", segmentationUpidType);
	}

}

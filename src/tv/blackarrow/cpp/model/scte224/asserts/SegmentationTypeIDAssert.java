package tv.blackarrow.cpp.model.scte224.asserts;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;

/**
 * 
 * @author nyadav
 *
 */
public class SegmentationTypeIDAssert extends BaseAssert {
	@Expose
	private List<Short> segmentationTypeIDs = new ArrayList<Short>();

	public SegmentationTypeIDAssert() {
		super();
	}

	public SegmentationTypeIDAssert(List<Short> segmentationTypeIDs) {
		super();
		this.segmentationTypeIDs = segmentationTypeIDs;
	}

	/**
	 * @param assertOperation
	 */
	public SegmentationTypeIDAssert(AssertOperation assertOperation) {
		super(assertOperation);
	}

	public List<Short> getSegmentatonTypeID() {
		return segmentationTypeIDs;
	}

	public void setSegmentatonTypeID(List<Short> segmentatonTypeIDs) {
		this.segmentationTypeIDs = segmentatonTypeIDs;
	}

	@Override
	public boolean isTrue(SegmentationDescriptorType segmentationDescriptorType) {
		for (Short segmentTypeId : segmentationTypeIDs) {
			if (isTrue(segmentTypeId, segmentationDescriptorType.getSegmentTypeId())) {
				return true;
			}
		}
		return false;
	}

	public static SegmentationTypeIDAssert getNewInstance() {
		return new SegmentationTypeIDAssert();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("SegmentatonTypeIDAssert [segmentatonTypeIDs=%s]", segmentationTypeIDs);
	}
}

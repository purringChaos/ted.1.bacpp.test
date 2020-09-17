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
public class CompositeAssert extends BaseAssert {
	
	public CompositeAssert() {
		super(AssertOperation.AND);
	}

	/**
	 * @param assertOperation
	 */
	public CompositeAssert(AssertOperation assertOperation) {
		super(assertOperation);
	}

	public CompositeAssert(List<Assert> asserts) {
		super();
		this.asserts = asserts;
	}

	@Expose
	private List<Assert> asserts = new ArrayList<Assert>();

	@Override
	public boolean isTrue(SegmentationDescriptorType segmentationDescriptorType) {
		boolean returnFlag = true;
		for (Assert assert1 : asserts) {
			returnFlag = this.assertOperation.execute(returnFlag, assert1.isTrue(segmentationDescriptorType));
		}
		return returnFlag;
	}

	public void addAssert(Assert ast) {
		getAsserts().add(ast);
	}

	public List<Assert> getAsserts() {

		return asserts;
	}

	public void setAsserts(List<Assert> asserts) {
		this.asserts = asserts;
	}

	public static CompositeAssert getNewInstance() {
		return new CompositeAssert();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("CompositeAssert [asserts=%s]", asserts);
	}

}

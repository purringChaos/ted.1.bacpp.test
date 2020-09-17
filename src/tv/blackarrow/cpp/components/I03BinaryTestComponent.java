/**
 * 
 */
package tv.blackarrow.cpp.components;

import org.mule.api.lifecycle.Callable;

import tv.blackarrow.cpp.i03.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SpliceInsertType;

/**
 * @author asharma
 * Meant only for casual testing activities. Not production hardened. A lot of stuff is just copied pasted from old test components. 
 * Go easy on this component while reviewing the code :)
 */
public abstract class I03BinaryTestComponent implements Callable{
	
	protected SCTE35PointDescriptorType convertToI03SCTE35PointDescriptor(tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType scte35PointDescriptor) {
		if(scte35PointDescriptor == null){return null;}
		SCTE35PointDescriptorType t = new SCTE35PointDescriptorType();
		t.setSpliceCommandType(scte35PointDescriptor.getSpliceCommandType());
		for(tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType seg: scte35PointDescriptor.getSegmentationDescriptorInfo()){
			t.getSegmentationDescriptorInfo().add(convertToI03SegmentationDescriptor(seg));
		}
		t.setSpliceInsert(convertToI03SpliceInsert(scte35PointDescriptor.getSpliceInsert()));
		t.getOtherAttributes().putAll(scte35PointDescriptor.getOtherAttributes());
		return t;
	}


	protected tv.blackarrow.cpp.i03.signaling.SpliceInsertType convertToI03SpliceInsert(tv.blackarrow.cpp.signal.signaling.SpliceInsertType spliceInsert) {
		if(spliceInsert == null){return null;}
		tv.blackarrow.cpp.i03.signaling.SpliceInsertType t = new tv.blackarrow.cpp.i03.signaling.SpliceInsertType();
		t.setAvailNum(spliceInsert.getAvailNum());
		t.setAvailsExpected(spliceInsert.getAvailsExpected());
		t.setDuration(spliceInsert.getDuration());
		t.setOutOfNetworkIndicator(spliceInsert.isOutOfNetworkIndicator());
		t.setSpliceEventCancelIndicator(spliceInsert.isSpliceEventCancelIndicator());
		t.setSpliceEventId(spliceInsert.getSpliceEventId());
		t.setUniqueProgramId(spliceInsert.getUniqueProgramId());
		t.getOtherAttributes().putAll(spliceInsert.getOtherAttributes());
		return t;
	}

	protected tv.blackarrow.cpp.i03.signaling.SegmentationDescriptorType convertToI03SegmentationDescriptor(tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType seg) {
		if(seg == null){return null;}
		tv.blackarrow.cpp.i03.signaling.SegmentationDescriptorType s= new tv.blackarrow.cpp.i03.signaling.SegmentationDescriptorType();
		s.setDuration(seg.getDuration());
		s.setSegmentationEventCancelIndicator(seg.isSegmentationEventCancelIndicator());
		s.setSegmentEventId(seg.getSegmentEventId());
		s.setSegmentNum(seg.getSegmentNum());
		s.setSegmentsExpected(seg.getSegmentsExpected());
		s.setSegmentTypeId(seg.getSegmentTypeId());
		s.setUpid(seg.getUpid());
		s.setUpidType(seg.getUpidType());
		s.getOtherAttributes().putAll(seg.getOtherAttributes());
		return s;
	}

	protected tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType convertToI01SCTE35PointDescriptor(tv.blackarrow.cpp.i03.signaling.SCTE35PointDescriptorType scte35PointDescriptor) {
		if(scte35PointDescriptor == null){return null;}
		tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType des = new tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType();
		des.setSpliceCommandType(scte35PointDescriptor.getSpliceCommandType());
		des.setSpliceInsert(convertToI01SpliceInsert(scte35PointDescriptor.getSpliceInsert()));
		
		for(tv.blackarrow.cpp.i03.signaling.SegmentationDescriptorType type:scte35PointDescriptor.getSegmentationDescriptorInfo()){
			des.getSegmentationDescriptorInfo().add(convertToI01SegmentationDescriptorType(type));
		}
		return des;
	}
	
	protected SpliceInsertType convertToI01SpliceInsert(tv.blackarrow.cpp.i03.signaling.SpliceInsertType spliceInsert) {
		if(spliceInsert == null){return null;}
		SpliceInsertType t = new SpliceInsertType();
		t.setAvailNum(spliceInsert.getAvailNum());
		t.setAvailsExpected(spliceInsert.getAvailsExpected());
		t.setDuration(spliceInsert.getDuration());
		t.setOutOfNetworkIndicator(spliceInsert.isOutOfNetworkIndicator());
		t.setSpliceEventCancelIndicator(spliceInsert.isSpliceEventCancelIndicator());
		t.setSpliceEventId(spliceInsert.getSpliceEventId());
		t.setUniqueProgramId(spliceInsert.getUniqueProgramId());
		t.getOtherAttributes().putAll(spliceInsert.getOtherAttributes());
		return t;
	}
	
	protected SegmentationDescriptorType convertToI01SegmentationDescriptorType(tv.blackarrow.cpp.i03.signaling.SegmentationDescriptorType type) {
		if(type == null){return null;}
		SegmentationDescriptorType t = new SegmentationDescriptorType();
		t.setDuration(type.getDuration());
		t.setSegmentationEventCancelIndicator(type.isSegmentationEventCancelIndicator());
		t.setSegmentEventId(type.getSegmentEventId());
		t.setSegmentNum(type.getSegmentNum());
		t.setSegmentsExpected(type.getSegmentsExpected());
		t.setSegmentTypeId(type.getSegmentTypeId());
		t.setUpid(type.getUpid());
		t.setUpidType(type.getUpidType());
		t.getOtherAttributes().putAll(type.getOtherAttributes());
		return t;
	}
}

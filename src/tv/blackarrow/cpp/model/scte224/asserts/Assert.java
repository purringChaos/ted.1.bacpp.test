package tv.blackarrow.cpp.model.scte224.asserts;

import java.util.Objects;

import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
/**
 * 
 * @author nyadav
 *
 */
public interface Assert {
	
	enum AssertOperation{
		EQUALS, 
		NOTEQUALS {
			@Override
			public boolean execute(Object firstObject, Object secondObject) {
				return !Objects.equals(firstObject, secondObject);
			}
		},
		OR {
			@Override
			public boolean execute(Object firstObject, Object secondObject) {
				return Boolean.logicalOr((Boolean)firstObject, (Boolean)secondObject);
			}
		},
		AND {
			@Override
			public boolean execute(Object firstObject, Object secondObject) {
				return Boolean.logicalAnd((Boolean)firstObject, (Boolean)secondObject);
			}
		};
		
		public boolean execute(Object firstObject, Object secondObject) {
			return Objects.equals(firstObject, secondObject);
		}
	}
	
	public boolean isTrue(SegmentationDescriptorType segmentationDescriptorType);
}

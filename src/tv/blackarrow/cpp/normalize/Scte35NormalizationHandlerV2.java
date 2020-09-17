package tv.blackarrow.cpp.normalize;

import tv.blackarrow.cpp.signal.signaling.BinarySignalType;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.SpliceCommandType;

/**
 * Scte35NormalizationHandlerV2 will handle LG Telenet related SCTE 35 signal normalization
 * 
 * @author jwang
 *
 */
public final class Scte35NormalizationHandlerV2 extends AbstractScte35NormalizationHandler {
	private static final Scte35NormalizationHandlerV2 INSTANCE = new Scte35NormalizationHandlerV2();
	
	/**
	 * make it private
	 */
	private Scte35NormalizationHandlerV2() {}
	
	/**
	 * return single instance
	 * @return
	 */
	public static Scte35NormalizationHandlerV2 getInstance() {
		return INSTANCE;
	}

	@Override
	public void updateResponseSignalActionAndBinary(ResponseSignalType responseSignal, final SCTE35PointDescriptorType scte35Pt,
			final StringBuilder pts, final StringBuilder ptsAdjustments, final StringBuilder providerAvailId) {
		if(scte35Pt.getSpliceCommandType() == SpliceCommandType.TIME_SIGNAL.getCommandtype()) {  // normalize time_signal
			responseSignal.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_REPLACE);
			final BinarySignalType binarySignal = generateNormalizedBinary(scte35Pt, pts, ptsAdjustments, providerAvailId);
			responseSignal.setBinaryData(binarySignal);			
		} else { // ignore Invidi splice_insert or other command types
			responseSignal.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP);
			responseSignal.setBinaryData(null);
		}
	
	}
	

}

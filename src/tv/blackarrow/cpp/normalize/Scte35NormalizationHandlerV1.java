package tv.blackarrow.cpp.normalize;

import tv.blackarrow.cpp.signal.signaling.BinarySignalType;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.SpliceCommandType;

/**
 * Scte35NormalizationHandlerV1  will handle VMUK and VMTV related SCTE 35 signal normalization
 *	if incoming SCTE 35 binary is splice_insert command
 *    case 1: splice_out of program, then normalize
 *    case 2: splice_in to program, then delete
 *  if time_signal, then do nothing
 * 
 * @author jwang
 *
 */
public final class Scte35NormalizationHandlerV1 extends AbstractScte35NormalizationHandler {
	private static final Scte35NormalizationHandlerV1 INSTANCE = new Scte35NormalizationHandlerV1();
		
	private Scte35NormalizationHandlerV1() {}
	
	public static Scte35NormalizationHandlerV1 getInstance() {
		return INSTANCE;
	}

	@Override
	public void updateResponseSignalActionAndBinary(ResponseSignalType responseSignal, final SCTE35PointDescriptorType scte35Pt,
			final StringBuilder pts, final StringBuilder ptsAdjustments, final StringBuilder providerAvailId) {
		if(scte35Pt.getSpliceCommandType() == SpliceCommandType.SPLICE_INSERT.getCommandtype() && 
				!scte35Pt.getSpliceInsert().isOutOfNetworkIndicator()) { // for Discovery HD case
			responseSignal.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE);
			responseSignal.setBinaryData(null);
		} else if(scte35Pt.getSpliceCommandType() == SpliceCommandType.TIME_SIGNAL.getCommandtype()) { // ignore time_signal
			responseSignal.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP);
			responseSignal.setBinaryData(null);
		} else { // normalize splice_insert
			final BinarySignalType binarySignal = generateNormalizedBinary(scte35Pt, pts, ptsAdjustments, providerAvailId);
			responseSignal.setBinaryData(binarySignal);
		}
	}	
	
}

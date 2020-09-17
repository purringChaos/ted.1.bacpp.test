package tv.blackarrow.cpp.components.mcc.other.hss;



public class TWCBlackoutHSSManifestResponseDecorator extends TWCHSSManifestResponseDecorator{
	
	public static String SPARSE_TRACK_NAME = "blackout";
	
	@Override
	protected String getSparseTrackName(){
		return SPARSE_TRACK_NAME;
	}
	
	@Override
	protected String getHssStreamTimeAbortActionValue(){
		return HSS_BLACKOUT_SIGNAL_ABORT_STREAM_TIME_VALUE;
	}

}

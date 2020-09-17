//
// Copyright (c) 2012 BlackArrow, Inc. All rights reserved.
//
// The information contained herein is confidential, proprietary to BlackArrow Inc., and
// considered a trade secret as defined in section 499C of the penal code of the State of
// California. Use of this information by anyone other than authorized employees of
// BlackArrow Inc. is granted only under a written non-disclosure agreement, expressly
// prescribing the scope and manner of such use.
//
// $Change$
// $Author$
// $Id$
// $DateTime$
//

package tv.blackarrow.cpp.utils;

import java.util.List;

import javax.xml.namespace.QName;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.signal.signaling.StreamTimeType;

public final class ESAMHelper {

	public static final String UPID_PREFIX = "SIGNAL:";
	private ESAMHelper() {}
	
	public static String generateUpidString(final String signalId) {
		String upid = Scte35BinaryUtil.stringToHex(new StringBuilder().append(UPID_PREFIX).append(signalId).toString());
		return upid;
	}
	
	public static String getSignalIdFromUPIDHexString(final String upidHexString) {
		String signalId = Scte35BinaryUtil.hexToString(upidHexString);
		if (signalId != null && signalId.startsWith(UPID_PREFIX)) {
			signalId = signalId.substring(UPID_PREFIX.length());
		}
		return signalId;
	}
	
	/**
	 * 
	 * @param streamTimes
	 * @return
	 */
	public static boolean isOnlyHLSStreamType(List<StreamTimeType> streamTimes) {
		for (StreamTimeType streamTime : streamTimes) {
			String streamType = streamTime.getTimeType();
			if (streamType == null) { // Sometime JAXB unmarshals stream times
										// into the other attributes.
				streamType = streamTime.getOtherAttributes().get(
						QName.valueOf("TimeType"));
			}
			if (streamType 
					.equalsIgnoreCase(CppConstants.SIGNAL_STREAM_TYPE_HSS)
					) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean isOpenEndedBlackoutOverlapping(DataManager dataManager, String acquisitionPointIdentity) {
		SignalProcessorCursor cursor = dataManager.getSignalProcessorCursor(acquisitionPointIdentity);
		ConfirmedPlacementOpportunity lastConfirmedBlackout = dataManager.getConfirmedBlackoutForGivenAP(acquisitionPointIdentity, cursor.getLastConfirmedBlackoutSignalId());
		if(lastConfirmedBlackout != null && !lastConfirmedBlackout.isProgramEnded()){
			return true;
		} else {
			return false;
		}
	}
}

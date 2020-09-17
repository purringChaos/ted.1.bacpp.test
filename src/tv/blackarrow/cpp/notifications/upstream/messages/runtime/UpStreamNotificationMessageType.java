/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.messages.runtime;

import org.apache.commons.lang.StringUtils;

import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.utils.AlternateContentVersion;
import tv.blackarrow.cpp.utils.CppConstants;

/**
 * @author 
 *
 */
public enum UpStreamNotificationMessageType {
	I02_IP_Inband, I02_IP_OOB, I02_QAM_Inband, I02_QAM_OOB, I02_OpendEnded_IP_Inband, I02_OpendEnded_QAM_Inband, SCTE224_IP_Manifest_Level, SCTE224_QAM, SCTE224_IP_EncoderLevel, SCTE224_IP_ENCODER_LEVEL_INBAND, OLDER_MESSAGE_FROM_9_1_RELEASE;

	public static UpStreamNotificationMessageType getNotificationMessageType(AcquisitionPoint aqpt) {
		UpStreamNotificationMessageType messgaeType = null;
		
		//Find I02 Notification Message Type
		if (AlternateContentVersion.ESNI_I02.equals(aqpt.getFeedsAlternateContentVersion())) {
			boolean case1_I02_IP_Inband = !aqpt.isFeedAllowsOpenEndedBlackouts() && aqpt.isIpAcquisitionPoint() && aqpt.isInBand();
			boolean case2_I02_IP_OOB = !aqpt.isFeedAllowsOpenEndedBlackouts() && aqpt.isIpAcquisitionPoint() && aqpt.isOutBand() && !aqpt.isFeedAllowsOpenEndedBlackouts();

			boolean case3_I02_QAM_Inband = !aqpt.isFeedAllowsOpenEndedBlackouts() && aqpt.isQAMAcquisitionPoint() && aqpt.isInBand();
			boolean case4_I02_QAM_OOB = !aqpt.isFeedAllowsOpenEndedBlackouts() && aqpt.isQAMAcquisitionPoint() && aqpt.isOutBand() && !aqpt.isFeedAllowsOpenEndedBlackouts();

			boolean case5_I02_OpendEnded_IP_Inband = aqpt.isFeedAllowsOpenEndedBlackouts() && aqpt.isIpAcquisitionPoint() && aqpt.isInBand();
			boolean case6_I02_OpendEnded_QAM_Inband = aqpt.isFeedAllowsOpenEndedBlackouts() && aqpt.isQAMAcquisitionPoint() && aqpt.isInBand();

			if (case1_I02_IP_Inband) {
				messgaeType = I02_IP_Inband;
			} else if (case2_I02_IP_OOB) {
				messgaeType = I02_IP_OOB;
			} else if (case3_I02_QAM_Inband) {
				messgaeType = I02_QAM_Inband;
			} else if (case4_I02_QAM_OOB) {
				messgaeType = I02_QAM_OOB;
			} else if (case5_I02_OpendEnded_IP_Inband) {
				messgaeType = I02_OpendEnded_IP_Inband;
			} else if (case6_I02_OpendEnded_QAM_Inband) {
				messgaeType = I02_OpendEnded_QAM_Inband;
			}

		}//Find SCTE225 Notification Message Type
		else if (AlternateContentVersion.ESNI_224.equals(aqpt.getFeedsAlternateContentVersion())) {
			switch (aqpt.getExecutionType()) {
			case ENCODER_LEVEL: 
				 if(aqpt.isIpAcquisitionPoint() && StringUtils.isNotBlank(aqpt.getZoneIdentity())){
					 messgaeType = SCTE224_IP_EncoderLevel; 
				 }
				 break;
			case MANIFEST_LEVEL :
				if(aqpt.isIpAcquisitionPoint() && (StringUtils.isNotBlank(aqpt.getZoneIdentity()) && aqpt.getZoneIdentity().equals(CppConstants.CADENT_OOH_ZONE))) {
					messgaeType = SCTE224_IP_Manifest_Level;
				}
				break;
			default:
				break;
			}
			if (aqpt.isQAMAcquisitionPoint()) {
				messgaeType = SCTE224_QAM;
			}
		}

		return messgaeType;

	}
}

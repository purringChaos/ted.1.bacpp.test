/**
 * 
 */
package tv.blackarrow.cpp.mcctemplate.macros;

import static tv.blackarrow.cpp.mcctemplate.MCCTemplateCompiledConfiguration.MACRO_VALUE_PATTERN;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateCompiledConfiguration;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateCompiledConfiguration.MCCTemplateMacro;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateConstants;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.SegmentationDescriptor;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.UPIDType;

/**
 * @author shwetanks
 *
 */
public class BaseMCCTemplateMacroResolver {

	private final static Logger LOGGER = LogManager.getLogger(BaseMCCTemplateMacroResolver.class);
	public static final String EMPTY_STRING = "";
	protected final ManifestResponseType response;
	protected final AcquiredSignal signal;
	protected final AcquisitionPoint acquisitionPoint;

	protected final ManifestInfo info;
	protected final Map<String, String> textReplacements;
	protected final MCCTemplateCompiledConfiguration mccTemplateCompiledConfiguration = MCCTemplateCompiledConfiguration.getInstance();
	protected static final String DURATION_FORMAT = "#0.000";
	protected SegmentationDescriptorType segmentationDescriptorType = null;

	public BaseMCCTemplateMacroResolver(final ManifestResponseType response, final AcquiredSignal signal, 
			final ManifestInfo info, final AcquisitionPoint acquisitionPoint) {
		super();
		this.response = response;
		this.signal = signal;
		this.acquisitionPoint = acquisitionPoint;
		this.info = info;
		this.textReplacements = new HashMap<String, String>();
		SCTE35PointDescriptorType scte35PointDescriptor = signal.getSCTE35PointDescriptor();
		List<SegmentationDescriptorType> segmentationDescriptorInfo = scte35PointDescriptor.getSegmentationDescriptorInfo();
		if (segmentationDescriptorInfo != null) {
			segmentationDescriptorType = segmentationDescriptorInfo.get(0);
		}
	}

	protected void buildReplacements(boolean canSetDuration) {
		Map<String, MCCTemplateMacro> placeholderKeyVsMacroUsedInItsValue = mccTemplateCompiledConfiguration.getTextReplacementKeyVsMacroUsedInValue();
		for (String placeholderKeyText : placeholderKeyVsMacroUsedInItsValue.keySet()) {
			String valueWithMacroValuePlaceholder = mccTemplateCompiledConfiguration.getTextReplacementKeyVsValueWithPlaceholder().get(placeholderKeyText);
			MCCTemplateMacro macroUsedInPlaceholderValue = placeholderKeyVsMacroUsedInItsValue.get(placeholderKeyText);
			if (macroUsedInPlaceholderValue != null) {
				String resolvedMacroValue = String.valueOf(resolveMacro(macroUsedInPlaceholderValue.getMacroValue(), response, signal, info));
				if (macroUsedInPlaceholderValue == MCCTemplateMacro.SCTE35_SEGMENTATION_DURATION && !canSetDuration) {
					resolvedMacroValue = "";
				}
				String finalValue = MACRO_VALUE_PATTERN.matcher(valueWithMacroValuePlaceholder).replaceAll(Matcher.quoteReplacement(resolvedMacroValue));
				textReplacements.put(placeholderKeyText, finalValue);
			}
		}
	}

	protected void buildReplacements() {
		Map<String, MCCTemplateMacro> placeholderKeyVsMacroUsedInItsValue = mccTemplateCompiledConfiguration.getTextReplacementKeyVsMacroUsedInValue();
		for (String placeholderKeyText : placeholderKeyVsMacroUsedInItsValue.keySet()) {
			String valueWithMacroValuePlaceholder = mccTemplateCompiledConfiguration.getTextReplacementKeyVsValueWithPlaceholder().get(placeholderKeyText);
			MCCTemplateMacro macroUsedInPlaceholderValue = placeholderKeyVsMacroUsedInItsValue.get(placeholderKeyText);
			if (macroUsedInPlaceholderValue != null) {
				if (macroUsedInPlaceholderValue == MCCTemplateMacro.SCTE35_SEGMENTATION_DURATION) {
					textReplacements.put(placeholderKeyText, "");
					continue;
				}
				String resolvedMacroValue = String.valueOf(resolveMacro(macroUsedInPlaceholderValue.getMacroValue(), response, signal, info));
				String finalValue = MACRO_VALUE_PATTERN.matcher(valueWithMacroValuePlaceholder).replaceAll(Matcher.quoteReplacement(resolvedMacroValue));
				textReplacements.put(placeholderKeyText, finalValue);
			}
		}
	}
	
	public String replaceTextPlaceholdersWithActualValues(String tagsInnerText) {
		for (Map.Entry<String, String> entry : textReplacements.entrySet()) {
			tagsInnerText = mccTemplateCompiledConfiguration.getTextReplacementKeyVsCompiledPattern().get(entry.getKey()).matcher(tagsInnerText).replaceAll(entry.getValue());
		}
		return tagsInnerText;
	}

	private Object resolveMacro(final String macroTemplate, final ManifestResponseType response, final AcquiredSignal signal, final ManifestInfo info) {

		Object macroValue = EMPTY_STRING;
		MCCTemplateMacro macroEnum = MCCTemplateMacro.fromString(macroTemplate);
		SegmentationDescriptorType segmentationDescriptorType = null;
		String networkId = info.getNetwork();
		String feedId = info.getFeed();

		SCTE35PointDescriptorType scte35PointDescriptor = signal.getSCTE35PointDescriptor();
		List<SegmentationDescriptorType> segmentationDescriptorInfo = scte35PointDescriptor.getSegmentationDescriptorInfo();
		if (segmentationDescriptorInfo != null) {
			segmentationDescriptorType = segmentationDescriptorInfo.get(0);
		}
		switch (macroEnum) {
		case SCTE35_FEED_PROVIDER_ID_FEED_ID:
			StringBuilder value = new StringBuilder();
			if (networkId != null) {
				value.append(networkId);
			} else {
				LOGGER.warn("We have not received value for (Network) " + MCCTemplateMacro.SCTE35_FEED_PROVIDER_ID_FEED_ID.getMacroValue());
			}
			value.append("/");
			if (feedId != null) {
				value.append(feedId);
			} else {
				LOGGER.warn("We have not received value for (Feed) " + MCCTemplateMacro.SCTE35_FEED_PROVIDER_ID_FEED_ID.getMacroValue());
			}
			macroValue = value;
			break;
		case SCTE35_SPLICE_COMMAND_TYPE:
			if (scte35PointDescriptor != null && scte35PointDescriptor.getSpliceCommandType() != 0L) {
				macroValue = MCCTemplateConstants.SPLICE_TYPES.get(String.valueOf(scte35PointDescriptor.getSpliceCommandType()));
				if (macroValue == null) {
					throw new RuntimeException("unsupport segment type id " + segmentationDescriptorType.getSegmentTypeId());
				}
			} else {
				//@TODO set format  
				LOGGER.warn("We have not received value for " + MCCTemplateMacro.SCTE35_SPLICE_COMMAND_TYPE.getMacroValue());
			}
			break;
		case SCTE35_SEGMENTATION_EVENT_ID:

			if (segmentationDescriptorInfo != null && segmentationDescriptorType != null && segmentationDescriptorType.getSegmentEventId() != null) {
				macroValue = segmentationDescriptorType.getSegmentEventId();
			} else {
				//@TODO set format 
				LOGGER.warn("We have not received value for " + MCCTemplateMacro.SCTE35_SEGMENTATION_EVENT_ID.getMacroValue());
			}
			break;
		case SCTE35_SEGMENTATION_TYPE_ID:
			if (segmentationDescriptorInfo != null && segmentationDescriptorType != null && segmentationDescriptorType.getSegmentTypeId() != null) {
				macroValue = segmentationDescriptorType.getSegmentTypeId();
			} else {
				//@TODO set format  
				LOGGER.warn("We have not received value for " + MCCTemplateMacro.SCTE35_SEGMENTATION_TYPE_ID.getMacroValue());
			}
			break;
		case SCTE35_WEB_DELIVERY_ALLOWED_FLAG:
			if (segmentationDescriptorInfo != null && segmentationDescriptorType != null) {
				Object flag = segmentationDescriptorType.getOtherAttributes().get(new QName(CppConstants.WEB_DELIVERY_ALLOW_FLAG));
				if (flag != null) {
					macroValue = flag;
				} else {
					LOGGER.warn("We have not received value for " + MCCTemplateMacro.SCTE35_WEB_DELIVERY_ALLOWED_FLAG.getMacroValue());
				}
			}
			break;
		case SCTE35_NO_REGIONAL_BLACKOUT_FLAG:
			if (segmentationDescriptorInfo != null && segmentationDescriptorType != null) {
				Object flag = segmentationDescriptorType.getOtherAttributes().get(new QName(CppConstants.NO_REGIONAL_BLACKOUT_FLAG));
				if (flag != null) {
					macroValue = flag;
				} else {
					LOGGER.warn("We have not received value for " + MCCTemplateMacro.SCTE35_NO_REGIONAL_BLACKOUT_FLAG.getMacroValue());
				}
			}
			break;
		case SCTE35_ARCHIVE_ALLOWED_FLAG:
			if (segmentationDescriptorInfo != null && segmentationDescriptorType != null) {
				Object flag = segmentationDescriptorType.getOtherAttributes().get(new QName(CppConstants.ARCHIVE_ALLOWED_FLAG));
				if (flag != null) {
					macroValue = flag;
				} else {
					LOGGER.warn("We have not received value for " + MCCTemplateMacro.SCTE35_ARCHIVE_ALLOWED_FLAG.getMacroValue());
				}
			}
			break;
		case SCTE35_DEVICE_RESTRICTIONS:
			if (segmentationDescriptorInfo != null && segmentationDescriptorType != null) {
				Object flag = segmentationDescriptorType.getOtherAttributes().get(new QName(CppConstants.DEVICE_RESTRICTIONS));
				if (flag != null) {
					macroValue = flag;
				} else {
					LOGGER.warn("We have not received value for " + MCCTemplateMacro.SCTE35_DEVICE_RESTRICTIONS.getMacroValue());
				}
			}
			break;
		case SCTE35_SEGMENTATION_DURATION:
			macroValue = calculateDurationInSeconds(info, response.getDuration());
			break;
		case SCTE35_SEGMENTATION_UPID:
			macroValue = deriveSegmentationUpid(info, macroValue, segmentationDescriptorType, segmentationDescriptorInfo);
			break;
		case SCTE35_SEGMENTATION_UPID_TYPE:
			SegmentationDescriptor segmentDescriptor = null;
			if (info != null && info.getSignalId() != null) {
				segmentDescriptor = getSegmentationDescriptor(info.getSignalId());
			}
			if (info != null && info.getSignalId() != null) {
				if (segmentDescriptor != null) {
					macroValue = segmentDescriptor.getUpidType();
				} else {
					if (segmentationDescriptorInfo != null && segmentationDescriptorType != null) {
						macroValue = segmentationDescriptorType.getUpidType();
					}
				}
			} else {
				LOGGER.warn("We have not received value for " + MCCTemplateMacro.SCTE35_SEGMENTATION_UPID_TYPE.getMacroValue());
			}
			break;
		case SCTE35_SEGMENTATION_SIGNAL_ID:
			macroValue = getSignalId(info, macroValue, segmentationDescriptorType, segmentationDescriptorInfo);
			break;
		case SCTE35_FEED_FEED_ID:
			if (feedId != null) {
				macroValue = feedId;
			} else {
				LOGGER.warn("We have not received value for " + MCCTemplateMacro.SCTE35_FEED_FEED_ID.getMacroValue());
			}
			break;
		case SCTE35_FEED_PROVIDER_ID:
			if (networkId != null) {
				macroValue = networkId;
			} else {
				LOGGER.warn("We have not received value for " + MCCTemplateMacro.SCTE35_FEED_PROVIDER_ID.getMacroValue());
			}
			break;
		case SCTE35_UTC_POINT:
			if (info != null && info.getUtc() != null) {
				macroValue = info.getUtc();
			} else {
				LOGGER.warn("We have not received value for " + MCCTemplateMacro.SCTE35_UTC_POINT.getMacroValue());
			}
			break;
		case SCTE35_ACQUISITION_POINT_IDENTITY:
			if (signal != null && signal.getAcquisitionPointIdentity() != null) {
				macroValue = signal.getAcquisitionPointIdentity();
			} else {
				LOGGER.warn("We have not received value for " + MCCTemplateMacro.SCTE35_ACQUISITION_POINT_IDENTITY.getMacroValue());
			}
			break;
		case SCTE35_SEGMENTATION_NUM:
			macroValue = getSegmentationNum(info);
			break;
		case SCTE35_SEGMENTATION_COUNT:
			macroValue = getSegmentationCount(info);
			break;	
		case SCTE35_SEGMENTATION_UPID_PROGRAM_ID:
			macroValue = deriveSegmentationUpidProgramId(info, macroValue, segmentationDescriptorType, segmentationDescriptorInfo);
			break;	
		default:
			break;//Do Nothing.
		}

		return macroValue;
	}

	protected Object getSignalId(final ManifestInfo info, Object macroValue, SegmentationDescriptorType segmentationDescriptorType,
			List<SegmentationDescriptorType> segmentationDescriptorInfo) {
		if (info != null && info.getSignalId() != null) {
			macroValue = info.getSignalId();
		} else if (segmentationDescriptorInfo != null && segmentationDescriptorType != null
				&& UPIDType.CABLELAB_ADI == UPIDType.valueOf(segmentationDescriptorType.getUpidType())) {
			macroValue = ESAMHelper.getSignalIdFromUPIDHexString(new HexBinaryAdapter().marshal(segmentationDescriptorType.getUpid()));
		} else {
			LOGGER.warn("We have not received value for " + MCCTemplateMacro.SCTE35_SEGMENTATION_SIGNAL_ID.getMacroValue());
		}
		return macroValue;
	}

	protected Object deriveSegmentationUpidProgramId(final ManifestInfo info, Object macroValue, SegmentationDescriptorType segmentationDescriptorType,
			List<SegmentationDescriptorType> segmentationDescriptorInfo) {
		return EMPTY_STRING;
	}

	protected Object getSegmentationCount(final ManifestInfo info) {
		return EMPTY_STRING;
	}
	
	protected Object getSegmentationNum(final ManifestInfo info) {
		return EMPTY_STRING;
	}

	protected Object deriveSegmentationUpid(final ManifestInfo info, Object macroValue, SegmentationDescriptorType segmentationDescriptorType,
			List<SegmentationDescriptorType> segmentationDescriptorInfo) {
		SegmentationDescriptor descriptor = null;
		if (info != null && info.getSignalId() != null) {
			descriptor = getSegmentationDescriptor(info.getSignalId());
		}
		if (descriptor != null) {
			macroValue = descriptor.getSegmentationUpid();
		} else {
			if (segmentationDescriptorInfo != null && segmentationDescriptorType != null) {
				byte[] upidBinary = segmentationDescriptorType.getUpid();
				macroValue = new HexBinaryAdapter().marshal(upidBinary);
			} else {
				LOGGER.warn("We have not received value for " + MCCTemplateMacro.SCTE35_SEGMENTATION_UPID.getMacroValue());
			}
		}
		return macroValue;
	}

	protected Object calculateDurationInSeconds(ManifestInfo info, Duration duration) {
		Object macroValue;
		if (duration != null) {
			macroValue = (convertDurationInMilliSeconds(duration)) / 1000;
			DecimalFormat df = new DecimalFormat(DURATION_FORMAT);
			macroValue = df.format(macroValue);
		} else {
			macroValue = 0;
			DecimalFormat df = new DecimalFormat(DURATION_FORMAT);
			macroValue = df.format(macroValue);
			LOGGER.warn("We have not received value for " + MCCTemplateMacro.SCTE35_SEGMENTATION_DURATION.getMacroValue());
		}
		return macroValue;
	}

	/**
	 * This method is used to calculate the xml.duration value in seconds
	 * 
	 * @param duration
	 *            java.xml.datatype.duration
	 * @return the length in seconds
	 */
	protected Long convertDurationInMilliSeconds(Duration duration) {
		Date baseDate = Calendar.getInstance().getTime();
		long baseTimeInMillis = baseDate.getTime();
		duration.addTo(baseDate);
		long baseTimePlusDurationInMillis = baseDate.getTime();
		return Long.valueOf(baseTimePlusDurationInMillis - baseTimeInMillis);
	}

	/**
	 * This method is used to get the SegmentationDescriptor from couchbase to get the UPID and UPIDType                             
	 * @param signalId
	 * @return SegmentationDescriptor
	 */
	protected SegmentationDescriptor getSegmentationDescriptor(String signalId) {
		DataManager dataManager = DataManagerFactory.getInstance();
		return dataManager.getSegmentationDescriptor(signalId);
	}

}

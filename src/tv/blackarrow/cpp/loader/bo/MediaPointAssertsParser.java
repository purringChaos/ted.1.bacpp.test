package tv.blackarrow.cpp.loader.bo;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.model.scte224.MediaPoint;
import tv.blackarrow.cpp.model.scte224.asserts.Assert;
import tv.blackarrow.cpp.model.scte224.asserts.CompositeAssert;
import tv.blackarrow.cpp.model.scte224.asserts.SegmentationTypeIDAssert;
import tv.blackarrow.cpp.model.scte224.asserts.SegmentationUpidTypeAssert;
import tv.blackarrow.cpp.model.scte224.asserts.SegmentationUpidValueAssert;

public class MediaPointAssertsParser {
	private static final Logger LOGGER = LogManager.getLogger(MediaPointAssertsParser.class);

	private static final String STR_CLOSE_SQUARE_BRAKET = "]";
	private static final String STR_SEGMENTATION_TYPE_ID = "@segmentationTypeId=";
	private static final String STR_SEGMENTATION_UPID_TYPE = "@segmentationUpidType=";
	private static final String STR_TEXT = "contains(text()";
	private static final String TEXT_I = "text()='";
	private static final String TEXT_II = "@text='";
	private static final String STR_NOT_TEXT = "!contains(text()";

	private MediaPointAssertsParser() {
	}

	public static void parse(MediaPoint mediaPoint) {

		if (Objects.isNull(mediaPoint.getMatchSignal())) {
			LOGGER.warn("NO match signal available for the mediapoint : " + mediaPoint.getSignalId());
			return;
		}
		List<Assert> assertList = mediaPoint.getMatchSignal().getParsedAsserts();
		String segmentatonTypeID = null;
		String segmentationUpid = null;
		String text = null;

		Set<Short> collectSegmentationTypeIds = new HashSet();
		for (String assertStr : mediaPoint.getMatchSignal().getAsserts()) {
			if (assertStr.contains(STR_TEXT)) {
				CompositeAssert assert1 = CompositeAssert.getNewInstance();
				assertList.add(assert1);

				if (assertStr.indexOf(STR_SEGMENTATION_TYPE_ID) != -1) {
					segmentatonTypeID = assertStr.substring(assertStr.indexOf(STR_SEGMENTATION_TYPE_ID) + STR_SEGMENTATION_TYPE_ID.length(),
							assertStr.indexOf(STR_CLOSE_SQUARE_BRAKET, assertStr.indexOf(STR_SEGMENTATION_TYPE_ID)));

					SegmentationTypeIDAssert idAssert = SegmentationTypeIDAssert.getNewInstance();
					assert1.addAssert(idAssert);
					idAssert.getSegmentatonTypeID().add(Short.valueOf(segmentatonTypeID));

				}
				if (assertStr.indexOf(STR_SEGMENTATION_UPID_TYPE) != -1) {
					segmentationUpid = assertStr.substring(assertStr.indexOf(STR_SEGMENTATION_UPID_TYPE) + STR_SEGMENTATION_UPID_TYPE.length(),
							assertStr.indexOf(" and ", assertStr.indexOf(STR_SEGMENTATION_UPID_TYPE)));
					SegmentationUpidTypeAssert upidAssert = SegmentationUpidTypeAssert.getNewInstance();
					assert1.addAssert(upidAssert);
					upidAssert.setSegmentationUpidType(Short.valueOf(segmentationUpid));

				}

				if (assertStr.indexOf(STR_TEXT) != -1) {
					int indexOfCommaAfterText = assertStr.indexOf(',', assertStr.indexOf(STR_TEXT));
					text = assertStr.substring(indexOfCommaAfterText + 1, assertStr.indexOf(')', indexOfCommaAfterText)).replaceAll("'", "");
					SegmentationUpidValueAssert textAssert = SegmentationUpidValueAssert.getNewInstance(assertStr.indexOf(STR_NOT_TEXT) == -1);
					assert1.addAssert(textAssert);
					textAssert.setText(text);
				}
			} else {
				// To remove white spaces
				assertStr = assertStr.replaceAll("\\s", "");
				if (assertStr.indexOf(STR_SEGMENTATION_TYPE_ID) != -1) {
					SegmentationTypeIDAssert idAssert = SegmentationTypeIDAssert.getNewInstance();
					assertList.add(idAssert);
					segmentatonTypeID = assertStr.substring(assertStr.indexOf(STR_SEGMENTATION_TYPE_ID) + STR_SEGMENTATION_TYPE_ID.length(),
							assertStr.indexOf(STR_CLOSE_SQUARE_BRAKET, assertStr.indexOf(STR_SEGMENTATION_TYPE_ID)));
					if (segmentatonTypeID.contains("or")) {

						String segmentatonTypeIDFirst = segmentatonTypeID.substring(0, 2);
						String segmentatonTypeIDSecond = segmentatonTypeID.substring(segmentatonTypeID.length() - 2);

						idAssert.getSegmentatonTypeID().add(Short.valueOf(segmentatonTypeIDFirst));
						idAssert.getSegmentatonTypeID().add(Short.valueOf(segmentatonTypeIDSecond));

					} else {
						idAssert.getSegmentatonTypeID().add(Short.valueOf(segmentatonTypeID));

					}

				} else if (assertStr.indexOf(STR_SEGMENTATION_UPID_TYPE) != -1) {
					SegmentationUpidTypeAssert upidAssert = SegmentationUpidTypeAssert.getNewInstance();
					assertList.add(upidAssert);
					segmentationUpid = assertStr.substring(assertStr.indexOf(STR_SEGMENTATION_UPID_TYPE) + STR_SEGMENTATION_UPID_TYPE.length(),
							assertStr.indexOf(STR_CLOSE_SQUARE_BRAKET, assertStr.indexOf(STR_SEGMENTATION_UPID_TYPE)));
					upidAssert.setSegmentationUpidType(Short.valueOf(segmentationUpid));
				} else {
					SegmentationUpidValueAssert textAssert = SegmentationUpidValueAssert.getNewInstance(true);
					assertList.add(textAssert);
					// To supports text()=
					if (assertStr.indexOf(TEXT_I) != -1) {
						text = assertStr.substring(assertStr.indexOf(TEXT_I) + TEXT_I.length(), assertStr.indexOf("']", assertStr.indexOf(TEXT_I)));
						textAssert.setText(text);
						// To supports @text=
					} else if (assertStr.indexOf(TEXT_II) != -1) {
						text = assertStr.substring(assertStr.indexOf(TEXT_II) + TEXT_II.length(), assertStr.indexOf("']", assertStr.indexOf(TEXT_II)));
						textAssert.setText(text);
					}
				}
			}

			if (StringUtils.isNotBlank(segmentatonTypeID) && StringUtils.isNumeric(segmentatonTypeID) && Short.valueOf(segmentatonTypeID).intValue() > 0) {
				//Populating segment Type ID in the high level to decide, whether to confirm or end program
				collectSegmentationTypeIds.add(Short.valueOf(segmentatonTypeID));
			}

		}

		mediaPoint.getMatchSignal().getSegmentationTypeIds().addAll(collectSegmentationTypeIds);
	}
}

/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.messages.queue.scte224.scheduling;

import java.io.StringReader;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.codec.binary.Base64;

import tv.blackarrow.cpp.handler.I03RequestHandler;
import tv.blackarrow.cpp.i03.signaling.BinarySignalType;
import tv.blackarrow.cpp.i03.signaling.ConditioningInfoType;
import tv.blackarrow.cpp.i03.signaling.EventScheduleType;
import tv.blackarrow.cpp.i03.signaling.ResponseSignalType;
import tv.blackarrow.cpp.i03.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.i03.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.i03.signaling.SpliceInsertType;
import tv.blackarrow.cpp.i03.signaling.UTCPointDescriptorType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.SpliceCommandType;
import tv.blackarrow.cpp.utils.UPIDType;

/**
 * @author Amit Kumar Sharma
 *
 */
public class MediaNotificationBuilder {
	
	
}

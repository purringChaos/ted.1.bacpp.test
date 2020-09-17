package tv.blackarrow.cpp.components;

import java.io.StringReader;
import java.util.Date;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEventContext;
import org.mule.api.annotations.param.Payload;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.setting.AcquisitionConfigBean;
import tv.blackarrow.cpp.signal.signaling.AcquisitionPointInfoType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.CppUtil;
import tv.blackarrow.cpp.utils.JAXBUtil;

/**
 * 
 * use Signaling interface
 *
 */
public class APRequestComponent {
	private static final Logger LOGGER = LogManager.getLogger(APRequestComponent.class);

	public String intercept(@Payload final String content) {
		LOGGER.debug(()->content);

		AcquisitionPointInfoType request = null;

		try {
			JAXBContext context = JAXBUtil.getLinearPOISSccJAXBContext();//JAXBContext.newInstance(AcquisitionPointInfoType.class.getPackage().getName());
			Unmarshaller unmarshaller	= context.createUnmarshaller();
			StringReader reader = new StringReader(content);
			//		PlacementRequestType request = (PlacementRequestType) unmarshaller.unmarshal(reader);

			JAXBElement jaxbObj = (JAXBElement)unmarshaller.unmarshal(reader);
			if(jaxbObj.getValue() instanceof AcquisitionPointInfoType) {
				request = (AcquisitionPointInfoType) jaxbObj.getValue();
				
				Date npt = request.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime();
				LOGGER.debug(()->"time -- " + npt);

				MuleEventContext ctx = RequestContext.getEventContext();
				
				Map<String, String> mapping = AcquisitionConfigBean.getInstance().getAcquisitionFeedMap();
				String feedId = mapping.get(request.getAcquisitionSignalID());
				if(feedId == null) {
					feedId = "NON_EXIST";
				}
				
				ctx.getMessage().setProperty(CppConstants.FEED_ID, feedId, PropertyScope.OUTBOUND);		
				ctx.getMessage().setProperty(CppConstants.EVENT_TIME, npt.toString(), PropertyScope.OUTBOUND);		
				ctx.getMessage().setProperty(CppConstants.FEED_ID_KEY, 
						CppUtil.getInstance().generateFeedIdKey(feedId, npt.toString()), PropertyScope.OUTBOUND);		
			} else {
				throw new RuntimeException("Wrong request data type.");
			}
			
			LOGGER.debug("AP identity in request -- " + request.getAcquisitionPointIdentity());
		}  catch(Exception ex) {
			LOGGER.error(()->ex.getMessage());
		} 

		return "";
	}	
}

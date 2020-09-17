package tv.blackarrow.cpp.components;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import tv.blackarrow.cpp.po.PoSchedule;
import tv.blackarrow.cpp.utils.CppUtil;

public class StringToScheduleComponent implements Callable {
//	private static final Logger LOG = LogManager.getLogger(StringToScheduleComponent.class);
	
	@Override
	public Object onCall(MuleEventContext context) throws Exception {
		String message =  context.getMessageAsString();

		return (PoSchedule)CppUtil.getInstance().convertStringToObject(message, PoSchedule.class);	
	}
	
	

}

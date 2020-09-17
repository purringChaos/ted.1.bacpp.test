package tv.blackarrow.cpp.components;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import tv.blackarrow.cpp.pretrigger.jobs.BisQueryTimeTask;

public class PretriggerMonitorComponent implements Callable{

  @Override
  public Object onCall(MuleEventContext context) throws Exception {
    // TODO Auto-generated method stub
    return BisQueryTimeTask.MONITOR_BEAN.toString();
  }

}

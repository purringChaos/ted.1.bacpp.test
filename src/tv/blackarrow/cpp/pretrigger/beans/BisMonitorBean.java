package tv.blackarrow.cpp.pretrigger.beans;

public class BisMonitorBean {
    long lastAttemptCallToBis;
    long lastSuccessCallToBis;
    int noOfRecordsFromBis;
    int noOfRecordsScheduled;
    
    public long getLastAttemptCallToBis() {
      return lastAttemptCallToBis;
    }
    public void setLastAttemptCallToBis(long lastAttemptCallToBis) {
      this.lastAttemptCallToBis = lastAttemptCallToBis;
    }
    public long getLastSuccessCallToBis() {
      return lastSuccessCallToBis;
    }
    public void setLastSuccessCallToBis(long lastSuccessCallToBis) {
      this.lastSuccessCallToBis = lastSuccessCallToBis;
    }
    public int getNoOfRecordsFromBis() {
      return noOfRecordsFromBis;
    }
    public void setNoOfRecordsFromBis(int noOfRecordsFromBis) {
      this.noOfRecordsFromBis = noOfRecordsFromBis;
    }
    public int getNoOfRecordsScheduled() {
      return noOfRecordsScheduled;
    }
    public void resetNoOfRecordsScheduled() {
      this.noOfRecordsScheduled = 0;
    }
    
    public void increaseNoOfRecordsScheduled(){
      this.noOfRecordsScheduled++;
    }
    
    @Override
    public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{ \"lastAttemptCallToBis\" : ");
    sb.append(lastAttemptCallToBis);
    sb.append(",\"lastSuccessCallToBis\" : ");
    sb.append(lastSuccessCallToBis);
    sb.append(",\"noOfRecordsFromBis\" : ");
    sb.append(noOfRecordsFromBis);
    sb.append(",\"noOfRecordsScheduled\" : ");
    sb.append(noOfRecordsScheduled);
    sb.append("}");
    return sb.toString();
    }
    
    
}

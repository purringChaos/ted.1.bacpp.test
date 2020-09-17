package tv.blackarrow.cpp.log.model;

public class ALEssAuditLogVO {
  
  private String uniqueId;
  private String ipAddressOfClient;
  
  public void setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
  }

  public void setIpAddressOfClient(String ipAddressOfClient) {
    this.ipAddressOfClient = ipAddressOfClient;
  }
  public String getUniqueId() {
    return uniqueId;
  }

  public String getIpAddressOfClient() {
    return ipAddressOfClient;
  }
  
  
  
}

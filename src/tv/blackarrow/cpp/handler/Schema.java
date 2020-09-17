package tv.blackarrow.cpp.handler;


public enum Schema {
	i01(new I01RequestHandler(), new I01ResponseHandler()), 
	i03(new I03RequestHandler(), new I03ResponseHandler()),
	Envivio(new I03RequestHandler(), new EnvivioResponseHandler());
	
	private RequestHandler  requestHandler;
	private ResponseHandler responseHandler;
	
	private Schema(RequestHandler requestHandler, ResponseHandler responseHandler){
		this.requestHandler = requestHandler;
		this.responseHandler = responseHandler;
	}
	
	public RequestHandler getRequestHandler(){
		return requestHandler;
	}
	
	public ResponseHandler getResponseHandler(){
		return responseHandler;
	}
	
	public static Schema getSchema(String schemaStr){
		try{
			return valueOf(schemaStr.toLowerCase());
		}catch(Exception e){
			return i01;
		}
	}

}

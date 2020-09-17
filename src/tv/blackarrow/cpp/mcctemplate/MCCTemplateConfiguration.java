package tv.blackarrow.cpp.mcctemplate;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import tv.blackarrow.cpp.model.mccresponse.MccTemplates;
import tv.blackarrow.cpp.setting.SettingUtils;
import tv.blackarrow.ds.common.util.DSUtils;
import tv.blackarrow.ds.common.util.FileChangeListener;
import tv.blackarrow.ds.common.util.XmlUtils;

public class MCCTemplateConfiguration implements FileChangeListener {

	private static final Logger log = LogManager.getLogger(MCCTemplateConfiguration.class);
	private static final MCCTemplateConfiguration instance = new MCCTemplateConfiguration();
	//private static volatile MccTemplateCompliedConfiguration instance1 = new MccTemplateConfiguration();
	private static final long interval = 60000;
	private static final String fileName;
	private static volatile MccTemplates template = null;
	
	static {
		fileName = getconfigurationFileName();
		File file = new File(fileName);
		if(file.exists()){
			loadConfiguration();
			log.debug("Putting file monitors on configuration file: " + fileName);		
			//Add File change Listener
			DSUtils.initializeMonitors(instance, fileName, interval);
			log.debug("File monitors initialized for configuration file: " + fileName);
		} else {
			log.debug("MCC Response template configuration file: " + fileName + " doesn't exist.");
		}
	}
	
	private MCCTemplateConfiguration() {}
	
	public static MCCTemplateConfiguration getInstance(){
		return instance;
	}

	private static void loadConfiguration(){
		log.debug("Loading configuration file: " + fileName);
		try {
			String configFileContent = DSUtils.getFileContents(fileName);
    		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    		Schema schema = sf.newSchema(MCCTemplateConfiguration.class.getResource(getConfigurationSchemaName()));
			template =  (MccTemplates) XmlUtils.getJAXBObject(MccTemplates.class, configFileContent, schema);
			log.debug("Loading complete for configuration file: " + fileName);
			
			MCCTemplateCompiledConfiguration.getInstance().compileMCCTemplate(template);
			
		} catch (IOException e) {
			log.error("Failed to read config file : " + fileName + " " + e.getMessage());
		} catch (JAXBException e) {
			log.error("Failed to parse file : " + fileName + " " + e.getMessage());
		} catch (SAXException e) {
			log.error("Invalid schema file : " + getConfigurationSchemaName() + e.getMessage());
		}
	}
	
	private static final String getconfigurationFileName(){
		 String configurationFileName = SettingUtils.getConfigurationPath() + "mcc_response_template.xml";
		 return configurationFileName;
	}
	
    private static final String getConfigurationSchemaName() {
        return "/schema/template/mcc_response_template.xsd";    	
    }

	@Override
	public void fileChanged(String fileName) {
		loadConfiguration();
	}
	
	public MccTemplates getTemplate() {
		return template;
	}
}

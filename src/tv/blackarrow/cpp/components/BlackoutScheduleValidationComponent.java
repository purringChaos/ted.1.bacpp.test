//
// Copyright 2012 BlackArrow, Inc. All rights reserved.
//
// The information contained herein is confidential, proprietary to BlackArrow, and
// considered a trade secret as defined in section 499C of the penal code of the State of
// California. Use of this information by anyone other than authorized employees of
// BlackArrow is granted only under a written non-disclosure agreement, expressly
// prescribing the scope and manner of such use.
//
// $Author: $
// $Date: $
// $Revision: $
//

package tv.blackarrow.cpp.components;

import java.io.StringReader;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;


/**
 * 
 * the component will validate Blackout schedule upload format
 *
 */
public class BlackoutScheduleValidationComponent implements org.mule.api.lifecycle.Callable {
	private static final Logger LOGGER = LogManager.getLogger(BlackoutScheduleValidationComponent.class);
			
	@Override
	public Object onCall(MuleEventContext context) throws Exception {
		String message =  context.getMessageAsString();
		LOGGER.debug(message);
		if(message.startsWith("blackout=")) {
			message = message.substring("blackout=".length()).trim();
		}
		
		try {
			//Define the type of schema - we use W3C:
			String schemaLang = "http://www.w3.org/2001/XMLSchema";

			//Get validation driver:
			SchemaFactory factory = SchemaFactory.newInstance(schemaLang);

			//Create schema by reading it from an XSD file:
			ApplicationContext ctx = new ClassPathXmlApplicationContext("conf/cpp_bean.xml");			
			Resource resource = ctx.getResource("classpath:schema/alternate_programs.xsd");
			Schema schema = factory.newSchema(resource.getURL());
			Validator validator = schema.newValidator();

			//Perform validation:
			StreamSource streamSource = new StreamSource();
			streamSource.setReader(new StringReader(message));
			validator.validate(streamSource);
		} catch(Exception ex) {
			LOGGER.error(()->ex.getMessage());
			message = ex.getMessage();
		}
		
		return message;
	}
}

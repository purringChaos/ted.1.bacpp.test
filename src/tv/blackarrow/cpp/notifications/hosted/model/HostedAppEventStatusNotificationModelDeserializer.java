/**
 * 
 */
package tv.blackarrow.cpp.notifications.hosted.model;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import tv.blackarrow.cpp.model.EventType;
import tv.blackarrow.cpp.notifications.hosted.model.i02.HostedAppEventStatusI02NotifyModel;
import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;

/**
 * @author asharma
 *
 */
public class HostedAppEventStatusNotificationModelDeserializer implements JsonDeserializer<HostedAppEventStatusNotificationModel> {

	private static final String EVENT_TYPE = "eventType";

	@Override
	public HostedAppEventStatusNotificationModel deserialize(JsonElement jsonElement, Type type,
			JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		if(jsonObject.has(EVENT_TYPE) && EventType.I02.equals(EventType.valueOf(jsonObject.get(EVENT_TYPE).getAsString()))) {
			return jsonDeserializationContext.deserialize(jsonObject, HostedAppEventStatusI02NotifyModel.class);
		} else if(jsonObject.has(EVENT_TYPE) && EventType.SCTE224.equals(EventType.valueOf(jsonObject.get(EVENT_TYPE).getAsString()))) {
			return jsonDeserializationContext.deserialize(jsonObject, HostedAppEventStatusScte224NotifyModel.class);
		}
		return jsonDeserializationContext.deserialize(jsonObject, type);
	}

}

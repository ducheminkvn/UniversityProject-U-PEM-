package upem.jarret.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */

public class JsonUtils {

	/**
	 * Verify if the JSON is valid
	 * @param json
	 * @return true if the String of format JSON is valid
	 */
	public static boolean isJSONValid(String json) {

		try {
			new JSONObject(json);
		} catch (JSONException ex) {
			try {
				new JSONArray(json);
			} catch (JSONException ex1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Verify if the JSON contains a Object
	 * @param json
	 * @return true if the JSON contained an Object
	 */
	public static boolean jsonContainsObject(JSONObject json){
		for(String key : json.keySet())
			try{
				if(json.getString(key).startsWith("{") && json.getString(key).startsWith("}"))
					return true;
			} catch (JSONException jex){
				continue;
			}
		return false;
	}
}

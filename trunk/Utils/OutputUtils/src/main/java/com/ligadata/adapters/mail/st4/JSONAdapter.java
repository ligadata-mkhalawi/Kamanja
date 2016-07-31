package com.ligadata.adapters.mail.st4;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ModelAdaptor;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

public class JSONAdapter implements ModelAdaptor {

	@Override
	public Object getProperty(Interpreter interp, ST self, Object o, Object property, String propertyName) throws STNoSuchPropertyException {
		
		JSONObject jsonObject = (JSONObject)o;
		Object value;
		
		if (property == null || !jsonObject.containsKey(propertyName))
        {
            throw new STNoSuchPropertyException(null, null, propertyName);
        }
		
		value = jsonObject.get(property);
		//Need to handle other types JSONObject and JSONArray
		if(value instanceof JSONArray){
			//Return an array
			value = convertJSONArray((JSONArray)value);			
		}else if( value instanceof JSONObject){
			//Return a Map
			value = convertJSONMap((JSONObject)value);
		}
		
		return value;
	}
	
	private Object[] convertJSONArray(JSONArray jsonArray){
		Object arr[] = new Object[jsonArray.size()]; 
		for(int j=0; j<jsonArray.size();j++){
			Object value = jsonArray.get(j);
			if(value instanceof JSONArray)
				arr[j] = convertJSONArray((JSONArray)value);
			else if(value instanceof JSONObject)
				arr[j] = convertJSONMap((JSONObject)value);
			else
				arr[j] = value;
		}
		return arr;	
	}
	
	private Map<String, Object> convertJSONMap(JSONObject jsonObject){
		Map<String, Object> map = new HashMap<String, Object>();
		Set<String> keySet = jsonObject.keySet();
		for(String key:keySet){
			Object value = jsonObject.get(key);
			if(value instanceof JSONArray)
				map.put(key,convertJSONArray((JSONArray)value));
			else if(value instanceof JSONObject)
				map.put(key,convertJSONMap((JSONObject)value));
			else
				map.put(key,value);
		}
		return map;
	}

}

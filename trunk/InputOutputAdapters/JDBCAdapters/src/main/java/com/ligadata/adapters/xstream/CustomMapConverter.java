package com.ligadata.adapters.xstream;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;


public class CustomMapConverter<T> extends MapConverter {

	public CustomMapConverter(Mapper mapper) {
		super(mapper);
	}

	@SuppressWarnings("rawtypes")
	public boolean canConvert(Class type) {
		return type == HashMap.class;
	}

	@SuppressWarnings("unchecked")
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		Map<String, T> map = (Map<String, T>) source;
		for (Map.Entry<String, T> entry : map.entrySet()) {
			T value = entry.getValue();
			writer.startNode(entry.getKey());
			context.convertAnother(value);
			writer.endNode();
		}
	}

	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		Map<String, T> map = new HashMap<String, T>();
		populateStringMap(reader, context, map);
		return map;
	}

	@SuppressWarnings("unchecked")
	protected void populateStringMap(HierarchicalStreamReader reader, UnmarshallingContext context, Map<String, T> map) {
		while (reader.hasMoreChildren()) {
			reader.moveDown();
			String key = reader.getNodeName();
			T value = (T) readItem(reader, context, map);
			reader.moveUp();
			map.put(key, value);
		}
	}
}

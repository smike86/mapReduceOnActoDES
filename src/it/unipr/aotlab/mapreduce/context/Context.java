package it.unipr.aotlab.mapreduce.context;

import java.util.Map;

public interface Context extends AutoCloseable {

	public void put(Object key, Object value);
	
	public void putAll(Map map);

}

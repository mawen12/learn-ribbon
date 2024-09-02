package com.mawen.learn.ribbon.client.http;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
public class CaseInsensitiveMultiMap implements HttpHeaders {

	Multimap<String, Map.Entry<String, String>> map = ArrayListMultimap.create();

	@Override
	public String getFirstValue(String headerName) {
		Collection<Map.Entry<String, String>> entries = map.get(headerName.toLowerCase());
		if (entries == null || entries.isEmpty()) {
			return null;
		}
		return entries.iterator().next().getValue();
	}

	@Override
	public List<String> getAllValues(String headerName) {
		Collection<Map.Entry<String, String>> entries = map.get(headerName.toLowerCase());
		if (entries == null || entries.isEmpty()) {
			return Collections.emptyList();
		}
		return entries.stream().map(Map.Entry::getValue).collect(Collectors.toList());
	}

	@Override
	public List<Map.Entry<String, String>> getAllHeaders() {
		Collection<Map.Entry<String, String>> all = map.values();
		return new ArrayList<>(all);
	}

	@Override
	public boolean containsHeader(String headerName) {
		return map.containsKey(headerName.toLowerCase());
	}

	public void addHeader(String name, String value) {
		if (getAllValues(name).contains(value)) {
			return;
		}

		AbstractMap.SimpleEntry<String, String> entry = new AbstractMap.SimpleEntry<>(name, value);
		map.put(name.toLowerCase(), entry);
	}

	Map<String, Collection<String>> asMap() {
		Multimap<String, String> result = ArrayListMultimap.create();
		Collection<Map.Entry<String, String>> all = map.values();
		for (Map.Entry<String, String> entry : all) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result.asMap();
	}
}

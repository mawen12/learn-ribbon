package com.mawen.learn.ribbon.template;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public class TemplateParser {

	public static List<Object> parseTemplate(String template) {
		List<Object> templateParts = new ArrayList<>();
		if (template == null) {
			return templateParts;
		}

		StringBuilder sb = new StringBuilder();
		String key;
		for (char c : template.toCharArray()) {
			switch (c) {
				case '{':
					key = sb.toString();
					sb = new StringBuilder();
					templateParts.add(key);
					break;
				case '}':
					key = sb.toString();
					sb = new StringBuilder();
					if (key.charAt(0) == ';') {
						templateParts.add(new MatrixVar(key.substring(1)));
					}
					else {
						templateParts.add(new PathVar(key));
					}
					break;
				default:
					sb.append(c);
			}
		}

		key = sb.toString();
		if (!key.isEmpty()) {
			templateParts.add(key);
		}

		return templateParts;
	}

	public static String toData(Map<String, Object> variables, ParsedTemplate parsedTemplate) throws TemplateParsingException {
		return toData(variables, parsedTemplate.getTemplate(), parsedTemplate.getParsed());
	}

	public static String toData(Map<String, Object> variables, String template, List<Object> parsedList) throws TemplateParsingException {
		int params = variables.size();
		if (variables.isEmpty() && template.indexOf('{') == 0) {
			return template;
		}

		StringBuilder sb = new StringBuilder();
		for (Object part : parsedList) {
			if (part instanceof TemplateVar) {

				Object var = variables.get(part.toString());
				if (part instanceof MatrixVar) {
					if (var != null) {
						sb.append(";").append(part.toString()).append('=').append(var);
						params--;
					}
				}
				else if (part instanceof PathVar) {
					if (var == null) {
						throw new TemplateParsingException(String.format("template variable %s was not supplied for template %s", part.toString(), template));
					}
					else {
						sb.append(var);
						params--;
					}
				}
				else {
					throw new TemplateParsingException(String.format("template variable type %s is not supplied for template %s", part.getClass().getCanonicalName(), template));
				}

			}
			else {
				sb.append(part.toString());
			}
		}

		return sb.toString();
	}
}

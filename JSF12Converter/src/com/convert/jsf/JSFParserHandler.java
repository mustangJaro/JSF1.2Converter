package com.convert.jsf;

import java.util.HashMap;
import java.util.Map;

import com.webapp.parse.Attribute;
import com.webapp.parse.Element;
import com.webapp.parse.JspParser;
import com.webapp.parse.ParserHandler;

public class JSFParserHandler implements ParserHandler {
	
	private Map<String, String> uris = new HashMap<String, String>();

	@Override
	public void startElement(Element el) {
		if(el.getTagType().equals(JspParser.TAG_TYPE.JSP)){
			
			String prefix = "";
			for(Attribute attr : el.getAttrs()){
				if(attr.getName().equals("prefix"))
					prefix = attr.getValue();
				else if(attr.getName().equals("uri"))
					uris.put(prefix, attr.getValue());
			}
		}else if(el.getqName().equals("html")){
			System.out.println("<!DOCTYPE html>");
			System.out.println("<html lang=\"en\" \n"
					+ "     xmlns=\"http://www.w3.org/1999/xhtml\"");
			
			for(String key : uris.keySet()){
				System.out.println("     xmlns:" + key + "=\"" + uris.get(key) + "\"");
			}
			
			System.out.print("     xmlns:ui=\"http://java.sun.com/jsf/facelets\">");
			
		}else{
			System.out.print(el.toString());
		}

	}

	@Override
	public void endElement(Element el) {
		if(!el.isOpenedAndClosed() && !el.getTagType().equals(JspParser.TAG_TYPE.JSP))
			System.out.print("</" + el.getqName() + ">");
	}

	@Override
	public void elementValue(String s) {
		System.out.print(s);

	}

	@Override
	public void showWhitespace(String s) {
		System.out.print(s);
	}

}

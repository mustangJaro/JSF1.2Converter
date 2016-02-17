package com.convert.jsf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.webapp.parse.Attribute;
import com.webapp.parse.Element;
import com.webapp.parse.JspParser;
import com.webapp.parse.ParserHandler;

public class JSFParserHandler implements ParserHandler {
	
	private Map<String, String> uris = new HashMap<String, String>();
	private boolean includeRichFacesConversion = false;

	@Override
	public void startElement(Element el) {
		if(el.getTagType().equals(JspParser.TAG_TYPE.JSP)){			
			String prefix = "";
			String value = "";
			for(Attribute attr : el.getAttrs()){
				if(attr.getName().equals("prefix"))
					prefix = attr.getValue();
				else if(attr.getName().equals("uri"))
					value = attr.getValue();
			}
			uris.put(prefix, value);
		}else{
			switch(el.getqName().toUpperCase()){
			case "HTML":
				print("<!DOCTYPE html>\n");
				print("<html lang=\"en\" \n"
						+ "     xmlns=\"http://www.w3.org/1999/xhtml\"\n");
				
				for(String key : uris.keySet()){
					print("     xmlns:" + key + "=\"" + uris.get(key) + "\"\n");
				}
				
				print("     xmlns:ui=\"http://java.sun.com/jsf/facelets\">");
				break;
			case "HEAD":
				String result = el.toString().replaceAll("head", "h:head");
				print(result);
				break;
			default:
				
				//RichFaces specific conversion changes go here
				if(includeRichFacesConversion){
					switch(el.getqName().toUpperCase()){
					case "RICH:MODALPANEL":
						print("<rich:popupPanel modal=\"true\"");
						printRichFacesAttrsAndEnd(el);
						break;
					case "RICH:SIMPLETOGGLEPANEL":
						print("<rich:collapsiblePanel");
						printRichFacesAttrsAndEnd(el);
						break;
					case "RICH:TOOLTIP":
						//case changed on this one
						print("<rich:tooltip");
						printRichFacesAttrsAndEnd(el);
						break;
					case "RICH:DATALIST":
						print("<rich:list ");
						printRichFacesAttrsAndEnd(el);
						break;
					case "RICH:DATASCROLLER":
						//case changed on this one
						print("<rich:dataScroller ");
						printRichFacesAttrsAndEnd(el);
						break;
					case "RICH:SUBTABLE":
						print("<rich:collapsibleSubTable ");
						printRichFacesAttrsAndEnd(el);
						break;
					case "A4J:ACTIONPARAM":
						print("<a4j:param");
						printRichFacesAttrsAndEnd(el);
						break;
					case "A4J:SUPPORT":
						print("<a4j:ajax");
						printRichFacesAttrsAndEnd(el);
						
						//TODO log messages about validating this tag
						/*
						 * 	
							Implemented as a behavior according to JSF specification. So
								Any Java code related to dynamical view creation should be reviewed/refactored accordingly.
								Do not accepts nested parameters
								queue related options(requestDelay, requestGroupingId, ignoreDupResponse) now could be set only using a4j:attachQueue nested tag.
							Important: consider that JSF 2 standartized behaviors mechanism and there are next points that should be remembered:
								events for behaviors should be defined without on* prefix. not onchange but change and so on.
								behavior is not an action component so can't have parameters attached(params expects action source parent).
								programatical creation should be done using app createBehavior() and (ClientBheviorHolder)XXXComponentClass.addBehavior().
						 */
						break;						
					case "A4J:FORM":
					case "A4J:AJAXLISTENER":
					case "RICH:SPACER":
						//TODO Log warning here that tags were removed
						break;
					default:
						print("<" + el.getqName());
						printRichFacesAttrsAndEnd(el);
					}
				}else
					print(el.toString());				
			}
		}
			

	}

	@Override
	public void endElement(Element el) {
		if(!el.isOpenedAndClosed() && !el.getTagType().equals(JspParser.TAG_TYPE.JSP)){
			switch(el.getqName().toUpperCase()){
			case "HEAD":
				print("</h:head>");
				break;
			default:
				//RichFaces specific conversion changes go here
				if(includeRichFacesConversion){
					switch(el.getqName().toUpperCase()){
					case "RICH:MODALPANEL":
						print("</rich:popupPanel>");
						break;
					case "RICH:SIMPLETOGGLEPANEL":
						print("</rich:collapsiblePanel>");
					case "RICH:TOOLTIP":
						//case changed on this one
						print("</rich:tooltip>");
					case "RICH:DATALIST":
						print("</rich:list>");
					case "RICH:DATASCROLLER":
						//case changed on this one
						print("</rich:dataScroller>");
					case "RICH:SUBTABLE":
						print("</rich:collapsibleSubTable>");
					case "A4J:ACTIONPARAM":
						print("</a4j:param>");
						break;
					case "A4J:SUPPORT":
						print("</a4j:ajax>");
						break;
					case "A4J:FORM":
					case "A4J:AJAXLISTENER":
					case "RICH:SPACER":
						//TODO Log warning here that tags were removed
						break;
					default:
						print("</" + el.getqName() + ">");
					}
				}else
					print("</" + el.getqName() + ">");
					
			}
		}
	}

	@Override
	public void elementValue(String s) {
		System.out.print(s);

	}

	@Override
	public void showWhitespace(String s) {
		System.out.print(s);
	}

	public boolean isIncludeRichFacesConversion() {
		return includeRichFacesConversion;
	}

	public void setIncludeRichFacesConversion(boolean includeRichFacesConversion) {
		this.includeRichFacesConversion = includeRichFacesConversion;
	}
	
	private void print(String s){
		System.out.print(s);
	}
	
	private void printRichFacesAttrsAndEnd(Element el){
		if(el.getAttrs() != null){
			for(Attribute attr : el.getAttrs()){
				Attribute convAttr = convertRichFacesAttribute(el.getqName(), attr);
				if(convAttr != null)
					print(" " + convAttr.getName() + "=\"" + convAttr.getValue() + "\"");
			}
		}
		if(el.isOpenedAndClosed())
			print("/");
		print(">");		
	}
	
	private Attribute convertRichFacesAttribute(String qName, Attribute attr){
		
		switch(attr.getName().toUpperCase()){
		case "PROCESS":
			attr.setName("execute");
			break;
		case "RERENDER":
			attr.setName("render");
			break;
		case "AJAXSINGLE":
			attr.setName("execute");
			attr.setValue("@this");
			break;
		case "LIMITTOLIST":
			attr.setName("limitRender");
			break;
		case "SELFRENDERED":
		case "RENDERREGIONONLY":
		case "AJAXLISTENER":
		case "IMMEDIATE":
			if(qName.equalsIgnoreCase("region")){
				//TODO log warning here that we're removing attribute
				attr = null;
			}
			break;
		default:
			if(attr.getValue().contains("Richfaces.hideModalPanel")){
				attr.setValue("#{rich:component('" + getRichFacesPanelName(attr.getValue()) + "')}.hide()");
			}else if(attr.getValue().contains("Richfaces.showModalPanel")){
				attr.setValue("#{rich:component('" + getRichFacesPanelName(attr.getValue()) + "')}.show()");
			}
		}
		return attr;
	}
	
	private String getRichFacesPanelName(String attrValue){
		String panelName = "";
		
		int startIndex = attrValue.indexOf("'") + 1;
		int endIndex = attrValue.lastIndexOf("'");
		
		panelName = attrValue.substring(startIndex, endIndex);
		
		return panelName;
	}
	
}

package com.convert.jsf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.webapp.parse.Attribute;
import com.webapp.parse.Element;
import com.webapp.parse.JspParser;
import com.webapp.parse.ParserHandler;
import com.webapp.parse.TAG_TYPE;

public class JSFParserHandler implements ParserHandler {
	private Logger logger = Logger.getLogger(JSFParserHandler.class.getName());
	
	private Map<String, String> uris = new HashMap<String, String>();
	private boolean includeRichFacesConversion = false;
	private boolean includePrimeFacesConversion = false;
	private boolean includedFile = false;
	private boolean gotFirstTag = false;
	private boolean gotHeadTag = false;
	private boolean gotFirstStatus = false;
	private boolean declaredRFURI = false;
	private File outputDir;
	private BufferedWriter output;
	
	@Override
	public void startFile(String filename){
		gotHeadTag = false;
		gotFirstTag = false;
		gotFirstStatus = false;
		declaredRFURI = false;
		uris = new HashMap<String, String>();
		
		
		File file = new File(outputDir.getAbsolutePath() + "\\" + filename);
		try {
			File dir = new File(outputDir.getAbsolutePath());
			dir.mkdirs();
			file.createNewFile();
			output = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			logger.error("Failure creating file", e);
		}
		logger.debug("====================Start of file: " + filename + "====================");
	}
	
	public void printHTMLStarter(){

		if(includedFile)
			print("<ui:composition ");
		else{
			print("<!DOCTYPE html>\n");
			print("<html ");
		}
		print("lang=\"en\" \n"
				+ "     xmlns=\"http://www.w3.org/1999/xhtml\"\n"
				+ "     xmlns:c=\"http://java.sun.com/jsp/jstl/core\"\n");
		
		for(String key : uris.keySet()){
			if(!key.equalsIgnoreCase("c")){
				if(includePrimeFacesConversion){
					//remove a4j reference and convert rich reference to 'p'
					if(!key.equalsIgnoreCase("a4j")){
						if(key.equalsIgnoreCase("rich")){
							print("     xmlns:p=\"http://primefaces.org/ui\"\n");							
						}else{
							print("     xmlns:" + key + "=\"" + uris.get(key) + "\"\n");							
						}
					}
				}else{
					print("     xmlns:" + key + "=\"" + uris.get(key) + "\"\n");
				}
			}
		}
		
		print("     xmlns:ui=\"http://java.sun.com/jsf/facelets\">\n");
		
	}

	@Override
	public void startElement(Element el) {
//		logger.trace("Start element " + el);
		switch(el.getTagType()){
		case JSP:
			switch(el.getqName().toUpperCase()){
			case "TAGLIB":
				String prefix = "";
				String value = "";
				for(Attribute attr : el.getAttrs()){
					if(attr.getName().equals("prefix")){
						prefix = attr.getValue();
						if(prefix.equalsIgnoreCase("rich"))
							declaredRFURI = true;
					}else if(attr.getName().equals("uri"))
						value = attr.getValue();
				}
				uris.put(prefix, value);
				break;
			case "INCLUDE":
				printUIInclude(el, "file");
				print("/>");
				break;
			}
			break;
		case DOCTYPE:
			break;
		case SCRIPT:
			if(!gotFirstTag){
				printHTMLStarter();
				gotFirstTag = true;
			}
			
			if(el.getAttrs() != null){
				boolean foundJavaScriptType = false;
				for(Attribute attr : el.getAttrs()){
					if(attr.getName().trim().equalsIgnoreCase("type") && attr.getValue().contains("javascript")){
						foundJavaScriptType = true;
						break;
					}else if(attr.getValue().contains("<%=")){
						String attrVal = attr.getValue();
						if(attrVal.contains(".jsf"))
							attrVal = attrVal.replace(".jsf", ".xhtml");
						attr.setValue("#{request.contextPath}" + attrVal.substring(attrVal.indexOf("%>")+2));						
					}
				}
				if(foundJavaScriptType){
					print(el.toString());
					print("//<![CDATA[");
				}
			}else{
				print(el.toString());	
				print("//<![CDATA[");
			}
			break;
		default:
			switch(el.getqName().toUpperCase()){
			case "HTML":
				printHTMLStarter();
				break;
			case "HEAD":
				String result = el.toString().replaceAll("head", "h:head");
				print(result);
				gotHeadTag = true;
				break;
			case "BR":
				el.setOpenedAndClosed(true);
				print(el.toString());
				break;
			case "SCRIPT":
				if(el.getAttrs() != null){
					boolean foundJavaScriptType = false;
					for(Attribute attr : el.getAttrs()){
						if(attr.getName().trim().equalsIgnoreCase("type") && attr.getValue().contains("javascript")){
							foundJavaScriptType = true;
							break;
						}else if(attr.getValue().contains("<%=")){
							String attrVal = attr.getValue();
							if(attrVal.contains(".jsf"))
								attrVal = attrVal.replace(".jsf", ".xhtml");
							attr.setValue("#{request.contextPath}" + attrVal.substring(attrVal.indexOf("%>")+2));						
						}
					}
					if(foundJavaScriptType){
						print(el.toString());
						print("//<![CDATA[");
					}
				}else{
					print(el.toString());	
					print("//<![CDATA[");
				}
				break;
			case "CCDS:MAIN":
				//wrap this with a JSF head tag, immediately close it and then create the JSF body tag
				if(!gotHeadTag){
					print("<h:head>\n");
				}
				el.setOpenedAndClosed(true);
				print(el.toString() + "\n");
				if(!gotHeadTag){
					print("</h:head>\n");
				}
				print("<h:body>\n");
				break;
			case "JSP:INCLUDE":
				print("<ui:include ");
				if(el.getAttrs() != null){
					for(Attribute attr : el.getAttrs()){
						if(attr.getName().equalsIgnoreCase("page")){
							if(attr.getValue().contains(".jsp"))
								attr.setValue(attr.getValue().replace(".jsp", ".xhtml"));
							print(" src=\"" + attr.getValue() + "\"");
						}else{
							print(attr.toString());
						}
					}
				}
				if(el.isOpenedAndClosed())
					print("/");
				print(">");		
				break;
			case "JSP:PARAM":
				print("<ui:param ");
				printAttrsAndEnd(el);
				break;
			default:
				if(!gotFirstTag){
					printHTMLStarter();
					gotFirstTag = true;
				}
				
				//RichFaces specific conversion changes go here
				if(includeRichFacesConversion){
					switch(el.getqName().toUpperCase()){
					case "RICH:COLUMNS":
						print("<c:forEach ");
						printAttrsAndEnd(el);
						break;
					case "RICH:MODALPANEL":
						print("<rich:popupPanel modal=\"true\"");
						printAttrsAndEnd(el);
						break;
					case "RICH:SIMPLETOGGLEPANEL":
						print("<rich:collapsiblePanel");
						printAttrsAndEnd(el);
						break;
					case "RICH:TOOLTIP":
						//case changed on this one
						print("<rich:tooltip");
						printAttrsAndEnd(el);
						break;
					case "RICH:DATALIST":
						print("<rich:list ");
						printAttrsAndEnd(el);
						break;
					case "RICH:DATASCROLLER":
						//case changed on this one
						print("<rich:dataScroller ");
						printAttrsAndEnd(el);
						break;
					case "RICH:SUBTABLE":
						print("<rich:collapsibleSubTable ");
						printAttrsAndEnd(el);
						break;
					case "A4J:ACTIONPARAM":
						print("<a4j:param");
						printAttrsAndEnd(el);
						break;
					case "A4J:SUPPORT":
						print("<a4j:ajax");
						printAttrsAndEnd(el);

						logger.warn("Line: " + el.getLineNumber() + " The open tag is changed to <a4j:ajax> and must be validated: " + el.toString());
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
					case "A4J:INCLUDE":
						printUIInclude(el, "viewId");
						if(el.isOpenedAndClosed())
							print("/");
						print(">");
						break;
					case "A4J:HTMLCOMMANDLINK":
						print("<a4j:commandLink ");
						printAttrsAndEnd(el);						
					case "A4J:FORM":
					case "A4J:AJAXLISTENER":
					case "RICH:SPACER":
						logger.warn("Line: " + el.getLineNumber() + " The tag was removed: " + el.toString());
						break;
					default:
						print("<");
						if(el.getTagType().equals(TAG_TYPE.COMMENT)){
							print("!--");
							print(el.getqName().replace("--", ""));
						}else{
							print(el.getqName());
						}
						printAttrsAndEnd(el);
					}
				}else if(includePrimeFacesConversion){			
					
					switch(el.getqName().toUpperCase()){
					case "H:COLUMN":
						if(declaredRFURI){
							print("<p:column ");
							printAttrsAndEnd(el);
						}else
							print(el.toString());
						break;
					case "H:INPUTTEXT":
						print("<p:inputText ");
						printAttrsAndEnd(el);
						break;
					case "H:SELECTONEMENU":
						print("<p:selectOneMenu ");
						if(el.getAttrs() != null && el.getAttrs().size() > 0){
							for(Attribute attr : el.getAttrs()){
								if(attr.getName().equalsIgnoreCase("styleClass")){
									if(attr.getValue().equalsIgnoreCase("inputSelect"))
										attr.setValue("width100");
									else
										attr.setValue(attr.getValue() + " width100");
								}
							}
						}
						printAttrsAndEnd(el);
						break;
					case "A4J:JSFUNCTION":
						print("<p:remoteCommand ");
						printAttrsAndEnd(el);
						break;
					case "RICH:MODALPANEL":
						print("<p:dialog modal=\"true\" resizable=\"false\" ");
						printAttrsAndEnd(el);
						break;		
					case "A4J:REGION":
					case "A4J:ACTIONPARAM":
					case "RICH:COMPONENTCONTROL":
						logger.warn("Line: " + el.getLineNumber() + " The open tag is removed and must be validated: " + el.toString());
						break;
					case "A4J:SUPPORT":
						print("<p:ajax ");
						printAttrsAndEnd(el);
						break;
					case "A4J:INCLUDE":
						printUIInclude(el, "viewId");
						if(el.isOpenedAndClosed())
							print("/");
						print(">");
						break;				
					case "A4J:FORM":
						logger.warn("Line: " + el.getLineNumber() + " Changed a4j:form to h:form and it must be validated: " + el.toString());
						print("<h:form ");
						printAttrsAndEnd(el);
						break;
					case "RICH:MESSAGES":
						print("<h:messages ");
						printAttrsAndEnd(el);
						break;
					case "A4J:STATUS":
						if(!gotFirstStatus){
							if(el.isOpenedAndClosed()){
								gotFirstStatus = true;
								print("<p:ajaxStatus>\n");
								if(el.getAttrs() != null){
									String startText = "";
									String startStyle = "";
									String stopText = "";
									for(Attribute attr : el.getAttrs()){
										if(attr.getName().equalsIgnoreCase("startText")){
											startText = attr.getValue();
										}else if(attr.getName().equalsIgnoreCase("stopText")){
											stopText = attr.getValue();
										}else if(attr.getName().equalsIgnoreCase("startStyleClass")){
											startStyle = attr.getValue();
										}
									}
									
									if(!startText.equals("")){
										print("<f:facet name=\"start\">\n");
										print("<h:outputText value=\"" + startText + "\" ");
										if(!startStyle.equals("")){
											print(" styleClass=\"" + startStyle + "\"");
										}
										print("/>\n");
										print("</f:facet>\n");
									}
									if(!stopText.equals("")){
										print("<f:facet name=\"complete\">\n");
										print("<h:outputText value=\"" + startText + "/>\n");
										print("</f:facet>\n");										
									}
								}
								print("</p:ajaxStatus>\n");
								
							}else{
								print("<p:ajaxStatus ");
								printAttrsAndEnd(el);
							}
						}else{
							logger.warn("Line: " + el.getLineNumber() + " Removed additional a4j:status since only one is allowed (blockUI is alternative): " + el.toString());							
						}
						break;
					case "RICH:PANEL":
						print("<p:outputPanel ");
						printAttrsAndEnd(el);
						break;
					case "A4J:HTMLCOMMANDLINK":
						print("<p:commandLink ");
						printAttrsAndEnd(el);
						break;
					case "RICH:DATASCROLLER":
						logger.warn("Line: " + el.getLineNumber() + " The open tag is removed and recommend to add pagination attribute to dataTable: " + el.toString());						
						break;
					case "RICH:SUGGESTIONBOX":
						print("<p:autoComplete ");
						printAttrsAndEnd(el);
						break;
					case "RICH:SIMPLETOGGLEPANEL":
						print("<p:accordionPanel>\n<p:tab ");
						printAttrsAndEnd(el);
						break;
					case "RICH:TABPANEL":
						print("<p:tabView ");
						printAttrsAndEnd(el);
						break;
					case "RICH:TOOLTIP":
						//case changed on this one
						print("<p:tooltip");
						printAttrsAndEnd(el);
						break;
					default:
						String qName = el.getqName();
						if(el.getqName().startsWith("rich:")){
							qName = el.getqName().replaceFirst("rich:", "p:");
						}else if(el.getqName().startsWith("a4j:")){
							qName = el.getqName().replaceFirst("a4j:", "p:");
						}
						
						print("<");
						if(el.getTagType().equals(TAG_TYPE.COMMENT)){
							print("!--");
							print(qName.replace("--", ""));
						}else{
							print(qName);
						}
						printAttrsAndEnd(el);
					}
				}else
					print(el.toString());				
			}
			break;
		}

	}

	@Override
	public void endElement(Element el) {
		if(!el.isOpenedAndClosed() 
				&& !el.getTagType().equals(TAG_TYPE.JSP)
				&& !el.getTagType().equals(TAG_TYPE.DOCTYPE)){
			switch(el.getqName().toUpperCase()){
			case "HEAD":
				print("</h:head>");
				break;
			case "SCRIPT":
				print("//]]>\n");
				print("</" + el.getqName() + ">");
				break;
			case "CCDS:MAIN":
				print("<ccds:devDBName/>\n");
				print("</h:body>\n");
				break;
			case "JSP:INCLUDE":
				if(!el.isOpenedAndClosed())
					print("</ui:include>");
				break;
			case "JSP:PARAM":
				if(!el.isOpenedAndClosed())
					print("</ui:param>");
				break;
			default:
				//RichFaces specific conversion changes go here
				if(includeRichFacesConversion){
					switch(el.getqName().toUpperCase()){
					case "RICH:COLUMNS":
						print("</c:forEach>");
						break;
					case "RICH:MODALPANEL":
						print("</rich:popupPanel>");
						break;
					case "RICH:SIMPLETOGGLEPANEL":
						print("</rich:collapsiblePanel>");
						break;
					case "RICH:TOOLTIP":
						//case changed on this one
						print("</rich:tooltip>");
						break;
					case "RICH:DATALIST":
						print("</rich:list>");
						break;
					case "RICH:DATASCROLLER":
						//case changed on this one
						print("</rich:dataScroller>");
						break;
					case "RICH:SUBTABLE":
						print("</rich:collapsibleSubTable>");
						break;
					case "A4J:ACTIONPARAM":
						print("</a4j:param>");
						break;
					case "A4J:SUPPORT":
						print("</a4j:ajax>");
						break;
					case "A4J:INCLUDE":
						print("</ui:include>");
						break;
					case "A4J:HTMLCOMMANDLINK":
						print("</a4j:commandLink>");
						break;
					case "A4J:FORM":
					case "A4J:AJAXLISTENER":
					case "RICH:SPACER":
						logger.warn("Line: " + el.getLineNumber() + " The close tag was removed: " + el.toString());
						break;
					default:
						print("</" + el.getqName() + ">");
					}
				}else if(includePrimeFacesConversion){			
					
					switch(el.getqName().toUpperCase()){
					case "H:COLUMN":
						if(declaredRFURI)
							print("</p:column>");
						else
							print("</" + el.getqName() + ">");
						break;
					case "H:INPUTTEXT":
						print("</p:inputText>");
						break;
					case "H:SELECTONEMENU":
						print("</p:selectOneMenu>");
						break;
					case "A4J:JSFUNCTION":
						print("</p:remoteCommand>");
						break;
					case "RICH:MODALPANEL":
						print("</p:dialog>");
						break;		
					case "A4J:REGION":
					case "A4J:ACTIONPARAM":
					case "RICH:COMPONENTCONTROL":
						logger.warn("Line: " + el.getLineNumber() + " The close tag is removed and must be validated: " + el.toString());
						break;
					case "A4J:SUPPORT":
						print("</p:ajax>");
						break;
					case "A4J:INCLUDE":
						print("</ui:include>");
						break;
					case "A4J:FORM":
						print("</h:form>");
						break;
					case "RICH:MESSAGES":
						print("</h:messages>");
						break;
					case "A4J:STATUS":
						if(!gotFirstStatus){
							gotFirstStatus = true;
							print("</p:ajaxStatus>");
						}
						break;
					case "RICH:PANEL":
						print("</p:outputPanel>");
						break;
					case "A4J:HTMLCOMMANDLINK":
						print("</p:commandLink>");
						break;
					case "RICH:DATASCROLLER":
						logger.warn("Line: " + el.getLineNumber() + " The close tag is removed: " + el.toString());						
						break;
					case "RICH:SUGGESTIONBOX":
						print("</p:autoComplete>");
						break;
					case "RICH:SIMPLETOGGLEPANEL":
						print("</p:tab>\n</p:accordionPanel>");
						break;
					case "RICH:TABPANEL":
						print("</p:tabView>");
						break;
					case "RICH:TOOLTIP":
						//case changed on this one
						print("</p:tooltip>");
						break;
					default:
						if(el.getqName().startsWith("rich:")){
							el.setqName(el.getqName().replaceFirst("rich:", "p:"));
						}else if(el.getqName().startsWith("a4j:")){
							el.setqName(el.getqName().replaceFirst("a4j:", "p:"));
						}

						print("</" + el.getqName() + ">");
					}
				}else
					print("</" + el.getqName() + ">");
					
			}
		}
	}

	@Override
	public void elementValue(String s) {
		s = replaceHTMLEntity(s);
		print(s);
	}

	@Override
	public void showWhitespace(String s) {
		print(s);
	}
	
	@Override
	public void endFile(String filename){
		if(includedFile)
			print("</ui:composition>");
		else
			print("</html>");
		try {
			output.close();
		} catch (IOException e) {
			logger.error("failed to close file", e);
		}
		logger.debug("====================End of file: " + filename + "====================");
	}

	public boolean isIncludeRichFacesConversion() {
		return includeRichFacesConversion;
	}

	public void setIncludeRichFacesConversion(boolean includeRichFacesConversion) {
		this.includeRichFacesConversion = includeRichFacesConversion;
	}
	
	public boolean isIncludePrimeFacesConversion() {
		return includePrimeFacesConversion;
	}

	public void setIncludePrimeFacesConversion(boolean includePrimeFacesConversion) {
		this.includePrimeFacesConversion = includePrimeFacesConversion;
	}

	public boolean isIncludedFile() {
		return includedFile;
	}

	public void setIncludedFile(boolean includedFile) {
		this.includedFile = includedFile;
	}

	private void print(String s){
//		System.out.print(s);
		
		try {
			//fancy work here to translate newline characters
	        StringReader stringReader = new StringReader(s);
	        BufferedReader bufferedReader = new BufferedReader(stringReader);
	        boolean firstTimeThru = true;
	        for(String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
	        	if(!firstTimeThru)
		        	output.newLine();
	        	output.write(line);
	        	firstTimeThru = false;
	        }
	        if(s.endsWith("\n") || s.endsWith("\r"))
	        	output.newLine();
	        
	        bufferedReader.close();
		} catch (IOException e) {
			logger.error("failed to write to file: " + s, e);
		}
	}
	
	/**
	 * Translates an element into the <code>ui:include</code> tag
	 * 
	 * @param el
	 * @param srcAttrName  This is the name of the attribute that should be converted to <code>src</code>
	 */
	private void printUIInclude(Element el, String srcAttrName){
		print("<ui:include ");
		if(el.getAttrs() != null){
			for(Attribute attr : el.getAttrs()){
				if(attr.getName().equalsIgnoreCase(srcAttrName)){
					String src = attr.getValue();
					if(src.endsWith(".jsp"))
						src = src.replaceAll(".jsp", ".xhtml");
					print("src=\"" + src + "\" ");
				}else{
					print(attr.toString() + " ");
				}
			}
		}else{
			logger.warn("Line: " + el.getLineNumber() + " The include does not have any attributes: " + el.toString());
		}
		
	}
	
	/**
	 * Print all of the tag attributes and close out the tag.  Calls the {@link #convertAttribute(Element, Attribute)}
	 * method
	 * 
	 * @param el
	 */
	private void printAttrsAndEnd(Element el){
		if(el.getAttrs() != null){
			for(Attribute attr : el.getAttrs()){
				Attribute convAttr = convertAttribute(el, attr);
				
				if(convAttr != null)
					print(" " + convAttr.getName() + "=\"" + replaceSpecialCharacters(convAttr.getValue()) + "\"");
			}
		}
		if(el.getTagType().equals(TAG_TYPE.COMMENT))
			print("--");
		else if(el.isOpenedAndClosed())
			print("/");
		print(">");		
	}
	
	/**
	 * Convert generic JSP attributes to their new values.<br/><br/>
	 * 
	 * Calls the {@link #convertRichFacesAttribute(Element, Attribute)} or {@link #convertPrimeFacesAttribute(Element, Attribute)}
	 * depending on which boolean is set
	 * 
	 * @param el
	 * @param attr
	 * @return
	 */
	private Attribute convertAttribute(Element el, Attribute attr){
		switch(attr.getName().trim().toUpperCase()){

		case "HREF":
		case "SRC":
			if((el.getqName().trim().equalsIgnoreCase("a") || el.getqName().trim().equalsIgnoreCase("script"))
					&& attr.getValue().contains("<%=")){
				String attrVal = attr.getValue();
				if(attrVal.contains(".jsf"))
					attrVal = attrVal.replace(".jsf", ".xhtml");
				attr.setValue("#{request.contextPath}" + attrVal.substring(attrVal.indexOf("%>")+2));
			}
			break;
		}
		
		if(includeRichFacesConversion)
			return convertRichFacesAttribute(el, attr);
		else if(includePrimeFacesConversion)
			return convertPrimeFacesAttribute(el, attr);
		else
			return attr;
	}
	
	/**
	 * Converts RichFaces specific attributes to their new values
	 * 
	 * @param el
	 * @param attr
	 * @return
	 */
	private Attribute convertRichFacesAttribute(Element el, Attribute attr){
		
		switch(attr.getName().trim().toUpperCase()){
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
			if(el.getqName().equalsIgnoreCase("region")){
				logger.warn("Line: " + el.getLineNumber() + " The attribute 'immediate' was removed: " + el.toString());
				attr = null;
			}
			break;
		case "DIRECTION":
			if(el.getqName().equalsIgnoreCase("rich:tooltip")){
				String attrVal = attr.getValue();
				int dashIndex = attrVal.indexOf("-");
				if(dashIndex > 0){
					String prefix = attrVal.substring(0, dashIndex);
					String suffix = attrVal.substring(dashIndex + 1);
					suffix = suffix.substring(0, 1).toUpperCase() + suffix.substring(1);
					attrVal = prefix + suffix;
				}
				attr.setValue(attrVal);
			}
			break;
		case "RENDERED":
			if(attr.getValue() != null){
				if(attr.getValue().contains("<="))
					attr.setValue(attr.getValue().replace("<=", " le "));
				if(attr.getValue().contains(">="))
					attr.setValue(attr.getValue().replace(">=", " ge "));
				if(attr.getValue().contains("<"))
					attr.setValue(attr.getValue().replace("<", " lt "));
				if(attr.getValue().contains(">"))
					attr.setValue(attr.getValue().replace(">", " gt "));
				if(attr.getValue().contains("&&"))
					attr.setValue(attr.getValue().replace("&&", " and "));
				if(attr.getValue().contains("||"))
					attr.setValue(attr.getValue().replace("||", " or "));
			}
			break;
		case "INDEX":
			if(el.getqName().equalsIgnoreCase("RICH:COLUMNS")){
				logger.warn("Line: " + el.getLineNumber() + " Removed index attribute from rich:columns tag when converting to c:forEach: " + el.toString());
				attr = null;
			}
			break;
		case "FOR":
			if(el.getqName().equalsIgnoreCase("rich:tooltip")){
				attr.setName("target");
			}
			break;
		default:
			if(attr.getValue() != null){
				if(el.getqName().equalsIgnoreCase("RICH:COLUMNS") && attr.getName().equalsIgnoreCase("value")){
					attr.setName("items");
				}
				if(attr.getValue().contains("Richfaces.hideModalPanel")){
					attr.setValue("#{rich:component('" + getRichFacesPanelName(attr.getValue()) + "')}.hide()");
				}else if(attr.getValue().contains("Richfaces.showModalPanel")){
					attr.setValue("#{rich:component('" + getRichFacesPanelName(attr.getValue()) + "')}.show()");
				}
			}
		}
		return attr;
	}
	
	/**
	 * Converts PrimeFaces specific attributes to their new values
	 * 
	 * @param el
	 * @param attr
	 * @return
	 */
	private Attribute convertPrimeFacesAttribute(Element el, Attribute attr){
		
		//These first cases are attribute names that might be common amongst multiple tags, so we delineate them by tag name (qName) first
		//This makes sure that we don't set an attribute name improperly for the wrong tag
		switch(el.getqName().trim().toUpperCase()){
		case "A4J:SUPPORT":
			if(attr.getName().equalsIgnoreCase("action")){
				logger.warn("Line: " + el.getLineNumber() + " Renamed 'action' attribute to 'listener' on a4j:support (p:ajax) tag, update method to take event attribute: " + el.toString());
				attr.setName("listener");
				return attr;
			}else if(attr.getName().equalsIgnoreCase("ajaxSingle")){
				logger.warn("Line: " + el.getLineNumber() + " Removed ajaxSingle attribute from a4j:support tag: " + el.toString());
				attr = null;
				return attr;			
			}
			break;
		case "RICH:MODALPANEL":
			if(attr.getName().equalsIgnoreCase("onshow")){
				attr.setName("onShow");
				return attr;
			}else if(attr.getName().equalsIgnoreCase("onbeforeshow")){
				logger.warn("Line: " + el.getLineNumber() + " Changed onbeforeshow attribute to onShow for rich:modalPanel tag: " + el.toString());
				attr.setName("onShow");
				return attr;			
			}
			break;
		case "RICH:SUGGESTIONBOX":
			switch(attr.getName().toUpperCase()){
			case "HEIGHT":
				attr.setName("scrollHeight");
				return attr;
			case "SUGGESTACTION":
				attr.setName("completeMethod");
				return attr;
			case "FOR":
				logger.warn("Line: " + el.getLineNumber() + " Removed for attribute: " + el.toString());				
				logger.warn("Line: " + el.getLineNumber() + " An input text box near here may need to be removed: " + el.toString());
			case "WIDTH":
			case "USINGSUGGESTOBJECTS":
				logger.warn("Line: " + el.getLineNumber() + " Removed width/usingSuggestObjects attribute: " + el.toString());
				return null;
			}
			break;
		case "RICH:SIMPLETOGGLEPANEL":
			switch(attr.getName().toUpperCase()){
			case "OPENED":
			case "SWITCHTYPE":
				logger.warn("Line: " + el.getLineNumber() + " Removed opened/switchType attribute: " + el.toString());
				return null;
			}
			break;
		case "RICH:TAB":
			switch(attr.getName().toUpperCase()){
			case "STYLECLASS":
				attr.setName("titleStyleClass");
				return attr;
			case "NAME":
			case "LABELWIDTH":
			case "ONTABENTER":
			case "AJAXSINGLE":
				logger.warn("Line: " + el.getLineNumber() + " Removed name/labelWidth/onTabEnter/ajaxSingle attribute: " + el.toString());
				return null;
			}
			break;
		case "A4J:POLL":
			switch(attr.getName().toUpperCase()){
			case "INTERVAL":
				try{
					Integer interval = Integer.parseInt(attr.getValue());
					interval = interval/1000;
					attr.setValue(interval.toString());
				}catch(NumberFormatException e){
					logger.error("Line: " + el.getLineNumber() + " Failed to change interval from milliseconds to seconds: " + el.toString());
				}
				return attr;
			case "ENABLED":
				attr.setName("stop");
				logger.warn("Line: " + el.getLineNumber() + " The enabled attribute (changed to stop) value must be changed to the negation: " + el.toString());
				return attr;
			}
			break;
		case "A4J:COMMANDBUTTON":
			switch(attr.getName().toUpperCase()){
			case "IMAGE":
				if(attr.getValue().contains("add")){
					attr.setName("value");
					attr.setValue("Add");
					//print additional attribute for icon
					print(" icon=\"customIconPosition fa fa-fw fa-plus\"");
				}else if(attr.getValue().contains("edit")){
					attr.setName("value");
					attr.setValue("Edit");
					//print additional attribute for icon
					print(" icon=\"customIconPosition fa fa-fw fa-edit\"");					
				}else if(attr.getValue().contains("view")){
					attr.setName("value");
					attr.setValue("View");
					//print additional attribute for icon
					print(" icon=\"customIconPosition fa fa-fw fa-search\"");					
				}else if(attr.getValue().contains("copy")){
					attr.setName("value");
					attr.setValue("Copy");
					//print additional attribute for icon
					print(" icon=\"customIconPosition fa fa-fw fa-copy\"");					
				}else if(attr.getValue().contains("delete")){
					attr.setName("value");
					attr.setValue("Delete");
					//print additional attribute for icon
					print(" icon=\"customIconPosition fa fa-fw fa-remove\"");
					
				}
				return attr;
			}
		case "RICH:DATATABLE":
			if(attr.getName().equalsIgnoreCase("rowKeyVar")){
				attr.setName("rowIndexVar");
			}
			break;
		}

		
		//then we'll go through some more generic cases
		switch(attr.getName().trim().toUpperCase()){
		case "RENDER":
		case "RERENDER":
			attr.setName("update");
			break;
		case "RENDERED":
			if(attr.getValue() != null){
				if(attr.getValue().contains("<="))
					attr.setValue(attr.getValue().replace("<=", "le"));
				if(attr.getValue().contains(">="))
					attr.setValue(attr.getValue().replace(">=", "ge"));
				if(attr.getValue().contains("<"))
					attr.setValue(attr.getValue().replace("<", "lt"));
				if(attr.getValue().contains(">"))
					attr.setValue(attr.getValue().replace(">", "gt"));
				if(attr.getValue().contains("&&"))
					attr.setValue(attr.getValue().replace("&&", "and"));
				if(attr.getValue().contains("||"))
					attr.setValue(attr.getValue().replace("||", "or"));
			}
			break;
		case "EXECUTE":
			attr.setName("process");
			break;
		case "BREAKBEFORE":
			logger.warn("Line: " + el.getLineNumber() + " Removed breakBefore attribute from rich:column tag: " + el.toString());
			attr = null;
			break;
		case "CELLPADDING":
		case "CELLSPACING":
		case "BORDER":
		case "HEADERCLASS":
		case "WIDTH":
			if(!el.getqName().equalsIgnoreCase("rich:modalPanel")){
				if(attr.getValue().equalsIgnoreCase("100%")){
					attr.setName("class");
					attr.setValue("widthFull");
					logger.warn("Line: " + el.getLineNumber() + " Added class attribute, which may be duplicated; please double-check");
				}else if(el.getqName().equalsIgnoreCase("RICH:DATATABLE")){
					logger.warn("Line: " + el.getLineNumber() + " Removed cellpadding/spacing/border/headerClass/width attribute from rich:dataTable tag: " + el.toString());
					attr = null;
				}
			}else{
				logger.warn("Line: " + el.getLineNumber() + " Removed cellpadding/spacing/border/headerClass/width attribute from rich:modalPanel tag: " + el.toString());
				attr = null;
			}
			break;
		case "ROWCLASSES":
			if(el.getqName().equalsIgnoreCase("rich:datatable"))
				attr.setName("rowStyleClass");
			else if(el.getqName().equalsIgnoreCase("rich:dataGrid")){
				logger.warn("Line: " + el.getLineNumber() + " Removed rowClasses attribute from rich:dataGrid tag: " + el.toString());
				attr = null;				
			}
			break;
		case "COLUMNCLASSES":
			if(el.getqName().equalsIgnoreCase("rich:dataGrid")){
				logger.warn("Line: " + el.getLineNumber() + " Removed columnClasses attribute from rich:dataGrid tag: " + el.toString());
				attr = null;				
			}
			break;
		case "AJAXRENDERED":
			if(el.getqName().equalsIgnoreCase("a4j:outputPanel"))
				attr.setName("autoUpdate");
			break;
		case "EVENT":
			if(el.getqName().equalsIgnoreCase("a4j:support"))
				attr.setValue(attr.getValue().replace("on", ""));
			break;
		case "AUTOSIZED":
			attr.setName("responsive");
			break;
		case "MOVEABLE":
			attr.setName("draggable");
			break;
		case "TOP":
			attr.setName("position");
			attr.setValue("top");
			logger.warn("Line: " + el.getLineNumber() + " Replaced top attribute with position=\"top\": " + el.toString());
			break;
		case "DIRECTION":
			logger.warn("Line: " + el.getLineNumber() + " Removed direction attribute: " + el.toString());
			attr = null;
			break;
		case "ONCHANGED":
			attr.setName("onchange");
			break;
		case "LABEL":
			attr.setName("title");
			break;
		default:
			if(el.getqName().equalsIgnoreCase("rich:modalPanel") && attr.getName().equalsIgnoreCase("id")){
				print(" widgetVar=\"" + attr.getValue() + "\" ");
			}
			
			if(attr.getValue().contains("rich:component") || attr.getValue().contains("Richfaces")){
				String[] values = attr.getValue().split(";");
				String result = "";
				for(int i = 0; i < values.length; i++){
					if(values[i].contains("Richfaces.hideModalPanel")){
						result += "PF('" + getRichFacesPanelName(values[i]) + "')" + ".hide();";
					}else if(values[i].contains("Richfaces.showModalPanel")){
						result += "PF('" + getRichFacesPanelName(values[i]) + "')" + ".show();";
					}else if(values[i].contains("#{rich:component(") && values[i].contains("hide")){
						result += "PF('" + getRichFacesPanelName(values[i]) + "')" + ".hide();";				
					}else if(values[i].contains("#{rich:component(") && values[i].contains("show")){
						result += "PF('" + getRichFacesPanelName(values[i]) + "')" + ".show();";				
					}else{
						result += values[i] + ";";
					}
				}
				attr.setValue(result);
			}
		}
		
		return attr;
	}
	
	/**
	 * Parse the panel name from the hide/show modal panel call
	 * 
	 * @param attrValue
	 * @return
	 */
	private String getRichFacesPanelName(String attrValue){
		String panelName = "";
		
		int startIndex = attrValue.indexOf("'") + 1;
		int endIndex = attrValue.lastIndexOf("'");
		
		panelName = attrValue.substring(startIndex, endIndex);
		
		return panelName;
	}
	
	private String replaceHTMLEntity(String s){
		s = s.replace("&nbsp;", "&#160;");
		s = s.replace("&lt;", "&#60;");
		s = s.replace("&gt;", "&#62;");
		s = s.replace("&amp;", "&#38;");
		s = s.replace("&copy;", "&#169;");
		return s;
	}
	
	private String replaceSpecialCharacters(String s){
		s = s.replace("&", "&amp;");		
		return s;
	}

	public File getOutputDir() {
		return outputDir;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}
	
}

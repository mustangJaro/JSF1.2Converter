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
	private boolean gotFirstTag = false;
	private File outputDir;
	private BufferedWriter output;
	
	@Override
	public void startFile(String filename){
		String xhtmlFile = filename.replace(".jsp", ".xhtml");
		File file = new File(outputDir.getAbsolutePath() + "\\" + xhtmlFile);
		try {
			file.createNewFile();
			output = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			logger.error("Failure creating file", e);
		}
		logger.debug("====================Start of file: " + filename + "====================");
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
					if(attr.getName().equals("prefix"))
						prefix = attr.getValue();
					else if(attr.getName().equals("uri"))
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
		default:
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
				if(!gotFirstTag){
					print("<!DOCTYPE html>\n");
					print("<html lang=\"en\" \n"
							+ "     xmlns=\"http://www.w3.org/1999/xhtml\"\n");
					
					for(String key : uris.keySet()){
						print("     xmlns:" + key + "=\"" + uris.get(key) + "\"\n");
					}
					
					print("     xmlns:ui=\"http://java.sun.com/jsf/facelets\">\n");
					
					gotFirstTag = true;
				}
				
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

						logger.warn("Line: " + el.getLineNumber() + " The tag is changed to <a4j:ajax> and must be validated: " + el.toString());
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
						printRichFacesAttrsAndEnd(el);						
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
						printRichFacesAttrsAndEnd(el);
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
			default:
				//RichFaces specific conversion changes go here
				if(includeRichFacesConversion){
					switch(el.getqName().toUpperCase()){
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
						logger.warn("Line: " + el.getLineNumber() + " The tag was removed: " + el.toString());
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
		s = replaceHTMLEntity(s);
		print(s);
	}

	@Override
	public void showWhitespace(String s) {
		print(s);
	}
	
	@Override
	public void endFile(String filename){
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
	 * Print all of the tag attributes and close out the tag.  Calls {@link #convertRichFacesAttribute(Element, Attribute)}
	 * 
	 * @param el
	 */
	private void printRichFacesAttrsAndEnd(Element el){
		if(el.getAttrs() != null){
			for(Attribute attr : el.getAttrs()){
				Attribute convAttr = convertRichFacesAttribute(el, attr);
				if(convAttr != null)
					print(" " + convAttr.getName() + "=\"" + convAttr.getValue() + "\"");
			}
		}
		if(el.getTagType().equals(TAG_TYPE.COMMENT))
			print("--");
		else if(el.isOpenedAndClosed())
			print("/");
		print(">");		
	}
	
	/**
	 * Converts RichFaces specific attributes to their new values
	 * 
	 * @param el
	 * @param attr
	 * @return
	 */
	private Attribute convertRichFacesAttribute(Element el, Attribute attr){
		
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
			if(el.getqName().equalsIgnoreCase("region")){
				logger.warn("Line: " + el.getLineNumber() + " The attribute 'immediate' was removed: " + el.toString());
				attr = null;
			}
			break;
		case "HREF":
			if(el.getqName().equalsIgnoreCase("a") && attr.getValue().contains("<%=")){
				String attrVal = attr.getValue();
				if(attrVal.endsWith(".jsf"))
					attrVal = attrVal.replace(".jsf", ".xhtml");
				attr.setValue("#{request.contextPath}" + attrVal.substring(attrVal.indexOf("%>")+2));
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
		default:
			if(attr.getValue() != null){
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

	public File getOutputDir() {
		return outputDir;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}
	
}

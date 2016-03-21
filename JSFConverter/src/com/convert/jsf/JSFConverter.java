package com.convert.jsf;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.webapp.parse.JspParser;
import com.webapp.parse.ParserHandler;

public class JSFConverter {
	private Logger logger = Logger.getLogger(JSFConverter.class.getName());

	public static void main(String[] args) {
		JSFConverter converter = new JSFConverter();
		converter.convert();
	}
	
	public void convert(){
		//TODO loop through files
		JspParser parser = new JspParser();
		JSFParserHandler handler = new JSFParserHandler();
		handler.setOutputDir(new File("c:\\xhtml"));
		handler.setIncludeRichFacesConversion(true);
		File jsf12 = new File("C:\\workspace\\Version93 - JSF2\\CustomCallPortal\\www\\home.jsp");
		try {
			parser.parse(jsf12, handler);
		} catch (IOException e) {
			logger.error("Failure parsing file: " + jsf12.getAbsolutePath(), e);
		}
	}

}

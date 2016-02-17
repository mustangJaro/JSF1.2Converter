package com.convert.jsf;

import java.io.File;
import java.io.IOException;

import com.webapp.parse.JspParser;
import com.webapp.parse.ParserHandler;

public class JSFConverter {

	public static void main(String[] args) {
		JSFConverter converter = new JSFConverter();
		converter.convert();
	}
	
	public void convert(){
		JspParser parser = new JspParser();
		ParserHandler handler = new JSFParserHandler();
		File jsf12 = new File("HelloWorld.jsp");
		try {
			parser.parse(jsf12, handler);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

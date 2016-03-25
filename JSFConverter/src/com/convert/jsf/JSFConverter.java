package com.convert.jsf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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
		
		String subDir = "\\";
		String filename = "home.jsp";
		handler.setIncludedFile(false);
		

		String outputDir = "c:\\xhtml\\";
		handler.setOutputDir(new File(outputDir + subDir));
		handler.setIncludePrimeFacesConversion(true);
		File jsf12 = new File("C:\\workspace\\Version93 - JSF2\\CustomCallPortal\\www\\" + subDir + filename);
		try {
			parser.parse(jsf12, handler);
			
			//copy file to original location
			Path targetFile = Paths.get(jsf12.getAbsolutePath());
			Path sourceFile = Paths.get(outputDir + subDir + filename);
			Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
			
			//perform SVN command to rename
			Runtime rt = Runtime.getRuntime();
			rt.exec("svn mv \"" + targetFile + "\" \"" + targetFile.toString().replace(".jsp", ".xhtml") + "\"");
			
			//TODO commit SVN file
			//rt.exec("svn commit");
		} catch (IOException e) {
			logger.error("Failure parsing file: " + jsf12.getAbsolutePath(), e);
		}
	}

}

package com.convert.jsf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.log4j.Logger;

import com.webapp.parse.JspParser;
import com.webapp.parse.ParserHandler;

public class JSFConverter {
	private Logger logger = Logger.getLogger(JSFConverter.class.getName());

	public static void main(String[] args) throws FileNotFoundException {
		JSFConverter converter = new JSFConverter();
		converter.convert();
	}
	
	public void convert() throws FileNotFoundException{
		JspParser parser = new JspParser();
		JSFParserHandler handler = new JSFParserHandler();
		
		
		
		BufferedReader br = new BufferedReader(new FileReader(new File("pageIncludes.txt")));
		String line = null;
		try{
			while((line = br.readLine()) != null){
				
				String[] data = line.split(",");
				
//				String[] data = new String[]{"\\care\\documents\\", "documents.jsp", "0"};
				
				String subDir = data[0];
				String filename = data[1];
				if(data[2].equals("1"))
					handler.setIncludedFile(true);
				else
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
					Process proc = rt.exec("svn mv \"" + targetFile + "\" \"" + targetFile.toString().replace(".jsp", ".xhtml") + "\"");
					
					proc.waitFor();					
					
					BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
					BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
					
					logger.debug("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
					logger.debug("Output from svn mv on " + filename + " - " + proc.exitValue());
					String s = null;
					while ((s = stdInput.readLine()) != null){
						logger.debug(s);						
					}
					logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					logger.error("Error output from svn mv on " + filename);
					while ((s = stdError.readLine()) != null){
						logger.debug(s);						
					}
					logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					
					//TODO commit SVN file
					//rt.exec("svn commit");
				} catch (IOException | InterruptedException e) {
					logger.error("Failure parsing file: " + jsf12.getAbsolutePath(), e);
				}
				
			}
		}catch(IOException e){
			logger.error("Failure reading through file", e);
		}
	}

}

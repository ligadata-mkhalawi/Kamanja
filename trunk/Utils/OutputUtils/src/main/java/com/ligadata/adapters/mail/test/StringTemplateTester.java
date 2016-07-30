package com.ligadata.adapters.mail.test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import com.ligadata.adapters.mail.pojo.Constants;
import com.ligadata.adapters.mail.st4.CustomErrorListener;
import com.ligadata.adapters.mail.st4.JSONAdapter;

public class StringTemplateTester {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try{
		String location = "/home/centuser/workspace/BOFAMailService/src/main/resources";
		Path templateDir = FileSystems.getDefault().getPath(location);
		if(Files.exists(templateDir)){
			System.out.println("Location found.."+templateDir.toAbsolutePath().toString());
			
			InputStream is = null;
			BufferedReader reader = null;
			is = StringTemplateTester.class.getResourceAsStream("/mail-message.json");
			reader = new BufferedReader(new InputStreamReader(is));
					
			StringBuilder b = new StringBuilder();
			String line = reader.readLine();
			while(line != null){
				b.append(line);
				line = reader.readLine();
			}
			reader.close();
			
			
			JSONObject obj = (JSONObject)JSONValue.parse(b.toString());
			
			STGroup stGroup;
			
			String templateName = "template1";
			if(templateName != null && !templateName.isEmpty()){
				stGroup = new STGroupFile(templateDir.toAbsolutePath().toString()+"/"+templateName+".stg", '$', '$');
				System.out.println(stGroup.show());
			}else{
				templateName = "template1";
				stGroup = new STGroupFile(templateDir.toAbsolutePath().toString()+"/"+templateName+".stg", '$', '$');
				System.out.println(stGroup.show());
			}
			
			stGroup.setListener(new CustomErrorListener());
			stGroup.registerModelAdaptor(JSONObject.class, new JSONAdapter());
			System.out.println("Registered ErrorLisnener and ModelAdapter");
			
			ST subject = stGroup.getInstanceOf("subject");
			subject.add("x",obj);
			String subjectString = subject.render();
			System.out.println(subjectString);
			
			ST header = stGroup.getInstanceOf("header");
			header.add("x",obj);
			String headerString = header.render();
			System.out.println(headerString);
			
			ST body = stGroup.getInstanceOf("body");
			body.add("x",obj);
			String bodyString = body.render();
			System.out.println(bodyString);
			
			ST footer = stGroup.getInstanceOf("footer");
			footer.add("x",obj);
			String footerString = footer.render();
			System.out.println(footerString);
			
			ST testFooter = stGroup.getInstanceOf("testFooter");
			testFooter.add("x",obj);
			String testFooterString = testFooter.render();
			System.out.println(testFooterString);
			
			
		}else
			System.out.println(Constants.ERROR_TEMPLATE_DIRECTORY_UNAVAILABLE+templateDir.toAbsolutePath().toString());
		}catch(Exception exc){
			exc.printStackTrace();
		}
	}

}

package com.ligadata.adapters.mail.pojo;


import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j;

import org.json.simple.JSONObject;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import com.ligadata.adapters.AdapterConfiguration;
import com.ligadata.adapters.mail.st4.CustomErrorListener;
import com.ligadata.adapters.mail.st4.JSONAdapter;

@AllArgsConstructor
@NoArgsConstructor
@Log4j
@ToString
public class SimpleMailBean {
	
	@Setter @Getter
	AdapterConfiguration conf;
	
	@Getter @Setter
	String from;
	@Getter @Setter
	String to;
	@Getter @Setter
	String cc;
	@Getter @Setter
	String bcc;
	
	@Getter @Setter
	String subjectHTML;
	@Getter @Setter
	String headerHTML;
	@Getter @Setter
	String bodyHTML;
	@Getter @Setter
	String footerHTML;
	
	@Getter @Setter
	String templateDirectory;
	@Getter @Setter
	JSONObject fillers;
	
	@Getter @Setter
	String templateName;
	
	STGroup stGroup;
	
	private static HashMap<String, String> map;
	
	public void populateData(){
		if(templateDirectory != null && !templateDirectory.isEmpty()){
			Path templateDir = FileSystems.getDefault().getPath(templateDirectory);
			if(Files.exists(templateDir)){
				
				if(map ==null){
					map = new HashMap<>();
					String template_defs = conf.getProperty(AdapterConfiguration.TEMPLATE_MAPPING);
					for (String mapping: template_defs.split(",")){
						map.put(mapping.split("::")[0],mapping.split("::")[1]);
					}
				}
				
				templateName = map.get((String)fillers.get("alertType"));
				
				if(templateName != null && !templateName.isEmpty()){
					stGroup = new STGroupFile(templateDir.toAbsolutePath().toString()+"/"+templateName+".stg", '$', '$');
					System.out.println(stGroup.show());
				}else{
					templateName = map.get("default");
					stGroup = new STGroupFile(templateDir.toAbsolutePath().toString()+"/"+templateName+".stg");
					System.out.println(stGroup.show());
				}
				
				//Register Listener and Adapters here
				stGroup.setListener(new CustomErrorListener());
				stGroup.registerModelAdaptor(JSONObject.class, new JSONAdapter());
				
				for(String name:stGroup.getTemplateNames()){
					
					log.debug("loading template name from STG..."+name);
					
					ST st = stGroup.getInstanceOf(name);
					st.add("x", fillers);
					
					if(name.equalsIgnoreCase("/subject"))
						subjectHTML = st.render();
					else if(name.equalsIgnoreCase("/header"))
						headerHTML = st.render();
					else if(name.equalsIgnoreCase("/body"))
						bodyHTML = st.render();
					else if(name.equalsIgnoreCase("/testFooter"))
						if(conf.getProperty(AdapterConfiguration.TEST_FLAG).equalsIgnoreCase("true"))
							footerHTML = st.render();
					else if(name.equalsIgnoreCase("/footer"))
						if(!conf.getProperty(AdapterConfiguration.TEST_FLAG).equalsIgnoreCase("true"))
							footerHTML = st.render();
				}
				
				//Populate the other fields from JSON
				//Currently doing a direct filling, later it may be rule based (based on AlertType)
				if(conf.getProperty(AdapterConfiguration.TEST_FLAG).equalsIgnoreCase("true")){
					to = conf.getProperty(AdapterConfiguration.TESTMAIL_RECEIPENTS);
					from = conf.getProperty(AdapterConfiguration.MAIL_FROM);
				}else{
					
					String toMail = conf.getProperty(AdapterConfiguration.TO_MAIL);
					String ccMail = conf.getProperty(AdapterConfiguration.CC_MAIL);
					String bccMail = conf.getProperty(AdapterConfiguration.BCC_MAIL);
					
					if(fillers.get(toMail)!=null && !fillers.get(toMail).toString().isEmpty())
						to = (String)fillers.get(toMail);
					if(fillers.get(ccMail)!=null && !fillers.get(ccMail).toString().isEmpty())
						cc = (String)fillers.get(ccMail);
					if(fillers.get(bccMail)!=null && !fillers.get(bccMail).toString().isEmpty())
						bcc = (String)fillers.get(bccMail);
					
					//to = (String)fillers.get("associateEmail");
					//cc = (String)fillers.get("supervisorEmail");
					from = conf.getProperty(AdapterConfiguration.MAIL_FROM);
				}
				
			}else{
				log.error(Constants.ERROR_TEMPLATE_DIRECTORY_UNAVAILABLE+templateDir.toAbsolutePath().toString());
			}
		}
	}
}

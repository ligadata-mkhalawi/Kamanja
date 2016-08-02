package com.ligadata.adapters.mail;

import java.util.Properties;

import javax.mail.Message.RecipientType;

import lombok.extern.log4j.Log4j;

import org.codemonkey.simplejavamail.Email;
import org.codemonkey.simplejavamail.Mailer;

import com.ligadata.adapters.AdapterConfiguration;
import com.ligadata.adapters.mail.pojo.SimpleMailBean;
import com.ligadata.adapters.mail.util.SecretKeyFactoryImpl;

@Log4j
public class MailProcessor {
	private static MailProcessor processor = null;
	private AdapterConfiguration conf;
	
	private static Properties mailServerProperties;
	
	private static SecretKeyFactoryImpl skf;
	private static String password;
	private static Mailer mailer;
	
	private MailProcessor(AdapterConfiguration configs){
		conf = configs;
		
		mailServerProperties = System.getProperties();

	    mailServerProperties.put(AdapterConfiguration.MAIL_PROP_SSL, conf.getProperty(AdapterConfiguration.MAIL_PROP_SSL));
	    mailServerProperties.put(AdapterConfiguration.MAIL_PROP_AUTH, conf.getProperty(AdapterConfiguration.MAIL_PROP_AUTH));
	    mailServerProperties.put(AdapterConfiguration.MAIL_PROP_TTLS, conf.getProperty(AdapterConfiguration.MAIL_PROP_TTLS));


	    // Generate the key and decode the password
	    /*
	    skf = SecretKeyFactoryImpl.getInstance(conf);
	    skf.createKey();
	    password = skf.decoder(conf.getProperty(AdapterConfiguration.MAIL_PROP_PWD.trim()).toString());
	    */
	    password = conf.getProperty(AdapterConfiguration.MAIL_PROP_PWD.trim()).toString();
	    
	    mailer = new Mailer(conf.getProperty(AdapterConfiguration.MAIL_PROP_HOST), 
	    		Integer.parseInt(conf.getProperty(AdapterConfiguration.MAIL_PROP_PORT)), 
	    		conf.getProperty(AdapterConfiguration.MAIL_FROM), password);
	    mailer.setDebug(true);
	    mailer.applyProperties(mailServerProperties);
	}
	
	public static MailProcessor getInstance(AdapterConfiguration configs){
		if(processor == null){
			synchronized (MailProcessor.class) {
				if(processor == null)
					processor = new MailProcessor(configs);
			}
		}
		return processor;
	}
	
	public void processBean(SimpleMailBean bean){
		log.debug("Bean Values..."+bean.toString());
		
		final Email email = new Email();
		
		if(bean.getTo() != null && !bean.getTo().isEmpty())
			for(String to_mail: bean.getTo().split(";"))
				email.addRecipient(to_mail, to_mail, RecipientType.TO);
		
		if(bean.getCc() != null && !bean.getCc().isEmpty())
			for(String cc_mail: bean.getCc().split(";"))
				email.addRecipient(cc_mail, cc_mail,RecipientType.CC);
		
		if(bean.getBcc() != null && !bean.getBcc().isEmpty())
			for(String bcc_mail: bean.getBcc().split(";"))
				email.addRecipient(bcc_mail, bcc_mail,RecipientType.BCC);
		
		email.setSubject(bean.getSubjectHTML());
		email.setTextHTML(bean.getHeaderHTML()+"<br/>"+bean.getBodyHTML()+"<br/>"+bean.getFooterHTML());
		email.setFromAddress(bean.getFrom(), bean.getFrom());
		
		mailer.sendMail(email);
	}
	
}

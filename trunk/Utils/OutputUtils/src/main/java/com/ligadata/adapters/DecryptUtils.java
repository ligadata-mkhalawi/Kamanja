/*
 * Copyright 2015 ligaDATA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ligadata.adapters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import org.apache.logging.log4j.*;

import com.ligadata.adapters.AdapterConfiguration;
import com.ligadata.EncryptUtils.EncryptionUtil;

public class DecryptUtils{
    static String loggerName = "DecryptUtils";
    static Logger logger = LogManager.getLogger(loggerName);

    /**
     *  Get the original password from encrypted-base64-encoded password
     * 
     * @param config
     *          : AdapterConfiguraton contains property hash map
     * @return password as plain text
     * @throws java.lang.Exception
     */
    private static String getDecryptedPassword(AdapterConfiguration config) throws Exception {
	try{
	    // use the enrypted password if available
	    String encryptedPass = config.getProperty(AdapterConfiguration.ENCRYPTED_ENCODED_PASSWORD);
	    if (encryptedPass == null ){
		logger.warn("The properties " + AdapterConfiguration.ENCRYPTED_ENCODED_PASSWORD + " are not defined in properties file ");
		return null;
	    }
	    else{
		String privateKeyFile = config.getProperty(AdapterConfiguration.PRIVATE_KEY_FILE);		       
		if ( privateKeyFile == null ){
		    logger.warn("The property " + AdapterConfiguration.ENCRYPTED_ENCODED_PASSWORD + " is defined but the property " +  AdapterConfiguration.PRIVATE_KEY_FILE + " not defined");
		    return null;
		}
		String algorithm = config.getProperty(AdapterConfiguration.ENCRYPT_DECRYPT_ALGORITHM);
		if ( algorithm == null ){
		    logger.warn("The property " + AdapterConfiguration.ENCRYPTED_ENCODED_PASSWORD + " is defined but the property " +  AdapterConfiguration.ENCRYPT_DECRYPT_ALGORITHM + " not defined");
		    return null;
		}
		logger.info("algorithm      => " + algorithm);
		logger.info("encryptedPass  => " + encryptedPass);
		logger.info("privateKeyFile => " + privateKeyFile);
		byte[] decodedBytes = EncryptionUtil.decode(encryptedPass);
		String pass = EncryptionUtil.decrypt(algorithm,decodedBytes, privateKeyFile);
		return pass;
	    }
	} catch (Exception e) {
	    throw new Exception(e);
	}
    }

    /**
     *  Get the original password from base64-encoded password
     * 
     * @param config
     *          : AdapterConfiguraton contains property hash map
     * @return password as plain text
     * @throws java.lang.Exception
     */
    private static String getDecodedPassword(AdapterConfiguration config) throws Exception {
	try{
	    // use the enrypted password if available
	    String encodedPass = config.getProperty(AdapterConfiguration.ENCODED_PASSWORD);
	    if (encodedPass == null ){
		logger.warn("The properties " + AdapterConfiguration.ENCODED_PASSWORD + " are not defined in properties file ");
		return null;
	    }
	    else{
		logger.info("encodedPass  => " + encodedPass);
		String pass = new String(EncryptionUtil.decode(encodedPass));
		return pass;
	    }
	} catch (Exception e) {
	    throw new Exception(e);
	}
    }

    /**
     *  Get the original password
     * 
     * @param config
     *          : AdapterConfiguraton contains property hash map
	 * @param PasswordPropertyName Password property name
     * @return password as plain text
     */
    public static String getPassword(AdapterConfiguration config, String PasswordPropertyName) {
	try{
	    String pass = getDecryptedPassword(config);
	    if( pass == null ){
		pass = getDecodedPassword(config);
		if( pass == null ){
		    pass = config.getProperty(PasswordPropertyName);
		}
	    }
	    return pass;
	} catch (Exception e) {
	    logger.error("failed to decode password ",e);
	    return null;
	}
    }
}

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
import java.util.Base64;

import com.ligadata.adapters.AdapterConfiguration;

public class DecryptUtils{
    static String loggerName = "DecryptUtils";
    static Logger logger = LogManager.getLogger(loggerName);

    /**
     * Convert given hex string to byte array
     * 
     * @param s
     *          : a string containing hex characters
     * @return byte array
     * @throws java.lang.Exception
     */

    public static byte[] hexStringToByteArray(String s) throws Exception{
	try{
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
		data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
				      + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	} catch (Exception e) {
	    logger.error("Failed to convert hexString to byte array",e);
	    throw e;
	}
    }

    /**
     * Decode(Base64 decoding) given string to a byte array
     * 
     * @param s
     *          : an encoded string 
     * @return byte array
     * @throws java.lang.Exception
     */

    public static byte[] decode(String s) throws Exception{
	try{
	    byte[] decoded = Base64.getDecoder().decode(s.getBytes());
	    return decoded;
	} catch (Exception e) {
	    logger.error("Failed to decode a byte array",e);
	    throw e;
	}
    }

    /**
     * Decode(Base64 decoding) given byte array to a string
     * 
     * @param bytes
     *          : an array of bytes corresponding to string being decoded
     * @return byte array
     * @throws java.lang.Exception
     */

    public static String decode(byte[] bytes) throws Exception{
	try{
	    String decoded = new String(Base64.getDecoder().decode(bytes));
	    return decoded;
	} catch (Exception e) {
	    logger.error("Failed to decode a byte array",e);
	    throw e;
	}
    }

    /**
     * Decrypt text using private key.
     * 
     * @param algorithm
     *          : algorithm used
     * @param text
     *          :encrypted text
     * @param key
     *          :The private key
     * @return plain text
     * @throws java.lang.Exception
     */
    public static String decrypt(String algorithm,byte[] text, String privateKeyFile) throws Exception {
	byte[] dectyptedText = null;
	try {
	    ObjectInputStream inputStream = null;
	    // Decrypt the cipher text using the private key.
	    inputStream = new ObjectInputStream(new FileInputStream(privateKeyFile));
	    final PrivateKey privateKey = (PrivateKey) inputStream.readObject();
	    // get a cipher object and print the provider
	    final Cipher cipher = Cipher.getInstance(algorithm);
	    // decrypt the text using the private key
	    cipher.init(Cipher.DECRYPT_MODE, privateKey);
	    dectyptedText = cipher.doFinal(text);

	} catch (Exception e) {
	    logger.error("Failed to decrypt given password",e);
	    throw e;
	}
	return new String(dectyptedText);
    }

    private static String getDecryptedPassword(AdapterConfiguration config) throws Exception {
	try{
	    // use the enrypted password if available
	    String encryptedPass = config.getProperty(AdapterConfiguration.ENCRYPTED_PASSWORD);
	    if (encryptedPass == null ){
		logger.warn("The properties " + AdapterConfiguration.ENCRYPTED_PASSWORD + " are not defined in properties file ");
		return null;
	    }
	    else{
		String privateKeyFile = config.getProperty(AdapterConfiguration.PRIVATE_KEY_FILE);		       
		if ( privateKeyFile == null ){
		    logger.warn("The property " + AdapterConfiguration.ENCRYPTED_PASSWORD + " is defined but the property " +  AdapterConfiguration.PRIVATE_KEY_FILE + " not defined");
		    return null;
		}
		String algorithm = config.getProperty(AdapterConfiguration.ENCRYPT_DECRYPT_ALGORITHM);
		if ( algorithm == null ){
		    logger.warn("The property " + AdapterConfiguration.ENCRYPTED_PASSWORD + " is defined but the property " +  AdapterConfiguration.ENCRYPT_DECRYPT_ALGORITHM + " not defined");
		    return null;
		}
		logger.info("algorithm      => " + algorithm);
		logger.info("encryptedPass  => " + encryptedPass);
		logger.info("privateKeyFile => " + privateKeyFile);
		byte[] passBytes = decode(encryptedPass);
		String pass = decrypt(algorithm,passBytes,privateKeyFile);
		return pass;
	    }
	} catch (Exception e) {
	    throw new Exception(e);
	}
    }

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
		String pass = new String(decode(encodedPass));
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
     * @return password as plain text
     * @throws java.lang.Exception
     */

    public static String getPassword(AdapterConfiguration config, String PasswordPropertyName) throws Exception {
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
	    throw new Exception(e);
	}
    }
}

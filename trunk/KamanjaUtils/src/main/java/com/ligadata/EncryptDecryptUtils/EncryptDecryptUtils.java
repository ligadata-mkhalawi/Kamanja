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

package com.ligadata.Utils;

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

import org.apache.commons.codec.binary.Base64;

/**
 * @author 
 * 
 */
public class EncryptDecryptUtils {
    static String loggerName = "EncryptDecryptUtils";
    static Logger logger = LogManager.getLogger(loggerName);

    /**
     * String to hold name of the encryption algorithm.
     */
    public static final String ALGORITHM = "RSA";

    /**
     * String to hold the name of the private key file.
     */
    public static final String PRIVATE_KEY_FILE = "/tmp/keys/private.key";

    /**
     * String to hold name of the public key file.
     */
    public static final String PUBLIC_KEY_FILE = "/tmp/keys/public.key";

    public static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * The method checks if the pair of public and private key has been generated.
     * @param publicKeyFile
     *          :The file containing public key
     * @param privateKeyFile
     *          :The file containing private key
     * 
     * @return flag indicating if the pair of keys were generated.
     */
    private static boolean areKeysPresent(String publicKeyFile,String privateKeyFile) {
	try {
	    File privateKey = new File(privateKeyFile);
	    File publicKey = new File(publicKeyFile);
	    if (privateKey.exists() && publicKey.exists()) {
		return true;
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return false;
    }

    /**
     * Generate key which contains a pair of private and public key using 1024
     * bytes. Store the set of keys in given files publicKeyFile,privateKeyFile
     * @param algorithm
     *          : algorithm used
     * @param publicKeyFile
     *          :The file containing public key
     * @param privateKeyFile
     *          :The file containing private key
     * 
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static void generateSampleKeys(String algorithm,String publicKeyFile,String privateKeyFile) {
	try {
	    if( areKeysPresent(publicKeyFile,privateKeyFile) ){
		return;
	    }
	    final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
	    keyGen.initialize(1024);
	    final KeyPair key = keyGen.generateKeyPair();

	    File privateKeyFD = new File(privateKeyFile);
	    File publicKeyFD = new File(publicKeyFile);

	    // Create files to store public and private key
	    if (privateKeyFD.getParentFile() != null) {
		privateKeyFD.getParentFile().mkdirs();
	    }
	    privateKeyFD.createNewFile();

	    if (publicKeyFD.getParentFile() != null) {
		publicKeyFD.getParentFile().mkdirs();
	    }
	    publicKeyFD.createNewFile();

	    // Saving the Public key in a file
	    ObjectOutputStream publicKeyOS = new ObjectOutputStream(
								    new FileOutputStream(publicKeyFD));
	    publicKeyOS.writeObject(key.getPublic());
	    publicKeyOS.close();

	    // Saving the Private key in a file
	    ObjectOutputStream privateKeyOS = new ObjectOutputStream(
								     new FileOutputStream(privateKeyFD));
	    privateKeyOS.writeObject(key.getPrivate());
	    privateKeyOS.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Generate key which contains a pair of private and public key using 1024
     * bytes. Store the set of keys in Prvate.key and Public.key files.
     * @param algorithm
     *          : algorithm used
     * @param publicKeyFile
     *          :The file containing public key
     * @param privateKeyFile
     *          :The file containing private key
     * 
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static void generateSampleKeys() {
	generateSampleKeys(ALGORITHM,PUBLIC_KEY_FILE,PRIVATE_KEY_FILE);
    }

    /**
     * Convert given byte array to a hex string
     * 
     * @param bytes
     *          : an array of bytes
     * @return String
     * @throws java.lang.Exception
     */


    public static String bytesToHex(byte[] bytes) {
	try{
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
		int v = bytes[j] & 0xFF;
		hexChars[j * 2] = hexArray[v >>> 4];
		hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	} catch (Exception e) {
	    logger.error("Failed to convert hexString to byte array",e);
	    throw e;
	}
    }

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
     * Encode(Base64 encoding) given byte array to a string
     * 
     * @param bytes
     *          : an array of bytes corresponding to string being encoded
     * @return String
     * @throws java.lang.Exception
     */


    public static String encode(byte[] bytes) {
	try{
	    Base64 base64 = new Base64();
	    String encoded = new String(base64.encode(bytes));
	    return encoded;
	} catch (Exception e) {
	    logger.error("Failed to encode a  byte array",e);
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
	    Base64 base64 = new Base64();
	    byte[] decoded = base64.decode(s.getBytes());
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
     *          : the encoded value as array of bytes 
     * @return decoded value as a String
     * @throws java.lang.Exception
     */

    public static String decode(byte[] bytes) throws Exception{
	try{
	    Base64 base64 = new Base64();
	    String decoded = new String(base64.decode(bytes));
	    return decoded;
	} catch (Exception e) {
	    logger.error("Failed to decode a byte array",e);
	    throw e;
	}
    }

    /**
     * Encrypt the plain text using public key.
     * 
     * @param algorithm
     *          : algorithm used
     * @param text
     *          : original plain text
     * @param keyFile
     *          :The public key file
     * @return Encrypted text
     * @throws java.lang.Exception
     */
    public static byte[] encrypt(String algorithm,String text, String publicKeyFile) throws Exception {
	byte[] cipherText = null;
	try {
	    ObjectInputStream inputStream = null;
	    // Encrypt the string using the public key
	    inputStream = new ObjectInputStream(new FileInputStream(publicKeyFile));
	    final PublicKey publicKey = (PublicKey) inputStream.readObject();

	    // get a cipher object and print the provider
	    final Cipher cipher = Cipher.getInstance(algorithm);
	    // encrypt the plain text using the public key
	    cipher.init(Cipher.ENCRYPT_MODE, publicKey);
	    cipherText = cipher.doFinal(text.getBytes());
	} catch (Exception e) {
	    logger.error("Failed to encrypt given password",e);
	    throw e;
	}
	return cipherText;
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

    public static String getDecryptedPassword(String encryptedEncodedPass, String privateKeyFile,String encryptDecryptAlgorithm) throws Exception {
	try{
	    // use the enrypted password if available
	    if (encryptedEncodedPass == null ){
		throw new Exception("The parameter  encryptedEncodedPass is null ");
	    }
	    if ( privateKeyFile == null ){
		throw new Exception("The parameter privateKeyFile is null ");
	    }
	    if ( encryptDecryptAlgorithm == null ){
		throw new Exception("The parameter encryptDecryptAlgorithm is null ");
	    }
	    logger.info("algorithm      => " + encryptDecryptAlgorithm);
	    logger.info("encryptedPass  => " + encryptedEncodedPass);
	    logger.info("privateKeyFile => " + privateKeyFile);
	    byte[] decodedBytes = decode(encryptedEncodedPass);
	    String pass = decrypt(encryptDecryptAlgorithm,decodedBytes, privateKeyFile);
	    return pass;
	} catch (Exception e) {
	    throw new Exception(e);
	}
    }

    /**
     *  Get the original password from base64-encoded password
     * 
     * @param encodedPassword
     *          : Base64 Encoded Password as a String
     * @return password as plain text
     * @throws java.lang.Exception
     */

    public static String getDecodedPassword(String encodedPass) throws Exception {
	try{
	    // use the enrypted password if available
	    if (encodedPass == null ){
		throw new Exception("The parameter encodedPass is null ");
	    }
	    logger.info("encodedPass  => " + encodedPass);
	    String pass = new String(decode(encodedPass));
	    return pass;
	} catch (Exception e) {
	    throw new Exception(e);
	}
    }
}

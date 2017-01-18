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
import org.apache.commons.cli.*;
import org.apache.logging.log4j.*;

public class GenerateKeys {
    static String loggerName = "GenerateKeys";
    static Logger logger = LogManager.getLogger(loggerName);

    private static boolean help = false;
    private static boolean generateSampleKeys = false;
    private static String password = null;
    private static String algorithm = null;
    private static String publicKeyFile = null;
    private static String privateKeyFile = null;

    private static void PrintUsage() {
	System.out.println("Available commands:");
	System.out.println("    --help");
	System.out.println("    --generateSampleKeys, optional, generate key files for testing");
	System.out.println("    --algorithm <encryptionAlgorithm>");
	System.out.println("    --password <textpassword>");
	System.out.println("    --publicKeyFile <FileNameContainingPublicKey>");
	System.out.println("    --privateKeyFile, optional, used to validate the encrypted value  <FileNameContainingPrivateKey>");
    }

    private static int generateKeys() {
	if( help ){
	    PrintUsage();
	    return -1;
	}

	if (algorithm == null) {
	    System.out.println("Need algorithm as parameter");
	    PrintUsage();
	    return -1;
	}

	if (password == null) {
	    if( ! generateSampleKeys ){
		System.out.println("Need password as parameter");
		PrintUsage();
		return -1;
	    }
	}


	if (publicKeyFile == null) {
	    if( ! generateSampleKeys ){
		System.out.println("Need public key file as parameter");
		PrintUsage();
		return -1;
	    }
	}

	if (privateKeyFile == null) {
	    if( ! generateSampleKeys ){
		System.out.println("Need private key file as parameter");
		PrintUsage();
		return -1;
	    }
	}

	try{
	    if( generateSampleKeys ){
		if( publicKeyFile == null ){
		    publicKeyFile = "public.key";
		}
		if( privateKeyFile == null ){
		    privateKeyFile = "private.key";
		}
		EncryptDecryptUtils.generateSampleKeys(algorithm,publicKeyFile,privateKeyFile);
		return 0;
	    }
	    else{
		byte[] cipherText = EncryptDecryptUtils.encrypt(algorithm,password, publicKeyFile);
		String encodedStr = EncryptDecryptUtils.encode(cipherText);
		System.out.println("Encrypted Password => " + encodedStr);

		if( privateKeyFile != null ){
		    byte[] decodedBytes = EncryptDecryptUtils.decode(encodedStr);
		    String pass = EncryptDecryptUtils.decrypt(algorithm,decodedBytes, privateKeyFile);
		    System.out.println("Decrypted Password => " + pass);
		}
		return 0;
	    }
	} catch (Exception e) {
	    logger.error("Failed to encrypt given password",e);
	    return -1;
	}
    }

    public static void main(String[] args) {
	CommandLine commandLine;
	if (args.length == 0) {
	    PrintUsage();
	    System.exit(-1);
	}

	Option o_help = new Option("help", "The help option");
	Option o_generateSampleKeys = new Option("generateSampleKeys", "a flag to generate sample keys");
	Option o_algorithm = OptionBuilder.withArgName("algorithm").hasArg().withDescription("The encryption algorithm").create("algorithm");
	Option o_password = OptionBuilder.withArgName("password").hasArg().withDescription("ascii password").create("password");
	Option o_publicKeyFile = OptionBuilder.withArgName("publicKeyFile").hasArg().withDescription("File containing public key").create("publicKeyFile");
	Option o_privateKeyFile = OptionBuilder.withArgName("privateKeyFile").hasArg().withDescription("File containing private key").create("privateKeyFile");
	Options options = new Options();
	CommandLineParser parser = new GnuParser();

	options.addOption(o_help);
	options.addOption(o_generateSampleKeys);
	options.addOption(o_algorithm);
	options.addOption(o_password);
	options.addOption(o_publicKeyFile);
	options.addOption(o_privateKeyFile);

	try{
	    commandLine = parser.parse(options, args);
	    if (commandLine.hasOption("algorithm")) {
		logger.info("Option algorithm is present.  The value is: ");
		algorithm = commandLine.getOptionValue("algorithm");
		logger.info(algorithm);
	    }
	    if (commandLine.hasOption("password")) {
		logger.info("Option password is present.  The value is: ");
		password = commandLine.getOptionValue("password");
		logger.info(password);
	    }
	    if (commandLine.hasOption("publicKeyFile")) {
		logger.info("Option publicKeyFile is present.  The value is: ");
		publicKeyFile = commandLine.getOptionValue("publicKeyFile");
		logger.info(publicKeyFile);
	    }
	    if (commandLine.hasOption("privateKeyFile")) {
		logger.info("Option privateKeyFile is present.  The value is: ");
		privateKeyFile = commandLine.getOptionValue("privateKeyFile");
		logger.info(privateKeyFile);

	    }
	    if (commandLine.hasOption("help")){
		logger.info("Option help is present.  This is a flag option.");
		help = true;
	    }
	    if (commandLine.hasOption("generateSampleKeys")){
		logger.info("Option generateSampleKeys is present.  This is a flag option.");
		generateSampleKeys = true;
	    }
	    String[] remainder = commandLine.getArgs();
	    for (String argument : remainder){
		logger.info(argument);
		logger.info(" ");
	    }
	    int rc = generateKeys();
	    System.exit(rc);
	}
	catch (ParseException exception){
	    System.out.print("Parse error: ");
	    System.out.println(exception.getMessage());
	    System.exit(-2);
	}
    }
}

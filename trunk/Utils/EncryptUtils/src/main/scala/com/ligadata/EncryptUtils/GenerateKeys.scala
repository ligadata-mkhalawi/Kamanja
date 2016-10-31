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

package com.ligadata.EncryptUtils;
import org.apache.logging.log4j._

class GenerateKeys {

  private val logger = LogManager.getLogger(getClass);
  private type OptionMap = Map[Symbol, Any]

  private def PrintUsage(): Unit = {
    logger.warn("Available commands:")
    logger.warn("    --help")
    logger.warn("    --generateSampleKeys, optional, generate key files for testing")
    logger.warn("    --algorithm <encryptionAlgorithm>")
    logger.warn("    --password <textpassword>")
    logger.warn("    --publicKeyFile <FileNameContainingPublicKey>")
    logger.warn("    --privateKeyFile, optional, used to validate the encrypted value  <FileNameContainingPrivateKey>")
  }


  private def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    list match {
      case Nil => map
      case "--help" :: tail =>
        nextOption(map ++ Map('help -> "true"), tail)
      case "--algorithm" :: value :: tail =>
        nextOption(map ++ Map('algorithm -> value), tail)
      case "--password" :: value :: tail =>
        nextOption(map ++ Map('password -> value), tail)
      case "--publicKeyFile" :: value :: tail =>
        nextOption(map ++ Map('publicKeyFile -> value), tail)
      case "--privateKeyFile" :: value :: tail =>
        nextOption(map ++ Map('privateKeyFile -> value), tail)
      case "--generateSampleKeys" :: tail =>
        nextOption(map ++ Map('generateSampleKeys -> "true"), tail)
      case option :: tail => {
        logger.error("Unknown option " + option)
        throw new Exception("Unknown option " + option)
      }
    }
  }

  def run(args: Array[String]): Int = {
    if (args.length == 0) {
      PrintUsage()
      return -1
    }

    val options = nextOption(Map(), args.toList)
    val pHelp = options.getOrElse('help, null)
    if( pHelp != null ){
      PrintUsage()
      return -1
    }

    var pAlgorithm:String = null
    val algorithm = options.getOrElse('algorithm, null)
    if (algorithm == null) {
      logger.error("Need algorithm as parameter")
      PrintUsage()
      return -1
    }
    else{
      pAlgorithm = algorithm.asInstanceOf[String]
    }

    val pGenerateSampleKeys = options.getOrElse('generateSampleKeys, null)

    var pPassword:String = null
    val password = options.getOrElse('password, null)
    if (password == null) {
      if( pGenerateSampleKeys == null ){
	logger.error("Need password as parameter")
	PrintUsage()
	return -1
      }
    }
    else{
      pPassword = password.asInstanceOf[String]
    }

    var keyFile = options.getOrElse('publicKeyFile, null)
    var publicKeyFile:String = null
    if (keyFile == null) {
      if( pGenerateSampleKeys == null ){
	logger.error("Need public key file as parameter")
	PrintUsage()
	return -1
      }
    }
    else{
      publicKeyFile = keyFile.asInstanceOf[String]
    }

    keyFile = options.getOrElse('privateKeyFile, null)
    var privateKeyFile:String = null
    if (keyFile == null) {
      logger.info("Need private key file as parameter to validate encrypted value")
    }
    else{
      privateKeyFile = keyFile.asInstanceOf[String]
    }
   
    try{
      if( pGenerateSampleKeys != null ){
	if( publicKeyFile == null ){
	  publicKeyFile = "public.key";
	}
	if( privateKeyFile == null ){
	  privateKeyFile = "private.key";
	}
	EncryptionUtil.generateSampleKeys(pAlgorithm,publicKeyFile,privateKeyFile);
	return 0;
      }
      else{
	val cipherText = EncryptionUtil.encrypt(pAlgorithm,pPassword, publicKeyFile);
	val encodedStr = EncryptionUtil.encode(cipherText);
	System.out.println("Encrypted Password => " + encodedStr);

	if( privateKeyFile != null ){
	  val decodedBytes = EncryptionUtil.decode(encodedStr);
	  val pass = EncryptionUtil.decrypt(pAlgorithm,decodedBytes, privateKeyFile);
	  System.out.println("Decrypted Password => " + pass);
	}
	logger.info("Encrypted Password => " + encodedStr);
	return 0
      }
    } catch {
      case e: Exception => throw new Exception("Failed to encrypt password", e)
    }
  }
}

object GenerateKeys {
  private val logger = LogManager.getLogger(getClass);

  def main(args: Array[String]): Unit = {
    val gk:GenerateKeys = new GenerateKeys
    sys.exit(gk.run(args))
  }
}

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

package com.ligadata.automation.unittests.encryptdecryptutils

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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.scalatest._
import Matchers._

import com.ligadata.Utils.EncryptDecryptUtils
import com.ligadata.Utils.GenerateKeys

import util.control.Breaks._
import scala.io._
import java.util.Date
import java.io._

import sys.process._
import org.apache.logging.log4j._

class EncryptDEcryptUtilsSpec extends FunSpec with BeforeAndAfter with BeforeAndAfterAll with GivenWhenThen {

  private val loggerName = this.getClass.getName
  private val logger = LogManager.getLogger(loggerName)

  override def beforeAll = {
    try {
      logger.info("starting...");
      logger.info("resource dir => " + getClass.getResource("/").getPath)
      // Method generates a pair of keys using the RSA algorithm and stores it
      // in their respective files
      EncryptDecryptUtils.generateSampleKeys();
    }
    catch {
      case e: Exception => throw new Exception("Failed to initialize properly", e)
    }
  }

  describe("Verify Encrypt/Decrypt Utils ") {

    ignore("Encrypt and decrypt a password using default key files use hex functions for ascii display..."){
      val originalText = "bofauser";
      
      // Encrypt the string using the public key
      val cipherText = EncryptDecryptUtils.encrypt(EncryptDecryptUtils.ALGORITHM,
					       originalText, 
					       EncryptDecryptUtils.PUBLIC_KEY_FILE);

      val hexStr = EncryptDecryptUtils.bytesToHex(cipherText);
      System.out.println(hexStr);

      val byteStr = EncryptDecryptUtils.hexStringToByteArray(hexStr);

      // Decrypt the cipher text using the private key.
      val  plainText = EncryptDecryptUtils.decrypt(EncryptDecryptUtils.ALGORITHM,
					       byteStr, 
					       EncryptDecryptUtils.PRIVATE_KEY_FILE);

      // Printing the Original, Encrypted and Decrypted Text
      logger.info("Original:  " + originalText);
      //System.out.println(EncryptDecryptUtils.bytesToHex(cipherText));
      logger.info("Decrypted: " + plainText);

      assert(originalText == plainText)
    }


    ignore("Encrypt and decrypt a password using default key files use encode/decode/hex functions for ascii display..."){
      val originalText = "bofauser";
      
      // Encrypt the string using the public key
      val cipherText = EncryptDecryptUtils.encrypt(EncryptDecryptUtils.ALGORITHM,
					       originalText, 
					       EncryptDecryptUtils.PUBLIC_KEY_FILE);
      System.out.println("cipherText => " + new String(cipherText));

      val hexStr = EncryptDecryptUtils.bytesToHex(cipherText);

      System.out.println("hexStr     => " + hexStr)
      val encodedStr = EncryptDecryptUtils.encode(hexStr.getBytes());
      System.out.println("enodedStr  => " + encodedStr);

      // Decrypt the cipher text using the private key.
      val decodedStr = EncryptDecryptUtils.decode(encodedStr.getBytes())
      System.out.println("decodedStr => " + decodedStr);

      val bytes = EncryptDecryptUtils.hexStringToByteArray(decodedStr);
      System.out.println("cipherText => " + new String(bytes));

      val  plainText = EncryptDecryptUtils.decrypt(EncryptDecryptUtils.ALGORITHM,
					       bytes, 
					       EncryptDecryptUtils.PRIVATE_KEY_FILE);

      // Printing the Original, Encrypted and Decrypted Text
      logger.info("Original:  " + originalText);
      //System.out.println(EncryptDecryptUtils.bytesToHex(cipherText));
      logger.info("Decrypted: " + plainText);

      assert(originalText == plainText);
    }

    it("Encrypt and decrypt a password using default key files use encode/decode functions for ascii display..."){
      val originalText = "bofauser";
      
      // Encrypt the string using the public key
      val cipherText = EncryptDecryptUtils.encrypt(EncryptDecryptUtils.ALGORITHM,
					       originalText, 
					       EncryptDecryptUtils.PUBLIC_KEY_FILE);
      System.out.println("cipherText => " + new String(cipherText));

      val encodedStr = EncryptDecryptUtils.encode(cipherText);
      System.out.println("enodedStr  => " + encodedStr);

      // Decrypt the cipher text using the private key.
      val decodedBytes = EncryptDecryptUtils.decode(encodedStr)
      System.out.println("decodedStr => " + new String(decodedBytes));

      val  plainText = EncryptDecryptUtils.decrypt(EncryptDecryptUtils.ALGORITHM,
					       decodedBytes, 
					       EncryptDecryptUtils.PRIVATE_KEY_FILE);

      // Printing the Original, Encrypted and Decrypted Text
      logger.info("Original:  " + originalText);
      //System.out.println(EncryptDecryptUtils.bytesToHex(cipherText));
      logger.info("Decrypted: " + plainText);

      assert(originalText == plainText);
    }

    ignore("Encrypt and decrypt a sample password using Base64 encoding and decoding..."){
      val originalText = "bofauser";
      
      // Encrypt the string 
      val cipherText = EncryptDecryptUtils.encode(originalText.getBytes());
      System.out.println(cipherText);

      // Decrypt the cipher text
      val  plainText = EncryptDecryptUtils.decode(cipherText.getBytes());

      // Printing the Original, Encrypted and Decrypted Text
      logger.info("Original:  " + originalText);
      //System.out.println(EncryptDecryptUtils.bytesToHex(cipherText));
      logger.info("Decrypted: " + plainText);

      assert(originalText == plainText);
    }
  }

  override def afterAll = {
  }
}

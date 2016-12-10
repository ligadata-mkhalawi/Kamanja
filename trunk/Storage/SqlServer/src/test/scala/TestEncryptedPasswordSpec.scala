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

package com.ligadata.automation.unittests.sqlserveradapter

import org.scalatest._
import Matchers._

import com.ligadata.Utils._
import util.control.Breaks._
import scala.io._
import java.util.{Date,Calendar,TimeZone}
import java.text.{SimpleDateFormat}
import java.io._

import sys.process._
import org.apache.logging.log4j._

import com.ligadata.keyvaluestore._
import com.ligadata.KvBase._
import com.ligadata.StorageBase._
import com.ligadata.Serialize._
import com.ligadata.Utils.Utils._
import com.ligadata.Utils.{ KamanjaClassLoader, KamanjaLoaderInfo }
import com.ligadata.StorageBase.StorageAdapterFactory
import com.ligadata.keyvaluestore.SqlServerAdapter

import com.ligadata.Exceptions._

import com.ligadata.Utils.EncryptDecryptUtils
import com.ligadata.Utils.GenerateKeys

class TestEncryptedPasswordSpec extends FunSpec with BeforeAndAfter with BeforeAndAfterAll with GivenWhenThen {
  var res : String = null;
  var statusCode: Int = -1;
  var adapter:DataStore = null
  var serializer:Serializer = null

  private val loggerName = this.getClass.getName
  private val logger = LogManager.getLogger(loggerName)

  val dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
  val dateFormat1 = new SimpleDateFormat("yyyy/MM/dd")
  // set the timezone to UTC for all time values
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  var containerName = ""
  private val kvManagerLoader = new KamanjaLoaderInfo
  private val maxConnectionAttempts = 10

  serializer = SerializerManager.GetSerializer("kryo")
  logger.info("Initialize SqlServerAdapter")
  var dataStoreInfo1 = """{"StoreType": "sqlserver","hostname": "192.168.56.1","instancename":"KAMANJA","portnumber":"1433","database": "bofa","user":"bofauser","SchemaName":"bofauser","encryptedEncodedPassword":"encryptedEncodedPasswordPlaceHolder","privateKeyFile":"privateKeyFilePlaceHolder","encryptDecryptAlgorithm":"RSA","jarpaths":"/media/home2/jdbc","jdbcJar":"sqljdbc4-2.0.jar","clusteredIndex":"YES","autoCreateTables":"YES","appendOnly":"YES"}"""

  var dataStoreInfo2 = """{"StoreType": "sqlserver","hostname": "192.168.56.1","instancename":"KAMANJA","portnumber":"1433","database": "bofa","user":"bofauser","SchemaName":"bofauser","encodedPassword":"encodedPasswordPlaceHolder","jarpaths":"/media/home2/jdbc","jdbcJar":"sqljdbc4-2.0.jar","clusteredIndex":"YES","autoCreateTables":"YES","appendOnly":"YES"}"""

  var dataStoreInfo3 = """{"StoreType": "sqlserver","hostname": "192.168.56.1","instancename":"KAMANJA","portnumber":"1433","database": "bofa","user":"bofauser","SchemaName":"bofauser","password":"passwordPlaceHolder","jarpaths":"/media/home2/jdbc","jdbcJar":"sqljdbc4-2.0.jar","clusteredIndex":"YES","autoCreateTables":"YES","appendOnly":"YES"}"""

  val publicKeyFile  = "public.key";
  val privateKeyFile = "private.key";

  private def CreateAdapter(dataStoreInfo:String): DataStore = {
    var connectionAttempts = 0
    while (connectionAttempts < maxConnectionAttempts) {
      try {
        adapter = SqlServerAdapter.CreateStorageAdapter(kvManagerLoader, dataStoreInfo, null, null)
        return adapter
      } catch {
        case e: Exception => {
          logger.error("will retry after one minute ...", e)
          Thread.sleep(60 * 1000L)
          connectionAttempts = connectionAttempts + 1
        }
      }
    }
    return null;
  }

  override def beforeAll = {
    logger.info("starting...");
  }


  def deleteFile(path:File):Unit = {
    if(path.exists()){
      if (path.isDirectory){
	for(f <- path.listFiles) {
          deleteFile(f)
	}
      }
      path.delete()
    }
  }

  describe("Create sqlserver adapter using encrypted/encoded password") {
    it ("Create an adapter Using encryptedEncoded password") {
      // Create new public/private key files every time
      val myTextPassword = "bofauser";
      val algorithm = "RSA";
      EncryptDecryptUtils.generateSampleKeys(algorithm,publicKeyFile,privateKeyFile);
      val cipherText = EncryptDecryptUtils.encrypt(algorithm,myTextPassword, publicKeyFile);
      val encodedStr = EncryptDecryptUtils.encode(cipherText);
      System.out.println("Encrypted.Encoded Password => " + encodedStr);
      val decodedBytes = EncryptDecryptUtils.decode(encodedStr);
      val pass = EncryptDecryptUtils.decrypt(algorithm,decodedBytes, privateKeyFile);
      System.out.println("Decrypted Password => " + pass);

      dataStoreInfo1 = dataStoreInfo1.replaceAll("encryptedEncodedPasswordPlaceHolder",encodedStr);
      dataStoreInfo1 = dataStoreInfo1.replaceAll("privateKeyFilePlaceHolder",privateKeyFile);

      System.out.println("AdapterConfig => " + dataStoreInfo1);
      CreateAdapter(dataStoreInfo1);
    }
    it ("Create an adapter Using Encoded password") {
      val myTextPassword = "bofauser";
      val encodedStr = EncryptDecryptUtils.encode(myTextPassword.getBytes());
      System.out.println("Encoded Password => " + encodedStr);
      val pass = EncryptDecryptUtils.getDecodedPassword(encodedStr);
      System.out.println("Decoded Password => " + pass);

      dataStoreInfo2 = dataStoreInfo2.replaceAll("encodedPasswordPlaceHolder",encodedStr);

      System.out.println("AdapterConfig => " + dataStoreInfo2);
      CreateAdapter(dataStoreInfo2);
    }
    it ("Create an adapter Using Text password") {
      val myTextPassword = "bofauser";
      dataStoreInfo3 = dataStoreInfo3.replaceAll("passwordPlaceHolder",myTextPassword);
      System.out.println("AdapterConfig => " + dataStoreInfo3);
      CreateAdapter(dataStoreInfo3);
    }
  }
  override def afterAll = {
    var logFile = new java.io.File("logs")
    if( logFile != null ){
      deleteFile(logFile)
    }
    logFile = new java.io.File(publicKeyFile)
    if( logFile != null ){
      deleteFile(logFile)
    }
    logFile = new java.io.File(privateKeyFile)
    if( logFile != null ){
      deleteFile(logFile)
    }
  }
}

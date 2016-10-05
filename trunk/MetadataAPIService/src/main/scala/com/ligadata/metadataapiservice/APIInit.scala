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

package com.ligadata.metadataapiservice

import akka.actor.{ActorSystem, Props}
import akka.actor.ActorDSL._
import akka.event.Logging
import akka.io.IO
import akka.io.Tcp._
import spray.can.Http

import com.ligadata.kamanja.metadata.ObjType._
import com.ligadata.kamanja.metadata._
import com.ligadata.kamanja.metadataload.MetadataLoad
import com.ligadata.MetadataAPI._
import org.apache.logging.log4j._
import com.ligadata.Utils._

object APIInit {
  val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  var databaseOpen = false
  var configFile:String = _
  private[this] val lock = new Object()
  // 646 - 676 Change begins - replace MetadataAPIImpl
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
  // 646 - 676 Change ends


  def Shutdown(exitCode: Int): Unit = lock.synchronized{
    if( databaseOpen ){
      MetadataAPIImpl.shutdown
      getMetadataAPI.CloseDbStore
      databaseOpen = false
    }
    MetadataAPIServiceLeader.Shutdown
  }

  def SetConfigFile(cfgFile:String): Unit = {
    configFile = cfgFile
  }

  def SetDbOpen : Unit = {
    databaseOpen = true
  }

  def InitLeaderLatch: Unit = {
      // start Leader detection component
      val nodeId     = getMetadataAPI.GetMetadataAPIConfig.getProperty("NODE_ID")
      val zkNode     = getMetadataAPI.GetMetadataAPIConfig.getProperty("API_LEADER_SELECTION_ZK_NODE")  + "/metadataleader"
      val zkConnStr  = getMetadataAPI.GetMetadataAPIConfig.getProperty("ZOOKEEPER_CONNECT_STRING")
      val sesTimeOut = getMetadataAPI.GetMetadataAPIConfig.getProperty("ZK_SESSION_TIMEOUT_MS").toInt
      val conTimeOut = getMetadataAPI.GetMetadataAPIConfig.getProperty("ZK_CONNECTION_TIMEOUT_MS").toInt

      MetadataAPIServiceLeader.Init(nodeId,zkConnStr,zkNode,sesTimeOut,conTimeOut)
  }

  def Init : Unit = {
    try{
      // Open db connection
      //getMetadataAPI.InitMdMgrFromBootStrap(configFile)
      //databaseOpen = true

      // start Leader detection component
      logger.debug("Initialize Leader Latch")
      InitLeaderLatch
    } catch {
      case e: Exception => {
           logger.error("", e)
      }
    }
  }
}

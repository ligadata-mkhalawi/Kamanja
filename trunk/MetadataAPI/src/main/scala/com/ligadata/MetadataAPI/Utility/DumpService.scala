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

package com.ligadata.MetadataAPI.Utility

import com.ligadata.MetadataAPI.MetadataAPIImpl
import org.apache.logging.log4j._
import com.ligadata.kamanja.metadata.MdMgr
/**
 * Created by dhaval on 8/13/15.
 */
object DumpService {
  private val userid: Option[String] = Some("kamanja")
  val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  // 646 - 676 Change begins - replace MetadataAPIImpl
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
  // 646 - 676 Chagne ends

  def dumpMetadata: String ={
    var response=""
    try{
      MdMgr.GetMdMgr.dump
      response="Metadata dumped in DEBUG mode"
    }catch{
      case e: Exception => {
        logger.warn("", e)
        response=e.getStackTrace.toString
      }
    }
    response
  }
  def dumpAllNodes: String ={
    var response=""
    try{
      response=getMetadataAPI.GetAllNodes("JSON", userid)
    }
    catch {
      case e: Exception => {
        logger.warn("", e)
        response=e.getStackTrace.toString
      }
    }
    response
  }
  def dumpAllClusters: String ={
    var response=""
    try{
      response=getMetadataAPI.GetAllClusters("JSON", userid)
    }
    catch {
      case e: Exception => {
        logger.warn("", e)
        response=e.getStackTrace.toString
      }
    }
    response
  }
  def dumpAllClusterCfgs: String ={
    var response=""
    try{
      response=getMetadataAPI.GetAllClusterCfgs("JSON", userid)
    }
    catch {
      case e: Exception => {
        logger.warn("", e)
        response=e.getStackTrace.toString
      }
    }
    response
  }
  def dumpAllAdapters: String ={
    var response=""
    try{
      response=getMetadataAPI.GetAllAdapters("JSON", userid)
    }
    catch {
      case e: Exception => {
        logger.warn("", e)
        response=e.getStackTrace.toString
      }
    }
    response
  }
}

/*
 * Copyright 2016 ligaDATA
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
package com.ligadata.jtm

import java.io.File

import com.ligadata.kamanja.metadata.MdMgr
import com.ligadata.kamanja.metadataload.MetadataLoad
import org.apache.commons.io.FileUtils
import org.json4s.jackson.JsonMethods._
import org.rogach.scallop.ScallopConf
import scala.io.Source
import com.ligadata.msgcompiler._

/*
run --in /home/joerg/Kamanja/trunk/GenerateModels/jtm/src/test/resources/metadata/messages/msg1.json   --out /home/joerg/msg1.scala
run --in /home/joerg/Kamanja/trunk/GenerateModels/jtm/src/test/resources/metadata/messages/msg2.json   --out /home/joerg/msg2.scala
run --in /home/joerg/Kamanja/trunk/GenerateModels/jtm/src/test/resources/metadata/messages/msg3.json   --out /home/joerg/msg3.scala
run --in /home/joerg/Kamanja/trunk/GenerateModels/jtm/src/test/resources/metadata/messages/TransactionMsg.json   --out ~/MsgOut.scala
run --in /home/joerg/Kamanja/trunk/GenerateModels/jtm/src/test/resources/metadata/messages/TransactionMsgIn.json   --out ~/MsgIn.scala
run --in /home/joerg/Kamanja/trunk/GenerateModels/jtm/src/test/resources/metadata/messages/hl7_Medical.json --out /tmp/hl7.scala
 */
object MsgCompiler extends App with LogTrait {

  class ConfMsgCompiler (arguments: Seq[String] ) extends ScallopConf (arguments)  with LogTrait {

    val in = opt[String] (required = true, descr = "Directory/File with json to compiler", default = Option("/home/joerg/Kamanja/trunk/GenerateModels/jtm/src/test/resources/metadata"))
    val out = opt[String] (required = true, descr = "Direcory/File where to place the output", default = Option("/home/joerg/Kamanja/trunk/GenerateModels/jtm/src/test/scala/com/ligadata/jtm/test/compile"))
    val all = opt[Boolean] (required = false, descr = "true: traversedirectory false: single file", default = Option(true))
  }

  override def main (args: Array[String] ) {

    try {
      val cmdconf = new ConfMsgCompiler(args)

      val mgr : MdMgr = MdMgr.GetMdMgr

      // If the compiler is called again this will throw
      // To Do: move the metadata and improve handling
      try {
        val mdLoader = new MetadataLoad(mgr, "", "", "", "")
        mdLoader.initialize
      } catch {
        case _ : Throwable => ;
      }

      if(cmdconf.all.get.get) {

        val files = getRecursiveListOfFiles(new File(cmdconf.in.get.get))

        // Load all json files for the metadata directory
        files.map ( jsonFile => {
          val fn = jsonFile.getName.replaceAll(".json", ".scala")
          val json = FileUtils.readFileToString(jsonFile, null:String)
          val map = parse(json).values.asInstanceOf[Map[String, Any]]
          val msg = new MessageCompiler()
          val ((classStrVer, classStrVerJava), msgDef, (classStrNoVer, classStrNoVerJava), rawMsgStr) = msg.processMsgDef(json, "JSON", mgr, 0, null, false)
          val msg1 = msgDef.asInstanceOf[com.ligadata.kamanja.metadata.MessageDef]
          mgr.AddMsg(msg1)
          val fo = new File(cmdconf.out.get.get, fn)
          logger.info("Output to {}", fo.getPath)
          FileUtils.writeStringToFile(fo, classStrVer)
        })

      } else {

        val json = FileUtils.readFileToString(new File(cmdconf.in.get.get))
        val map = parse(json).values.asInstanceOf[Map[String, Any]]
        val msg = new MessageCompiler
        val ((classStrVer, classStrVerJava), msgDef, (classStrNoVer, classStrNoVerJava), rawMsgStr) = msg.processMsgDef(json, "JSON", mgr, 0, null, false)
        val msg1 = msgDef.asInstanceOf[com.ligadata.kamanja.metadata.MessageDef]
        mgr.AddMsg(msg1)
        FileUtils.writeStringToFile(new File(cmdconf.out.get.get), classStrVer)

      }
    }
    catch {
      case e: Exception => {
        logger.error("Exception {}", e.getMessage)
        System.exit(1)
      }
    }
  }
}

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
import java.nio.file.Path
import org.apache.commons.io.FileUtils
import org.rogach.scallop.ScallopConf
import scala.io.Source
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.Files.copy
import java.nio.file.Paths.get

object UpdateExpects extends App with LogTrait {

  class ConfUpdateExpects (arguments: Seq[String] ) extends ScallopConf (arguments)  with LogTrait {

    val src = opt[String] (required = false, descr = "", default = Option("/home/joerg/Kamanja_Jtm/trunk/GenerateModels/jtm/target/scala-2.10/test-classes") )
    val target = opt[String] (required = false, descr = "", default = Option("/home/joerg/Kamanja_Jtm/trunk/GenerateModels/jtm/src/test/resources") )
  }

  override def main(args: Array[String]) {

    //    try {
    val cmdconf = new ConfUpdateExpects(args)

    def getRecursiveListOfFiles(dir: File): Array[File] = {
      val these = dir.listFiles.filter(_.isFile)
      val those = dir.listFiles.filter(_.isDirectory)
      these ++ those.flatMap(getRecursiveListOfFiles)
    }

    def PathToComponents(p1: Path): Array[Path] = {
      def split(p1: Path): Array[Path] = {
        if (p1.getParent == null)
          return Array.empty[Path]
        Array(p1) ++ split(p1.getParent)
      }
      split(p1).reverse
    }

    def CommonPath(p1: Path, p2: Path): Path = {

      val c1 = PathToComponents(p1)
      val c2 = PathToComponents(p2)
      val c = math.min(c1.length, c2.length)

      //logger.error("C1 {}", c.toString)

      for (i <- 0 to c - 1) {
        //logger.error("C1 {}", c1(i).toString)
        //logger.error("C2 {}", c2(i).toString)

        if (c1(i) != c2(i)) {
          if (i > 0)
            return c1(i - 1)
          else
            new File("/").toPath
        }
      }

      c2(c - 1)
    }

    def RelPath(common: Path, full: Path): Path = {
      val c1 = common.toString
      val c2 = full.toString
      new File(c2.substring(c1.length)).toPath
    }

    // Created expects
    //
    val basepath: String = cmdconf.src.get.get
    val base: File = new File(basepath)

    // Original expects
    //
    val targetpath: String = cmdconf.target.get.get
    val target: File = new File(targetpath)

    logger.info(basepath)
    logger.info(targetpath)

    // is path?
    if (!base.isDirectory)
      throw new Exception(s"Path $basepath is not a directory ")

    // Get all the files we have
    val files = getRecursiveListOfFiles(base)

    // Load all json files for the metadata directory
    val toUpdate = files.foldLeft(Map.empty[String, String])((r, file) => {

      val cf = file.getCanonicalFile
      val p2 = cf.toPath
      val cp = CommonPath(p2, base.toPath)
      val rel = RelPath(cp, p2)
      val name = cf.getName
      val index = name.indexOf('.')
      val ext = if (index > 0) name.substring(index) else ""

      //logger.error("File Ext {}", ext)

      if (ext == ".scala.actual") {
        r ++ Map(cf.toString -> new File(target.toString, rel.toString).toString.replaceAll(".scala.actual", ".scala.expected")  )
      } else {
        r
      }
    })

    implicit def toPath(filename: String) = get(filename)

    toUpdate.foreach(m => {
      logger.info("cp {} {}", m._1, m._2)
      copy(m._1, m._2, REPLACE_EXISTING)
    })
  }
}

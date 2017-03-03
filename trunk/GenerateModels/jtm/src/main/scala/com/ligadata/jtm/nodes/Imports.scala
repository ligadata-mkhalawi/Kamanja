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
package com.ligadata.jtm.nodes

import com.google.gson.annotations.SerializedName

/**
  *
  */
class Imports {

  /** List with package names
    *
    */
  val packages: Array[String] = Array.empty[String]

  /** List of code lines to be added to the package
    * the function names should not conflict with any generated
    * names
    *
    * Code always comes after packages in the generated scala file
    */
  val packagecode: Array[String] = Array.empty[String]

  /** List of code lines to be added to the model class
    * the function names should not conflict with any generated
    * names
    *
    * Code always comes after packages in the generated scala file
    */
  val modelcode: Array[String] = Array.empty[String]

  /** List of code lines to be added to the factory class
    * the function names should not conflict with any generated
    * names
    *
    * Code always comes after packages in the generated scala file
    */
  val factorycode: Array[String] = Array.empty[String]

  /**
    * map of message/container type with its alias
    * these types will be resolved and imported
    */
  val types : scala.collection.Map[String, String] = scala.collection.Map.empty[String, String]

}

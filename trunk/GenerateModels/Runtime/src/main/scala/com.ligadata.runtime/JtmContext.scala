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
package com.ligadata.runtime

class JtmContext
{
  case class ErrorEntry(errorDescription: String, additionalInfo: String)

  // Add error to the current scope
  def AddError(error: String) = {
    collection(current_section)(current_scope) :+ error
  }

  // Set's the current section
  def SetSection(section: String) = {
    current_section = section
    current_scope = ""
  }

  // Set's the current input
  def SetScope(scope: String) = {
    current_scope = scope
  }

  // Returns the error in the list
  def Errors(): Int = {
    collection.foldLeft(0) ((r, section) => {
      section._2.foldLeft(r) ((k, scope) => {
        k + scope._2.length
      })
    })
  }

  def CurrentErrors(): Int = {
    if(collection.contains(current_section)) {
      val section = collection(current_section)
      if(section.contains(current_scope)) {
        section(current_scope).length
      }
    }
    0
  }

  def Reset() = {
    collection = Map.empty[String, Map[String, Array[String]]]
  }

  var current_section: String = ""
  var current_scope: String = ""
  var message: Array[String] = Array.empty[String]
  var collection: Map[String, Map[String, Array[String]]] = Map.empty[String, Map[String, Array[String]]]

}

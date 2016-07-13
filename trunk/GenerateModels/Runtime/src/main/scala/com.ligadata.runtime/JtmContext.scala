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

  }

  // Set's the current section
  def SetSection(section: String) = {

  }

  // Set's the current input
  def SetScope(section: String) = {

  }

  // Returns the error in the list
  def Errors(): Int = {
    collection.size
  }

  def CurrentErrors(): Int = {
    0
  }

  def Reset() = {
    collection = Map.empty[String, Map[String, Array[ErrorEntry]]]
  }

  var current_section: String = ""
  var current_input: String = ""
  var message: Array[String] = Array.empty[String]
  var collection: Map[String, Map[String, Array[ErrorEntry]]] = Map.empty[String, Map[String, Array[ErrorEntry]]]

}

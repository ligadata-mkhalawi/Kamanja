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
  private def updateCollection() = {
    if(collection.contains(current_section))
      collection(current_section) ++= scala.collection.mutable.Map(current_scope -> CurrentErrorList())
    else
      collection ++= scala.collection.mutable.Map(current_section -> scala.collection.mutable.Map(current_scope -> CurrentErrorList()))
  }

  // Add error to the current section/scope
  def AddError(error: String) = {

    if(current_section.isEmpty)
      throw new Exception("The section can't be empty when you add errors")

    if(current_scope.isEmpty)
      currentSectionErrors :+= error
    else
      currentScopeErrors :+= error

    updateCollection()
  }

  // Set's the current section
  def SetSection(section: String) = {

    if(section.isEmpty)
      throw new Exception("The section can't be empty")

    if(Errors(section, "")>0)
      throw new Exception("Can't open the same section a second time")

    current_section = section
    current_scope = ""
    currentSectionErrors = Array.empty[String]
    currentScopeErrors = Array.empty[String]
  }

  // Set's the current scope
  def SetScope(scope: String) = {

    if(scope.isEmpty)
      throw new Exception("The scope can't be empty")

    if(Errors(current_section, scope)>0)
      throw new Exception("Can't open the same scope a second time")

    current_scope = scope
    currentScopeErrors ++= currentSectionErrors

    updateCollection()
  }

  def CurrentErrors(): Int = {
    if(current_scope.isEmpty)
      currentSectionErrors.length
    else
      currentScopeErrors.length
  }

  def CurrentErrorList(): Array[String] = {
    if(current_scope.isEmpty)
      currentSectionErrors
    else
      currentScopeErrors
  }

  def ScopeErrors(scope: String): Int = {
    Errors(current_section, scope)
  }

  def Errors(section: String, scope: String ): Int = {
    if(collection.contains(section) && collection(section).contains(scope)) {
      collection(section)(scope).length
    } else {
      0
    }
  }

  def ScopeErrorList(scope: String): Array[String] = {
    ErrorList(current_section, scope)
  }

  def ErrorList(section: String, scope: String): Array[String] = {
    if(collection.contains(section) && collection(section).contains(scope)) {
      collection(section)(scope)
    } else {
      Array.empty[String]
    }
  }

  def Reset() = {
    collection = scala.collection.mutable.Map.empty[String, scala.collection.mutable.Map[String, Array[String]]]
    currentSectionErrors  = Array.empty[String]
    currentScopeErrors = Array.empty[String]
  }

  var current_section: String = ""
  var current_scope: String = ""
  var currentSectionErrors : Array[String] = Array.empty[String]
  var currentScopeErrors : Array[String] = Array.empty[String]
  var collection: scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, Array[String]]] = scala.collection.mutable.Map.empty[String, scala.collection.mutable.Map[String, Array[String]]]

}

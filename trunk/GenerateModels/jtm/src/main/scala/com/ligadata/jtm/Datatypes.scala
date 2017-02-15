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

object Datatypes
{
  def isStringArray(typeName: String) : Boolean = {
    if(typeName=="Array[String]" || typeName=="System.ArrayOfString")
      true
    else
      false
  }

  def isStringDictionary(typeName: String) : Boolean = {
    if(typeName=="Map[String]" || typeName=="System.MapOfString")
      true
    else
      false
  }

  def isNumericType(typ : String) = typ.toLowerCase match{
    case "int" => true
    case "integer" => true
    case "long" => true
    case "float" => true
    case "double" => true
    case "byte" => true
    case "short" => true
    case "char" => true
    case _ => false
  }
  def isStringType(typ : String) = typ.toLowerCase match{
    case "string" => true
    case _ => false
  }
  def isBooleanType(typ : String) = typ.toLowerCase match{
    case "boolean" => true
    case _ => false
  }
  def getTypeDefaultVal(typ : String): String ={
    if(isNumericType(typ)) "0"
    else if(isBooleanType(typ)) "false"
    else if(isStringType(typ)) "\"\""
    else "null"
  }
}

class Datatypes
{

}

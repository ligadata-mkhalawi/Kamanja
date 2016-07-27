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

import java.sql.Timestamp
import java.util.Date
import scala.math.Ordering.BigIntOrdering

/*
 jtm locations

 // Each step can cause an error
 // track back to the originating instruction
 transformation -> grok instance
 transformation -> [dependency set] -> compute
 transformation -> [dependency set] -> output -> compute|where|mapping

a) string
b) id -> to map

*/
object Conversion {

  // Source -> Map[Target, Function]  // Source -> Map[Target, Function]
  val builtin: Map[String, Map[String, String]] = Map(
    "Int" -> Map("String" -> "ToString"),
    "Double" -> Map("String" -> "ToString"),
    "Boolean" -> Map("String" -> "ToString"),
    "Date" -> Map("String" -> "ToString"),
    "BigDecimal" -> Map("String" -> "ToString"),
    "Long" -> Map("String" -> "ToString"),
    "Char" -> Map("String" -> "ToString"),
    "Float" -> Map("String" -> "ToString"),
    "String" -> Map("Int" -> "ToInteger",
      "Char" -> "ToChar",
      "Double" -> "ToDouble",
      "Boolean" -> "ToBoolean",
      "Date" -> "ToDate",
      "Timestamp" -> "ToTimestamp",
      "BigDecimal" -> "ToBigDecimal",
      "Long" -> "ToLong"),
    "Any" -> Map("Int" -> "ToInteger",
      "Char" -> "ToChar",
      "Float" -> "ToFloat",
      "Double" -> "ToDouble",
      "Boolean" -> "ToBoolean",
      "Date" -> "ToDate",
      "Timestamp" -> "ToTimestamp",
      "BigDecimal" -> "ToBigDecimal",
      "Long" -> "ToBLong")
  )

  val formatDate = new java.text.SimpleDateFormat("yyyy-MM-dd")
  val formatTs = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
}

class Conversion {
  //var errors : Map[Integer, String] = Map.empty[Integer, String]

  def ToInteger(v: Any): Int = {
    v match {
      case null => 0
      case y: String => {
        if (y.trim.size > 0)
          y.trim.toInt
        else
          0
      }
      case y: Int => y
    }
  }

  def ToLong(v: Any): Long = {
    v match {
      case null => 0
      case y: String => {
        if (y.trim.size > 0)
          y.trim.toLong
        else
          0
      }
      case y: Long => y
    }
  }

  def ToFloat(v: Any): Float = {
    v match {
      case null => 0
      case y: String => {
        if (y.trim.size > 0)
          y.trim.toFloat
        else
          0
      }
      case y: Float => y
    }
  }

  def ToDouble(v: Any): Double = {
    v match {
      case null => 0
      case y: String => {
        if (y.trim.size > 0)
          y.trim.toDouble
        else
          0
      }
      case y: Double => y
    }
  }

  def ToBoolean(v: Any): Boolean = {
    v match {
      case null => false
      case y: String => {
        if (y.trim.size > 0)
          y.trim.toBoolean
        else
          false
      }
      case y: Boolean => y
    }
  }

  def ToDate(v: Any): Date = {
    v match {
      case null => null
      case y: String => Conversion.formatDate.parse(y)
      case y: Date => y
    }
  }

  def ToDate(v: Any, format : String): Date = {
    val formatDate = if(format == null || format.length ==0) None else Some(new java.text.SimpleDateFormat(format))
    v match {
      case null => null
      case y: String => if(formatDate.isDefined) formatDate.get.parse(y) else Conversion.formatDate.parse(y)
      case y: Date => y
    }
  }

  def ToTimestamp(v: Any): Timestamp = {
    v match {
      case null => null
      case y: String => new Timestamp(Conversion.formatTs.parse(y).getTime)
      case y: Timestamp => y
    }
  }

  def ToTimestamp(v: Any, format : String): Timestamp = {
    val formatTs = if(format == null || format.length ==0) None else Some(new java.text.SimpleDateFormat(format))
    v match {
      case null => null
      case y: String => if(formatTs.isDefined) new Timestamp(formatTs.get.parse(y).getTime)
                        else new Timestamp(Conversion.formatTs.parse(y).getTime)
      case y: Timestamp => y
    }
  }

  def ToBigDecimal(v: Any): BigDecimal = {
    v match {
      case null => 0
      case y: String => BigDecimal.apply(y)
      case y: BigDecimal => y
    }
  }

  def ToChar(v: Any): Char = {
    v match {
      case null => ' '
      case x: String => {
        if (x.trim.size > 0)
          x.charAt(0)
        else
          ' '
      }
      case x: Char => x
    }
  }

  def ToString(v: Any): String = {
    v match {
      case y: String => y
      case y: Char => y.toString
      case y: Int => y.toString
      case y: Long => y.toString
      case y: Float => y.toString
      case y: Double => y.toString
      case y: Boolean => y.toString
      case y: Timestamp => Conversion.formatTs.format(y)
      case y: Date => Conversion.formatDate.format(y)
      case y: BigDecimal => y.toString
    }
  }

  def ToStringArray(InputData: String, valueDelim: String): Array[String] = {
    if (InputData == null || valueDelim == null) return Array[String]()
    InputData.split(valueDelim, -1)
  }

  def ToIntArray(InputData: String, valueDelim: String): Array[Int] = {
    if (InputData == null || valueDelim == null) return Array[Int]()
    InputData.split(valueDelim, -1).map(v => ToInteger(v))
  }

  def ToIntegerArray(InputData: String, valueDelim: String): Array[Int] = ToIntArray(InputData, valueDelim)

  def ToLongArray(InputData: String, valueDelim: String): Array[Long] = {
    if (InputData == null || valueDelim == null) return Array[Long]()
    InputData.split(valueDelim, -1).map(v => ToLong(v))
  }

  def ToFloatArray(InputData: String, valueDelim: String): Array[Float] = {
    if (InputData == null || valueDelim == null) return Array[Float]()
    InputData.split(valueDelim, -1).map(v => ToFloat(v))
  }

  def ToDoubleArray(InputData: String, valueDelim: String): Array[Double] = {
    if (InputData == null || valueDelim == null) return Array[Double]()
    InputData.split(valueDelim, -1).map(v => ToDouble(v))
  }

  def ToBooleanArray(InputData: String, valueDelim: String): Array[Boolean] = {
    if (InputData == null || valueDelim == null) return Array[Boolean]()
    InputData.split(valueDelim, -1).map(v => ToBoolean(v))
  }

  def ToCharArray(InputData: String, valueDelim: String): Array[Char] = {
    if (InputData == null || valueDelim == null) return Array[Char]()
    InputData.split(valueDelim, -1).map(v => ToChar(v))
  }
}

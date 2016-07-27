package com.ligadata.runtime

import java.text.SimpleDateFormat


object Validation {

  def isNull(value : String, fieldName : String, errHandler : (String, String)=>Unit) : Boolean = {
    if(value == null || value.length == 0){
      errHandler(fieldName, "isNull")
      true
    }
    else false
  }

  def isInt(value : String, fieldName : String, errHandler : (String, String)=>Unit) : Boolean = {
    val res = (
      try{
        Some(value.toInt)
      }
      catch{ case ex:NumberFormatException => None}
      ).nonEmpty

    if(!res)
      errHandler(fieldName, "isInt")

    res
  }

  def isLong(value : String, fieldName : String, errHandler : (String, String)=>Unit) : Boolean = {
    val res = (
      try{
        Some(value.toLong)
      }
      catch{ case ex:NumberFormatException => None}
      ).nonEmpty

    if(!res)
      errHandler(fieldName, "isLong")

    res
  }

  def isDouble(value : String, fieldName : String, errHandler : (String, String)=>Unit) : Boolean = {
    val res = (
      try{
        Some(value.toDouble)
      }
      catch{ case ex:NumberFormatException => None}
      ).nonEmpty

    if(!res)
      errHandler(fieldName, "isDouble")

    res
  }

  def isFloat(value : String, fieldName : String, errHandler : (String, String)=>Unit) : Boolean = {
    val res = (
      try{
        Some(value.toFloat)
      }
      catch{ case ex:NumberFormatException => None}
      ).nonEmpty

    if(!res)
      errHandler(fieldName, "isFloat")

    res
  }

  def isBoolean(value : String, fieldName : String, errHandler : (String, String)=>Unit) : Boolean = {
    val valid = (
      try{
        Some(value.toBoolean)
      }
      catch{ case ex:NumberFormatException => None}
      ).nonEmpty

    if(!valid)
      errHandler(fieldName, "isBoolean")

    valid
  }

  private def isValidRange[T](value : T, startRange : Option[T], endRange : Option[T],
                      fieldName : String, errHandler : (String, String)=>Unit
                     )
                     (implicit comp: Ordering[T]) : Boolean = {
    /*value match {
      case _ : Int =>
        val cond1 = if (startRange.isDefined) (value.asInstanceOf[Int]) >= (startRange.get.asInstanceOf[Int]) else true
        val cond2 = if (endRange.isDefined) (value.asInstanceOf[Int]) <= (endRange.get.asInstanceOf[Int]) else true
        cond1 && cond2
      case _ : String =>
    }*/
    val cond1 = if (startRange.isDefined) (comp.compare(value,  startRange.get) >= 0) else true
    val cond2 = if (endRange.isDefined) (comp.compare(value,  endRange.get) <= 0) else true

    val valid = cond1 && cond2
    if (!valid)
      errHandler(fieldName, "isValidRange")

    valid
  }

  /**
    *
    * @param value
    * @param fieldType
    * @param startRange if empty, no min condition will be checked
    * @param endRange if empty, no max condition will be checked
    * @param valuesPattern necessary for date/time values, if empty, default value from Conversion class is used
    * @param fieldName
    * @param errHandler
    * @return
    */
  def isValidRange(value : String, fieldType : String, startRange : String, endRange : String, valuesPattern : String,
                      fieldName : String, errHandler : (String, String)=>Unit
                     ): Boolean = {

    val conversion = new Conversion

    fieldType.toLowerCase match {
      case "int" =>
        val v = conversion.ToInteger(value)
        val min = if(startRange == null || startRange.length == 0) None else Some(conversion.ToInteger(startRange))
        val max = if(endRange == null || endRange.length == 0) None else Some(conversion.ToInteger(endRange))
        isValidRange(v, min, max, fieldName, errHandler)
      case "long" =>
        val v = conversion.ToLong(value)
        val min = if(startRange == null || startRange.length == 0) None else Some(conversion.ToLong(startRange))
        val max = if(endRange == null || endRange.length == 0) None else Some(conversion.ToLong(endRange))
        isValidRange(v, min, max, fieldName, errHandler)
      case "bigint" =>
        val v = conversion.ToLong(value)
        val min = if(startRange == null || startRange.length == 0) None else Some(conversion.ToLong(startRange))
        val max = if(endRange == null || endRange.length == 0) None else Some(conversion.ToLong(endRange))
        isValidRange(v, min, max, fieldName, errHandler)
      case "float" =>
        val v = conversion.ToFloat(value)
        val min = if(startRange == null || startRange.length == 0) None else Some(conversion.ToFloat(startRange))
        val max = if(endRange == null || endRange.length == 0) None else Some(conversion.ToFloat(endRange))
        isValidRange(v, min, max, fieldName, errHandler)
      case "double" =>
        val v = conversion.ToDouble(value)
        val min = if(startRange == null || startRange.length == 0) None else Some(conversion.ToDouble(startRange))
        val max = if(endRange == null || endRange.length == 0) None else Some(conversion.ToDouble(endRange))
        isValidRange(v, min, max, fieldName, errHandler)
      case "bigdecimal" =>
        val v = conversion.ToBigDecimal(value)
        val min = if(startRange == null || startRange.length == 0) None else Some(conversion.ToBigDecimal(startRange))
        val max = if(endRange == null || endRange.length == 0) None else Some(conversion.ToBigDecimal(endRange))
        isValidRange(v, min, max, fieldName, errHandler)
      case "boolean" =>
        val v = conversion.ToBoolean(value)
        val min = if(startRange == null || startRange.length == 0) None else Some(conversion.ToBoolean(startRange))
        val max = if(endRange == null || endRange.length == 0) None else Some(conversion.ToBoolean(endRange))
        isValidRange(v, min, max, fieldName, errHandler)
      case "string" =>
        val v = value
        val min = if(startRange == null || startRange.length == 0) None else Some(startRange)
        val max = if(endRange == null || endRange.length == 0) None else Some(endRange)
        isValidRange(v, min, max, fieldName, errHandler)
      case "char" =>
        val v = conversion.ToChar(value)
        val min = if(startRange == null || startRange.length == 0) None else Some(conversion.ToChar(startRange))
        val max = if(endRange == null || endRange.length == 0) None else Some(conversion.ToChar(endRange))
        isValidRange(v, min, max, fieldName, errHandler)
      case "date" =>
        val v = conversion.ToDate(value, valuesPattern)
        val min = if(startRange == null || startRange.length == 0) None else Some(conversion.ToDate(startRange, valuesPattern))
        val max = if(endRange == null || endRange.length == 0) None else Some(conversion.ToDate(endRange, valuesPattern))
        isValidRange(v, min, max, fieldName, errHandler)
      case "timestamp" =>
        val v = conversion.ToTimestamp(value, valuesPattern)
        val min = if(startRange == null || startRange.length == 0) None else Some(conversion.ToTimestamp(startRange, valuesPattern))
        val max = if(endRange == null || endRange.length == 0) None else Some(conversion.ToTimestamp(endRange, valuesPattern))
        val cond1 = if (min.isDefined) v.compareTo(min.get) >= 0 else true
        val cond2 = if (max.isDefined) v.compareTo(max.get) <= 0 else true
        val valid = cond1 && cond2
        if (!valid)
          errHandler(fieldName, "isValidRange")

        valid

        //TODO : support complex types?
      case _ => throw new Exception("Unsupported field type " + fieldType)
    }

  }

  def isValidLength(value : String, length : Int,
                    fieldName : String, errHandler : (String, String)=>Unit): Boolean = {

    val valid = if(length > 0) value.length <= length else true
    if (!valid)
      errHandler(fieldName, "isValidLength")

    valid
  }

  def isValidDatePattern(value : String, format : String,
                    fieldName : String, errHandler : (String, String)=>Unit): Boolean = {

    //truncate the last 6 digits from nanoS
    val parseFormat =
      if (format.count(_ == 'S') >= 4)
        format.substring(0, format.lastIndexOf('.') + 4)
      else format

    val valid =
      try {
        val parsedDate: SimpleDateFormat = new SimpleDateFormat(parseFormat)
        true
      }
      catch{
        case e : Exception => false
      }

    if (!valid)
      errHandler(fieldName, "isValidDatePattern")

    valid
  }

  def isValidNumberPattern(value : String, pattern : String,
                    fieldName : String, errHandler : (String, String)=>Unit): Boolean = {

    //val formatter = new java.text.DecimalFormat("#.###")
    //formatter.format(10.123456)

    //TODO : should we use decimal formats or regex ??
    val valid = true
    if (!valid)
      errHandler(fieldName, "isValidNumberPattern")

    valid
  }

  def isValidStringPattern(value : String, pattern : String,
                           fieldName : String, errHandler : (String, String)=>Unit): Boolean = {

    val valid = isPatternMatch(value, pattern)
    if (!valid)
      errHandler(fieldName, "isValidStringPattern")

    valid
  }

  def isFromList(value : String, lov : String,
                       fieldName : String, errHandler : (String, String)=>Unit): Boolean = {

    val validValues = lov.split(",").toSet
    val valid = validValues contains value

    if (!valid)
      errHandler(fieldName, "isFromList")

    valid
  }

  def fieldsExist(primaryFieldValue : String, dependentFieldsValues : Array[String],
                 fieldName : String, errHandler : (String, String)=>Unit): Boolean = {

    val valid =
      if(primaryFieldValue == null || primaryFieldValue.length == 0) true
      else dependentFieldsValues.exists(df => df == null || df.length == 0)

    if (!valid)
      errHandler(fieldName, "fieldsExist")

    valid
  }

  def isPatternMatch(name : String, regex : String): Boolean ={
    val pattern = regex.r
    val matchList = pattern.findAllIn(name).matchData.toList
    matchList.nonEmpty
  }


}

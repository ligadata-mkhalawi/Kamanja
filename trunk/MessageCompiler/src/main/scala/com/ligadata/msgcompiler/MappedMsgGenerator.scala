package com.ligadata.msgcompiler

import com.ligadata.Exceptions._;
import com.ligadata.Exceptions.StackTrace;
import org.apache.logging.log4j.{ Logger, LogManager }
import com.ligadata.kamanja.metadata._;

class MappedMsgGenerator {

  var msgObjectGenerator = new MessageObjectGenerator
  var msgConstants = new MessageConstants
  val logger = this.getClass.getName
  lazy val log = LogManager.getLogger(logger)
  val primitives = List("string", "int", "boolean", "float", "double", "long", "char");

  def AddArraysInConstructor(fields: List[Element]): String = {
    AddArraysInConstrtor(fields)

  }

  private def AddArraysInConstrtor(fields: List[Element]): String = {
    var addArrays = new StringBuilder(8 * 1024)
    try {

      fields.foreach(field => {
        if (field != null) {
          var addArraysStr: String = ""
          val fieldBaseType: BaseTypeDef = field.FldMetaataType
          val fieldType = fieldBaseType.tType.toString().toLowerCase()
          val fieldTypeType = fieldBaseType.tTypeType.toString().toLowerCase()
          fieldType match {
            case "tarray" => {
              addArraysStr = "\tfields(\"" + field.Name + "\") = (-1, new " + field.FieldTypePhysicalName + "(0));\n "
            }
            case _ => { addArraysStr = "" }
          }
          addArrays = addArrays.append(addArraysStr.toString())
        }
      })
    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        throw e
      }
    }
    val addArrayConstrter: String = addArrays.toString
    if (addArrayConstrter == null || addArrayConstrter.trim == "")
      return ""
    else return """
    AddArraysInConstructor
    private def AddArraysInConstructor: Unit = {
      """ + addArrayConstrter + """
    }
    """
  }

  /**
   * Primary Keys variable declaration
   */

  def primayPartitionKeysVar(message: Message): String = {
    //log.info("primary Keys decl " + primayParitionKeysDecl(message))
    primayParitionKeysDecl(message)

  }

  /**
   * Primary Keys variable declaration
   */

  private def primayParitionKeysDecl(message: Message): String = {
    var primaryKeysStr: String = ""
    var primaryKeys = new StringBuilder(8 * 1024)
    var keysSet = Set[String]()

    if (message.PrimaryKeys == null || message.PrimaryKeys.size == 0) {
      primaryKeysStr = "";
      primaryKeys = primaryKeys.append(primaryKeysStr)
    } else {
      message.PrimaryKeys.foreach(key => {
        keysSet = keysSet + key
      })
      message.PartitionKeys.foreach(key => {
        keysSet = keysSet + key
      })
      keysSet.foreach(key => {
        message.Elements.foreach(field => {
          if (field != null)
            if (field.Name.equalsIgnoreCase(key))
              primaryKeysStr = "\t var " + field.Name + ":" + field.FieldTypePhysicalName + "= _;\n";
        })
        primaryKeys = primaryKeys.append(primaryKeysStr)
      })
    }
    return primaryKeys.toString()

  }

  /*
   * Set Method Generation Function for Mapped Messages
   */
  def setFuncGenerationforMapped(fields: List[Element], fldsMap: Map[String, Int], mdMgr: MdMgr): String = {
    setFuncGenforMapped(fields, fldsMap, mdMgr)
  }

  private def setFuncGenforMapped(fields: List[Element], fldsMap: Map[String, Int], mdMgr: MdMgr): String = {
    var setMethod = new StringBuilder(8 * 1024)

    try {
      fields.foreach(field => {
        if (field != null) {
          var setmethodStr: String = ""
          val fieldBaseType: BaseTypeDef = field.FldMetaataType
          val fieldType = fieldBaseType.tType.toString().toLowerCase()
          val fieldTypeType = fieldBaseType.tTypeType.toString().toLowerCase()

          // log.info("fieldTypeType " + fieldTypeType)
          // log.info("fieldBaseType 1 " + fieldBaseType.tType)
          //  log.info("fieldBaseType 2 " + fieldBaseType.typeString)
          //  log.info("fieldBaseType 3" + fieldBaseType.tTypeType)

          //   log.info("fieldType " + fieldType)

          fieldTypeType match {

            case "tscalar" => {
              setmethodStr = setMethodForScalarMapped(field, fldsMap)
              //log.info("fieldBaseType.implementationName    " + fieldBaseType.implementationName)

            }
            case "tcontainer" => {
              fieldType match {
                case "tarray" => {
                  var arrayType: ArrayTypeDef = null
                  arrayType = fieldBaseType.asInstanceOf[ArrayTypeDef]
                  setmethodStr = setMethodForStructMapped(field)
                }
                case "tstruct" => {
                  var ctrDef: ContainerDef = mdMgr.Container(field.Ttype, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
                  setmethodStr = setMethodForStructMapped(field)
                }
                case "tmap" => {
                  setmethodStr = setMethodForMap(field)
                }

                case _ => {
                  throw new Exception("This types is not handled at this time ") // BUGBUG - Need to handled other cases
                }
              }
            }
            case _ => {
              throw new Exception("This types is not handled at this time ") // BUGBUG - Need to handled other cases
            }
          }

          setMethod = setMethod.append(setmethodStr.toString())
          // log.info("=========SET Methods ===============" + setMethod.toString());
        }
      })
    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        throw e
      }
    }
    return setMethod.toString
  }

  /**
   * Set method for mapped messages
   */
  private def setMethodForScalarMapped(field: Element, fldsMap: Map[String, Int]): String = {

    val fieldBaseType: BaseTypeDef = field.FldMetaataType
    fieldBaseType.typeString

    """   
    def set""" + field.Name.capitalize + """(value: """ + field.FieldTypePhysicalName + """): Unit = {
     	fields("""" + field.Name + """") = (""" + fldsMap(field.FieldTypePhysicalName) + """, value);
    }
   """
  }

  /**
   * Set method for struct type
   */
  private def setMethodForStructMapped(field: Element): String = {

    val fieldBaseType: BaseTypeDef = field.FldMetaataType
    fieldBaseType.typeString

    """   
    def set""" + field.Name.capitalize + """(value: """ + field.FieldTypePhysicalName + """): Unit = {
     	fields("""" + field.Name + """") = (-1, value);
    }
   """
  }

  /**
   * Set method for map type
   */
  private def setMethodForMap(field: Element): String = {

    val fieldBaseType: BaseTypeDef = field.FldMetaataType
    fieldBaseType.typeString

    """   
    def set""" + field.Name.capitalize + """(value: """ + field.FieldTypePhysicalName + """): Unit = {
     	fields("""" + field.Name + """") = (-1, value);
    }
   """
  }
  /**
   * Get method of scalar types for mapped messages
   */
  private def getMethodForScalarMapped(field: Element): String = {

    """    
    def get""" + field.Name.capitalize + """: """ + field.FieldTypePhysicalName + """= {
      val fldVal = fields.getOrElse("""" + field.Name + """", null)
        if(fldVal == null)
          return """ + field.FieldTypeImplementationName + """.Input(null);
      return """ + field.FieldTypeImplementationName + """.Input(fields("""" + field.Name + """")._2.toString);
    }          
    """

  }

  /*
   * Get method of Array types for Mapped messages
   */

  private def getMethodArrayMapped(field: Element): String = {
    val fieldBaseType: BaseTypeDef = field.FldMetaataType
    var arrayType: ArrayTypeDef = null
    arrayType = fieldBaseType.asInstanceOf[ArrayTypeDef]
    var returnStr: String = ""

    if (arrayType.elemDef.tTypeType.toString().equalsIgnoreCase("tscalar")) {
      val fname = arrayType.elemDef.implementationName + ".Input"
      returnStr = """    
     def get""" + field.Name.capitalize + """: """ + field.FieldTypePhysicalName + """ = {
    var ret: """ + field.FieldTypePhysicalName + """ = """ + field.FieldTypePhysicalName + """()
    if (fields.contains("""" + field.Name + """")) {
      val arr = fields.getOrElse("""" + field.Name + """", null)._2
      if (arr != null) {
        val arrFld = CollectionAsArrString(arr)
        ret = arrFld.map(v => { """ + fname + """(v.toString) }).toArray
      }
    }
    return ret
  }     
    """
    } else if (arrayType.elemDef.tTypeType.toString().equalsIgnoreCase("tcontainer")) {
      val fname = arrayType.elemDef.PhysicalName
      log.info("11111111111111******************" + arrayType.tType + "-----" + arrayType.elemDef.tType + "------------" + arrayType.elemDef.tTypeType.toString() + "-----------" + fname)

      returnStr = """    
     def get""" + field.Name.capitalize + """: """ + field.FieldTypePhysicalName + """ = {
    var ret: """ + field.FieldTypePhysicalName + """ = """ + field.FieldTypePhysicalName + """()
    if (fields.contains("""" + field.Name + """")) {
      val arr = fields.getOrElse("""" + field.Name + """", null)._2.asInstanceOf[""" + field.FieldTypePhysicalName + """]
      if (arr != null) {
        ret = arr.map(v => {  v.asInstanceOf[""" + fname + """]}).toArray
      }
    }
    return ret
  }     
    """
    }

    return returnStr

  }

  /**
   * Get Method for the message/container type
   */

  private def getMethodForStructType(field: Element, mdMgr: MdMgr): String = {
    var ctrDef: ContainerDef = mdMgr.Container(field.Ttype, -1, true).getOrElse(null)
    if (ctrDef != null) {
      return """    
    def get""" + field.Name.capitalize + """: """ + field.FieldTypePhysicalName + """  = {
      var ret : """ + field.FieldTypePhysicalName + """  = null
      if (fields.contains("""" + field.Name + """")) {
        var id = fields.getOrElse("""" + field.Name + """", null)
        if (id != null)
          ret = fields("""" + field.Name + """")._2.asInstanceOf[""" + field.FieldTypePhysicalName + """];
       }
      return ret
    }
    """
    } else return ""
  }
  /*
   * get method for map type
   */
  private def getMethodForMappedType(field: Element, mdMgr: MdMgr): String = {
    """  
  def get""" + field.Name.capitalize + """: """ + field.FieldTypePhysicalName + """ = {
    var ret: """ + field.FieldTypePhysicalName + """  = """ + field.FieldTypePhysicalName + """()
    if (fields.contains("""" + field.Name + """")) {
      val arr = fields.getOrElse("""" + field.Name + """", null)
      if (arr != null && arr._2.isInstanceOf[scala.collection.mutable.Map[_,_]]) {
        val map = arr._2.asInstanceOf[""" + field.FieldTypePhysicalName + """]
        map.foreach(k => { ret(k._1) = map(k._1) })
      }
    }
    return ret
  }   
   """
  }
  /*
   * Get Method generation function for Mapped Messages
   */

  def getFuncGenerationForMapped(fields: List[Element], mdMgr: MdMgr): String = {

    getFuncGenForMapped(fields, mdMgr)
  }

  private def getFuncGenForMapped(fields: List[Element], mdMgr: MdMgr): String = {
    var getMethod = new StringBuilder(8 * 1024)
    var getmethodStr: String = ""
    try {
      fields.foreach(field => {
        if (field != null) {
          var getmethodStr: String = ""
          val fieldBaseType: BaseTypeDef = field.FldMetaataType
          val fieldType = fieldBaseType.tType.toString().toLowerCase()
          val fieldTypeType = fieldBaseType.tTypeType.toString().toLowerCase()

          // log.info("fieldTypeType " + fieldTypeType)
          // log.info("fieldBaseType 1 " + fieldBaseType.tType)
          // log.info("fieldBaseType 2 " + fieldBaseType.typeString)
          // log.info("fieldBaseType 3" + fieldBaseType.tTypeType)

          //  log.info("fieldType " + fieldType)

          fieldTypeType match {

            case "tscalar" => {
              getmethodStr = getMethodForScalarMapped(field)
              // log.info("fieldBaseType.implementationName    " + fieldBaseType.implementationName)
            }
            case "tcontainer" => {
              fieldType match {
                case "tarray" => {
                  getmethodStr = getMethodArrayMapped(field)
                }
                case "tstruct" => {
                  getmethodStr = getMethodForStructType(field, mdMgr)
                }
                case "tmap" => {
                  val fieldBaseType: BaseTypeDef = field.FldMetaataType
                  val fieldType = fieldBaseType.tType.toString().toLowerCase()

                  val fieldTypeType = fieldBaseType.tTypeType.toString().toLowerCase()
                  var arrayType: ArrayTypeDef = null
                  if (fieldBaseType.isInstanceOf[ArrayTypeDef])
                    arrayType = fieldBaseType.asInstanceOf[ArrayTypeDef]

                  log.info("fieldTypeType===== " + fieldTypeType)
                  log.info("fieldBaseType 1===== " + fieldBaseType.tType)
                  var maptypeDef: MapTypeDef = null;

                  maptypeDef = fieldBaseType.asInstanceOf[MapTypeDef]
                  log.info("field ElemType : " + maptypeDef.typeString)
                  log.info("field ElemType : " + maptypeDef.valDef.typeString)
                  log.info("field ElemType : " + maptypeDef.valDef.tType)
                  log.info("field ElemType : " + maptypeDef.valDef.tTypeType)

                  val keyValueType = maptypeDef.valDef.tTypeType.toString().toLowerCase()
                  getmethodStr = getMethodForMapType(field, mdMgr, keyValueType)

                }

                case _ => {
                  throw new Exception("This types is not handled at this time ") // BUGBUG - Need to handled other cases
                }
              }
            }
            case _ => {
              throw new Exception("This types is not handled at this time ") // BUGBUG - Need to handled other cases
            }
          }
          getMethod = getMethod.append(getmethodStr.toString())
        }
      })
    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        throw e
      }
    }
    return getMethod.toString
  }

  /*
   * get method for Map type mapped messages
   */
  private def getMethodForMapType(field: Element, mdMgr: MdMgr, keyValueType: String): String = {
    var getmethodStr: String = ""

    keyValueType match {
      case "tscalar" => {
        getmethodStr = getMethodForMappedType(field, mdMgr)
      }
      case "tcontainer" => {
        getmethodStr = getMethodForMappedType(field, mdMgr)
      }
      case _ => {
        throw new Exception("This types is not handled at this time ") // BUGBUG - Need to handled other cases
      }
    }
    return getmethodStr
  }

  /*
   * generate keys variable for mapped message
   */
  def keysVarforMapped(fields: List[Element], fieldIndexMap: Map[String, Int]): String = {
    var mappedTypesABuf = new scala.collection.mutable.ArrayBuffer[String]
    var baseTypId = -1
    var firstTimeBaseType: Boolean = true
    var keysStr = new StringBuilder(8 * 1024)
    val stringType = MdMgr.GetMdMgr.Type("System.String", -1, true)
    if (stringType.getOrElse("None").equals("None"))
      throw new Exception("Type not found in metadata for String ")

    mappedTypesABuf += stringType.get.implementationName
    fields.seq.foreach(field => {
      if (field != null) {
        var typstring = field.FieldTypePhysicalName
        if (fieldIndexMap.contains(typstring)) {
          baseTypId = fieldIndexMap(typstring)
          log.info("typstring " + typstring + " basetypeId" + baseTypId)
        }
        keysStr.append("(\"" + field.Name + "\", " + baseTypId + "),")
      }
    })

    /*fields.seq.foreach(field => {
      var typstring = field.FieldTypePhysicalName.toLowerCase()
      if (primitives.contains(typstring)) {
        if (mappedTypesABuf.contains(typstring)) {
          if (mappedTypesABuf.size == 1 && firstTimeBaseType)
            baseTypId = mappedTypesABuf.indexOf(typstring)-1
        } else {
          mappedTypesABuf += typstring
          baseTypId = mappedTypesABuf.indexOf(typstring)-1
        }
      } else baseTypId = -1
      * 
      */
    //keysStr.append("(\"" + field.Name + "\", " + mappedTypesABuf.indexOf(typstring) + "),")

    //log.info(keysStr.toString().substring(0, keysStr.toString().length - 1))
    var keys = "var keys = Map(" + keysStr.toString().substring(0, keysStr.toString().length - 1) + ")";
    log.info("keys " + keys)
    return keys
  }

  /*
   * From Function for Mappped messages
   */
  def getFromFuncFixed(message: Message, mdMgr: MdMgr): String = { //""" + fromFuncScalarMapped + getFromFuncStr(message, mdMgr) + """   
    """
    private def fromFunc(other: """ + message.Name + """): """ + message.Name + """ = {  
      """ + fromFuncScalarMapped + getFromFuncStr(message, mdMgr) + """   
      this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));   
      return this;
    }
    
"""
  }

  /*
   * generate FromFunc code for message fields 
   */
  private def getFromFuncStr(message: Message, mdMgr: MdMgr): String = {
    var fromFuncBuf = new StringBuilder(8 * 1024)
    try {
      if (message.Elements != null) {
        message.Elements.foreach(field => {
          if (field != null) {
            val fieldBaseType: BaseTypeDef = field.FldMetaataType
            val fieldType = fieldBaseType.tType.toString().toLowerCase()
            val fieldTypeType = fieldBaseType.tTypeType.toString().toLowerCase()
            fieldTypeType match {
              case "tscalar" => {
                // do nothiong already handled
              }
              case "tcontainer" => {
                fieldType match {
                  case "tarray" => {
                    var arrayType: ArrayTypeDef = null
                    arrayType = fieldBaseType.asInstanceOf[ArrayTypeDef]
                    fromFuncBuf = fromFuncBuf.append(fromFuncForArrayMapped(field, true))
                  }
                  case "tstruct" => {
                    var ctrDef: ContainerDef = mdMgr.Container(field.Ttype, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
                    fromFuncBuf = fromFuncBuf.append(fromFuncForStructMapped(field, ctrDef))
                  }
                  case "tmap" => {
                    fromFuncBuf = fromFuncBuf.append(fromFuncForMapMapped(field, mdMgr))
                  }
                  case "tmsgmap" => {
                    var ctrDef: ContainerDef = mdMgr.Container(field.Ttype, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
                    fromFuncBuf = fromFuncBuf.append(fromFuncForStructMapped(field, ctrDef))
                  }
                  case _ => {
                    throw new Exception("This types is not handled at this time ") // BUGBUG - Need to handled other cases
                  }
                }
              }
              case _ => {
                throw new Exception("This types is not handled at this time ") // BUGBUG - Need to handled other cases
              }
            }
          }
        })
      }
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    }

    return fromFuncBuf.toString();
  }

  /*
   * From Func - generate code for array
   */
  private def fromFuncForArrayMapped(field: Element, isArray: Boolean): String = {
    var fromFuncBuf = new StringBuilder(8 * 1024)
    var typeStr: String = ""
    var typetype: String = ""
    try {
      val implName = field.FieldTypeImplementationName
      if (field.Ttype.contains("arrayof")) {
        var arrayType = field.FldMetaataType.asInstanceOf[ArrayTypeDef]
        typetype = arrayType.elemDef.tTypeType.toString().toLowerCase()
        if (field.FldMetaataType.typeString.toString().split("\\[").size == 2) {
          typeStr = field.FldMetaataType.typeString.toString().split("\\[")(1)
        }
      }
      if (typetype.equals("tscalar")) {
        if (implName != null && implName.trim() != "") {
          fromFuncBuf.append(fromFuncForArrayScalarMapped(field, isArray));
        }
      } else if (typetype.equals("tcontainer")) {
        fromFuncBuf.append(fromFuncForArrayContainerMapped(field, isArray, typeStr));
      }
    } catch {
      case e: Exception => throw e
    }
    fromFuncBuf.toString
  }

  /*
   * From Func for Array of Scalar
   */
  private def fromFuncForArrayScalarMapped(field: Element, isArray: Boolean): String = {
    var fromFuncBuf = new StringBuilder(8 * 1024)
    try {
      val implName = field.FieldTypeImplementationName
      var arrayType = field.FldMetaataType.asInstanceOf[ArrayTypeDef]
      var typeStr: String = ""
      if (field.FldMetaataType.typeString.toString().split("\\[").size == 2) {
        typeStr = field.FldMetaataType.typeString.toString().split("\\[")(1)
      }
      if (implName != null && implName.trim() != "") {
        fromFuncBuf = fromFuncBuf.append("%s { %s".format(msgConstants.pad2, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s  if (other.valuesMap.contains(\"%s\")) { %s".format(msgConstants.pad3, field.Name, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s val fld = other.valuesMap(\"%s\").getValue  ;%s".format(msgConstants.pad3, field.Name, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s if (fld == null) valuesMap.put(\"%s\", null); %s".format(msgConstants.pad2, field.Name, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s else { %s".format(msgConstants.pad2, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s  val o = fld.asInstanceOf[%s] %s".format(msgConstants.pad3, field.FldMetaataType.typeString, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s var %s = new %s(o.size) %s".format(msgConstants.pad3, field.Name, field.FldMetaataType.typeString, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s for (i <- 0 until o.length) { %s".format(msgConstants.pad3, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s %s(i) = %s.Clone(o(i)).asInstanceOf[%s %s".format(msgConstants.pad3, field.Name, implName, typeStr, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s } %s".format(msgConstants.pad3, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s  valuesMap.put(\"%s\", new AttributeValue(%s, other.valuesMap(\"%s\").getValueType))%s".format(msgConstants.pad3, field.Name, field.Name, field.Name, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s  } %s".format(msgConstants.pad3, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s  } else valuesMap.put(\"%s\", null);%s".format(msgConstants.pad3, field.Name, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s } ;%s".format(msgConstants.pad2, msgConstants.newline))

      }

    } catch {
      case e: Exception => throw e
    }
    fromFuncBuf.toString
  }
  /*
   * From Func for Array of Container
   */
  private def fromFuncForArrayContainerMapped(field: Element, isArray: Boolean, typeStr: String): String = {
    var fromFuncBuf = new StringBuilder(8 * 1024)
    try {
      /*var arrayType = field.FldMetaataType.asInstanceOf[ArrayTypeDef]
      var typeStr: String = ""
      if (field.FldMetaataType.typeString.toString().split("\\[").size == 2) {
        typeStr = field.FldMetaataType.typeString.toString().split("\\[")(1)
      }*/
      fromFuncBuf = fromFuncBuf.append("%s { %s".format(msgConstants.pad2, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s if (other.valuesMap.contains(\"%s\")) {  %s".format(msgConstants.pad3, field.Name, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s val fld = other.valuesMap(\"%s\").getValue  ;%s".format(msgConstants.pad3, field.Name, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s if (fld == null) valuesMap.put(\"%s\", null); %s".format(msgConstants.pad2, field.Name, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s else { %s".format(msgConstants.pad2, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s  val o = fld.asInstanceOf[%s] ; %s".format(msgConstants.pad3, field.FldMetaataType.typeString, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s var %s = new %s(o.size) ;%s".format(msgConstants.pad3, field.Name, field.FldMetaataType.typeString, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s for(i <- 0 until o.length){ %s".format(msgConstants.pad3, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s  %s(i) = o(i).Clone.asInstanceOf[%s  ;%s".format(msgConstants.pad3, field.Name, typeStr, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s  } %s".format(msgConstants.pad3, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s valuesMap.put(\"%s\", new AttributeValue(%s, other.valuesMap(\"%s\").getValueType)); %s".format(msgConstants.pad3, field.Name, field.Name, field.Name, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s  } %s".format(msgConstants.pad3, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s  } else valuesMap.put(\"%s\", null);%s".format(msgConstants.pad3, field.Name, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s } ;%s".format(msgConstants.pad2, msgConstants.newline))

    } catch {
      case e: Exception => throw e
    }
    fromFuncBuf.toString
  }

  /*
   * From Func for Containertype as Message
   */
  private def fromFuncForStructMapped(field: Element, ctrDef: ContainerDef): String = {
    var fromFuncBuf = new StringBuilder(8 * 1024)
    try {
      fromFuncBuf = fromFuncBuf.append("%s { %s".format(msgConstants.pad2, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s if (other.valuesMap.contains(\"%s\")) { %s".format(msgConstants.pad3, field.Name, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s val %s = other.valuesMap(\"%s\").getValue; %s".format(msgConstants.pad3, field.Name, field.Name, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s if(%s == null) valuesMap.put(\"%s\", null); %s".format(msgConstants.pad2, field.Name, field.Name, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s else { %s".format(msgConstants.pad2, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s valuesMap.put(\"%s\", new AttributeValue(%s.asInstanceOf[%s].Clone.asInstanceOf[%s], other.valuesMap(\"%s\").getValueType)); %s".format(msgConstants.pad3, field.Name, field.Name, field.FieldTypePhysicalName, field.FieldTypePhysicalName, field.Name, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s  } %s".format(msgConstants.pad3, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s  } else valuesMap.put(\"%s\", null);%s".format(msgConstants.pad3, field.Name, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s } ;%s".format(msgConstants.pad2, msgConstants.newline))
    } catch {
      case e: Exception => throw e
    }
    fromFuncBuf.toString

  }

  /*
   * From Func for Containertype as Message
   */
  private def fromFuncForMapMapped(field: Element, mdMgr: MdMgr): String = {
    var maptypeDef: MapTypeDef = null;
    var fromFuncBuf = new StringBuilder(8 * 1024)
    maptypeDef = field.FldMetaataType.asInstanceOf[MapTypeDef]
    if (maptypeDef.valDef != null) {
      val typeInfo = maptypeDef.valDef.tTypeType.toString().toLowerCase()
      val valType = maptypeDef.valDef
      var typetyprStr: String = maptypeDef.valDef.tType.toString().toLowerCase()

      typeInfo match {
        case "tscalar" => {
          log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! typeInfo " + typeInfo + typetyprStr);

          fromFuncBuf = fromFuncBuf.append("%s { %s".format(msgConstants.pad2, msgConstants.newline))
          fromFuncBuf = fromFuncBuf.append("%s if (other.valuesMap.contains(\"%s\")) { %s".format(msgConstants.pad2, field.Name, msgConstants.newline))
          fromFuncBuf = fromFuncBuf.append("%s val %s = other.valuesMap(\"%s\").getValue; %s".format(msgConstants.pad2, field.Name, field.Name, msgConstants.newline))
          fromFuncBuf = fromFuncBuf.append("%s if (%s == null) valuesMap.put(\"%s\", null); %s".format(msgConstants.pad2, field.Name, field.Name, msgConstants.newline))
          fromFuncBuf = fromFuncBuf.append("%selse { %s".format(msgConstants.pad2, msgConstants.newline))
          fromFuncBuf = fromFuncBuf.append("%sif (%s.isInstanceOf[%s]) { %s".format(msgConstants.pad2, field.Name, field.FieldTypePhysicalName, msgConstants.newline))
          fromFuncBuf = fromFuncBuf.append("%s val mapmap = %s.asInstanceOf[%s].map { a => (com.ligadata.BaseTypes.StringImpl.Clone(a._1), %s.Clone(a._2)) }.toMap%s".format(msgConstants.pad2, field.Name, field.FieldTypePhysicalName, maptypeDef.valDef.implementationName, msgConstants.newline))
          fromFuncBuf = fromFuncBuf.append("%s valuesMap.put(\"%s\", new AttributeValue(mapmap, other.valuesMap(\"%s\").getValueType)); %s".format(msgConstants.pad2, field.Name, field.Name, msgConstants.newline))
          fromFuncBuf = fromFuncBuf.append("%s } %s".format(msgConstants.pad2, msgConstants.newline))
          fromFuncBuf = fromFuncBuf.append("%s } %s".format(msgConstants.pad2, msgConstants.newline))
          fromFuncBuf = fromFuncBuf.append("%s } else valuesMap.put(\"%s\", null);%s".format(msgConstants.pad2, field.Name, msgConstants.newline))
          fromFuncBuf = fromFuncBuf.append("%s } %s".format(msgConstants.pad2, msgConstants.newline))
        }
        case "tcontainer" => {
          typetyprStr match {
            case "tarray" => { throw new Exception("Not supporting array of array"); }
            case "tstruct" => {

              var msgDef: ContainerDef = null;
              msgDef = mdMgr.Message(maptypeDef.valDef.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
              if (msgDef == null)
                msgDef = mdMgr.Container(maptypeDef.valDef.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
            }
            case "tmsgmap" => {
              var ctrDef: ContainerDef = null;
              ctrDef = mdMgr.Message(maptypeDef.valDef.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
              if (ctrDef == null)
                ctrDef = mdMgr.Container(maptypeDef.valDef.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
            }
            case "tmap" => { throw new Exception("Not supporting map of array"); }
            case _ => {
              throw new Exception("This types is not handled at this time ") // BUGBUG - Need to handled other cases
            }
          }
        }
        case _ => {
          throw new Exception("This types is not handled at this time ") // BUGBUG - Need to handled other cases
        }
      }
    }
    return fromFuncBuf.toString()
  }

  /**
   * From Func generation for mapped messages
   */
  private def fromFuncScalarMapped() = {
    """
     if (other.valuesMap != null) {
      other.valuesMap.foreach(vMap => {
        val key = caseSensitiveKey(vMap._1)
        val attribVal = vMap._2
        val valType = attribVal.getValueType.getTypeCategory.getValue
        if (attribVal.getValue != null && attribVal.getValueType != null) {
          var attributeValue: AttributeValue = null
          valType match {
            case 1 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.StringImpl.Clone(attribVal.getValue.asInstanceOf[String]), attribVal.getValueType) }
            case 0 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.IntImpl.Clone(attribVal.getValue.asInstanceOf[java.lang.Integer]), attribVal.getValueType) }
            case 2 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.FloatImpl.Clone(attribVal.getValue.asInstanceOf[java.lang.Float]), attribVal.getValueType) }
            case 3 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.DoubleImpl.Clone(attribVal.getValue.asInstanceOf[java.lang.Double]), attribVal.getValueType) }
            case 7 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.BoolImpl.Clone(attribVal.getValue.asInstanceOf[java.lang.Boolean]), attribVal.getValueType) }
            case 4 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.LongImpl.Clone(attribVal.getValue.asInstanceOf[java.lang.Long]), attribVal.getValueType) }
            case 6 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.CharImpl.Clone(attribVal.getValue.asInstanceOf[java.lang.Character]), attribVal.getValueType) }
            case _ => {} // do nothhing
          }
          valuesMap.put(key, attributeValue);
        };
      })
    }
        
    """
  }

}
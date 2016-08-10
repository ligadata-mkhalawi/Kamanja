package com.ligadata.msgcompiler

import com.ligadata.Exceptions._;
import com.ligadata.Exceptions.StackTrace;
import org.apache.logging.log4j.{ Logger, LogManager }
import com.ligadata.kamanja.metadata._;

class MessageGenerator {

  var builderGenerator = new MessageBuilderGenerator
  var msgObjectGenerator = new MessageObjectGenerator
  var mappedMsgGen = new MappedMsgGenerator
  var msgConstants = new MessageConstants

  val logger = this.getClass.getName
  lazy val log = LogManager.getLogger(logger)

  /*
   * Generate the versioned and non versioned class for both mapped and Fixed messages
 	 * add import stamts -- still need to add
   * Generate Message Class
   * Message Class lines generation
   * Generate all the getter methods in the class generation
   * Generation all the setter methods in class generation
   * 
   */
  def generateMessage(message: Message, mdMgr: MdMgr): (String, String) = {
    generateMsg(message, mdMgr)
  }

  private def generateMsg(message: Message, mdMgr: MdMgr): (String, String) = {

    var messageVerGenerator = new StringBuilder(8 * 1024)
    var messageNonVerGenerator = new StringBuilder(8 * 1024)
    var messageGenerator = new StringBuilder(8 * 1024)
    try {
      val msgObj = msgObjectGenerator.generateMessageObject(message, mdMgr)

      messageVerGenerator = messageVerGenerator.append(msgConstants.newline + msgConstants.packageStr.format(message.Pkg, msgConstants.newline));
      messageVerGenerator = messageVerGenerator.append(msgConstants.importStatements + msgConstants.newline);
      messageVerGenerator = messageVerGenerator.append(msgObj._1 + msgConstants.newline);

      messageNonVerGenerator = messageNonVerGenerator.append(msgConstants.newline + msgConstants.packageStr.format(message.NameSpace, msgConstants.newline));
      messageNonVerGenerator = messageNonVerGenerator.append(msgConstants.importStatements + msgConstants.newline);
      messageNonVerGenerator = messageNonVerGenerator.append(msgObj._2 + msgConstants.newline);

      messageGenerator = messageGenerator.append(msgConstants.newline);
      messageGenerator = messageGenerator.append(classGen(message));
      messageGenerator = messageGenerator.append(getMessgelogDetails(message));
      messageGenerator = messageGenerator.append(getAttributeTypes(message));
      messageGenerator = messageGenerator.append(msgConstants.newline + keyTypesMap(message.Elements));
      messageGenerator = messageGenerator.append(methodsFromMessageInterface(message));
      messageGenerator = messageGenerator.append(msgConstants.newline + generateParitionKeysData(message) + msgConstants.newline);
      messageGenerator = messageGenerator.append(msgConstants.newline + generatePrimaryKeysData(message) + msgConstants.newline);
      messageGenerator = messageGenerator.append(generateGetAttributeType());
      if (message.Fixed.equalsIgnoreCase("true")) {
        messageGenerator = messageGenerator.append(msgConstants.newline + generatedMsgVariables(message));
        messageGenerator = messageGenerator.append(getGetSetMethodsFixed(message, mdMgr));
        messageGenerator = messageGenerator.append(getFromFuncFixed(message, mdMgr));
      } else if (message.Fixed.equalsIgnoreCase("false")) {
        var fieldIndexMap: Map[String, Int] = msgConstants.getScalarFieldindex(message.Elements)
        messageGenerator = messageGenerator.append(msgConstants.getGetSetMethods);
        messageGenerator = messageGenerator.append(mappedMsgGen.getFromFuncFixed(message, mdMgr))
      }
      messageGenerator = messageGenerator.append(msgConstants.generateWithMethods(message))
      messageGenerator = messageGenerator.append(isCaseSensitiveFunc(message));
      messageGenerator = messageGenerator.append(caseSensitiveFunc(message))
      messageGenerator = messageGenerator.append(messageContructor(message))
      messageGenerator = messageGenerator.append(msgConstants.newline + msgConstants.closeBrace);
      messageVerGenerator = messageVerGenerator.append(messageGenerator.toString())
      messageNonVerGenerator = messageNonVerGenerator.append(messageGenerator.toString())

    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        throw e
      }
    }
    return (messageVerGenerator.toString, messageNonVerGenerator.toString())
  }
  /*
   * Message Class
   */
  private def classGen(message: Message): String = {
    var baseMsgType: String = "";
    var factoryStr: String = "";

    val msgType = message.MsgType
    if (msgType == null || msgType.trim() == "")
      throw new Exception("Message Definition root element should be either Message or Container")

    if (msgType.equalsIgnoreCase(msgConstants.messageStr)) {
      baseMsgType = msgConstants.baseMsg
      factoryStr = msgConstants.baseMsgObj
    } else if (msgType.equalsIgnoreCase(msgConstants.containerStr)) {
      baseMsgType = msgConstants.baseContainer
      factoryStr = msgConstants.baseContainerObj
    }
    // (var transactionId: Long, other: CustAlertHistory) extends ContainerInterface {
    return msgConstants.classStr.format(message.Name, factoryStr, message.Name, baseMsgType, msgConstants.newline)

  }

  /*
   * getSet methods for Fixed Message
   */
  private def getGetSetMethodsFixed(message: Message, mdMgr: MdMgr): String = {
    var getSetFixed = new StringBuilder(8 * 1024)
    try {
      getSetFixed = getSetFixed.append(msgConstants.getAttributeTypesMethodFixed)
      getSetFixed = getSetFixed.append(getWithReflection(message));
      getSetFixed = getSetFixed.append(getByStringhFixed(message));
      getSetFixed = getSetFixed.append(getByName(message));
      getSetFixed = getSetFixed.append(getOrElseFunc());
      getSetFixed = getSetFixed.append(getFuncByOffset(message.Elements, message.Name));
      getSetFixed = getSetFixed.append(getOrElseByIndexFunc);
      getSetFixed = getSetFixed.append(getAttributeNamesFixed);
      getSetFixed = getSetFixed.append(getAllAttributeValuesFixed(message));
      // getSetFixed = getSetFixed.append(getAttributeNameAndValueIterator);
      getSetFixed = getSetFixed.append(setByKeyFunc(message));
      if (message.Elements != null && message.timePartition != null) {
        getSetFixed = getSetFixed.append(setFuncByOffset(message.Elements, message.Name, mdMgr, message.timePartition.Key));
      } else if (message.Elements != null) {
        getSetFixed = getSetFixed.append(setFuncByOffset(message.Elements, message.Name, mdMgr, null));

      }
      getSetFixed = getSetFixed.append(setValueAndValueTypeByKeyFunc);
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    }
    getSetFixed.toString()
  }

  /*
   * message basic details in class
   */
  private def getMessgelogDetails(message: Message): String = {
    """ 
  val log = """ + message.Name + """.log
"""
  }

  /*
   * Generate the variables for the message 
   */
  private def generatedMsgVariables(message: Message): String = {
    var msgVariables = new StringBuilder(8 * 1024)
    try {
      message.Elements.foreach(field => {
        if (field != null) {
          msgVariables.append(" %svar %s: %s = _; %s".format(msgConstants.pad2, field.Name, field.FieldTypePhysicalName, msgConstants.newline))
        }
      })
    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        throw e
      }
    }
    msgVariables.toString()
  }

  /*
   * Message constructor generation
   */

  private def msgConstructor(message: Message, msgStr: String, constructorStmts: String): String = {
    var msgClassConsGen: String = ""

    msgClassConsGen = """
        def """ + message.Name + """(""" + msgStr.substring(0, msgStr.length() - 2) + """) {
    """ + constructorStmts + """
        }"""

    return msgClassConsGen
  }

  /*
   * Message Class Constructor Generation
   */

  private def msgClassConstructorGen(message: Message): String = {
    var msgClassConsGen: String = ""
    var msgConsStr = new StringBuilder(8 * 1024)
    var constructorStmts = new StringBuilder(8 * 1024)

    try {
      message.Elements.foreach(element => {
        if (element != null) {
          msgConsStr.append("%s: %s, ".format(element.Name, element.FieldTypePhysicalName))
          constructorStmts.append("%s this.%s = %s; %s ".format(msgConstants.pad2, element.Name, element.Name, msgConstants.newline))
        }
      })
      val msgStr = msgConsStr.toString
      //log.info("constructor Generation ===================" + msgStr.substring(0, msgStr.length() - 1))
      msgClassConsGen = msgConstructor(message, msgStr, constructorStmts.toString)
    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        throw e
      }
    }
    return msgClassConsGen.toString
  }

  /*
   * Genertae getAttributetype(name: String)
   */
  private def generateGetAttributeType() = {
    """
    override def getAttributeType(name: String): AttributeTypeInfo = {
      if (name == null || name.trim() == "") return null;
      attributeTypes.foreach(attributeType => {
        if(attributeType.getName == caseSensitiveKey(name))
          return attributeType
      }) 
      return null;
    }
  
  """
  }

  /*
   * generateAttributeTypes 
   */
  private def getAttributeTypes(message: Message): String = {
    val getAttributeTypes = genGetAttributeMethod(message)
    """
      var attributeTypes = generateAttributeTypes;
      """ + getAttributeTypes

  }

  /*
   * generate getAttributeTypes method
   */
  private def genGetAttributeMethod(message: Message): String = {
    var arrsize: Int = 0;
    if (message.Elements == null)
      arrsize = 0
    else arrsize = message.Elements.size
    """
    private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
      var attributeTypes = new Array[AttributeTypeInfo](""" + arrsize + """);
   """ + genGetAttributeTypes(message) + """
     
      return attributeTypes
    } 
    """

  }

  private def genGetAttributeTypes(message: Message): String = {
    var getAttributeTypes = new StringBuilder(8 * 1024)

    if (message.Elements != null) {
      message.Elements.foreach(field => {
        if (field.AttributeTypeInfo != null)
          getAttributeTypes.append("%s attributeTypes(%s) = new AttributeTypeInfo(\"%s\", %s, AttributeTypeInfo.TypeCategory.%s, %s, %s, %s)%s".format(msgConstants.pad2, field.FieldOrdinal, field.Name, field.FieldOrdinal, field.AttributeTypeInfo.typeCategaryName, field.AttributeTypeInfo.valTypeId, field.AttributeTypeInfo.keyTypeId, field.AttributeTypeInfo.valSchemaId, msgConstants.newline))
      })

    }

    return getAttributeTypes.toString()
  }

  /*
   * Get Method generation function for Fixed Messages
   */
  private def getFuncGeneration(fields: List[Element]): String = {
    var getMethod = new StringBuilder(8 * 1024)
    var getmethodStr: String = ""
    try {
      fields.foreach(field => {
        if (field != null) {
          getmethodStr = """
        def get""" + field.Name.capitalize + """: """ + field.FieldTypePhysicalName + """= {
        	return this.""" + field.Name + """;
        }          
        """
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
   * Get All Attribute Values of the message/Container
   */

  private def getAllAttributeValuesFixed(message: Message): String = {
    val arraysize = message.Elements.size
    """
    override def getAllAttributeValues(): Array[AttributeValue] = { // Has ( value, attributetypeinfo))
      var attributeVals = new Array[AttributeValue](""" + arraysize + """);
      try{
 """ + getAttributeFixed(message.Elements) + """       
      }""" + msgConstants.catchStmt + """
      return attributeVals;
    }      
    """
  }

  /*
   * Get attributess for Fixed - GetAllAttributeValues
   */
  private def getAttributeFixed(fields: List[Element]): String = {
    var getAttributeFixedStrBldr = new StringBuilder(8 * 1024)
    var getAttributeFixed: String = ""
    try {
      fields.foreach(field => {
        if (field != null) {
          getAttributeFixedStrBldr.append("%sattributeVals(%s) = new AttributeValue(this.%s, keyTypes(\"%s\")) %s".format(msgConstants.pad4, field.FieldOrdinal, field.Name, field.Name, msgConstants.newline));
        }
      })
    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        throw e
      }
    }
    return getAttributeFixedStrBldr.toString

  }

  /*
   * Get By Ordinal Function generation
   */
  private def getFuncByOffset(fields: List[Element], msgName: String): String = {
    var getFuncByOffset: String = ""
    getFuncByOffset = """
      
    override def get(index : Int) : AnyRef = { // Return (value, type)
      try{
        index match {
   """ + getByOffset(fields) + """
      	 case _ => throw new Exception(s"$index is a bad index for message """ + msgName + """");
    	  }       
     }""" + msgConstants.catchStmt + """
    }      
    """
    return getFuncByOffset
  }

  /*
   * Get By Ordinal Function generation
   */
  private def getByOffset(fields: List[Element]): String = {
    var getByOffset = new StringBuilder(8 * 1024)
    try {
      fields.foreach(field => {
        if (field != null) {
          getByOffset.append("%scase %s => return this.%s.asInstanceOf[AnyRef]; %s".format(msgConstants.pad2, field.FieldOrdinal, field.Name, msgConstants.newline))
        }
      })
    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        throw e
      }
    }
    return getByOffset.toString
  }

  /*
   * Set By Ordinal Function generation  -- setByOffset(fields, msgName) 
   */
  private def setFuncByOffset(fields: List[Element], msgName: String, mdMgr: MdMgr, timePartKey: String): String = {
    var getFuncByOffset: String = ""
    getFuncByOffset = """
      
    def set(index : Int, value :Any): Unit = {
      if (value == null) throw new Exception(s"Value is null for index $index in message """ + msgName + """ ")
      try{
        index match {
 """ + getSetByIndexStr(fields, msgName, mdMgr, timePartKey) + """
        case _ => throw new Exception(s"$index is a bad index for message """ + msgName + """");
        }
    	}""" + msgConstants.catchStmt + """
    }      
    """
    return getFuncByOffset
  }

  /*
   * Set method by index for fixed Messages
   */

  private def getSetByIndexStr(fields: List[Element], msgName: String, mdMgr: MdMgr, timePartKey: String): String = {
    var getSetByIndexStr = new StringBuilder(8 * 1024)
    try {
      fields.foreach(field => {
        val fieldBaseType: BaseTypeDef = field.FldMetaataType
        val fieldType = fieldBaseType.tType.toString().toLowerCase()
        val fieldTypeType = fieldBaseType.tTypeType.toString().toLowerCase()
        fieldTypeType match {
          case "tscalar" => {
            getSetByIndexStr = getSetByIndexStr.append(setByIndexScalar(field, msgName, timePartKey))
          }
          case "tcontainer" => {
            fieldType match {
              case "tarray" => {
                var arrayType: ArrayTypeDef = null
                arrayType = fieldBaseType.asInstanceOf[ArrayTypeDef]
                getSetByIndexStr = getSetByIndexStr.append(setByIndexArrays(field, msgName))
              }
              case "tstruct" => {
                var ctrDef: ContainerDef = null;
                ctrDef = mdMgr.Container(field.Ttype, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
                if (ctrDef == null)
                  ctrDef = mdMgr.Message(field.Ttype, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
                getSetByIndexStr = getSetByIndexStr.append(setByIndexContainer(field, msgName))
              }
              case "tmap" => {
                getSetByIndexStr = getSetByIndexStr.append(setByIndexForMapFixed(field, msgName, mdMgr))
              }
              case "tmsgmap" => {
                var ctrDef: ContainerDef = mdMgr.Container(field.Ttype, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
                getSetByIndexStr = getSetByIndexStr.append(setByIndexContainer(field, msgName))
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
      })
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    }
    return getSetByIndexStr.toString();
  }

  /*
   * Set By Ordinal Function generation
   */
  private def setByIndexScalar(field: Element, msgName: String, timePartKey: String): String = {
    var setByOffset = new StringBuilder(8 * 1024)
    try {
      if (field != null) {
        if (timePartKey != null && timePartKey.trim().size != 0 && field.Name.equalsIgnoreCase(timePartKey)) {
          setByOffset.append("%scase %s => { %s".format(msgConstants.pad4, field.FieldOrdinal, msgConstants.newline))
          setByOffset.append("%sif(value.isInstanceOf[%s]){ %s".format(msgConstants.pad4, field.FieldTypePhysicalName, msgConstants.newline))
          setByOffset.append("%s  this.%s = value.asInstanceOf[%s]; %s".format(msgConstants.pad4, field.Name, field.FieldTypePhysicalName, msgConstants.newline))
          setByOffset.append("%s  setTimePartitionData; %s".format(msgConstants.pad4, msgConstants.newline))
          setByOffset.append("%s} else throw new Exception(s\"Value is the not the correct type for field %s in message %s\") %s".format(msgConstants.pad4, field.Name, msgName, msgConstants.newline))
          setByOffset.append("%s} %s".format(msgConstants.pad4, msgConstants.newline))
        } else {
          setByOffset.append("%scase %s => { %s".format(msgConstants.pad4, field.FieldOrdinal, msgConstants.newline))
          setByOffset.append("%sif(value.isInstanceOf[%s]) %s".format(msgConstants.pad4, field.FieldTypePhysicalName, msgConstants.newline))
          setByOffset.append("%s  this.%s = value.asInstanceOf[%s]; %s".format(msgConstants.pad4, field.Name, field.FieldTypePhysicalName, msgConstants.newline))
          setByOffset.append("%s else throw new Exception(s\"Value is the not the correct type for field %s in message %s\") %s".format(msgConstants.pad4, field.Name, msgName, msgConstants.newline))
          setByOffset.append("%s} %s".format(msgConstants.pad4, msgConstants.newline))
        }
      }
    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        throw e
      }
    }
    return setByOffset.toString
  }

  private def setByIndexArrays(field: Element, msgName: String): String = {
    var setByOffset = new StringBuilder(8 * 1024)
    var ttype: String = ""
    try {
      val implName = field.FieldTypeImplementationName
      var arrayType = field.FldMetaataType.asInstanceOf[ArrayTypeDef]
      val typetype = arrayType.elemDef.tTypeType.toString().toLowerCase()

      if (typetype.equals("tscalar")) {
        if (implName != null && implName.trim() != "") {
          ttype = "_"
          //setByOffset.append(setByIndexArrayScalar(field, msgName));
        }
      } else if (typetype.equals("tcontainer")) {
        ttype = "ContainerInterface"

      }
      setByOffset.append(setByIndexForArrayContainerFixed(field, msgName, ttype));

    } catch {
      case e: Exception => throw e
    }
    setByOffset.toString
  }

  /*
   *Set ByIndex for Array of Containerinterface
   */
  private def setByIndexForArrayContainerFixed(field: Element, msgName: String, ttype: String): String = {
    var setByOffset = new StringBuilder(8 * 1024)
    try {
      val implName = field.FieldTypeImplementationName
      var arrayType = field.FldMetaataType.asInstanceOf[ArrayTypeDef]
      var typeStr: String = ""
      if (field.FldMetaataType.typeString.toString().split("\\[").size == 2) {
        typeStr = field.FldMetaataType.typeString.toString().split("\\[")(1)
      }
      if (field != null) {
        setByOffset.append("%scase %s => { %s".format(msgConstants.pad4, field.FieldOrdinal, msgConstants.newline))
        setByOffset.append("%sif(value.isInstanceOf[%s]) %s".format(msgConstants.pad4, field.FieldTypePhysicalName, msgConstants.newline))
        setByOffset.append("%s  this.%s = value.asInstanceOf[%s]; %s".format(msgConstants.pad4, field.Name, field.FieldTypePhysicalName, msgConstants.newline))
        setByOffset.append("%selse if(value.isInstanceOf[scala.Array[%s]]) %s".format(msgConstants.pad4, ttype, msgConstants.newline))
        setByOffset.append("%s  this.%s = value.asInstanceOf[scala.Array[%s]].map(v => v.asInstanceOf[%s); %s".format(msgConstants.pad4, field.Name, ttype, typeStr, msgConstants.newline))
        setByOffset.append("%s else throw new Exception(s\"Value is the not the correct type for field %s in message %s\") %s".format(msgConstants.pad4, field.Name, msgName, msgConstants.newline))
        setByOffset.append("%s} %s".format(msgConstants.pad4, msgConstants.newline))
      }
    } catch {
      case e: Exception => throw e
    }
    setByOffset.toString
  }

  /*
   * Set By Ordinal Function generation
   */
  private def setByIndexContainer(field: Element, msgName: String): String = {
    var setByOffset = new StringBuilder(8 * 1024)
    try {
      if (field != null) {

        setByOffset.append("%scase %s => { %s".format(msgConstants.pad4, field.FieldOrdinal, msgConstants.newline))
        setByOffset.append("%sif(value.isInstanceOf[%s]) %s".format(msgConstants.pad4, field.FieldTypePhysicalName, msgConstants.newline))
        setByOffset.append("%s  this.%s = value.asInstanceOf[%s]; %s".format(msgConstants.pad4, field.Name, field.FieldTypePhysicalName, msgConstants.newline))
        setByOffset.append("%selse if(value.isInstanceOf[ContainerInterface]) %s".format(msgConstants.pad4, msgConstants.newline))
        setByOffset.append("%s  this.%s = value.asInstanceOf[%s]; %s".format(msgConstants.pad4, field.Name, field.FieldTypePhysicalName, msgConstants.newline))
        setByOffset.append("%s else throw new Exception(s\"Value is the not the correct type for field %s in message %s\") %s".format(msgConstants.pad4, field.Name, msgName, msgConstants.newline))
        setByOffset.append("%s} %s".format(msgConstants.pad4, msgConstants.newline))
      }
    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        throw e
      }
    }
    return setByOffset.toString
  }

  /*
   * Set Byindex - type Map of Scalar, Container
   */
  private def setByIndexForMapFixed(field: Element, msgName: String, mdMgr: MdMgr): String = {
    var setByOffset = new StringBuilder(8 * 1024)
    try {

      var maptypeDef = field.FldMetaataType.asInstanceOf[MapTypeDef]
      val typeInfo = maptypeDef.valDef.tTypeType.toString().toLowerCase()
      var typetyprStr: String = maptypeDef.valDef.tType.toString().toLowerCase()
      typeInfo match {
        case "tscalar" => {
          setByOffset = setByOffset.append(setByIndexScalar(field, msgName, null))
        }
        case "tcontainer" => {
          typetyprStr match {
            case "tarray" => { throw new Exception("Not supporting array of array"); }
            case "tstruct" => {

              var msgDef: ContainerDef = null;
              msgDef = mdMgr.Message(maptypeDef.valDef.FullName, -1, true).getOrElse(null)
              if (msgDef == null)
                msgDef = mdMgr.Container(maptypeDef.valDef.FullName, -1, true).getOrElse(null)
              setByOffset = setByOffset.append(setByIndexForMapofContainerFixed(field, msgName, msgDef))
            }
            case "tmsgmap" => {
              var ctrDef: ContainerDef = null;
              ctrDef = mdMgr.Message(maptypeDef.valDef.FullName, -1, true).getOrElse(null)
              if (ctrDef == null)
                ctrDef = mdMgr.Container(maptypeDef.valDef.FullName, -1, true).getOrElse(null)
              setByOffset = setByOffset.append(setByIndexForMapofContainerFixed(field, msgName, ctrDef))
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
    } catch {
      case e: Exception => throw e
    }
    setByOffset.toString

  }
  /*
   * Set By induax map of container
   */

  private def setByIndexForMapofContainerFixed(field: Element, msgName: String, ctrDef: ContainerDef): String = {
    var setByOffset = new StringBuilder(8 * 1024)
    if (field != null) {
      setByOffset.append("%scase %s => { %s".format(msgConstants.pad4, field.FieldOrdinal, msgConstants.newline))
      setByOffset.append("%sif(value.isInstanceOf[%s]) %s".format(msgConstants.pad4, field.FieldTypePhysicalName, msgConstants.newline))
      setByOffset.append("%s  this.%s = value.asInstanceOf[%s]; %s".format(msgConstants.pad4, field.Name, field.FieldTypePhysicalName, msgConstants.newline))
      setByOffset.append("%selse if(value.isInstanceOf[scala.collection.immutable.Map[String, ContainerInterface]]) %s".format(msgConstants.pad4, msgConstants.newline))
      setByOffset.append("%s  this.%s = value.asInstanceOf[scala.collection.immutable.Map[String, ContainerInterface]].map(v => (v._1, v._2.asInstanceOf[%s])); %s".format(msgConstants.pad4, field.Name, ctrDef.PhysicalName, msgConstants.newline))
      setByOffset.append("%s else throw new Exception(s\"Value is the not the correct type for field %s in message %s\") %s".format(msgConstants.pad4, field.Name, msgName, msgConstants.newline))
      setByOffset.append("%s} %s".format(msgConstants.pad4, msgConstants.newline))
    }
    setByOffset.toString;
  }

  /*
   * Set By Ordinal Function generation
   */
  private def setByOffset(fields: List[Element], msgName: String): String = {
    var setByOffset = new StringBuilder(8 * 1024)
    try {
      fields.foreach(field => {
        if (field != null) {
          setByOffset.append("%scase %s => { %s".format(msgConstants.pad4, field.FieldOrdinal, msgConstants.newline))
          setByOffset.append("%sif(value.isInstanceOf[%s]) %s".format(msgConstants.pad4, field.FieldTypePhysicalName, msgConstants.newline))
          setByOffset.append("%s  this.%s = value.asInstanceOf[%s]; %s".format(msgConstants.pad4, field.Name, field.FieldTypePhysicalName, msgConstants.newline))
          setByOffset.append("%s else throw new Exception(s\"Value is the not the correct type for index $index in message %s\") %s".format(msgConstants.pad4, msgName, msgConstants.newline))
          setByOffset.append("%s} %s".format(msgConstants.pad4, msgConstants.newline))
        }
      })
    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        throw e
      }
    }
    return setByOffset.toString
  }

  private def messageContructor(message: Message): String = {
    var msgInterfaceType: String = " ";

    if (message.MsgType.equalsIgnoreCase("message")) {
      msgInterfaceType = "MessageFactoryInterface";
    } else if (message.MsgType.equalsIgnoreCase("container")) {
      msgInterfaceType = "ContainerFactoryInterface";
    }
    """
    def this(factory:""" + msgInterfaceType + """) = {
      this(factory, null)
     }
    
    def this(other: """ + message.Name + """) = {
      this(other.getFactory.asInstanceOf[""" + msgInterfaceType + """], other)
    }
"""
  }

  /** isCaseSensitiveFunc function in generate message code **/
  private def isCaseSensitiveFunc(message: Message): String = {
    """    def isCaseSensitive(): Boolean = """ + message.Name + """.isCaseSensitive(); """
  }

  /** caseSensitiveFunc function in generate message code **/

  private def caseSensitiveFunc(message: Message): String = {
    """
    def caseSensitiveKey(keyName: String): String = {
      if(isCaseSensitive)
        return keyName;
      else return keyName.toLowerCase;
    }


    """

  }

  /*
   * some overridable methods from MessageInterface
   */
  private def methodsFromMessageInterface(message: Message): String = {
    """    
     if (other != null && other != this) {
      // call copying fields from other to local variables
      fromFunc(other)
    }
    
    override def save: Unit = { """ + message.Name + """.saveOne(this) }
  
    def Clone(): ContainerOrConcept = { """ + message.Name + """.build(this) }
"""
  }

  /*
   * GetOrElse method for Fixed messages
   */
  private def getOrElseFunc(): String = {
    """
    override def getOrElse(keyName: String, defaultVal: Any): AnyRef = { // Return (value)
      if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name "+keyName);
      val key = caseSensitiveKey(keyName);
      try {
        return get(key)
       } catch {
        case e: Exception => {
          log.debug("", e)
          if(defaultVal == null) return null;
          return defaultVal.asInstanceOf[AnyRef];
        }
      }
      return null;
    }
   """
  }

  /*
   * GetOrElse by index
   */
  private def getOrElseByIndexFunc = {
    """
    override def getOrElse(index: Int, defaultVal: Any): AnyRef = { // Return (value)
      try {
        return get(index);
        } catch {
        case e: Exception => {
          log.debug("", e)
          if(defaultVal == null) return null;
          return defaultVal.asInstanceOf[AnyRef];
        }
      }
      return null;
    }
  """
  }
  /*
   * parititon keys code generation
   */

  private def generateParitionKeysData(message: Message): String = {
    var paritionKeysGen = new StringBuilder(8 * 1024)
    var returnPartitionKeyStr: String = ""
    val arryOfStr: String = "Array[String]()";

    if (message.PartitionKeys != null && message.PartitionKeys.size > 0) {
      paritionKeysGen.append("{" + msgConstants.newline)
      paritionKeysGen.append(msgConstants.partitionKeyVar.format(msgConstants.pad2, msgConstants.newline))
      paritionKeysGen.append(msgConstants.pad2 + "try {" + msgConstants.newline)

      message.PartitionKeys.foreach(key => {
        message.Elements.foreach(element => {
          if (element.Name.equalsIgnoreCase(key)) {
            paritionKeysGen.append("%s partitionKeys += %s.toString(get(caseSensitiveKey(\"%s\")).asInstanceOf[%s]);%s".format(msgConstants.pad2, element.FldMetaataType.implementationName, element.Name, element.FieldTypePhysicalName, msgConstants.newline)) //"+ com.ligadata.BaseTypes.StringImpl+".toString(get"+element.Name.capitalize+") ")
          }
        })
      })
      paritionKeysGen.append("%s }".format(msgConstants.pad2))
      paritionKeysGen.append(msgConstants.catchStmt)
      paritionKeysGen.append("%s partitionKeys.toArray; %s".format(msgConstants.pad2, msgConstants.newline))
      paritionKeysGen.append("%s ".format(msgConstants.newline))
      paritionKeysGen.append("%s} %s".format(msgConstants.pad2, msgConstants.newline))
      returnPartitionKeyStr = msgConstants.paritionKeyData.format(msgConstants.pad2, paritionKeysGen.toString)
    } else {
      returnPartitionKeyStr = msgConstants.paritionKeyData.format(msgConstants.pad2, arryOfStr)
    }

    returnPartitionKeyStr
  }

  /*
   * primary keys code generation
   */
  private def generatePrimaryKeysData(message: Message): String = {
    var primaryKeysGen = new StringBuilder(8 * 1024)
    var returnPrimaryKeyStr: String = ""
    val arryOfStr: String = "Array[String]()";

    if (message.PrimaryKeys != null && message.PrimaryKeys.size > 0) {
      primaryKeysGen.append("{" + msgConstants.newline)
      primaryKeysGen.append(msgConstants.primaryKeyVar.format(msgConstants.pad2, msgConstants.newline))
      primaryKeysGen.append(msgConstants.pad2 + "try {" + msgConstants.newline)
      message.PrimaryKeys.foreach(key => {
        message.Elements.foreach(element => {
          if (element.Name.equalsIgnoreCase(key)) {
            primaryKeysGen.append("%s primaryKeys += %s.toString(get(caseSensitiveKey(\"%s\")).asInstanceOf[%s]);%s".format(msgConstants.pad2, element.FldMetaataType.implementationName, element.Name, element.FieldTypePhysicalName, msgConstants.newline)) //"+ com.ligadata.BaseTypes.StringImpl+".toString(get"+element.Name.capitalize+") ")
          }
        })
      })
      primaryKeysGen.append("%s }".format(msgConstants.pad2))
      primaryKeysGen.append(msgConstants.catchStmt)
      primaryKeysGen.append("%s primaryKeys.toArray; %s".format(msgConstants.pad2, msgConstants.newline))
      primaryKeysGen.append("%s ".format(msgConstants.newline))
      primaryKeysGen.append("%s} %s".format(msgConstants.pad2, msgConstants.newline))
      returnPrimaryKeyStr = msgConstants.primaryKeyData.format(msgConstants.pad2, primaryKeysGen.toString)
    } else {
      returnPrimaryKeyStr = msgConstants.primaryKeyData.format(msgConstants.pad2, arryOfStr)
    }
    returnPrimaryKeyStr
  }

  /*
   * Builder method in the message definition class
   */

  private def builderMethod(): String = {
    """
    def newBuilder(): Builder = {
    	return new Builder();
    }
    
    """

  }

  private def partitionKeys(message: Message): String = {

    var paritionKeysStr: String = ""
    if (message.PartitionKeys == null || message.PartitionKeys.size == 0) {
      paritionKeysStr = "val partitionKeys: Array[String] = Array[String](); ";
    } else {
      paritionKeysStr = "val partitionKeys = Array(" + message.PartitionKeys.map(p => { " \"" + p.toLowerCase + "\"" }).mkString(", ") + ");";

    }

    return paritionKeysStr

  }

  private def primaryKeys(message: Message): String = {

    var primaryKeysStr: String = ""
    if (message.PrimaryKeys == null || message.PrimaryKeys.size == 0) {
      primaryKeysStr = "val primaryKeys: Array[String] = Array[String](); ";
    } else {
      primaryKeysStr = "val primaryKeys: Array[String] = Array(" + message.PrimaryKeys.map(p => { "\"" + p.toLowerCase + "\"" }).mkString(", ") + ");";

    }

    return primaryKeysStr

  }

  private def hasPrimaryKeysFunc() = {
    """
  override def hasPrimaryKey(): Boolean = {
    if (primaryKeys == null) return false;
    (primaryKeys.size > 0);
  }
  """
  }

  private def hasPartitionKeyFunc() = {
    """
  override def hasPartitionKey(): Boolean = {
    if (partitionKeys == null) return false;
    (partitionKeys.size > 0);
  }
  """
  }

  /*
   * handling system colums in Mapped Messages
   */

  private def getSystemColumns(): String = {

    return ""

  }

  /*
   * set method with key as arguments
   */

  private def getSetByName(): String = {

    return ""

  }

  /*
   * Get By String - Fixed Messages
   */

  private def getByStringhFixed(message: Message): String = {
    """
    override def get(key: String): AnyRef = {
    try {
      // Try with reflection
      return getByName(caseSensitiveKey(key))
    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        // Call By Name
        return getWithReflection(caseSensitiveKey(key))
        }
      }
    }      
    """
  }

  /*
   * GetBy Name method for fixed messages
   */
  private def getByName(message: Message): String = {
    """
    private def getByName(keyName: String): AnyRef = {
     if(keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name "+keyName);
      val key = caseSensitiveKey(keyName);
   
      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container """ + message.Name + """", null);
      return get(keyTypes(key).getIndex)
  }
  """
  }

  /*
   * Get With Reflection
   */
  private def getWithReflection(msg: Message): String = {
    """
    private def getWithReflection(keyName: String): AnyRef = {
      if(keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name "+keyName);
      val key = caseSensitiveKey(keyName);
      val ru = scala.reflect.runtime.universe
      val m = ru.runtimeMirror(getClass.getClassLoader)
      val im = m.reflect(this)
      val fieldX = ru.typeOf[""" + msg.Name + """].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
      val fmX = im.reflectField(fieldX)
      return fmX.get.asInstanceOf[AnyRef];      
    } 
   """
  }

  /*
   * getAttributeNameAndValueIterator for Fixed messages to retrieve all attributes of message
   */

  private def getAttributeNameAndValueIterator = {
    """
    override def getAttributeNameAndValueIterator(): java.util.Iterator[AttributeValue] = {
      //getAllAttributeValues.iterator.asInstanceOf[java.util.Iterator[AttributeValue]];
    return null; // Fix - need to test to make sure the above iterator works properly
  
    }
    """
  }

  /*
     * Set By Key - Fixed Messages
     */
  private def setByKeyFunc(message: Message): String = {

    """
    override def set(keyName: String, value: Any) = {
      if(keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name "+keyName);
      val key = caseSensitiveKey(keyName);
      try {
   
  """ + setByKeyFuncStr(message) + """
      }""" + msgConstants.catchStmt + """
    }
  """
  }

  /*
   * SetBy KeynameFunc - Generation
   */

  private def setByKeyFuncStr(message: Message): String = {
    if (message.Elements == null)
      return "";
    var keysStr = new StringBuilder(8 * 1024)
    try {
      keysStr.append("%s if (!keyTypes.contains(key)) throw new KeyNotFoundException(s\"Key $key does not exists in message %s\", null)%s".format(msgConstants.pad3, message.Name, msgConstants.newline));
      keysStr.append("%s set(keyTypes(key).getIndex, value); %s".format(msgConstants.pad3, msgConstants.newline));

    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        throw e
      }
    }
    keysStr.toString()
  }

  /*
   * Set Value Type and Value By  atrribute name
   */
  private def setValueAndValueTypeByKeyFunc = {
    """
    override def set(key: String, value: Any, valTyp: String) = {
      throw new Exception ("Set Func for Value and ValueType By Key is not supported for Fixed Messages" )
    }
  """
  }

  private def keyTypesMap(fields: List[Element]): String = {
    //val keysStr = keyTypesStr(fields);
    //if (keysStr == null || keysStr.trim == "" || keysStr.length() < 2)
    return "%s var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;%s".format(msgConstants.pad2, msgConstants.newline)
    //return "%sprivate var keyTypes = Map(%s);%s".format(msgConstants.pad2, keysStr.substring(0, keysStr.length() - 1), msgConstants.newline);
  }

  /*
   * Set Method Generation Function for Fixed Messages
   */
  private def setFuncGeneration(fields: List[Element]): String = {
    var setMethod = new StringBuilder(8 * 1024)
    var setmethodStr: String = ""
    try {
      fields.foreach(field => {
        if (field != null) {
          setmethodStr = """
        def set""" + field.Name.capitalize + """(value: """ + field.FieldTypePhysicalName + """): Unit = {
        	this.""" + field.Name + """ = value;
        }
        """
          setMethod = setMethod.append(setmethodStr.toString())
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

  /*
   * Get All Attribute Names
   */
  private def getAttributeNamesFixed = {
    """
    override def getAttributeNames(): Array[String] = {
      try {
        if (keyTypes.isEmpty) {
          return null;
          } else {
          return keyTypes.keySet.toArray;
        }
      } catch {
        case e: Exception => {
          log.debug("", e)
          throw e
        }
      }
      return null;
    }
 """
  }

  /*
   * From Function for Fixed messages
   */
  private def getFromFuncFixed(message: Message, mdMgr: MdMgr): String = {
    """
    private def fromFunc(other: """ + message.Name + """): """ + message.Name + """ = {  
   """ + getFromFuncStr(message, mdMgr) + """
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
      message.Elements.foreach(field => {
        val fieldBaseType: BaseTypeDef = field.FldMetaataType
        val fieldType = fieldBaseType.tType.toString().toLowerCase()
        val fieldTypeType = fieldBaseType.tTypeType.toString().toLowerCase()
        fieldTypeType match {
          case "tscalar" => {
            fromFuncBuf = fromFuncBuf.append(fromFuncForScalarFixed(field))
            //log.info("fieldBaseType.implementationName    " + fieldBaseType.implementationName)
          }
          case "tcontainer" => {
            fieldType match {
              case "tarray" => {
                var arrayType: ArrayTypeDef = null
                arrayType = fieldBaseType.asInstanceOf[ArrayTypeDef]
                fromFuncBuf = fromFuncBuf.append(fromFuncForArrayFixed(field))
              }
              case "tstruct" => {
                var ctrDef: ContainerDef = null;
                ctrDef = mdMgr.Container(field.Ttype, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
                if (ctrDef == null)
                  ctrDef = mdMgr.Message(field.Ttype, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
                fromFuncBuf = fromFuncBuf.append(fromFuncForStructFixed(field))
              }
              case "tmap" => {
                fromFuncBuf = fromFuncBuf.append(fromFuncForMapFixed(field, mdMgr))
              }
              case "tmsgmap" => {
                var ctrDef: ContainerDef = mdMgr.Container(field.Ttype, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
                fromFuncBuf = fromFuncBuf.append(fromFuncForStructFixed(field))
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
      })
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    }

    return fromFuncBuf.toString();
  }

  /*
   * From Func - generate code for scala types
   */
  private def fromFuncForScalarFixed(field: Element): String = {
    var fromFuncBuf = new StringBuilder(8 * 1024)
    try {
      val implClone = field.FieldTypeImplementationName + ".Clone";
      if (implClone != null && implClone.trim() != "") {
        fromFuncBuf = fromFuncBuf.append("%sthis.%s = %s(other.%s);%s".format(msgConstants.pad3, field.Name, implClone, field.Name, msgConstants.newline))
      }
    } catch {
      case e: Exception => throw e
    }
    fromFuncBuf.toString
  }

  /*
   * From Func - generate code for array
   */
  private def fromFuncForArrayFixed(field: Element): String = {
    var fromFuncBuf = new StringBuilder(8 * 1024)
    try {
      val implName = field.FieldTypeImplementationName
      var arrayType = field.FldMetaataType.asInstanceOf[ArrayTypeDef]
      val typetype = arrayType.elemDef.tTypeType.toString().toLowerCase()

      if (typetype.equals("tscalar")) {
        if (implName != null && implName.trim() != "") {
          fromFuncBuf.append(fromFuncForArrayScalarFixed(field));
        }
      } else if (typetype.equals("tcontainer")) {
        val fieldType = //arrayType.elemDef.tTypeType.toString().equalsIgnoreCase
          ///  log.info("22222222222222222" + fieldType);
          // if (fieldType.equalsIgnoreCase("tstruct") || (fieldType.equalsIgnoreCase("tmsgmap"))){
          fromFuncBuf.append(fromFuncForArrayContainerFixed(field));
        //  }

      }
    } catch {
      case e: Exception => throw e
    }
    fromFuncBuf.toString
  }

  /*
   * From Func for Array of Scalar
   */
  private def fromFuncForArrayScalarFixed(field: Element): String = {
    var fromFuncBuf = new StringBuilder(8 * 1024)
    try {
      var arrayType = field.FldMetaataType.asInstanceOf[ArrayTypeDef]
      var typeStr: String = ""
      if (field.FldMetaataType.typeString.toString().split("\\[").size == 2) {
        typeStr = field.FldMetaataType.typeString.toString().split("\\[")(1)
      }

      val implName = field.FieldTypeImplementationName
      if (implName != null && implName.trim() != "") {
        fromFuncBuf = fromFuncBuf.append("%s if (other.%s != null ) { %s".format(msgConstants.pad2, field.Name, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s %s = new %s(other.%s.length); %s".format(msgConstants.pad2, field.Name, field.FldMetaataType.typeString, field.Name, msgConstants.newline)) //typ.get.typeString
        fromFuncBuf = fromFuncBuf.append("%s %s = other.%s.map(v => %s.Clone(v).asInstanceOf[%s); %s".format(msgConstants.pad2, field.Name, field.Name, implName, typeStr, msgConstants.newline)) //arrayType.elemDef.implementationName
        fromFuncBuf = fromFuncBuf.append("%s } %s".format(msgConstants.pad2, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s else this.%s = null; %s".format(msgConstants.pad2, field.Name, msgConstants.newline))
      }

    } catch {
      case e: Exception => throw e
    }
    fromFuncBuf.toString
  }
  /*
   * From Func for Array of Container
   */
  /*
   * From Func for Array of Scalar
   */
  private def fromFuncForArrayContainerFixed(field: Element): String = {
    var fromFuncBuf = new StringBuilder(8 * 1024)
    try {
      val implName = field.FieldTypeImplementationName
      var arrayType = field.FldMetaataType.asInstanceOf[ArrayTypeDef]
      var typeStr: String = ""
      if (field.FldMetaataType.typeString.toString().split("\\[").size == 2) {
        typeStr = field.FldMetaataType.typeString.toString().split("\\[")(1)
      }
      fromFuncBuf = fromFuncBuf.append("%s if (other.%s != null) { %s".format(msgConstants.pad2, field.Name, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s %s = new %s(other.%s.length) %s".format(msgConstants.pad2, field.Name, field.FldMetaataType.typeString, field.Name, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s %s = other.%s.map(f => f.Clone.asInstanceOf[%s ); %s".format(msgConstants.pad2, field.Name, field.Name, typeStr, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s } %s".format(msgConstants.pad2, msgConstants.newline))
      fromFuncBuf = fromFuncBuf.append("%s else %s = null; %s".format(msgConstants.pad2, field.Name, msgConstants.newline))
    } catch {
      case e: Exception => throw e
    }
    fromFuncBuf.toString
  }
  /*
   * From Func - Generate code for ArrayBuf
   */
  /*
  private def fromFuncForArrayBufFixed(field: Element): String = {
    var fromFuncBuf = new StringBuilder(8 * 1024)
    try {
      val implName = field.FieldTypeImplementationName
      if (implName != null && implName.trim() != "") {
        fromFuncBuf = fromFuncBuf.append("%s if (other.%s != null ) { %s".format(msgConstants.pad2, field.Name, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s %s.clear;  %s".format(msgConstants.pad2, field.Name, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s other.%s.map(v =>{ %s :+= %s.Clone(v)}); %s".format(msgConstants.pad2, field.Name, field.Name, implName, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s } %s".format(msgConstants.pad2, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s else this.%s = null; %s".format(msgConstants.pad2, field.Name, msgConstants.newline))
      }
    } catch {
      case e: Exception => throw e
    }
    fromFuncBuf.toString

  }
*/

  /*
   * From Func for Containertype as Message
   */
  private def fromFuncForStructFixed(field: Element): String = {
    var fromFuncBuf = new StringBuilder(8 * 1024)
    try {
      val implName = field.FieldTypeImplementationName
      if (implName != null && implName.trim() != "") {
        fromFuncBuf = fromFuncBuf.append("%s if (other.%s != null) { %s".format(msgConstants.pad2, field.Name, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s %s = other.%s.Clone.asInstanceOf[%s] %s".format(msgConstants.pad2, field.Name, field.Name, field.FieldTypePhysicalName, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s } %s ".format(msgConstants.pad2, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s else %s = null; %s".format(msgConstants.pad2, field.Name, msgConstants.newline))
      }
    } catch {
      case e: Exception => throw e
    }
    fromFuncBuf.toString

  }

  /*
   * From Func for Containertype as Message
   */
  private def fromFuncForMapFixed(field: Element, mdMgr: MdMgr): String = {
    var fromFuncBuf = new StringBuilder(8 * 1024)
    try {

      var maptypeDef = field.FldMetaataType.asInstanceOf[MapTypeDef]
      val typeInfo = maptypeDef.valDef.tTypeType.toString().toLowerCase()
      var typetyprStr: String = maptypeDef.valDef.tType.toString().toLowerCase()

      typeInfo match {
        case "tscalar" => {
          fromFuncBuf = fromFuncBuf.append("%s if (other.%s != null) { %s ".format(msgConstants.pad2, field.Name, msgConstants.newline))
          fromFuncBuf = fromFuncBuf.append("%s this.%s = other.%s.map { a => (com.ligadata.BaseTypes.StringImpl.Clone(a._1), %s.Clone(a._2).asInstanceOf[%s]) }.toMap%s ".format(msgConstants.pad2, field.Name, field.Name, maptypeDef.valDef.implementationName, maptypeDef.valDef.physicalName, msgConstants.newline))
          fromFuncBuf = fromFuncBuf.append("%s} else this.%s = null;%s ".format(msgConstants.pad2, field.Name, msgConstants.newline))
        }
        case "tcontainer" => {
          typetyprStr match {
            case "tarray" => { throw new Exception("Not supporting array of array"); }
            case "tstruct" => {

              var msgDef: ContainerDef = null;
              msgDef = mdMgr.Message(maptypeDef.valDef.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
              if (msgDef == null)
                msgDef = mdMgr.Container(maptypeDef.valDef.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
              /* if (other.maptest != null) {
                this.maptest = other.maptest.map { a => (com.ligadata.BaseTypes.StringImpl.Clone(a._1), a._2.Clone.asInstanceOf[com.ligadata.messages.V1000000000000.IdCodeDimFixedTest]) }.toMap
              }*/
              fromFuncBuf = fromFuncBuf.append("%s if (other.%s != null) { %s ".format(msgConstants.pad2, field.Name, msgConstants.newline))
              fromFuncBuf = fromFuncBuf.append("%s this.%s = other.%s.map { a => (com.ligadata.BaseTypes.StringImpl.Clone(a._1), a._2.Clone.asInstanceOf[%s]) }.toMap%s ".format(msgConstants.pad2, field.Name, field.Name, msgDef.PhysicalName, msgConstants.newline))
              fromFuncBuf = fromFuncBuf.append("%s} else this.%s = null; %s ".format(msgConstants.pad2, field.Name, msgConstants.newline))
            }
            case "tmsgmap" => {
              var ctrDef: ContainerDef = null;
              ctrDef = mdMgr.Message(maptypeDef.valDef.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
              if (ctrDef == null)
                ctrDef = mdMgr.Container(maptypeDef.valDef.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
              fromFuncBuf = fromFuncBuf.append("%s if (other.%s != null) { %s ".format(msgConstants.pad2, field.Name, msgConstants.newline))
              fromFuncBuf = fromFuncBuf.append("%s this.%s = other.%s.map { a => (com.ligadata.BaseTypes.StringImpl.Clone(a._1), a._2.Clone.asInstanceOf[%s]) }.toMap%s ".format(msgConstants.pad2, field.Name, field.Name, ctrDef.PhysicalName, msgConstants.newline))
              fromFuncBuf = fromFuncBuf.append("%s} else this.%s = null; %s ".format(msgConstants.pad2, field.Name, msgConstants.newline))

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
      val implName = field.FieldTypeImplementationName
      /*  if (implName != null && implName.trim() != "") {
        fromFuncBuf = fromFuncBuf.append("%s if (other.%s != null) { %s".format(msgConstants.pad2, field.Name, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s %s = other.%s.Clone.asInstanceOf[%s] %s".format(msgConstants.pad2, field.Name, field.Name, field.FieldTypePhysicalName, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s } %s ".format(msgConstants.pad2, msgConstants.newline))
        fromFuncBuf = fromFuncBuf.append("%s else %s = null; %s".format(msgConstants.pad2, field.Name, msgConstants.newline))
      }
  */
    } catch {
      case e: Exception => throw e
    }
    fromFuncBuf.toString

  }

  /*
    def getKeysStr(keysStr: String) = {
    if (keysStr != null && keysStr.trim() != "") {

      """ 
      var keys = Map(""" + keysStr.toString.substring(0, keysStr.toString.length - 1) + ") \n " +
        """
      var fields: scala.collection.mutable.Map[String, (Int, Any)] = new scala.collection.mutable.HashMap[String, (Int, Any)];
 	"""
    } else {
      """ 
	    var keys = Map[String, Int]()
	    var fields: scala.collection.mutable.Map[String, (Int, Any)] = new scala.collection.mutable.HashMap[String, (Int, Any)];
	  
	  """
    }
  }
    */

  /*
   * GetByName Str - For Fixed Messages
   */
  private def getByNameStr(message: Message): String = {
    if (message.Elements == null)
      return "";
    var keysStr = new StringBuilder(8 * 1024)
    try {
      message.Elements.foreach(field => {
        if (field != null) {
          keysStr.append("%sif (key.equals(\"%s\")) { attributeValue.setValue(this.%s); }%s".format(msgConstants.pad3, field.Name, field.Name, msgConstants.newline));
        }
      })
    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        throw e
      }
    }
    keysStr.toString()
  }
  /*
   * Generate the schema String of the input message/container
   */
  private def generateSchema(message: Message): String = {

    return "val schema: String = \" " + message.Schema + " \" ;"

  }

  /* 
 * SetByName for the mapped messages
 */
  private def setByNameFuncForMappedMsgs(): String = {

    return ""
  }

  /*
   * SetByName for the Fixed Messages
   */
  private def setByNameFuncForFixedMsgs(): String = {

    return ""
  }

  /*
   * GetByName for mapped Messages
   */
  private def getByNameFuncForMapped(): String = {

    return ""
  }

  /*
   * GetByName merthod for Fixed Messages
   */
  private def getByNameFuncForFixed: String = {

    return ""

  }
  private def keyTypesStr(fields: List[Element]): String = {
    var keysStr = new StringBuilder(8 * 1024)
    try {
      if (fields != null) {
        fields.foreach(field => {
          if (field != null) {
            keysStr.append("\"" + field.Name + "\"-> \"" + field.FieldTypePhysicalName + "\",")
          }
        })
      }
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    }
    keysStr.toString()
  }

}
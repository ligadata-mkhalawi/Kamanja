package com.ligadata.msgcompiler

import org.apache.logging.log4j.{ Logger, LogManager }
import com.ligadata.kamanja.metadata._;
import com.ligadata.Exceptions._;
import com.ligadata.KamanjaBase._

class MessageFieldTypesHandler {
  val logger = this.getClass.getName
  lazy val log = LogManager.getLogger(logger)
  val tInt: String = "tInt"
  val tChar: String = "tChar"
  val tString: String = "tString"
  val tLong: String = "tLong"
  val tFloat: String = "tFloat"
  val tDouble: String = "tDouble"
  val tBoolean: String = "tBoolean"
  val tArray: String = "tArray"
  val tContainer: String = "tContainer"
  val tScalar: String = "tScalar"

  //Handling the field types - get the metadata type for each field 
  def handleFieldTypes(message: Message, mdMgr: MdMgr): Message = {
    var argsList: List[(String, String, String, String, Boolean, String)] = List[(String, String, String, String, Boolean, String)]()
    var jarset: Set[String] = Set[String]();
    if (message.Elements == null && message.Fixed.equalsIgnoreCase("true"))
      throw new Exception("Please provide the fields in the message definition since fields are mandatory for Fixed Message");
    if (message.Elements != null) {
      message.Elements.foreach(field => {
        if (field != null) {
          //log.info("field.Ttype =================" + field.Ttype);

          val typ = MdMgr.GetMdMgr.Type(field.Ttype, -1, true) // message.Version.toLong

          if (typ.getOrElse("None").equals("None"))
            throw new Exception("Type not found in metadata for " + field.Name + " given type is " + field.Ttype)

          // to do - check null pointer if type is not avaialble.....
          field.FldMetaataType = typ.get

          val types = getMetadataTypesForMsgFields(field, mdMgr, message.Name)
          field.FieldTypePhysicalName = types(0).asInstanceOf[String]
          field.FieldTypeImplementationName = types(1).asInstanceOf[String]
          field.AttributeTypeInfo = types(2).asInstanceOf[ArrtibuteInfo]

          if (field.AttributeTypeInfo != null) {
            log.info("field.AttributeTypeInfo keyTypeId====" + field.AttributeTypeInfo.keyTypeId);
            log.info("field.AttributeTypeInfo valSchemaId====" + field.AttributeTypeInfo.valSchemaId);
            log.info("field.AttributeTypeInfo valTypeId====" + field.AttributeTypeInfo.valTypeId);
            log.info("field.AttributeTypeInfo typeCategaryName====" + field.AttributeTypeInfo.typeCategaryName);
          }
          //get the fields jarset for adding msg in the metadata
          jarset = jarset ++ getDependencyJarSet(field.FldMetaataType)

          //get the fields args list for addding message in the metadata 
          argsList = (field.NameSpace, field.Name, field.FldMetaataType.NameSpace, field.FldMetaataType.Name, false, null) :: argsList
        }
      })
    }

    /* message.Elements.foreach(field => {

      log.info("*************==================================== " + field.FieldTypeImplementationName);
      log.info("****************** TYPES FROM METADATA START  --- In Message******************************")
      log.info("=========mesage fld type " + field.Name)
      log.info("=========mesage fld type " + field.Ttype)
      log.info("=========mesage fld metadata type " + field.FldMetaataType.tType.toString())
      log.info("=========mesage fld metadata tTypeType " + field.FldMetaataType.tTypeType.toString())
      log.info("=========mesage fld metadata implementationName " + field.FldMetaataType.implementationName)
      //log.info("=========mesage fld size " + types.size)
      //log.info("=========mesage fld 2 :  " + types(1))
      log.info("=========mesage fld  " + field.FieldTypePhysicalName)
      log.info("******************TYPES FROM METADATA End --- In Message ******************************")

    })
*/
    // set the field args list and jarset in message object to retrieve while adding mesage to metadata
    message.ArgsList = argsList
    message.Jarset = jarset

    return message
  }

  /*
   * Get the Dependecy Jar Set for each field
   */

  private def getDependencyJarSet(fieldBaseType: BaseTypeDef): Set[String] = {

    var jarset: Set[String] = Set[String]();

    if ((fieldBaseType.dependencyJarNames != null) && (fieldBaseType.JarName != null))
      jarset = jarset + fieldBaseType.JarName ++ fieldBaseType.dependencyJarNames
    else if (fieldBaseType.JarName != null)
      jarset = jarset + fieldBaseType.JarName
    else if (fieldBaseType.dependencyJarNames != null)
      jarset = jarset ++ fieldBaseType.dependencyJarNames

    return jarset

  }

  /*
   * Get MetadataType based on the type of field
   */

  private def getMetadataTypesForMsgFields(field: Element, mdMgr: MdMgr, msgName: String): Array[Any] = {

    val fieldBaseType: BaseTypeDef = field.FldMetaataType
    var types: Array[Any] = new Array[Any](3);
    val fieldType = fieldBaseType.tType.toString().toLowerCase()

    val fieldTypeType = fieldBaseType.tTypeType.toString().toLowerCase()
    var arrayType: ArrayTypeDef = null
    if (fieldBaseType.isInstanceOf[ArrayTypeDef])
      arrayType = fieldBaseType.asInstanceOf[ArrayTypeDef]

    fieldTypeType match {
      case "tscalar" => {
        types(0) = fieldBaseType.PhysicalName
        types(1) = fieldBaseType.implementationName
        log.info("fieldBaseType.implementationName====" + fieldBaseType.PhysicalName);
        val valTypeId = -1 //getAttributeValTypeId(fieldBaseType.PhysicalName.toLowerCase());
        val keyTypeId = -1 //getAttributeValTypeId(fieldBaseType.PhysicalName.toLowerCase());
        types(2) = new ArrtibuteInfo(getAttributeTypeName(fieldBaseType.Name.toLowerCase()), valTypeId, keyTypeId, 0) //new ArrtibuteInfo(fieldBaseType.PhysicalName.toUpperCase(), valTypeId, keyTypeId, 0)

      }
      case "tcontainer" => {
        fieldType match {
          case "tarray" => {
            var arrayType: ArrayTypeDef = null
            arrayType = fieldBaseType.asInstanceOf[ArrayTypeDef]
            types(0) = arrayType.typeString
            types(1) = arrayType.elemDef.implementationName
            types(2) = getAttributeTypeInfo(arrayType.elemDef.tTypeType.toString().toLowerCase(), arrayType.elemDef, mdMgr, field.Name, msgName)

          }
          case "tstruct" => {
            var msgDef: ContainerDef = null;
            msgDef = mdMgr.Message(field.Ttype, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
            if (msgDef != null) {
              throw new Exception("Message " + field.Name + " cannot be a field in Message/Container " + msgName)
            }
            if (msgDef == null)
              msgDef = mdMgr.Container(field.Ttype, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version

            if (msgDef != null) {
              types(0) = msgDef.PhysicalName
              types(1) = msgDef.FullName
              val valTypeId = -1
              val keyTypeId = -1
              types(2) = new ArrtibuteInfo("CONTAINER", valTypeId, keyTypeId, msgDef.containerType.SchemaId)
            } else throw new Exception("struct not exists in metadata" + field.Ttype)
          }
          case "tmsgmap" => {
            var msgDef: ContainerDef = null;
            msgDef = mdMgr.Message(field.Ttype, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
            if (msgDef != null)
              throw new Exception("Message " + field.Name + " cannot be a field in Message/Container " + msgName)

            var ctrDef: ContainerDef = mdMgr.Container(field.Ttype, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
            types(0) = ctrDef.PhysicalName
            types(1) = ctrDef.FullName
            val valTypeId = -1
            val keyTypeId = -1
            types(2) = new ArrtibuteInfo("CONTAINER", valTypeId, keyTypeId, ctrDef.containerType.SchemaId)
          }
          case "tmap" => {
            var maptypeDef: MapTypeDef = null;
            maptypeDef = fieldBaseType.asInstanceOf[MapTypeDef]
            types(0) = maptypeDef.typeString
            types(1) = maptypeDef.valDef.implementationName

            val valTypeIdInfo = getMapAttributeTypeInfo(maptypeDef.valDef.tTypeType.toString().toLowerCase(), maptypeDef.valDef, mdMgr, field.Name, msgName)
            val keyTypeId = AttributeTypeInfo.TypeCategory.STRING.getValue;
            types(2) = new ArrtibuteInfo("MAP", valTypeIdInfo.valTypeId, keyTypeId, valTypeIdInfo.valSchemaId)
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
    return types
  }

  /*
   * get the AttributeValTypeId
   */
  private def getAttributeValTypeId(implName: String): Int = {
    implName match {
      case "string"    => return AttributeTypeInfo.TypeCategory.STRING.getValue;
      case "integer"   => return AttributeTypeInfo.TypeCategory.INT.getValue;
      case "int"       => return AttributeTypeInfo.TypeCategory.INT.getValue;
      case "float"     => return AttributeTypeInfo.TypeCategory.FLOAT.getValue;
      case "double"    => return AttributeTypeInfo.TypeCategory.DOUBLE.getValue;
      case "long"      => return AttributeTypeInfo.TypeCategory.LONG.getValue;
      case "byte"      => return AttributeTypeInfo.TypeCategory.BYTE.getValue;
      case "char"      => return AttributeTypeInfo.TypeCategory.CHAR.getValue;
      case "boolean"   => return AttributeTypeInfo.TypeCategory.BOOLEAN.getValue;
      case "bool"      => return AttributeTypeInfo.TypeCategory.BOOLEAN.getValue;
      case "container" => return AttributeTypeInfo.TypeCategory.CONTAINER.getValue;
      case "map"       => return AttributeTypeInfo.TypeCategory.MAP.getValue;
      case "array"     => return AttributeTypeInfo.TypeCategory.ARRAY.getValue;

    }

    return -1
  }

  /*
   * get the AttributeValType
   */
  private def getAttributeTypeName(typeName: String): String = {
    typeName match {
      case "string"    => return "STRING";
      case "integer"   => return "INT";
      case "int"       => return "INT";
      case "float"     => return "FLOAT";
      case "double"    => return "DOUBLE";
      case "long"      => return "LONG";
      case "byte"      => return "BYTE";
      case "char"      => return "CHAR";
      case "boolean"   => return "BOOLEAN";
      case "bool"      => return "BOOLEAN";
      case "container" => return "CONTAINER";
      case "map"       => return "MAP";
      case "array"     => return "ARRAY";

    }
    return ""
  }

  private def getAttributeTypeInfo(typeInfo: String, typtytpe: BaseTypeDef, mdMgr: MdMgr, fieldName: String, msgName: String): ArrtibuteInfo = {

    var typetyprStr: String = typtytpe.tType.toString().toLowerCase()
    typeInfo match {
      case "tscalar" => {
        log.info("typtytpe.Name.toLowerCase() " + typtytpe.Name.toLowerCase())
        val valTypeId = getAttributeValTypeId(typtytpe.Name.toLowerCase()); //getAttributeValTypeId(typtytpe.PhysicalName.toLowerCase());
        val keyTypeId = -1
        return new ArrtibuteInfo("ARRAY", valTypeId, keyTypeId, 0)
      }
      case "tcontainer" => {
        typetyprStr match {
          case "tarray" => { throw new Exception("Not supporting array of array"); }
          case "tstruct" => {

            var msgDef: ContainerDef = null;
            msgDef = mdMgr.Message(typtytpe.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
            if (msgDef != null) {
              throw new Exception("Array of Messages cannot be allowed for field " + fieldName + " in Message/Container " + msgName)
            }

            if (msgDef == null)
              msgDef = mdMgr.Container(typtytpe.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
            val valTypeId = AttributeTypeInfo.TypeCategory.CONTAINER.getValue
            val keyTypeId = -1
            val SchemaId = msgDef.containerType.SchemaId
            return new ArrtibuteInfo("ARRAY", valTypeId, keyTypeId, SchemaId)
          }
          case "tmsgmap" => {
            var ctrDef: ContainerDef = null;
            ctrDef = mdMgr.Message(typtytpe.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
            if (ctrDef != null) {
              throw new Exception("Array of Messages cannot be allowed for field " + fieldName + " in Message/Container " + msgName)
            }

            if (ctrDef == null)
              ctrDef = mdMgr.Container(typtytpe.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version

            val valTypeId = AttributeTypeInfo.TypeCategory.CONTAINER.getValue
            val keyTypeId = -1
            val SchemaId = ctrDef.containerType.SchemaId
            return new ArrtibuteInfo("ARRAY", valTypeId, keyTypeId, SchemaId)
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
    return null
  }

  /*
   * Handle maps
   */

  private def getMapAttributeTypeInfo(typeInfo: String, typtytpe: BaseTypeDef, mdMgr: MdMgr, fieldName: String, msgName: String): ArrtibuteInfo = {

    var typetyprStr: String = typtytpe.tType.toString().toLowerCase()
    typeInfo match {
      case "tscalar" => {
        log.info("getMapAttributeTypeInfo - typtytpe.name.toLowerCase() " + typtytpe.name.toLowerCase())
        val valTypeId = getAttributeValTypeId(typtytpe.name.toLowerCase()); //getAttributeValTypeId(typtytpe.PhysicalName.toLowerCase());
        val keyTypeId = 1
        return new ArrtibuteInfo("MAP", valTypeId, keyTypeId, 0)
      }
      case "tcontainer" => {
        typetyprStr match {
          case "tarray" => { throw new Exception("Not supporting array of array"); }
          case "tstruct" => {

            var msgDef: ContainerDef = null;
            msgDef = mdMgr.Message(typtytpe.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
            if (msgDef != null) {
              throw new Exception("Message cannot be a Value Type of field " + fieldName + " Message/Container " + msgName)
            }

            if (msgDef == null)
              msgDef = mdMgr.Container(typtytpe.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
            val valTypeId = AttributeTypeInfo.TypeCategory.CONTAINER.getValue
            val keyTypeId = 1
            val SchemaId = msgDef.containerType.SchemaId
            return new ArrtibuteInfo("MAP", valTypeId, keyTypeId, SchemaId)
          }
          case "tmsgmap" => {
            var ctrDef: ContainerDef = null;
            ctrDef = mdMgr.Message(typtytpe.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version
            if (ctrDef != null) {
              throw new Exception("Message cannot be a Value Type of field " + fieldName + " Message/Container " + msgName)
            }

            if (ctrDef == null)
              ctrDef = mdMgr.Container(typtytpe.FullName, -1, true).getOrElse(null) //field.FieldtypeVer is -1 for now, need to put proper version

            val valTypeId = AttributeTypeInfo.TypeCategory.CONTAINER.getValue
            val keyTypeId = 1
            val SchemaId = ctrDef.containerType.SchemaId
            return new ArrtibuteInfo("MAP", valTypeId, keyTypeId, SchemaId)
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
    return null
  }

  /*
   * get the AttributeKeyTypeId
   */
  private def getAttributeKeyTypeId(implName: String): Int = {
    return 0
  }

}
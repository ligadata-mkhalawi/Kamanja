package com.ligadata.queryutility

import java.util.Properties
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.ResultSet

import scala.collection.mutable.HashMap
import org.json4s._
import org.json4s.native.JsonMethods._
import com.ligadata.kamanja.metadata._
import org.json4s

import scala.collection.immutable.HashMap.HashMap1;
/**
  * Created by Yousef on 6/16/2016.
  */
class QueryBuilder extends LogTrait {

  def createQuery(setQuery: String, elementType: String, className: String , linkFrom: Option[String] = None, linkTo: Option[String] = None,
                  extendsClass: Option[String] = None): String = {
    var query: String = ""
    if(elementType.equalsIgnoreCase("vertex")){
      query = "create vertex %s %s".format(className,setQuery)
    } else if(elementType.equalsIgnoreCase("edge")){
      //query= "create edge %s from (select @rid from V where FullName = %s) to (select @rid from V where FullName = %s) %s;".format(className,linkFrom.get,linkTo.get, setQuery)
      query= "create edge %s from %s to %s %s".format(className,linkFrom.get,linkTo.get, setQuery)
    } else if(elementType.equalsIgnoreCase("class")){
      query = "create class %s extends %s".format(className, extendsClass.get)
    }
    return query
  }

  def checkQuery(elementType: String, objName: String ,className: String , linkFrom: Option[String] = None, linkTo: Option[String] = None): String ={
    var query: String = ""
    if(elementType.equalsIgnoreCase("vertex")){
      query = "select * from %s where Name = \"%s\"".format(className,objName)//use fullname
//    } else if(elementType.equalsIgnoreCase("edge")){
//      query= "select * from %s and Name = \"%s\" and in = (select @rid from V where Name = \"%s\");".format(className,objName,linkFrom.get,linkTo.get)
    } else if(elementType.equalsIgnoreCase("class")){
      query = "select distinct(@class) from V where @class = \"%s\"".format(className)
    }
    return query
  }

  def getAllProperty(className: String): List[String] ={
    var extendClass = ""
    if (className.equals("KamanjaEdge")) extendClass = "E" else extendClass = "V"
    var property: List[String] = Nil
    property = property ++ Array("Create Property %s.ID INTEGER".format(className))
    property = property ++ Array("Create Property %s.Name STRING".format(className))
    property = property ++ Array("Create Property %s.Namespace STRING".format(className))
    property = property ++ Array("Create Property %s.FullName STRING".format(className))
    property = property ++ Array("Create Property %s.Version STRING".format(className))
    property = property ++ Array("Create Property %s.CreatedBy STRING".format(className))
    property = property ++ Array("Create Property %s.CreatedTime STRING".format(className))
    property = property ++ Array("Create Property %s.LastModifiedTime STRING".format(className))
    property = property ++ Array("Create Property %s.Tenant STRING".format(className))
    property = property ++ Array("Create Property %s.Description STRING".format(className))
    property = property ++ Array("Create Property %s.Author STRING".format(className))
    property = property ++ Array("Create Property %s.Active BOOLEAN".format(className))
    property = property ++ Array("Create Property %s.Type STRING".format(className))
    property = property ++ Array("ALTER PROPERTY %s.Type DEFAULT \'%s\'".format(className, extendClass))
    return property
  }
  def getDBConnection(configObj: ConfigBean): Connection ={
    Class.forName("com.orientechnologies.orient.jdbc.OrientJdbcDriver")
    var info: Properties = new Properties
    info.put("user", configObj.username);  //username==> form configfile
    info.put("password", configObj.password); //password ==> from configfile
    info.put("db.usePool", "true"); // USE THE POOL
    info.put("db.pool.min", "1");   // MINIMUM POOL SIZE
    info.put("db.pool.max", "16");  // MAXIMUM POOL SIZE
    val conn = DriverManager.getConnection(configObj.url, info); // url==> jdbc:orient:remote:localhost/test"

    return conn
  }

  def getAllExistDataQuery(elementType: String, extendClass: Option[String] = None): String ={
    var query: String = ""
    if(elementType.equals("vertex")){
      //query = "select @rid as id, FullName from V"
      query = "select @this.toJSON() as jsonrec from (select @rid as @rid, @class as @class, FullName from V)"
    } else if(elementType.equals("edge")){
      //query = "select @rid as id, FullName, in, out from E"
      query = "select @this.toJSON() as jsonrec from (select @rid as @rid, @class as @class, FullName, Name, in ,out from E)"
    } else if(elementType.equals("class")){
      query = "select distinct(@class) from %s".format(extendClass.get)
    }
    return query
  }

  def getAllVertices(conn: Connection, query: String): Map[String, String] ={
//    val dataByRid = scala.collection.mutable.Map[String, String]()
    val dataByTypAndFullName = scala.collection.mutable.Map[String, String]()
    val stmt: Statement = conn.createStatement()
    val result: ResultSet = stmt.executeQuery(query)
    var json: json4s.JValue = null
    while (result.next()){
      //data +=  (result.getString(1) -> result.getString(2))
      json = parse(result.getString("jsonrec"))
      val adapCfgValues = json.values.asInstanceOf[Map[String, Any]]
      //data +=  (result.getString("id") -> result.getString("FullName"))
      val rid = adapCfgValues.get("@rid").get.toString
      val cls = adapCfgValues.get("@class").get.toString
      val fullName = adapCfgValues.get("FullName").get.toString
//      dataByRid += (rid -> fullName)
      dataByTypAndFullName += ((cls + "," + fullName).toLowerCase -> rid)
    }
//    return (dataByRid.toMap, dataByTypAndFullName.toMap)
    return dataByTypAndFullName.toMap
  }

  def getAllEdges(conn: Connection, query: String): Map[String, String] ={
    val data = scala.collection.mutable.Map[String, String]()
    val stmt: Statement = conn.createStatement()
    val result: ResultSet = stmt.executeQuery(query)
    var Key = ""
    var json: json4s.JValue = null
    while (result.next()){
      json = parse(result.getString("jsonrec"))
      val adapCfgValues = json.values.asInstanceOf[Map[String, Any]]
      val linkFrom = adapCfgValues.get("out").get.toString
      val linkTo = adapCfgValues.get("in").get.toString
      val getName =  adapCfgValues.get("Name").get.toString
      val fullName = adapCfgValues.getOrElse("FullName", getName).toString
      Key = linkFrom + "," + linkTo + "," + fullName
      val rid = adapCfgValues.get("@rid").get.toString
      data +=  (Key.toLowerCase -> rid)
    }
    return data.toMap
  }

  def getAllClasses(conn: Connection, query: String): List[String] ={
    var data: List[String]=Nil
    val stmt: Statement = conn.createStatement()
    val result: ResultSet = stmt.executeQuery(query)
    while (result.next()){
      data =  data ++ Array(result.getString(1))
    }
    return data
  }

  def createclassInDB(conn: Connection, query: String): Boolean ={
    val stmt: Statement = conn.createStatement()
    try {
      stmt.execute(query)
      stmt.close()
      return false
    }
    catch {
      case e: Exception => stmt.close(); return true
    }
  }
  def executeQuery(conn: Connection, query: String): Unit ={
    try {
      var stmt: Statement = conn.createStatement()
      stmt.execute(query)
      stmt.close()
    } catch {
      case e: Exception => logger.error("Failed to execute query:" + query, e)
    }
  }

  def checkObjexsist(conn: Connection, query: String): Boolean ={
    var stmt: Statement = conn.createStatement()
    var result : ResultSet = stmt.executeQuery(query)
    if(result.next())
      return true // there is data
    else
      return false // there is no data
  }

  def createSetCommand(baseElem: Option[BaseElemDef]= None, adapter: Option[AdapterInfo] = None
                       , tenant: Option[TenantInfo] = None): String ={
    var setQuery: String = ""

    if(baseElem != None){
      val tenantID: String = if(baseElem.get.TenantId.isEmpty || baseElem.get.TenantId == null) "" else baseElem.get.TenantId
      val name = if(baseElem.get.Name == null) "" else baseElem.get.Name
      val namespace = if(baseElem.get.NameSpace == null) "" else baseElem.get.NameSpace
      val fullName = if(baseElem.get.FullName == null) "" else baseElem.get.FullName
      val version = if(baseElem.get.Version.toString == null) "0" else baseElem.get.Version
      val creationTime = if(baseElem.get.CreationTime == null) "" else baseElem.get.CreationTime
      val description = if(baseElem.get.Description == null) "" else baseElem.get.Description
      val author = if(baseElem.get.Author == null) "" else baseElem.get.Author

      setQuery = "set ID = " + baseElem.get.MdElementId + ", Name = \"%s\", Namespace = \"%s\", FullName = \"%s\", Version = \"%s\",  CreatedTime = \"%s\",".format(
        name, namespace, fullName, version, creationTime) +
        " Tenant = \"%s\", Description = \"%s\", Author = \"%s\", Active = %b".format(
          tenantID, description, author, baseElem.get.Active)
    } else if(adapter != None){
      val tenantID: String = if(adapter.get.TenantId.isEmpty) "" else adapter.get.TenantId
      setQuery = "set Name = \"%s\", FullName = \"%s\", Tenant = \"%s\"".format(adapter.get.Name, adapter.get.Name, tenantID)
    } else if(tenant != None){
      val tenantID: String = if(tenant.get.tenantId.isEmpty) "" else tenant.get.tenantId
      val description: String = if(tenant.get.description == null) "" else tenant.get.description
      var primaryStroage: String = ""
      if(tenant.get.primaryDataStore != null) {
        val json = parse(tenant.get.primaryDataStore)
        val adapCfgValues = json.values.asInstanceOf[Map[String, Any]]
        primaryStroage = if (adapCfgValues.get("StoreType").get.toString == null) "" else adapCfgValues.get("StoreType").get.toString
      }
      setQuery = "set Tenant = \"%s\", Name = \"%s\", FullName = \"%s\", Description = \"%s\"".format(
        tenantID, tenantID + "_" + primaryStroage + "_primaryStroage", tenantID + "_" + primaryStroage + "_primaryStroage", description)
    }
    return setQuery
  }

  def PrintAllResult(map: Map[String, String], elementType: String): Unit ={
    if(map != null && map.size != 0) {
      println("These are all existing %s in metadata".format(elementType))
      logger.debug("These are all existing %s in metadata".format(elementType))
      for ((key, value) <- map) {
        logger.debug("key: " + key + " , value: " + value)
        println("key: " + key + " , value: " + value)
      }
    } else {
      logger.debug("There are no %s in metadata".format(elementType))
      println("There are no %s in metadata".format(elementType))
    }
  }

  def isCotnainer(name: String, cont: scala.collection.immutable.Set[ContainerDef]): Boolean ={
    if (!cont.isEmpty){
      for (container <- cont){
        if(container.FullName.equalsIgnoreCase(name)){
          return true
        }
      }
    }
    return false
  }

}

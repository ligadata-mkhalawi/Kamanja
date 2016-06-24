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

  def getAllVertices(conn: Connection, query: String): HashMap[String, String] ={
    val data = HashMap[String, String]()
    val stmt: Statement = conn.createStatement()
    val result: ResultSet = stmt.executeQuery(query)
    var json: json4s.JValue = null
    while (result.next()){
      //data +=  (result.getString(1) -> result.getString(2))
      json = parse(result.getString("jsonrec"))
      val adapCfgValues = json.values.asInstanceOf[Map[String, Any]]
      //data +=  (result.getString("id") -> result.getString("FullName"))
      data += (adapCfgValues.get("@rid").get.toString -> adapCfgValues.get("FullName").get.toString)
    }
    return data
  }

  def getAllEdges(conn: Connection, query: String): HashMap[String, String] ={
    val data = HashMap[String, String]()
    val stmt: Statement = conn.createStatement()
    val result: ResultSet = stmt.executeQuery(query)
    var Key = ""
    var json: json4s.JValue = null
    while (result.next()){
      json = parse(result.getString("jsonrec"))
      val adapCfgValues = json.values.asInstanceOf[Map[String, Any]]
      //val linkFrom = result.getString("in").substring(result.getString("in").indexOf("#"),result.getString("in").indexOf("{"))
      val linkFrom = adapCfgValues.get("in").get.toString
      //val linkTo = result.getString("out").substring(result.getString("out").indexOf("#"),result.getString("out").indexOf("{"))
      val linkTo = adapCfgValues.get("out").get.toString
      Key = linkFrom + "," + linkTo
      val getName =  adapCfgValues.get("Name").get.toString
      data +=  (Key -> adapCfgValues.getOrElse("FullName", getName).toString )
    }
    return data
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
      case e: Exception => logger.error(e)
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
  def createSetCommand(message: Option[MessageDef]= None, contianer: Option[ContainerDef]= None, model: Option[ModelDef]= None, adapter: Option[AdapterInfo] = None
                       , tenant: Option[TenantInfo] = None): String ={
    var setQuery: String = ""

    if(message != None){
      val tenantID: String = if(message.get.TenantId.isEmpty || message.get.TenantId == null) "" else message.get.TenantId
      val name = if(message.get.Name == null) "" else message.get.Name
      val namespace = if(message.get.NameSpace == null) "" else message.get.NameSpace
      val fullName = if(message.get.FullName == null) "" else message.get.FullName
      val version = if(message.get.Version.toString == null) "0" else message.get.Version
      val creationTime = if(message.get.CreationTime == null) "" else message.get.CreationTime
      val description = if(message.get.Description == null) "" else message.get.Description
      val author = if(message.get.Author == null) "" else message.get.Author

      setQuery = "set ID = " + message.get.UniqId + ", Name = \"%s\", Namespace = \"%s\", FullName = \"%s\", Version = \"%s\",  CreatedTime = \"%s\",".format(
        name, namespace, fullName, version, creationTime) +
        " Tenant = \"%s\", Description = \"%s\", Author = \"%s\", Active = %b".format(
          tenantID, description, author, message.get.Active)
    } else if(contianer != None){
      val tenantID: String = if(contianer.get.TenantId.isEmpty) "" else contianer.get.TenantId
      val name = if(contianer.get.Name == null) "" else contianer.get.Name
      val namespace = if(contianer.get.NameSpace == null) "" else contianer.get.NameSpace
      val fullName = if(contianer.get.FullName == null) "" else contianer.get.FullName
      val version = if(contianer.get.Version.toString == null) "0" else contianer.get.Version
      val creationTime = if(contianer.get.CreationTime == null) "" else contianer.get.CreationTime
      val description = if(contianer.get.Description == null) "" else contianer.get.Description
      val author = if(contianer.get.Author == null) "" else contianer.get.Author

      setQuery = "set ID = "+ contianer.get.UniqId + ", Name = \"%s\", Namespace = \"%s\", FullName = \"%s\", Version = \"%s\", CreatedTime = \"%s\", ".format(
         name, namespace, fullName, version, creationTime) +
        " Tenant = \"%s\", Description = \"%s\", Author = \"%s\", Active = %b".format(
          tenantID, description, author, contianer.get.Active)
    } else if (model != None){
      val tenantID: String = if(model.get.TenantId.isEmpty) "" else model.get.TenantId
      val name = if(model.get.Name == null) "" else model.get.Name
      val namespace = if(model.get.NameSpace == null) "" else model.get.NameSpace
      val fullName = if(model.get.FullName == null) "" else model.get.FullName
      val version = if(model.get.Version.toString == null) "0" else model.get.Version
      val creationTime = if(model.get.CreationTime == null) "" else model.get.CreationTime
      val description = if(model.get.Description == null) "" else model.get.Description
      val author = if(model.get.Author == null) "" else model.get.Author


      setQuery = "set ID = " + model.get.UniqId + ", Name = \"%s\", Namespace = \"%s\", FullName = \"%s\", Version = \"%s\", CreatedTime = \"%s\", ".format(
         name, namespace, fullName, version, creationTime) +
        " Tenant = \"%s\", Description = \"%s\", Author = \"%s\", Active = %b".format(
          tenantID, description, author, model.get.Active)
    } else if(adapter != None){
      val tenantID: String = if(adapter.get.TenantId.isEmpty) "" else adapter.get.TenantId

      setQuery = "set Name = \"%s\", FullName = \"%s\", Tenant = \"%s\"".format(adapter.get.Name, adapter.get.Name, tenantID)
    } else if(tenant != None){
      val tenantID: String = if(tenant.get.tenantId.isEmpty) "" else tenant.get.tenantId
      val description: String = if(tenant.get.description == null) "" else tenant.get.description
      val primaryStroage: String = if(tenant.get.primaryDataStore == null) "" else tenant.get.primaryDataStore

      setQuery = "set Tenant = \"%s\", Name = \"%s\", FullName = \"%s\", Description = \"%s\"".format(
        tenantID, tenantID + "_" + primaryStroage, tenantID + "_" + primaryStroage, description)
    }
    return setQuery
  }

  def PrintAllResult(hashObj: HashMap[String, String], elementType: String): Unit ={
    if(hashObj.size != 0) {
      println("These are all existing %s in metadata".format(elementType))
      logger.debug("These are all existing %s in metadata".format(elementType))
      for ((key, value) <- hashObj) {
        logger.debug("key: " + key + " , value: " + value)
        println("key: " + key + " , value: " + value)
      }
    } else {
      logger.debug("There are no %s in metadata".format(elementType))
      println("There are no %s in metadata".format(elementType))
    }
  }

}

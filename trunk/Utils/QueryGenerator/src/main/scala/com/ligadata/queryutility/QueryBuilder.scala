package com.ligadata.queryutility

import java.util.Properties
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.ResultSet

import scala.collection.mutable.HashMap
import org.json4s._
import org.json4s.native.JsonMethods._
import com.ligadata.kamanja.metadata.{AdapterInfo, ContainerDef, MessageDef, ModelDef}
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
    var stmt: Statement = conn.createStatement()
    stmt.execute(query)
    stmt.close()
    // should add try catch exception and add it to log
  }

  def checkObjexsist(conn: Connection, query: String): Boolean ={
    var stmt: Statement = conn.createStatement()
    var result : ResultSet = stmt.executeQuery(query)
    if(result.next())
      return true // there is data
    else
      return false // there is no data
  }
  def createSetCommand(message: Option[MessageDef]= None, contianer: Option[ContainerDef]= None, model: Option[ModelDef]= None, adapter: Option[AdapterInfo] = None): String ={
    var setQuery: String = ""

    if(message != None){
      val tenantID: String =
        if(message.get.TenantId.isEmpty) ""
        else message.get.TenantId

      setQuery = "set ID = " + message.get.UniqId + ", Name = \"%s\", Namespace = \"%s\", FullName = \"%s\", Version = \"%s\",  CreatedTime = \"%s\",".format(
         message.get.Name, message.get.NameSpace, message.get.FullName, message.get.Version, message.get.CreationTime) +
        " Tenant = \"%s\", Description = \"%s\", Author = \"%s\", Active = %b, Type = \'V\'".format(
          tenantID, message.get.Description, message.get.Author, message.get.Active)
    } else if(contianer != None){
        val tenantID: String =
          if(contianer.get.TenantId.isEmpty) ""
          else contianer.get.TenantId

      setQuery = "set ID = "+ contianer.get.UniqId + ", Name = \"%s\", Namespace = \"%s\", FullName = \"%s\", Version = \"%s\", CreatedTime = \"%s\", ".format(
         contianer.get.Name, contianer.get.NameSpace, contianer.get.FullName, contianer.get.Version, contianer.get.CreationTime) +
        " Tenant = \"%s\", Description = \"%s\", Author = \"%s\", Active = %b, Type = \'V\'".format(
          tenantID, contianer.get.Description, contianer.get.Author, contianer.get.Active)
    } else if (model != None){
      val tenantID: String =
        if(model.get.TenantId.isEmpty) ""
        else model.get.TenantId

      setQuery = "set ID = " + model.get.UniqId + ", Name = \"%s\", Namespace = \"%s\", FullName = \"%s\", Version = \"%s\", CreatedTime = \"%s\", ".format(
         model.get.Name, model.get.NameSpace, model.get.FullName, model.get.Version, model.get.CreationTime) +
        " Tenant = \"%s\", Description = \"%s\", Author = \"%s\", Active = %b, Type = \'V\'".format(
          tenantID, model.get.Description, model.get.Author, model.get.Active)
    } else if(adapter != None){
      val tenantID: String =
        if(adapter.get.TenantId.isEmpty) ""
        else adapter.get.TenantId

      setQuery = "set Name = \"%s\", FullName = \"%s\", Tenant = \"%s\", Type = \'V\'".format(adapter.get.Name, adapter.get.Name, tenantID)
    }
    return setQuery
  }

}

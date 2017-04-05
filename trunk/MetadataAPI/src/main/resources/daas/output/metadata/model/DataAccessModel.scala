package com.ligadata.flare.daas

import scala.util.parsing.combinator._

import com.ligadata.KamanjaBase._
import RddUtils._
import RddDate._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.apache.logging.log4j.{Logger, LogManager}
//import org.joda.time.format.DateTimeFormat
//import org.joda.time.DateTime
import java.util.Locale
import java.io._
import com.ligadata.kamanja.metadata.ModelDef;

class DataAccessModelFactory(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) {

  override def createModelInstance(): ModelInstance = return new DataAccessModel(this)

  override def getModelName(): String = "com.ligadata.flare.daas.DataAccessModel"

  override def getVersion(): String = "0.0.1"
  
  var api: DataAccessAPI = null
  override def init(txnContext: TransactionContext) = {
    api = new DataAccessAPI(txnContext.nodeCtxt.getEnvCtxt)
  }
}

object Expression { 

  def lessThan(l: Any, r: Any): Boolean = (l, r) match {
    case (a:String, b:String) => a.compareTo(b) < 0
    case (a:java.lang.Number, b:java.lang.Number) => a.doubleValue() < b.doubleValue()
    case (_, _) => false
  }

  def greaterThan(l: Any, r: Any): Boolean = (l, r) match {
    case (a:String, b:String) => a.compareTo(b) > 0
    case (a:java.lang.Number, b:java.lang.Number) => a.doubleValue() > b.doubleValue()
    case (_, _) => false
  }

  def lessThanEqual(l: Any, r: Any): Boolean = (l, r) match {
    case (a:String, b:String) => a.compareTo(b) <= 0
    case (a:java.lang.Number, b:java.lang.Number) => a.doubleValue() <= b.doubleValue()
    case (_, _) => false
  }

  def greaterThanEqual(l: Any, r: Any): Boolean = (l, r) match {
    case (a:String, b:String) => a.compareTo(b) >= 0
    case (a:java.lang.Number, b:java.lang.Number) => a.doubleValue() >= b.doubleValue()
    case (_, _) => false
  }

}

import Expression._
sealed trait Expression { def eval(v: ContainerInterface) : Any }
sealed trait BooleanExpression { def eval(v: ContainerInterface) : Boolean }
sealed trait ArithmeticExpression { def eval(v: ContainerInterface) : Double }

case class And(expressions: Seq[BooleanExpression]) extends BooleanExpression { def eval(v: ContainerInterface): Boolean = expressions.foldLeft(true)((r,c) => r && c.eval(v))}
case class Or(expressions: Seq[BooleanExpression]) extends BooleanExpression { def eval(v: ContainerInterface): Boolean = expressions.foldLeft(false)((r,c) => r || c.eval(v))}
case class Not(expression: BooleanExpression) extends BooleanExpression { def eval(v: ContainerInterface): Boolean = !expression.eval(v) }
case class Equal(left: Expression, right: Expression) extends BooleanExpression { def eval(v: ContainerInterface): Boolean = left.eval(v) == right.eval(v)}
case class NotEqual(left: Expression, right: Expression) extends BooleanExpression { def eval(v: ContainerInterface): Boolean = left.eval(v) != right.eval(v)}
case class LessThan(left: Expression, right: Expression) extends BooleanExpression { def eval(v: ContainerInterface): Boolean = lessThan(left.eval(v), right.eval(v))}
case class LessThanEqual(left: Expression, right: Expression) extends BooleanExpression { def eval(v: ContainerInterface): Boolean = lessThanEqual(left.eval(v), right.eval(v))}
case class GreaterThan(left: Expression, right: Expression) extends BooleanExpression { def eval(v: ContainerInterface): Boolean = greaterThan(left.eval(v), right.eval(v))}
case class GreaterThanEqual(left: Expression, right: Expression) extends BooleanExpression { def eval(v: ContainerInterface): Boolean = greaterThanEqual(left.eval(v), right.eval(v))}

case class Identifer(name: String) extends Expression { def eval(v: ContainerInterface): Any = v.get(name) }
case class StringLiteral(value: String) extends Expression { def eval(v: ContainerInterface): Any = value }
case class NumberLiteral(value: String) extends Expression { def eval(v: ContainerInterface): Any = value.toDouble }

class FilterParser extends JavaTokenParsers {
  def booleanExpression: Parser[BooleanExpression] = booleanTerm~rep("OR"~>booleanTerm) ^^ {case l ~ r => if(r.length > 0) Or(l +: r) else l}
  def booleanTerm: Parser[BooleanExpression] = booleanFactor~rep("AND"~>booleanFactor) ^^ {case l ~ r => if(r.length > 0) And(l +: r) else l}
  def booleanFactor: Parser[BooleanExpression] = opt("NOT")~(equalExpression | notEqualExpression | lessThanExpression | lessThanEqualExpression | greaterThanExpression | 
      greaterThanEqualExpression) ^^ {case o~e => if(o.isEmpty) e else Not(e)} | "("~booleanExpression~")" ^^ { case l ~ e ~ r => e } 
  def equalExpression: Parser[BooleanExpression] = term~"EQ"~term ^^ {case l ~ o ~ r => Equal(l, r)}
  def notEqualExpression: Parser[BooleanExpression] = term~"NEQ"~term ^^ {case l ~ o ~ r => NotEqual(l, r)}
  def lessThanExpression: Parser[BooleanExpression] = term~"LT"~term ^^ {case l ~ o ~ r => LessThan(l, r)}
  def lessThanEqualExpression: Parser[BooleanExpression] = term~"LTE"~term ^^ {case l ~ o ~ r => LessThanEqual(l, r)}
  def greaterThanExpression: Parser[BooleanExpression] = term~"GT"~term ^^ {case l ~ o ~ r => GreaterThan(l, r)}
  def greaterThanEqualExpression: Parser[BooleanExpression] = term~"GTE"~term ^^ {case l ~ o ~ r => GreaterThanEqual(l, r)}
  def term: Parser[Expression] = stringLiteral ^^ {str => (StringLiteral(str.substring(1, str.length - 1)))} | wholeNumber  ^^ (NumberLiteral(_)) | ident ^^ (Identifer(_))  
  
  def parseFilter(input: String): BooleanExpression = {
    val result = parseAll(booleanExpression, input)
    result match {
      case Success(matched,_) => {
        println(matched)
        return matched
      }
      case Failure(msg,_) => {
        println("FAILURE: " + msg)
        return null
      }
      case Error(msg,_) => {
        println("ERROR: " + msg)
        return null
      }
    }
  }
}

case class Request(id: String, containerName: String, key: Array[String], projections: Array[String], filter: Option[String])
case class Response(id: String, status: String, statusCode: String, statusDescription: String, resultCount: Option[Int], result: Option[Array[Map[String, Any]]])

class DataAccessAPI(context: EnvContext) {
  implicit val formats = DefaultFormats
  
  def getData(reqJson: String): String = {
    val req = parse(reqJson).extract[Request]
    val res = try { getData(req) } catch { case e: Throwable => new Response(req.id, "error", "9000", e.getMessage, None, None)}
    write(res)
  }
  
  def getData(req: Request): Response = {
    val container = context.getContainerInstance(req.containerName)
    val factory = container.getFactory.asInstanceOf[RDDObject[ContainerInterface]]
    val data = req.filter match {
      case Some(filterStr: String) => {
        val parser = new FilterParser
        val filterExp = parser.parseFilter(filterStr)
        factory.getRDD(req.key, c => filterExp.eval(c))
      }
      case None => factory.getRDD(req.key)
    }
    val result = scala.collection.mutable.ArrayBuffer[Map[String, Any]]()
    data.foreach( rec => { 
      result += projectContainer(rec, req.projections)
    })
    
    return new Response(req.id, "success", "1000", "", Some(result.size), Some(result.toArray))
  }

  def projectContainer(container: ContainerInterface, projections: Array[String]): Map[String, Any] = {
    if (projections != null && projections.length > 0) {
      container.getAllAttributeValues.filter(att => projections.contains(att.getValueType().getName())).map(att => {
        if (att.getValue().isInstanceOf[ContainerInterface]) {
          (att.getValueType().getName(), projectContainer(att.getValue().asInstanceOf[ContainerInterface], null))
        } else {
          (att.getValueType().getName(), att.getValue())
        }
      }).toMap
    } else {
      container.getAllAttributeValues.map(att => {
        if (att.getValue().isInstanceOf[ContainerInterface])
          (att.getValueType().getName(), projectContainer(att.getValue().asInstanceOf[ContainerInterface], null))
        else
          (att.getValueType().getName(), att.getValue())
      }).toMap
    }
  }

}

class DataAccessModel(factory: ModelInstanceFactory) extends ModelInstance(factory) {
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  val api = factory.asInstanceOf[DataAccessModelFactory].api

  override def execute(txnCtxt: TransactionContext, execMsgsSet: Array[ContainerOrConcept], triggerdSetIndex: Int, outputDefault: Boolean): Array[ContainerOrConcept] = {
    
    if (execMsgsSet.size == 0) 
      throw new Exception("Execute called with no messages to process.")

    val msg = execMsgsSet(0).asInstanceOf[DataAccessRequest]
    if(msg.request == null || msg.request.length == 0)
      throw new Exception("Messages is empty.")
    logger.warn("Request = [" + msg.request + "]")    
    
    val result = DataAccessResponse.createInstance()
    result.response = api.getData(msg.request)
    logger.warn("Response = [" + result.response + "]")    
    
    return Array(result)
  }
}

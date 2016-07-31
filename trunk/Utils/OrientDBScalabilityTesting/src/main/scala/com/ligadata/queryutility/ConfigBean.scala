package com.ligadata.scalabilityutility

/**
  * Created by Yousef on 6/16/2016.
  */
class ConfigBean {

  private var _username = ","
  private var _password = ""
  private var _url = ""
  private var _numberOfVertices: Int = 0
  private var _numberOfEdges: Int= 0
  // Getter
  def username = _username
  def password = _password
  def url = _url
  def numberOfVertices = _numberOfVertices
  def numberOfEdges = _numberOfEdges

  // Setter
  def username_= (value:String):Unit = _username = value
  def password_= (value:String):Unit = _password = value
  def url_= (value:String):Unit = _url = value
  def numberOfVertices_= (value:Int):Unit = _numberOfVertices = value
  def numberOfEdges_= (value:Int):Unit = _numberOfEdges = value
}
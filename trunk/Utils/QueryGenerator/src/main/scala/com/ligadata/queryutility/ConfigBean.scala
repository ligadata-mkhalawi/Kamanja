package com.ligadata.queryutility

/**
  * Created by Yousef on 6/16/2016.
  */
class ConfigBean {

  private var _username = ","
  private var _password = ""
  private var _url = ""

  // Getter
  def username = _username
  def password = _password
  def url = _url


  // Setter
  def username_= (value:String):Unit = _username = value
  def password_= (value:String):Unit = _password = value
  def url_= (value:String):Unit = _url = value
}
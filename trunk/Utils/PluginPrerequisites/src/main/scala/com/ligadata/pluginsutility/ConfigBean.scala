package com.ligadata.pluginsutility

import scala.collection.immutable.Map

/**
  * Created by Yousef on 8/02/2016.
  */
class ConfigBean {

  private var _messagesArray:  Array[String] = Array.empty
  private var _containersArray:  Array[String] = Array.empty
  private var _modelConfigArray:  Array[String] = Array.empty
  private var _modelsArray: Array[String] = Array.empty
  private var _messageBindingArray: Array[String] = Array.empty
  private var _jtmsArray: Array[String] = Array.empty
  private var _messages = ""
  private var _containers = ""
  private var _modelConfig = ""
  private var _models = ""
  private var _messageBinding = ""
  private var _jtms = ""
  private var _hasMessages = false
  private var _hasContainers = false
  private var _hasModelConfig = false
  private var _hasModels = false
  private var _hasMessageBinding = false
  private var _hasJtms = false
  // Getter
  def messages = _messages
  def containers = _containers
  def modelConfig = _modelConfig
  def models = _models
  def messageBinding = _messageBinding
  def jtms = _jtms
  def messagesArray = _messagesArray
  def containersArray = _containersArray
  def modelConfigArray = _modelConfigArray
  def modelsArray = _modelsArray
  def messageBindingArray = _messageBindingArray
  def jtmsArray = _jtmsArray
  def hasMessages = _hasMessages
  def hasContainers = _hasContainers
  def hasModelConfig = _hasModelConfig
  def hasModels = _hasModels
  def hasMessageBinding = _hasMessageBinding
  def hasJtms = _hasJtms
  // Setter
  def messages_= (value: String):Unit = _messages = value
  def containers_= (value: String):Unit = _containers = value
  def modelConfig_= (value: String):Unit = _modelConfig = value
  def models_= (value: String):Unit = _models = value
  def jtms_= (value: String):Unit = _jtms = value
  def messageBinding_= (value: String):Unit = _messageBinding = value
  def messagesArray_= (value: Array[String]):Unit = _messagesArray = value
  def containersArray_= (value: Array[String]):Unit = _containersArray = value
  def modelConfigArray_= (value: Array[String]):Unit = _modelConfigArray = value
  def modelsArray_= (value: Array[String]):Unit = _modelsArray = value
  def messageBindingArray_= (value: Array[String]):Unit = _messageBindingArray = value
  def jtmsArray_= (value: Array[String]):Unit = _jtmsArray = value
  def hasMessages_= (value: Boolean): Unit = _hasMessages = value
  def hasContainers_= (value: Boolean): Unit = _hasContainers = value
  def hasModelConfig_= (value: Boolean): Unit =  _hasModelConfig = value
  def hasModels_= (value: Boolean): Unit = _hasModels = value
  def hasMessageBinding_= (value: Boolean): Unit = _hasMessageBinding = value
  def hasJtms_= (value: Boolean): Unit = _hasJtms = value
}

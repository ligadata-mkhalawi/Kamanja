package com.ligadata.MetadataAPI

import com.ligadata.Serialize.SerializerManager
import com.ligadata.kamanja.metadata._
import com.ligadata.kamanja.metadata.BaseElemDef
import org.apache.logging.log4j.LogManager

import scala.collection.mutable.Map

object AdapterMessageBindingUtils {

  /** initialize a logger */
  val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  // 646 - 676 Change begins - replase MetadataAPIImpl
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
  // 646 - 676 Chagne ends

  lazy val serializerType = "json4s"


  val AdapterNameKey: String = "AdapterName"
  val MessageNameKey: String = "MessageName"
  val MessageNamesKey: String = "MessageNames"
  val SerializerKey: String = "Serializer"
  val OptionsKey: String = "Options"

  val mdMgr: MdMgr = MdMgr.GetMdMgr

  /**
    * Add an AdapterMessageBinding to the metadata for the supplied values.
    *
    * @param adapterName    the adapter's name
    * @param messageName    the namespace.name of the message
    * @param serializerName the serializer's namespace.name to use
    * @param options        serializer options
    * @param userId         the user making this request
    * @return a result string and the object itself if successfully created (null when there are problems)
    */
  private def AddAdapterMessageBinding(adapterName: String
                                       , messageName: String
                                       , serializerName: String
                                       , options: scala.collection.immutable.Map[String, Any]
                                       , userId: Option[String]): (String, BaseElemDef) = {
    val (acceptableBinding, errorMsgs): (Boolean, String) = SemanticChecks(adapterName, messageName, serializerName, options)
    val result: (String, BaseElemDef) = if (acceptableBinding) {
      /** Create an AdapterMessageBinding from the supplied map values and catalog it to mdmgr */
      val binding: AdapterMessageBinding = mdMgr.MakeAdapterMessageBinding(adapterName
        , messageName
        , serializerName
        , options)
      val key: String = binding.FullBindingName

      /** formatting of the messages done in the top level method...for success no message is returned, only binding instance */
      val res: String = ""
      (res, binding)
    } else {
      val res: String = s"One or more problems detected for binding ${'"'}$adapterName.$messageName.$serializerName${'"'}...; $errorMsgs; "
      (res, null)
    }
    result
  }

  /**
    * Add AdapterMessageBinding(s) to the metadata for the supplied values in the map.  More than one may be added if
    * multiple messages were supplied in a MessageNames array (vs. simple MessageName value),  Notifiy the cluster
    * of this "Add" action.
    *
    * @param bindingMap binding values for adapter name, message name, serializer name, and optionally serializer options.
    * @param userId     the user requesting this operation
    * @return json result string.
    */
  def AddAdapterMessageBinding(bindingMap: Map[String, Any], userId: Option[String]): String = {

    val results: List[(String, BaseElemDef)] = AddAdapterMessageBinding1(bindingMap, userId)

    /** With the returned results (a list of result string/BaseElemDef pairs) catalog the results and
      * prepare a result string for all results.  See CatalogResults for the details.
      */
    val resultsOfOperations: String = CatalogResults(results)
    resultsOfOperations
  }


  /**
    * Add multiple AdapterMessageBindings from the supplied map list. .
    *
    * @param bindingMaps a list of AdapaterMessageBindings specs as a list of json maps.
    * @param userId      the user requesting this operation
    * @return result string for all of the AdapterMessageBindings added.
    */
  def AddAdapterMessageBinding(bindingMaps: List[Map[String, Any]], userId: Option[String]): String = {

    val bindingResults: List[List[(String, BaseElemDef)]] = bindingMaps.map(bindingMap => {
      val partialResults: List[(String, BaseElemDef)] = if (bindingMap != null) {
        AddAdapterMessageBinding1(bindingMap, userId)
      } else {
        val resultPair: (String, BaseElemDef) = ("AddAdapterMessageBinding failed ... json map was null; ", null)
        List[(String, BaseElemDef)](resultPair)
      }
      partialResults
    })

    val flattenedResults: List[(String, BaseElemDef)] = bindingResults.flatten
    val resultsOfOperations: String = CatalogResults(flattenedResults)
    resultsOfOperations
  }

  /**
    * Remove the binding with the supplied fully qualified binding name.  A fully qualified binding name consists
    * of the adapterName.namespace.messsagename.namespace.serializername. For example, if the name of the adapter is
    * "kafkaAdapterInput1" and the name of the message is "com.botanical.json.shippingmsg" and the name of the
    * serializer is "org.kamanja.serializer.json.JsonSerDeser", the key to remove the binding would be
    * "kafkaAdapterInput1.com.botanical.json.shippingmsg.org.kamanja.serializer.json.JsonSerDeser".
    *
    * @param fqBindingName the fully qualified binding name
    * @param userId        the user requesting this operation
    * @return result string for all of the AdapterMessageBindings added.
    */
  def RemoveAdapterMessageBinding(fqBindingName: String, userId: Option[String]): String = {

    val binding: AdapterMessageBinding = mdMgr.RemoveAdapterMessageBinding(fqBindingName)

    val result: String = if (binding != null) {

      /** Assign a current transaction id to the object so that we don't rezero the counter in NotifyEngine */
      val xid: Long = PersistenceUtils.GetNewTranId
      binding.tranId = xid

      ConfigUtils.RemoveAdapterMessageBindingFromCache(fqBindingName)
      val displayKey: String = binding.FullBindingName

      /** Notify the cluster via zookeeper of removal */
      val bindingsRemoved: Array[BaseElemDef] = Array[BaseElemDef](binding)
      val operations: Array[String] = bindingsRemoved.map(binding => "Remove")
      val bindingsThatWereRemoved: String = bindingsRemoved.map(b => b.FullName).mkString(", ")
      logger.debug(s"Notify cluster via zookeeper that this binding has been removed... $bindingsThatWereRemoved")
      getMetadataAPI.NotifyEngine(bindingsRemoved, operations)

      val apiResult = new ApiResult(ErrorCodeConstants.Success, "Remove AdapterMessageBinding", null, ErrorCodeConstants.Remove_AdapterMessageBinding_Successful + " : " + displayKey)
      apiResult.toString()
    } else {
      val apiResult = new ApiResult(ErrorCodeConstants.Failure, "Remove AdapterMessageBinding", null, ErrorCodeConstants.Remove_AdapterMessageBinding_Failed + " : " + fqBindingName)
      apiResult.toString()
    }

    result
  }

  /**
    * Remove the bindings found in the supplied list.  Their form should be:
    *
    * '''
    * "AdapterName,Namespace.MessageName,Namespace.SerializerName"
    * '''
    *
    * Note: If any of the bindings are not found in the metadata cache, no bindings are removed.
    *
    * @param bindingList a List of binding keys to be removed.
    * @param userId      the user requesting this operation
    * @return result string for all of the AdapterMessageBindings removed.
    */
  def RemoveAdapterMessageBindings(bindingList: List[String], userId: Option[String]): String = {

    /** Only remove the bindings iff all of them can be removed */
    val allBindingsPresent: List[String] = bindingList.map(bindingKey => {
      if (mdMgr.AllAdapterMessageBindings.contains(bindingKey))
        bindingKey
      else
        s"BINDING NOT PRESENT - $bindingKey"
    })

    val removalMayProceed: Boolean = allBindingsPresent.length == bindingList.length
    val result: String = if (removalMayProceed) {
      /** Create a list of bindings that were removed. This should not happen but due to the allBindingsPresent
        * check above... BUT should the key not be present, a null is returned by the code below.. that will
        * cause error message to be issued and warning that our "reject all remove" logic attempted with the
        * allBindingsPresent check above is not correct or complete in some way.
        */
      val cacheRemoveResults: List[(AdapterMessageBinding, String)] = bindingList.map(bindingKey => {
        (mdMgr.RemoveAdapterMessageBinding(bindingKey), bindingKey)
      })

      val interimresults: String = if (cacheRemoveResults != null) {

        val xid: Long = PersistenceUtils.GetNewTranId
        val results: List[(AdapterMessageBinding, String)] = cacheRemoveResults.map(bindingPair => {
          val (binding, keyUsed): (AdapterMessageBinding, String) = bindingPair
          val (bindingRem, removeResult): (AdapterMessageBinding, String) = if (binding != null) {
            /** Assign a current transaction id to the object so that we don't rezero the counter in NotifyEngine */
            binding.tranId = xid
            val fqBindingName: String = binding.FullBindingName
            ConfigUtils.RemoveAdapterMessageBindingFromCache(fqBindingName)

            val apiResult = new ApiResult(ErrorCodeConstants.Success, "Remove AdapterMessageBinding", fqBindingName, ErrorCodeConstants.Remove_AdapterMessageBinding_Successful)
            (binding, apiResult.toString())
          } else {
            val apiResult = new ApiResult(ErrorCodeConstants.Failure, "Remove AdapterMessageBinding", keyUsed, ErrorCodeConstants.Remove_AdapterMessageBinding_Failed)
            (binding, apiResult.toString())

          }
          (bindingRem, removeResult)

        })

        /** Sanity check ... */
        val ok: Boolean = cacheRemoveResults.count(pair => pair._1 != null) == cacheRemoveResults.length
        val zkNotifyResults: String = if (ok) {
          /** Notify the cluster via zookeeper of removal */
          val bindingsRemoved: Array[BaseElemDef] = cacheRemoveResults.map(pair => pair._1).toArray
          val operations: Array[String] = bindingsRemoved.map(binding => "Remove")
          val bindingsThatWereRemoved: String = bindingsRemoved.map(b => b.asInstanceOf[AdapterMessageBinding].FullBindingName).mkString(", ")
          logger.debug(s"Notify cluster via zookeeper that this binding has been removed... $bindingsThatWereRemoved")
          getMetadataAPI.NotifyEngine(bindingsRemoved, operations)
          val apiResult = new ApiResult(ErrorCodeConstants.Success, "Remove AdapterMessageBinding Zookeeper Notification", bindingsThatWereRemoved, ErrorCodeConstants.Remove_AdapterMessageBinding_Successful)
          apiResult.toString
        } else {
          val apiResult = new ApiResult(ErrorCodeConstants.Failure, "Remove AdapterMessageBinding", s"${ErrorCodeConstants.Remove_AdapterMessageBinding_Failed} ; ${ErrorCodeConstants.Remove_AdapterMessageBinding_Failed}", "Verification logic that all adapters were present is either incorrect or incomplete in some way!")
          apiResult.toString()
        }
        /** The complete message consists of the cache removal results + the zkNotification result from the entire list. */
        val cacheRemoveMsgs: String = results.map(pair => pair._2).mkString(",\n")
        s"$cacheRemoveMsgs,\n$zkNotifyResults"
      } else {
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "Remove AdapterMessageBinding", null, s"${ErrorCodeConstants.Remove_AdapterMessageBinding_Failed} : no bindings could be removed... internal logic error")
        apiResult.toString()
      }
      interimresults
    } else {
      val bindingsPresentMsg: String = allBindingsPresent.mkString(", ")
      val overallApiResult = new ApiResult(ErrorCodeConstants.Failure, "Remove AdapterMessageBinding", null, s"${ErrorCodeConstants.Remove_AdapterMessageBinding_Failed} : no bindings could be removed... one or more bindings are missing... $bindingsPresentMsg")
      overallApiResult.toString()
    }

    s"[\n$result\n]"
  }

  /**
    * Answer all of the  AdapaterMessageBindings defined.
    *
    * @return a Map[String, AapterMessageBinding] with 0 or more kv pairs.
    */
  def ListAllAdapterMessageBindings: scala.collection.immutable.Map[String, AdapterMessageBinding] = {
    mdMgr.AllAdapterMessageBindings
  }

  /**
    * Answer a map of AdapaterMessageBindings that are used by the supplied adapter name.
    *
    * @param adapterName the adapter name that has the AdapterMessageBinding instances of interest
    * @return a Map[String, AapterMessageBinding] with 0 or more kv pairs.
    */
  def ListBindingsForAdapter(adapterName: String): scala.collection.immutable.Map[String, AdapterMessageBinding] = {
    val bindingMap: scala.collection.immutable.Map[String, AdapterMessageBinding] = mdMgr.BindingsForAdapter(adapterName)
    bindingMap
  }

  /**
    * Answer a map of AdapaterMessageBindings that operate on the supplied message name.
    *
    * @param namespaceMsgName the namespace.name of the message of interest
    * @return a Map[String, AapterMessageBinding] with 0 or more kv pairs.
    */
  def ListBindingsForMessage(namespaceMsgName: String): scala.collection.immutable.Map[String, AdapterMessageBinding] = {
    val bindingMap: scala.collection.immutable.Map[String, AdapterMessageBinding] = mdMgr.BindingsForMessage(namespaceMsgName)
    bindingMap
  }

  /**
    * Answer a map of AdapaterMessageBindings that are used by the serializer with the supplied name.
    *
    * @param namespaceSerializerName the serializer name that is used by the AdapterMessageBinding instances of interest
    * @return a Map[String, AapterMessageBinding] with 0 or more kv pairs.
    */
  def ListBindingsUsingSerializer(namespaceSerializerName: String): scala.collection.immutable.Map[String, AdapterMessageBinding] = {
    val bindingMap: scala.collection.immutable.Map[String, AdapterMessageBinding] = mdMgr.BindingsUsingSerializer(namespaceSerializerName)
    bindingMap
  }

  /**
    * Add AdapterMessageBinding(s) to the metadata for the supplied values in the map.  More than one may be added if
    * multiple messages were supplied in a MessageNames array (vs. simple MessageName value),
    *
    * @param bindingMap binding values for adapter name, message name, serializer name, and optionally serializer options.
    * @param userId     the user requesting this operation
    * @return List[JsonResultString, Elements]
    */
  private def AddAdapterMessageBinding1(bindingMap: Map[String, Any], userId: Option[String]): List[(String, BaseElemDef)] = {

    val results: List[(String, BaseElemDef)] = if (bindingMap != null) {
      val adapterName: String = bindingMap.getOrElse(AdapterNameKey, "**invalid adapter name**").asInstanceOf[String].trim
      val messageName: String = bindingMap.getOrElse(MessageNameKey, "**invalid message name**").asInstanceOf[String].trim
      val messageNames: List[String] = bindingMap.getOrElse(MessageNamesKey, List[String]()).asInstanceOf[List[String]]
      val serializerName: String = bindingMap.getOrElse(SerializerKey, "**invalid serializer name**").asInstanceOf[String].trim
      val options: scala.collection.immutable.Map[String, Any] =
        if (bindingMap.contains(OptionsKey) && bindingMap(OptionsKey) != null) {
          try {
            bindingMap(OptionsKey).asInstanceOf[scala.collection.immutable.Map[String, Any]]
          } catch {
            case e: Exception => {
              logger.error("Options should be a map of any values, not an array")
              null // complain if someone tries to slip us an array
            }
          }
        } else {
          scala.collection.immutable.Map[String, Any]()
        }

      val multipleMessages: Boolean = messageNames.length > 0
      val rslt: List[(String, BaseElemDef)] = if (multipleMessages) {
        val results: List[(String, BaseElemDef)] = messageNames.map(msg => {
          if (options == null) {
            ("Options should be a map of any values, not an array", null)
          } else {
            val resultObjPair: (String, BaseElemDef) = AddAdapterMessageBinding(adapterName, msg, serializerName, options, userId)
            resultObjPair
          }
        })
        results
      } else {
        if (options == null) {
          List[(String, BaseElemDef)](("Options should be a map of any values, not an array", null))
        } else {
          val resultObjPair: (String, BaseElemDef) = AddAdapterMessageBinding(adapterName, messageName, serializerName, options, userId)
          List[(String, BaseElemDef)](resultObjPair)
        }
      }
      rslt
    } else {
      List[(String, BaseElemDef)](("AddAdapterMessageBinding failed!... json map was null; ", null))
    }
    results
  }

  /**
    * Check out the AdapterMessageBinding values to be used are acceptable.  Checks are:
    *
    * 1) if the names start with "**" it means that the name was not supplied and this is itself an error message
    * substitution value
    * 2) the message and serializerName names should be checked for namespace.name form
    * 3) the adapter name should exist in MdMgr's adapters map
    * 4) the message should exist in the MdMgr's message map
    * 5) the serializer should exist in the MdMgr's serializers map
    * 6) if the serializer is the csv serializer, verify that none of the attributes presented in the
    * message are ContainerTypeDefs.  These are not handled by csv.  Fixed msgs with simple types only.
    * 7) if the serializer is csv, prevent mapped message binding
    * 8) prevent more than one binding on an input adapter.
    * 9) don't allow more than one binding per message on output
    *
    * @param adapterName    the name of the adapter
    * @param messageName    the namespace.name of the message to be bound
    * @param serializerName the serializer that will be used to flatten/resurrect the message on binder's behalf
    * @param options        the serializer options that may be used to configure the serializer
    * @return (boolean, string) If true returned, arguments are acceptable.  If false, the operation will be abandoned
    *         and the returned string contains the issues found.
    */
  private def SemanticChecks(adapterName: String
                             , messageName: String
                             , serializerName: String
                             , options: scala.collection.immutable.Map[String, Any]): (Boolean, String) = {
    val buffer: StringBuilder = new StringBuilder
    val isMsgEmpty = messageName.trim.isEmpty

    /** 1) the adapter name should exist in MdMgr's adapters map */
    val adapter: AdapterInfo = mdMgr.GetAdapter(adapterName)
    val adapterPresent: Boolean = adapter != null
    if (!adapterPresent) {
      buffer.append(s"The adapter $adapterName cannot be found in the metadata...; ")
    }

    var isTaggedAdapter = false
    if (isMsgEmpty && adapter != null && adapter.TypeString.equalsIgnoreCase("Input")) {
      try {
        isTaggedAdapter = options.getOrElse("taggedAdapter", "false").toString.toBoolean
      } catch {
        case e: Throwable => {
        }
      }
    }

    /** 2) if the names start with "**" it means that the name was not supplied ... issue name error */
    val adapterStartsWithAstsk: Boolean = (adapterName != null && adapterName.startsWith("**"))
    val messageStartsWithAstsk: Boolean = (messageName != null && messageName.startsWith("**"))
    val serializerStartsWithAstsk: Boolean = (serializerName != null && serializerName.startsWith("**"))
    if (adapterStartsWithAstsk) {
      buffer.append(s"The adapter name was not supplied ... $adapterName...; ")
    }
    if (messageStartsWithAstsk) {
      buffer.append(s"The message name was not supplied ... $messageName...; ")
    }
    if (serializerStartsWithAstsk) {
      buffer.append(s"The serlializer name was not supplied ... $serializerName...; ")
    }

    /** 3) the message and serializerName are checked for namespace.name form */
    val messageNameHasDots: Boolean = (!messageStartsWithAstsk && messageName.size > 2 && messageName.contains('.'))
    val serializerNameHasDots: Boolean = (!serializerStartsWithAstsk && serializerName.size > 2 && serializerName.contains('.'))
    if ((isTaggedAdapter == false && messageNameHasDots == false) || (serializerNameHasDots == false)) {
      buffer.append(s"$messageName and/or $serializerName do not conform to namespace.name form...; ")
    }

    val msgDef: MessageDef = if (messageNameHasDots) {
      /** 4) the message should exist in the MdMgr's message map */
      val msgNamespace: String = messageName.split('.').dropRight(1).mkString(".")
      val msgName: String = messageName.split('.').last
      val msgD: MessageDef = mdMgr.ActiveMessage(msgNamespace, msgName)
      val msgPresent: Boolean = msgD != null
      if (!msgPresent) {
        buffer.append(s"The message $messageName cannot be found in the metadata...; ")
      }
      msgD
    } else {
      null
    }

    val serializer: SerializeDeserializeConfig = if (serializerNameHasDots) {
      /** 5) he serializer should exist in the MdMgr's serializers map */
      val slzer: SerializeDeserializeConfig = mdMgr.GetSerializer(serializerName)
      val serializerPresent: Boolean = slzer != null
      if (!serializerPresent) {
        buffer.append(s"The serializer $serializerName cannot be found in the metadata...; ")
      }
      slzer
    } else {
      null
    }
    /* 6) 7) csv check ... if csv verify msg is fixed with simple (non-ContainerTypeDef types).  The nested
     * types (ContainerTypeDef fields) are only supported by Json and KBinary at this time.
     *
     * FIXME: if the namespace of the standard csv serializer changes, the constant below MUST change with it.
     */
    val csvNamespaceName: String = "com.ligadata.kamanja.serializer.csvserdeser"
    val serializerNamespaceName: String = if (serializer != null) s"${serializer.FullName.toLowerCase}" else "_no_name_"
    val isCsvSerializer: Boolean = (serializerNamespaceName == csvNamespaceName)
    val (msgFields, isMappedCSVAttempt): (Array[BaseAttributeDef], Boolean) = if (msgDef != null && isCsvSerializer) {
      if (msgDef.containerType.isInstanceOf[MappedMsgTypeDef]) {
        (null, true)

        /** 7) setup */
      } else if (msgDef.containerType.isInstanceOf[StructTypeDef]) {
        /** 6) setup */
        val structMsg: StructTypeDef = msgDef.containerType.asInstanceOf[StructTypeDef]
        (structMsg.memberDefs, false)
      } else {
        logger.error(s"A MessageDef ${msgDef.FullName} has a type is not a MappedMsgTypeDef or StructTypeDef.")
        (null, false)
      }
    } else {
      (null, false) // no need to check for json or kbinary serializers
    }

    val msgHasContainerFields: Boolean = (msgFields != null && (msgFields.filter(fld => (fld.typeDef.isInstanceOf[StructTypeDef] || fld.typeDef.isInstanceOf[MappedMsgTypeDef])).size > 0))
    if (msgHasContainerFields) {
      /** 6) test */
      buffer.append(s"The message $messageName has container type fields.  These are not handled by the $csvNamespaceName serializer...; ")
    }
    if (isMappedCSVAttempt) {
      /** 7) test */
      buffer.append(s"MAPPED messages like $messageName are not handled by the $csvNamespaceName serializer at this time...; ")
    }

    /** 8) input adapter check.  Prevent more than one binding on an input adapter */
    val isInputAdapter: Boolean = (adapter != null && adapter.TypeString.compareToIgnoreCase("input") == 0)
    if (isInputAdapter) {
      /** count the number of bindings for this input adapter... There should be NONE at this point */
      val existingBindings: scala.collection.immutable.Map[String, AdapterMessageBinding] = mdMgr.BindingsForAdapter(adapter.Name)
      val hasBindingAlready: Boolean = existingBindings.nonEmpty
      if (hasBindingAlready) {
        buffer.append(s"Input Adapter ${adapter.Name} already has a binding... only one binding per input adapter allowed at this time...; ")
        val bindingNames: String = existingBindings.values.map(binding => binding.FullBindingName).mkString(", ")
        buffer.append(s"Remove binding(s) '$bindingNames' before attempting to add the current binding... name='$adapterName,$messageName,$serializerName'")
      }
    }

    /** 9) only one binding on an output adapter message */
    if (adapter != null) {
      val existingAdapterBindings: Array[AdapterMessageBinding] = mdMgr.BindingsForAdapter(adapter.Name).values.toArray
      if (existingAdapterBindings.size > 0 && isTaggedAdapter == false) {
        if (msgDef != null) {
          val adapterBindingsWithThisMsg: Array[AdapterMessageBinding] =
            existingAdapterBindings.filter(binding => binding.messageName.compareToIgnoreCase(msgDef.FullName) == 0)
          if (adapterBindingsWithThisMsg.size > 0) {
            // ... should never get more than one, but code carefully anyway...
            // we blow out of here because that is what I was asked to do... there is no reason this can't simply be added to the error msg buffer and return.
            throw new RuntimeException("Already found adapter bindings for this message on this adapter. String:" + adapterBindingsWithThisMsg.mkString(","))
          }
        } else {
          buffer.append("MessageDef is null.")
        }
      } else if (existingAdapterBindings.size > 0 && isTaggedAdapter) {
        buffer.append("Same adapter should not have more than one tagged adapter binding.")
      }
    }

    val ok: Boolean = buffer.isEmpty
    val errorMsgs: String = buffer.toString
    (ok, errorMsgs)
  }

  /**
    * Examine the supplied results for this AddAdapterMessageBinding operation.  Embellish error messages as
    * needed, cache AdapterMessageBindingInstances, persist them, and notify the cluster that there are new
    * bindings in the working set.
    *
    * @param processingResults a List[(String, BaseElemDef)] instances that contain errors found (if any) and the
    *                          AdapterMessageBinding instance.
    * @return result string, a concatenation of all results.
    */
  private def CatalogResults(processingResults: List[(String, BaseElemDef)]): String = {

    /** Check if all bindings found in the json input were acceptable and AdapterMessageBinding instances created.
      * This is indicated by the BaseElemDef value (pair._2 being null */
    val acceptable: Boolean = !processingResults.exists(pair => pair._2 == null)
    val resultString: String = if (acceptable) {

      /** add the AdapterMessageBinding instances to the cache */
      var allResultsCached: Boolean = true /** optimisim */
      val tranId: Long = PersistenceUtils.GetNewTranId
      val cachedResults: List[(String, BaseElemDef)] = processingResults.map(bindingPair => {
        val (messages, elem): (String, BaseElemDef) = bindingPair

        elem.tranId = tranId

        val binding: AdapterMessageBinding = elem.asInstanceOf[AdapterMessageBinding]
        if (!mdMgr.AddAdapterMessageBinding(binding)) {
          allResultsCached = false
          val cacheUpdateFailure: String = s"binding ${binding.FullName}... cache update failed!; "
          (s"$cacheUpdateFailure; $messages", binding)
        } else {
          (messages, binding)
        }
      })

      /** If all results were cached, persist the to the store */
      val reString: String = if (allResultsCached) {

        cachedResults.foreach(bindingPair => {
          val (messages, elem): (String, BaseElemDef) = bindingPair
          val binding: AdapterMessageBinding = elem.asInstanceOf[AdapterMessageBinding]

          /** Persist the binding for next session.  NOTE: The adaptermessagebinding prefix in the key
            * is used by the metadata load process to find the write serializer in the case statement.
            * See ConfigUtils.LoadAllConfigObjectsIntoCache for more details.
            *
            * FIXME: Shouldn't the SaveObject issue an exception, trap it, log it, and then return
            * FIXME: a boolean or status return code that tells the caller that things are falling apart?
            * FIXME: Blasting out of an extremely low context with little visibility of higher level
            * FIXME: context seems unwise.
            */
          val key: String = s"adaptermessagebinding.${binding.FullBindingName}"
          val jsonstr = MetadataAPISerialization.serializeObjectToJson(binding)
          val value = jsonstr.getBytes
          getMetadataAPI.SaveObject(key.toLowerCase, value, "adapter_message_bindings", serializerType)

          logger.info(s"jsonStr for $key is\n$jsonstr ")

        })

        /** Notify the cluster that things are a'happening */
        /** zookeeper first */
        val bindingsAdded: Array[BaseElemDef] = cachedResults.map(pair => {
          pair._2
        }).toArray
        val operations: Array[String] = bindingsAdded.map(binding => "Add")
        val bindingsThatWereAdded: String = bindingsAdded.map(b => b.FullName).mkString(", ")
        logger.debug(s"Notify cluster via zookeeper that these bindings have been added... $bindingsThatWereAdded")
        getMetadataAPI.NotifyEngine(bindingsAdded, operations)


        /** results second */
        val rString: String = cachedResults.map(pair => {
          val (msg, elem): (String, BaseElemDef) = pair
          val binding: AdapterMessageBinding = elem.asInstanceOf[AdapterMessageBinding]
          new ApiResult(0, "AddAdapterMessageBinding", s"${binding.FullBindingName}", s"added.").toString
        }).mkString(s",\n")
        rString

      } else {
        val failMsg: String = new ApiResult(-1, "AddAdapterMessageBinding", "One or more of the bindings could not be cached!", s"Look in the following results for ${'"'}cache update failed!${'"'} to see which ones... ").toString
        val detailedString: String = processingResults.map(pair => {
          val (msg, elem): (String, BaseElemDef) = pair
          val binding: AdapterMessageBinding = elem.asInstanceOf[AdapterMessageBinding]
          val cmdMsg: String = s"binding could not be cached...not added ...$msg"
          new ApiResult(-1, "AddAdapterMessageBinding", s"${binding.FullBindingName} ", cmdMsg).toString
        }).mkString(s",\n")

        val resultStr: String = s"$failMsg,\n$detailedString"
        resultStr

      }
      reString
    } else {
      val overallMsg: String = new ApiResult(-1, "AddAdapterMessageBinding", "All binding additions were rejected!", "One or more issues were encountered processing the input... see following results for the details... ").toString
      val detailedString: String = processingResults.map(pair => {
        val (msg, elem): (String, BaseElemDef) = pair
        val bindingName: String = if (elem != null) elem.asInstanceOf[AdapterMessageBinding].FullBindingName else "Binding could not be created"
        val cmdMsg: String = if (elem != null) s"not added ...$msg" else s"and not added...$msg"
        new ApiResult(-1, "AddAdapterMessageBinding", s"$bindingName", cmdMsg).toString
      }).mkString(s",\n")
      val resultStr: String = s"$overallMsg,\n$detailedString"
      resultStr
    }

    s"\n[\n$resultString\n]"

  }
}

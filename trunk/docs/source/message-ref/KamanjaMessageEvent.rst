
.. _kamanjamessageevent-msg-ref:

KamanjaMessageEvent
===================

The **KamanjaMessageEvent** internal message
is created each time a message comes into the Kamanja engine.
An adapter binding is specified for each message.

This message includes the :ref:`kamanjaexceptionevent-msg-ref` array
that tracks Kamanja model events
and the :ref:`kamanjaexcecutionfailureevent-msg-ref` array
that tracks exceptions thrown by the adapter.


KamanjaMessageEvent structure
-----------------------------

::


  KamanjaMessageEvent

  {
   "Message": {
   "NameSpace": "com.ligadata.KamanjaBase",
   "Name": "KamanjaMessageEvent",
   "Version": "1.00",
   "Description": "Message Execution detail",
   "Fixed": "true",
   "Elements": [{
       "Field": {
           "Name": "messageId",
           "Type": "Long"
           }
           }, {
           "Field": {
               "Name": "modelinfo",
               "Type": "ArrayOfKamanjaModelEvent"
           }
           }, {
               "Field": {
                   "Name": "elapsedtimeinms",
                   "Type": "Float"
           }
           }, {
               "Field": {
                   "Name": "messagekey",
                   "Type": "String"
       }
       }, {
               "Field": {
                   "Name": "messagevalue",
                   "Type": "String"
       }
           }, {
               "Field": {
                   "Name": "error",
                   "Type": "String"
           }
           }, {
               "Field": {
                   "Name": "KamanjaExceptionEvent",
                   "Type": "String"
               }
           }]
       }
  }

KamanjaMessageEvent parameters
------------------------------

The parameters are:

- **messageId** - unique message for that message type.
  Map the messageId to its Name by directly querying the metadata data.
  There is no API for it; it must be created.
- **modelinfo** - array of Kamanja model events.
  See the :ref:`kamanjamodelevent-msg-ref` section below.
- **elapsedtimeinms** - time in milliseconds it took
  for the message to be processed.
- **messagekey** - key provided from the adapter.
- **messagevalue** - value provided from the adapter.
- **KamanjaExceptionEvent** - exception thrown by the adapter.
  See the :ref:`kamanjaexceptionevent-msg-ref` section below.


.. _kamanjamodelevent-msg-ref:

KamanjaModelEvent structure
---------------------------

::

  {
   "Container": {
       "NameSpace": "com.ligadata.KamanjaBase",
       "Name": "KamanjaModelEvent",
       "Version": "1.00",
       "Description": "Message Execution detail",
       "Fixed": "true",
       "Elements": [{
           "Field": {
           "Name": "modelid",
           "Type": "Long"
           }
       }, {
       "Field": {
       "Name": "elapsedtimeinms",
       "Type": "Float"
       }
       }, {
       "Field": {
           "Name": "eventepochtime",
           "Type": "Long"
       }
       }, {
           "Field": {
           "Name": "isresultproduced",
           "Type": "Boolean"
           }
       }, {
           "Field": {
               "Name": "consumedmessages",
               "Type": "ArrayOfLong"
           }
       }, {
           "Field": {
               "Name": "producedmessages",
               "Type": "ArrayOfLong"
           }
       }, {
           "Field": {
               "Name": "consumedcontainers",
               "Type": "ArrayOfLong"
           }
       }, {
           "Field": {
               "Name": "producedcontainers",
               "Type": "ArrayOfLong"
           }
       }, {
           "Field": {
               "Name": "error",
               "Type": "String"
           }
       }]
   }

KamanjaModelEvent parameters
----------------------------


- **modelid** - unique model ID.
- **elapsedtimeinms** - number of milliseconds it took to process the model.
- **eventepochtime** - time of the event, expressed as `Unix time
  <https://en.wikipedia.org/wiki/Unix_time>`_, which is the number
  of milliseconds since 1 January 1970.
- **isresultproduced** - TRUE or FALSE, depending on
  whether a result has been produced.
- **consumedmessages** - messages that were consumed.
- **producedmessages** - messages that were produced.
- **consumedcontainers** - containers that were consumed.
- **producedcontainers** - containers that were produced.



.. _kamanjaexceptionevent-msg-ref:

KamanjaExceptionEvent array
---------------------------

Kamanja produces a **KamanjaExceptionEvent**
if it encounters an error during its execution.
This exception message describes the error condition.

::

  KamanjaExceptionEvent

  {
   "Message": {
       "NameSpace": "com.ligadata.KamanjaBase",
       "Name": "KamanjaExceptionEvent",
       "Version": "1.02",
       "Description": "Exception Event detail",
       "Fixed": "true",
       "Elements": [{
           "Field": {
               "NameSpace": "com.ligadata.KamanjaBase",
               "Name": "ComponentName",
               "Type": "System.String"
           }
           }, {
           "Field": {
               "NameSpace": "com.ligadata.KamanjaBase",
               "Name": "TimeOfErrorEpochMs",
               "Type": "System.Long"
           }
           }, {
           "Field": {
               "NameSpace": "com.ligadata.KamanjaBase",
               "Name": "ErrorType",
               "Type": "System.String"
           }
           }, {
           "Field": {
               "NameSpace": "com.ligadata.KamanjaBase",
               "Name": "ErrorString",
               "Type": "System.String"
           }
       }]
   }
  }


KamanjaExceptionEvent parameters
--------------------------------

.. _kamanjaexcecutionfailureevent-msg-ref:

KamanjaExecutionFailureEvent structure
--------------------------------------

The **KamanjaExecutionFailureEvent** message is created
each time an error is encountered trying to process a message

::

  Execution Failures

  {
   "Message": {
   "NameSpace": "com.ligadata.kamanja",
   "Name": "KamanjaExecutionFailureEvent",
   "Version": "00.00.01",
   "Description": "kamanja error event description",
   "Fixed": "true",
   "Fields": [{
   "Name": "msgid",
   "Type": "Long"
   }, {
   "Name": "timeoferrorepochms",
   "Type": "Long"
   }, {
   "Name": "msgcontent",
   "Type": "String"
   }, {
   "Name": "msgadapterkey",
   "Type": "String"
   }, {
   "Name": "msgadaptervalue",
   "Type": "String"
   }, {
   "Name": "sourceadapter",
   "Type": "String"
   }, {
   "Name": "deserializer",
   "Type": "String"
   }, {
   "Name": "errordetail",
   "Type": "String"
   }]
   }
  }



KamanjaExecutionFailureEvent parameters
---------------------------------------



Usage
-----


Example
-------



See also
--------






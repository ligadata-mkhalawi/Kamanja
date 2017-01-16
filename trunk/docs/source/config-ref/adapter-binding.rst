
.. _adapter-binding-config-ref:

Adapter message binding definition
==================================

File structure
--------------

::

  [{
  "AdapterName": "testin_1",
  "MessageName": "com.ligadata.test.dag.NumberMessage",
  "Serializer": "com.ligadata.kamanja.serializer.csvserdeser",
  "Options": {
  "alwaysQuoteFields": false,
  "fieldDelimiter": ","
  }
  }, {
  "AdapterName": "testout_1",
  "MessageNames": [
  "com.ligadata.test.dag.DividedMessage"
  ],
  "Serializer": "com.ligadata.kamanja.serializer.jsonserdeser",
  "Options": {}
  }]

Parameters
----------

- **AdapterName** - name of the adapter to use to process this
  message.  This must match a **Name** that is defined in the
  :ref:`adapter definition<adapter-def-config-ref>`.

- **MessageName** -

- **Serializer**

- **Options**

  - **alwaysquoteFields** -

  - **fieldDelimiter** -


Usage
-----

See also
--------

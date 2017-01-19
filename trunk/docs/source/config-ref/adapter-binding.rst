
.. _adapter-binding-config-ref:

Adapter message binding definition
==================================

Adapter message bindings are independently cataloged
:ref:`metadata<metadata-term>` objects
that associate three other metadata objects together:

- an :ref:`adapter<adapter-term>` (input, output, or storage).
- a :ref:`message<messages-term>` that this adapter either deserializes
  from its source or serializes to send to its sink.
- the :ref:`serializer<serial-deserial-term>` to use that understands
  how to serialize and deserialize the message.

These bindings provide a flexible way to describe
the input, output, and storage streams
flowing to/from the adapter’s sink/source.
Being able to dial in the sort of serialization expected
or provided to others gives the Kamanja system administrator
a lot of flexibility to satisfy his/her objectives.

The Kamanja platform currently provides three builtin serializers:
a JSON serializer, a CSV serializer, and a KBinary serializer.
The KBinary is used principally by the Kamanja platform
to manage data in its stores and caches.

The adapter message bindings can be ingested one at a time.
If there are multiple messages that are managed by an adapter
that all share the same serializer, a compact representation is possible.
It is also possible to organize
one or more adapter message binding specifications
in a file and have the Kamanja ingestion tools consume that.

Note that adapters, messages, and serializers
must already be cataloged in the metadata
before a user can add a binding for them.
A rather comprehensive list of errors is returned if that is not the case.


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

:ref:`message-bindings-guide`


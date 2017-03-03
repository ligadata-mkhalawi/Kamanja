

.. _jtm-examples:

JTM by example
==============

This section presents annotated examples of the JTM section maps and their content.

Header
------

The header provides the identification information for the JTM.

::

  "header": {
  "namespace": "com.ligadata.jtm.test.filter",
  "version": "0.0.1",
  "language" : "scala",
  "minVersion" : "2.11"
  "description": "Add a short description here"
  "name": "className"
  }

The meaning of these fields is:


- **namespace** and **name** identify the fully-qualified class name
  that is generated in the target language.

- **language** is the target language for this JTM model.
  Scala is the only language that is supported at this time.

- **minVersion** describes the minimum Scala version that is acceptable.
  In other words, in this example, the Scala compiler running on the system
  should be 2.11.

- **description** should contain a meaningful description of this model.


- **version** is supplied to the metadata ingestion
  when the JTM is built and cataloged
  although it is not directly used in the computation.
  Should another model with the same namespace.name exist,
  the model version must be of a higher value
  when the digits of the version are considered as a long value.


Imports
-------

The imports: packages array specifies additional packages/libraries
that are used by the expressions declared in the JTM.

::

  "imports": {
  "packages": [
  "com.ligadata.Utils._",
  "com.ligadata.runtime._"
  ]
  }

In this example, all classes and objects found
in the com.ligadata.Utils and com.ligadata.runtime
are available for use in the JTM.

Note that is not necessary to include the org.aicer.grok._ libraries
(explained in the next section).
These are included automatically by the JTM compiler.


Grok
----

The grok JSON dictionary declares the characteristics
of the grok pattern dictionary that presumably are used
on string variables/field values in the transformations section that follows.
Any matches extracted with grok patterns are by definition of type String.
Only String inputs can be searched with the current implementation.
There currently can be only one grok declaration in the JTM source.
This is really not a limitation in that many patterns
may be declared in the grok instance.

::

  "grok": {
  "myGrokPatterns" : {
  "builtInDictionary": true,
  "match": "[
  \“{EMAIL: email}\",
  \“{IP: ipaddr}\"
  ],
  "file": ["path"],
  "patterns_dis": {
  "DOMAINTLD": "[a-zA-Z]+",
  "EMAIL": "%{NOTSPACE}@%{WORD}\\.%{DOMAINTLD}"
  }
  }
  }


In the example, the name of the grok instance is myGrokPatterns.

- **builtinDictionary** tells the JTM compiler to include
  the built-in patterns supplied in the `aicer
  <https://github.com/aicer/grok>`_ library.

- The **match** is the list of patterns from the supplied patterns
  (in the built-in dictionary, from the file path or declared in-line)
  that is used in the transformations section expressions.
  It may be either a list of aicer patterns or a single pattern.

- **patterns** can be composed of other patterns in grok,
  so specify a file path containing custom patterns
  that may be used in the transformations section.
  If desired, it is also possible to declare custom patterns
  in the grok declaration itself in the **patterns_dis** field.

When the built-in patterns contain a pattern that has the same name
as a pattern that is included in this file,
the version in this file takes precedence.
Similarly, should a pattern that shares a name
with a pattern defined either in this file
or in the builtInDictionary
be explicitly added in the **patterns_dis** declaration,
the **patterns_dis** declaration takes precedence.


Aliases
-------

The aliases provides a short-hand way to refer to the fully-qualified messages,
concepts, and general variables that may be used.
Instead of using the full name in the transformations section expressions,
the aliases can be used to shorten the expressions, making them less verbose.

::

    {
    "aliases": {
    "messages": {
    "m1": "com.ligadata.kamanja.test.msg5",
    "o1": "com.ligadata.kamanja.test.msg2"
    },
    "concepts": {
    "cm1": "com.ligadata.kamanja.test.msg5",
    "co1": "com.ligadata.kamanja.test.msg2"
    },
    "variables": {
    "cm1": "com.ligadata.kamanja.test.msg5",
    "co1": "com.ligadata.kamanja.test.msg2"
  }
  }

There are four types: messages, containers, concepts, and variables.

It is not necessary to use the aliases; full names can be used if desired.

Transformations
---------------

One or more named transformation maps can appear in the transformations section.
As introduced in the JTM Language Elements section,
each transformation has a number of elements describing the input messages,
output messages, and transformations done to produce those output messages.
They are:

- dependsOn
- computes
- grok match
- outputs

The following annotated snippets illustrate their use.

::

  "bankTransactionTransform": {

  "dependsOn": [
  [
  "m1"
  ]
  ],
	
The transformation has a name. In the example, it is bankTransactionTransform.
Following it is the **dependOn** list that contains one or more aliases
referring to the incoming message(s) to be transformed.

The **dependsOn** list is followed by the JSON map of computes expressions.

::

  "computes": {
  "inputFields": {
  "type": "Array[String]",
  "val": "$m1.split(\",\", -1)",
  "comment": "Split the incoming data into fields"
  },
  "preferencesTable": {
  "type": "Map[String, Any]",
  "val": "Preferences.fetch(inputFields(1).trim, Map[String,Any]())",
  "comment": "for this transaction, get the user's online bank preferences"
  },
  "connectString": {
  "type": "String",
  "val": "inputFields(3).trim",
  "comment": "connection string with email and ip addresses"
  }
  },

In this snippet, three variables are created:
inputFields, preferencesTable, and connectString.
The first is an array of all of the incoming fields that are created
by splitting the (presumably) comma-delimited input that has been supplied.
The second uses the second field value
(imagine this is the account ID for this transaction).
It calls the object Preferences method, fetch(String),
that has been included in the includes section previously.
It answers a Map[String,String] of preferences for the person with this account,
or if the account is new and there are no preferences for this ID,
an empty map is returned.

Notice that the exact type can be declared in the type value.
The expression executed (included in the compiler’s generation of Scala)
is found in the **val** value.
It is a good idea to comment every compute declaration.

When the values to be emitted are embedded in one or more fields of the input,
one uses a grok match expression:

::

  "grok match": {
  "m1": "{EMAIL: email} {DOMAIN: domain} {USER: user}",
  "${inputFields(2)}": "{IP: ipAddr1}",
  “${connectString}” : “{IP: ipAddr2}”
  },

Three match patterns are supplied:

- The first searches the entirety of input message referred by alias m1.
  It sets three variables: email, domain, and user,
  which may then be used for other computations or emission in the outputs section.
  The respective patterns used for the search are EMAIL, DOMAIN, and USER.
  These patterns and the patterns that may have been used to construct them
  are found in the patterns_dis, input, or built-in patterns
  described in the grok declaration.

- The second pattern searches the second field with the IP pattern
  to produce ipAddr1. The third pattern does the same except it uses
  a trimmed version of the third field stored in variable connectString.
  The extracted string is saved in ipAddr2.

Following the computes and/or grok declarations
are one or more output declarations found nested in the outputs JSON map.
The output map members are keyed by the name of
either the alias of an output message from the alias section
or the explicit package-qualified class name that is to be used.

An output JSON map provides the necessary transformations and assignments
to fill in an instance of the output message that has the given alias name as key.

Here is an example of an output message specification
that illustrates how JTM output formation works:

::
  "outputs": {
  "o1": {
  "mapbypositionb": {
  "inputFields": [ “-”, "in2", "-"]
  },

  "mapping": {
  "out1": "in1",
  "out2": "t1",
  "out4": "in3"
  },

  "where": "!($in2 != -1 &amp;&amp; $in2 &lt; 100)",

  "computes": {
  "t1": { "type": "String", "val": "\"s:\" + ($in2).toString()" }
  }
  }
  }

The o1 output message is comprised of a field (in2) from the inputFields array
followed by a field from the original input message (in1),
a computed field from the computes clause in this scope (t1),
followed by another input message field (in3).

In the example, two techniques are illustrated.
The **mapbypositionb** accesses the in2 of the inputFields by position.
Recall that the inputFields was the split of the incoming data.
The first field and the field after field in2 are ignored.
The compiler determines the ordinal of the in2 field
using the associated m1 message type information.

The second mechanism is a mapping where the m1 metadata
again is used to determine which field to select for output,
but this time the selection is by name.
Any Scala expression can be used for the value side (rhs)
in order to produce the value to be assigned to the named output field in the lhs.
This would include the use of a function library
that has been imported to derive these values.

This example also illustrates how a value can be computed
in this output scope and included in the output message.
Variable t1 in the computes clause is used as the fourth field.

Finally, there is the where clause in the example.
Using the where clause allows files to be split
between several output specifications found in the outputs JSON map
by using the appropriate Boolean expression in the where value.
In the example above, in2 values less than 100 are selected.
If those whose in2 value >= 100 was captured,
this where clause could be used in another output specification:

::

  "where": "!($in2 != -1 &amp;&amp; $in2 &gt;= 100)",




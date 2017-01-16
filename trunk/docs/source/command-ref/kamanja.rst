


.. _kamanja-command-ref:

kamanja
=======

The **kamanja** command is used to manage metadata
and other Kamanja engine maintenance tasks.

Syntax
------

::

  kamanja <config file path> <command> <command arguments>

Message operations
~~~~~~~~~~~~~~~~~~

::

  kamanja add message [input] TENANTID   [Properties | PropertiesFile] 
  kamanja update message [input] TENANTID  [Properties | PropertiesFile] 
  kamanja get message [input] [TENANTID]
  kamanja get all messages [TENANTID]
  kamanja remove message [input]

Model operations
~~~~~~~~~~~~~~~~

::

  kamanja add model kpmml [input] TENANTID   [Properties | PropertiesFile] 
  kamanja add model pmml input MODELNAME namespace.name MODELVERSION nn.nn.nn MESSAGENAME namespace.name TENANTID  [Properties | PropertiesFile] 
  kamanja add model java [input]  TENANTID  [Properties | PropertiesFile] 
  kamanja add model scala [input] TENANTID  [Properties | PropertiesFile] 
  kamanja add model jtm [input] TENANTID  [Properties | PropertiesFile] 
  kamanja get model [input] TENANTID(optional) 
  kamanja get all models TENANTID(optional) 
  kamanja remove model [input]
  kamanja update model kpmml [input] TENANTID  [Properties | PropertiesFile] 
  kamanja update model pmml input MODELNAME namespace.name MODELVERSION nn.nn.nn TENANTID  [Properties | PropertiesFile] 
  kamanja update model scala [input] TENANTID  [Properties | PropertiesFile] 
  kamanja update model java [input] TENANTID  [Properties | PropertiesFile] 
  kamanja update model jtm [input] TENANTID  [Properties | PropertiesFile] 
  kamanja deactivate model [input]
  kamanja activate model [input]

Container operations
~~~~~~~~~~~~~~~~~~~~

::

  kamanja add container [input] TENANTID  [Properties | PropertiesFile] 
  kamanja update container [input] TENANTID  [Properties | PropertiesFile] 
  kamanja get container [input] TENANTID(optional) 
  kamanja get all containers TENANTID(optional) 
  kamanja remove container [input]

Congurations operations
~~~~~~~~~~~~~~~~~~~~~~~

::

  kamanja upload cluster config [input]
  kamanja upload compile config [input]
  kamanja dump all cfg objects
  kamanja remove engine config


Jar operations
~~~~~~~~~~~~~~

::

  kamanja upload jar

Cluster, metadata and adapter operations
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

::

  kamanja dump metadata
  kamanja dump all nodes
  kamanja dump all clusters
  kamanja dump all cluster cfgs
  kamanja dump all adapters

Kamanja engine operations
~~~~~~~~~~~~~~~~~~~~~~~~~

::

  kamanja start [jvmoptions] [Properties | PropertiesFile]
  kamanja start -v (for verbose) [jvmoptions] [Properties | PropertiesFile]

Topic operations
~~~~~~~~~~~~~~~~

::

  kamanja watch status queue
  kamanja push data
  kamanja create queues

Web service
~~~~~~~~~~~

::

  kamanja start [jvmoptions] webservice


Adapter Message Bindings
~~~~~~~~~~~~~~~~~~~~~~~~

::

  kamanja add adaptermessagebinding FROMFILE input
  kamanja add adaptermessagebinding FROMSTRING input
  kamanja remove adaptermessagebinding KEY ',,'
  kamanja list adaptermessagebindings
  kamanja list adaptermessagebindings ADAPTERFILTER 
  kamanja list adaptermessagebindings MESSAGEFILTER 
  kamanja list adaptermessagebindings SERIALIZERFILTER 
  kamanja get typebyschemaid SCHEMAID 
  kamanja get typebyelementid ELEMENTID 

General operations
~~~~~~~~~~~~~~~~~~

::

  kamanja --version
  bash kamanja stop
  kamanja watch input queue
  kamanja watch output queue
  kamanja watch failed events queue

Options and arguments
---------------------

- **MODELNAME** - specify the name of the module
  (in the format <modulename>.<classname>)
  that contains the execute method.

- **MESSAGENAME** - name of the input message
  for the consumption of the input message.

- **OUTMESSAGE** – name of the output queue where produced messages are sent.

- **MODELOPTIONS** – active input fields

- **Properties|PropertiesFile** - properties can be specified
  on the **kamanja** command line, either using a command string
  or by using the **-PropertiesFile** argument to specify the file
  that contains the desired properties.

  The added properties can be seen when the objects are retrieved
  using the GET command.

  If the command line specifies both a PropertiesFile
  and command string properties, the PropertiesFile takes precedence;
  the command string Properties are ignored.

Usage
-----

The config file refers to the :ref:`metadataapiconfig-config-ref` file
that describes the Kamanja metadata store and associated values
needed by the MetadataAPI instance.
This file, by default, is $KAMANJA_HOME/config/MetadataAPIConfig.properties.
The command describes the operation requested.
The remaining values on the Kamanja command-line
are appropriate arguments for the command specified.

An example:

::

  bash $KAMANJA_HOME/bin/kamanja add message \
      $KAMANJA_HOME/input/SampleApplications/metadata/message/beneficiary_Medical.json TENANTID <id>

Note Kamanja has basic multi-tenancy (new in v1.4).
Therefore, every time a container, message, or model is added or updated,
specify the tenant ID.
Multi-tenancy allows deployment of more than one use case to a cluster.
Multiple use cases can exist on the same cluster.

Installing an Application – Order Matters
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The Kamanja utility typically installs one thing at a time.
To build an application that is useful, however,
many things are typically required.
For a simple custom Java/Scala application,
configuration files are installed in the following order:

- Create queues
- Containers
- Messages
- Compile instructions
- Scala/Java model

Should a container have another container or collection of other containers
as a field, the field’s type must be added
before it can be used in the enclosing container declaration.

For Kamanja PMML applications,
other kinds of objects may be needed, including function definitions. 

Examples
--------

Create queues
~~~~~~~~~~~~~

This command creates topics that contain the containers
and messages to add later:

::

  bash $KAMANJA_HOME/bin/kamanja create queues

Add commands
~~~~~~~~~~~~

The next command adds a container definition (either JSON or XML) from a file.
When no path argument is supplied,
a list of the files found at the default location is offered
and the user may choose one.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> add container \
    <Container definition path> TENANTID <id>

This message adds a message definition (either JSON or XML) from a file.
When no path argument is supplied,
a list of the files found at the default location is offered
and the user may choose one:

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> add message \
    <Message definition path> TENANTID <id>

To add a Kamanja kPMML definition from a file:

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> add model
    kpmml <Kamanja kPMML model path> TENANTID <id>


This command adds a Kamanja PMML definition (XML) from a file.
When no path argument is supplied,
a list of the files found at the default location is offered
and the user may choose one:

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> add model pmml \
    <Kamanja PMML model path> MODELNAME <model name> \
    MODELVERSION <model version> MESSAGENAME <message name> TENANTID <id>

This command adds a Java model to the metadata.
When no path argument is supplied,
a list of the files found at the default location is offered
and the user may choose one.
See Java Models for details about Java models.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> add model java
    <Java model path> TENANTID <id>

This command adds a Scala model to the metadata.
When no path argument is supplied,
a list of the files found at the default location is offered
and the user may choose one.
See Scala Models for details about Scala models.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> add model scala
    <Scala model path> TENANTID <id>

This command adds a :ref:`JTM<jtm-term>`.

bash $KAMANJA_HOME/bin/kamanja <config file path> add model jtm <JTM path> TENANTID <id>

Add a cluster configuration file:

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> \
    upload cluster config <Cluster configuration path>


Note: The upload command word is used for historical reasons.
The :ref:`ClusterConfig.json<clusterconfig-config-ref>` file
is not actually copied to any other servers.
The JSON is loaded in the local data store.
When Kamanja is started,
it checks for the config information in the local data store.

This command adds a Java or Scala compile metadata configuration.
Note that the compile configuration metadata must exist
before a compile of the corresponding model is attempted.
When no path argument is supplied,
a list of the files found at the default location is offered
and the user may choose one:

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> upload compile \
    config <Compile configuration path>

Add the function definition found in the supplied string:

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> add function <Function definition string>

Add an adapter message binding from a file.
See message bindings for details about adapter message bindings.

::

  bash $KAMANJA_HOME/bin/kamanja add adaptermessagebinding FROMFILE <Adapter binding JSON>

Add an adapter message binding from a string.
See message bindings for details about adapter message bindings.

::

  bash $KAMANJA_HOME/bin/kamanja add adaptermessagebinding FROMSTRING <Adapter binding string>

Remove Commands
~~~~~~~~~~~~~~~

Remove a message.
A list of messages is presented from which to choose
which message should be removed.
Alternatively, a fully-qualified name (namespace.name.version)
may be specified on the command-line.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> remove message

Remove a model.
A list of models is presented from which to choose
which model should be removed.
Alternatively, a fully-qualified name (namespace.name.version)
may be specified on the command-line.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> remove model

Remove a container.
A list of containers is presented from which to choose
which container should be removed.
Alternatively, a fully-qualified name (namespace.name.version)
may be specified on the command-line.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> remove container

Remove a function.
A list of functions is presented from which to choose
which function should be removed.
Alternatively, a fully-qualified name (namespace.name.version)
may be specified on the command-line.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> remove function

Remove an engine configuration.
A list of configurations is presented from which to choose
which node configuration is to be removed.
Alternatively a fully-qualified name (namespace.name.version)
may be specified on the command-line.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> remove engine config

Remove an adapter message binding.
See message bindings for details about adapter message bindings.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> \
    remove adaptermessagebinding
    KEY ‘<adapter name>,<namespace.msgname>,<namespace.serializername>’

Update Commands
~~~~~~~~~~~~~~~

Update a message.
A list of messages is presented from which to choose
which message should be updated.
Alternatively, a message definition file path may be supplied explicitly
bypassing the menu selection process.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> \
    update message <command parameters> TENANTID <id>

Update a container.
A list of containers is presented from which to choose
which container should be updated.
Alternatively, a container definition file path
may be supplied explicitly bypassing the menu selection process.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> \
    update container <command parameters> TENANTID <id>

Update a model.
A list of models is presented from which to choose
which model should be updated.
Alternatively, a model definition file path may be supplied explicitly
bypassing the menu selection process.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> \
    update model <command parameters> TENANTID <id>

Note: If you execute an update model command
and the does not match the existing , an error message is returned.

Update a function.  A list of functions is presented
from which to choose which message should be updated.
Alternatively, a function definition file path
may be supplied explicitly bypassing the menu selection process.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> \
    update function <command parameters>

Query Commands
~~~~~~~~~~~~~~

List message(s).  A list of messages is presented
from which to choose which message should be listed.
Alternatively, a message name or part of a message name
(namespace.name.version) can be supplied
on the command-line and used to filter the messages.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> get message

Get all messages.  List all messages.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> get all messages

List model(s).  A list of models is presented
from which to choose which model should be listed.
Alternatively, a model name or part of a model name
(namespace.name.version) can be supplied on the command-line
and used to filter the models.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> get model

Get all models.  List all models.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> get all models

List container(s).  A list of containers is presented
from which to choose which container should be listed.
Alternatively, a container name or part of a container name
(namespace.name.version) can be supplied on the command-line
and used to filter the containers.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> get container

Get all containers.  List all containers.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> get all containers

List function(s).  A list of functions is presented
from which to choose which function should be listed.
Alternatively, a function name or part of a function name
(namespace.name.version) can be supplied on the command-line
and used to filter the containers.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> get function

Retrieve the message/container of that particular schema ID.

::

  $KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties \
    get typebyschemaid SCHEMAID 2000001

Retrieve message/container/model of the element ID.

::

  $KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties \
    get typebyelementid ELEMENTID 2000002

Retrieves list of adapter message bindings.
See message bindings for details about adapter message bindings.

::

  bash $KAMANJA_HOME/bin/kamanja list adaptermessagebindings

Retrieves all bindings per adapter name.
See message bindings for details about adapter message bindings.

::

  bash $KAMANJA_HOME/bin/kamanja list adaptermessagebindings \
    ADAPTERFILTER <adapter name>

Retrieve all bindings per message name.
See message bindings for details about adapter message bindings.

::

  bash $KAMANJA_HOME/bin/kamanja list adaptermessagebindings
    MESSAGEFILTER <message name>

Retrieve all bindings per serializer name.
See message bindings for details about adapter message bindings.

::

  bash $KAMANJA_HOME/bin/kamanja list adaptermessagebindings \
    SERIALIZERFILTER <serializer name>

Get the Kamanja version.

::

  bash $KAMANJA_HOME/bin/kamanja --version

Dump Commands
~~~~~~~~~~~~~

Dump all functions known in the metadata.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> dump all functions

Dump all metadata information configurations
for custom Java/Scala models known in the metadata.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> dump all cfg objects

Dump all metadata.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> dump metadata

Dump all nodes in some cluster configuration.
A list of configurations is presented from which to choose.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> dump all nodes

Dump all clusters known in the system,
including their cluster node configurations.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> dump all clusters

Similar to dump all clusters but excludes the cluster node information.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> dump all cluster cfgs

Dump the adapter metadata for every adapter known in the system.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> dump all adapters

Administrative Commands
~~~~~~~~~~~~~~~~~~~~~~~

Activate a model.
A list of the inactive models described in the system
is presented so that the user can choose.
Alternatively, the name of the model to activate
may be supplied on the command-line.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> activate model

Deactivate a model.
A list of the active models is presented for consideration.
One of these active models can be selected for deactivation.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> deactivate model

Upload an arbitrary JAR that may be needed
by an arbitrary component in the cluster.

::

  bash $KAMANJA_HOME/bin/kamanja <config file path> upload jar

Start the Kamanja engine.

::

  bash $KAMANJA_HOME/bin/kamanja start

Start the Kamanja engine in verbose mode.

::

  bash $KAMANJA_HOME/bin/kamanja start -v

Watch the status queue after starting the Kamanja engine.

::

  bash $KAMANJA_HOME/bin/kamanja watch status queue

Push sample data to the Kamanja engine.

::

  bash $KAMANJA_HOME/bin/kamanja push data

Start the web service.

::

  bash $KAMANJA_HOME/bin/kamanja start web service

Output
------

Executing any of these commands returns an APIResult,
which contains a message that indicates the proper input
that is required to retrieve a model.

If an informative APIResult is not returned,
post the issue on The Kamanja Forums
and a LigaData engineer will look into it.

Examples
--------

Properties parameter
~~~~~~~~~~~~~~~~~~~~

Here is an example of adding a Message_Definition_HelloWorld.json message
with the PropertiesFile parameter:

::

  $KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties
    add message $KAMANJA_HOME/input/SampleApplications/metadata/message/Message_Definition_HelloWorld.json
    TENANTID hello PropertiesFile $KAMANJA_HOME/config/HelloProp.json

Expected output:

::

  Result: {
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "AddMessageDef",
      "Result Data" : null,
      "Result Description" : "Message Added Successfully:com.ligadata.kamanja.samples.messages.msg1.000000000001000000"
    }
  }

Here is an example of getting that last message to see if it was added:

::

  kamanja get message com.ligadata.kamanja.samples.messages.msg1.000000000001000000

Expected output:

::

  Result: {
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "GetMessageDefFromCache",
      "Result Data" : "{\"Message\":{\"NameSpace\":\"com.ligadata.kamanja.samples.messages\",\"Name\":\"msg1\",\"FullName\":\"com.ligadata.kamanja.samples.messages.msg1\",
          \"Version\":\"000000000001000000\",\"TenantId\":\"hello\",
          \"Description\":\"hello there howdy \\n hello there howdy\\n hello there howdy.\",\"Comment\":\"this is comment\",
          \"Author\":null,\"Tag\":\"1n343434\",
          \"OtherParams\":\"{\\\"b\\\":\\\"there\\\",
          \\\"a\\\":\\\"hello\\\"}\",\"CreatedTime\":1466538621856,
          \"UpdatedTime\":1466538621856,\"ElementId\":2000024,\"ReportingId\":2000062,
          \"SchemaId\":2000020,\"AvroSchema\":\"{ \\\"type\\\": \\\"record\\\",
            \\\"namespace\\\" : \\\"com.ligadata.kamanja.samples.messages\\\" , \\\"name\\\" : \\\"msg1\\\" ,
           \\\"fields\\\":[{ \\\"name\\\" : \\\"id\\\" , \\\"type\\\" : \\\"int\\\"},
          { \\\"name\\\" : \\\"name\\\" , \\\"type\\\" : \\\"string\\\"},
          { \\\"name\\\" : \\\"score\\\" , \\\"type\\\" : \\\"int\\\"}]}\",
          \"JarName\":\"com.ligadata.kamanja.samples.messages_msg1_1000000_1466538617144.jar\",
          \"PhysicalName\":\"com.ligadata.kamanja.samples.messages.V1000000.msg1\",
          \"ObjectDefinition\":\"{\\n  \\\"Message\\\": {\\n    \\\"NameSpace\\\": \\\"com.ligadata.kamanja.samples.messages\\\",
          \\n    \\\"Name\\\": \\\"msg1\\\",
          \\n    \\\"Version\\\": \\\"00.01.00\\\",
          \\n    \\\"Description\\\": \\\"Hello World Processing Message\\\",
          \\n    \\\"Fixed\\\": \\\"true\\\",\\n    \\\"Fields\\\": [\\n      {\\n        \\\"Name\\\": \\\"Id\\\",
          \\n        \\\"Type\\\": \\\"System.Int\\\"\\n      },
          \\n      {\\n        \\\"Name\\\": \\\"Name\\\",
          \\n        \\\"Type\\\": \\\"System.String\\\"\\n      },
          \\n      {\\n        \\\"Name\\\": \\\"Score\\\",
          \\n        \\\"Type\\\": \\\"System.Int\\\"\\n      }\\n    ]\\n  }\\n}\",
          \"ObjectFormat\":\"JSON\",\"DependencyJars\":[],
          \"MsgAttributes\":[{\"NameSpace\":\"system\",\"Name\":\"id\",
          \"TypNameSpace\":\"system\",\"TypName\":\"int\",\"Version\":1000000,
          \"CollectionType\":\"None\"},{\"NameSpace\":\"system\",\"Name\":\"name\",
          \"TypNameSpace\":\"system\",\"TypName\":\"string\",\"Version\":1000000,
          \"CollectionType\":\"None\"},{\"NameSpace\":\"system\",\"Name\":\"score\",
          \"TypNameSpace\":\"system\",\"TypName\":\"int\",\"Version\":1000000,
          \"CollectionType\":\"None\"}],\"PrimaryKeys\":[],\"ForeignKeys\":[],
          \"TransactionId\":34}}",
       "Result Description" : "Successfully fetched message from cache"
    }
  }

Here is an example of updating the Message_Definition_HelloWorld.json message
with the Properties parameter:

::

  kamanja update message $KAMANJA_HOME/input/SampleApplicationmetadata/message/Message_Definition_HelloWorld.json
    TENANTID hello Properties ‘{“Description” : “This is the new description”,
    “Comment” : “The update is done to test the new feature”,
    “Tag” : “NEWTAG”,
    “OtherParams” : “The test was executed by QA department to verify”}’

Expected output:

::

  Result: {
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "AddMessageDef",
      "Result Data" : null,
      "Result Description" : "Message Added Successfully:com.ligadata.kamanja.samples.messages.msg1.000000000001000001"
    }
  }
  RecompileModel results for com.ligadata.kamanja.samples.models.helloworldmodel.1
  {
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "RemoveModel",
      "Result Data" : null,
      "Result Description" : "Deleted Model Successfully:com.ligadata.kamanja.samples.models.helloworldmodel.000000000000000001"
    }
  }{
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "AddModel",
      "Result Data" : null,
      "Result Description" : "Model Added Successfully:com.ligadata.kamanja.samples.models.helloworldmodel.000000000000000001"
    }
  }

Here is an example of getting that last message to see if it was updated:

::

  kamanja get message com.ligadata.kamanja.samples.messages.msg1.000000000001000001

Expected output:

::
  
  Result: {
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "GetMessageDefFromCache",
      "Result Data" : "{\"Message\":{\"NameSpace\":\"com.ligadata.kamanja.samples.messages\",
          \"Name\":\"msg1\",\"FullName\":\"com.ligadata.kamanja.samples.messages.msg1\",
          \"Version\":\"000000000001000001\",\"TenantId\":\"hello\",
          \"Description\":\"this is the new description\",
          \"Comment\":\"the update is done to test the new feature\",
          \"Author\":null,\"Tag\":\"newtag\",
          \"OtherParams\":\"{\\\"otherparams\\\":\\\"the test was executed by qa department to verify\\\"}\",
          \"CreatedTime\":1466541054527,\"UpdatedTime\":1466541063645,
          \"ElementId\":2000024,\"ReportingId\":2000065,\"SchemaId\":2000021,
          \"AvroSchema\":\"{ \\\"type\\\": \\\"record\\\",
          \\\"namespace\\\" : \\\"com.ligadata.kamanja.samples.messages\\\" ,
          \\\"name\\\" : \\\"msg1\\\" , \\\"fields\\\":[{ \\\"name\\\" : \\\"id\\\" ,
          \\\"type\\\" : \\\"int\\\"},{ \\\"name\\\" : \\\"name\\\" , \\\"type\\\" : \\\"string\\\"},
          { \\\"name\\\" : \\\"score\\\" , \\\"type\\\" : \\\"int\\\"}]}\",
              \"JarName\":\"com.ligadata.kamanja.samples.messages_msg1_1000001_1466541059070.jar\",\"PhysicalName\":\"com.ligadata.kamanja.samples.messages.V1000001.msg1\",
              \"ObjectDefinition\":\"{\\n  \\\"Message\\\": {\\n    \\\"NameSpace\\\":
              \\\"com.ligadata.kamanja.samples.messages\\\",\\n
              \\\"Name\\\": \\\"msg1\\\",\\n    \\\"Version\\\": \\\"00.01.01\\\",\\n
              \\\"Description\\\": \\\"Hello World Processing Message\\\",\\n
              \\\"Fixed\\\": \\\"true\\\",\\n    \\\"Fields\\\":
              [\\n      {\\n        \\\"Name\\\": \\\"Id\\\",\\n        \\\"Type\\\": \\\"System.Int\\\"\\n      },\\n
              {\\n        \\\"Name\\\": \\\"Name\\\",\\n        \\\"Type\\\": \\\"System.String\\\"\\n      },
               \\n      {\\n        \\\"Name\\\": \\\"Score\\\",\\n
               \\\"Type\\\": \\\"System.Int\\\"\\n      }\\n    ]
               \\n  }\\n}\\n\",\"ObjectFormat\":\"JSON\",
               \"DependencyJars\":[\"com.ligadata.kamanja.samples.messages_msg1_1000000_1466538617144.jar\"],
               \"MsgAttributes\":[{\"NameSpace\":\"system\",\"Name\":\"id\",
               \"TypNameSpace\":\"system\",\"TypName\":\"int\",\"Version\":1000001,
               \"CollectionType\":\"None\"},{\"NameSpace\":\"system\",\"Name\":\"name\",
               \"TypNameSpace\":\"system\",\"TypName\":\"string\",\"Version\":1000001,
               \"CollectionType\":\"None\"},{\"NameSpace\":\"system\",\"Name\":\"score\",
               \"TypNameSpace\":\"system\",\"TypName\":\"int\",\"Version\":1000001,
               \"CollectionType\":\"None\"}],\"PrimaryKeys\":[],\"ForeignKeys\":[],\"TransactionId\":37}}",
      "Result Description" : "Successfully fetched message from cache"
    }
  }


See also
--------

- :ref:`MetadataAPIConfig.properties<metadataapiconfig-config-ref>`
  configuration file reference




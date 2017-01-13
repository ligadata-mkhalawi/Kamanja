


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

  kamanja add message input(optional) TENANTID   Properties/PropertiesFile(optional) 
  kamanja update message input(optional) TENANTID  Properties/PropertiesFile(optional) 
  kamanja get message input(optional) TENANTID(optional) 
  kamanja get all messages TENANTID(optional) 
  kamanja remove message input(optional)

Model operations
~~~~~~~~~~~~~~~~

::

  kamanja add model kpmml input(optional) TENANTID   Properties/PropertiesFile(optional) 
  kamanja add model pmml input MODELNAME namespace.name MODELVERSION nn.nn.nn MESSAGENAME namespace.name TENANTID  Properties/PropertiesFile(optional) 
  kamanja add model java input(optional)  TENANTID  Properties/PropertiesFile(optional) 
  kamanja add model scala input(optional) TENANTID  Properties/PropertiesFile(optional) 
  kamanja add model jtm input(optional) TENANTID  Properties/PropertiesFile(optional) 
  kamanja get model input(optional) TENANTID(optional) 
  kamanja get all models TENANTID(optional) 
  kamanja remove model input(optional)
  kamanja update model kpmml input(optional) TENANTID  Properties/PropertiesFile(optional) 
  kamanja update model pmml input MODELNAME namespace.name MODELVERSION nn.nn.nn TENANTID  Properties/PropertiesFile(optional) 
  kamanja update model scala input(optional) TENANTID  Properties/PropertiesFile(optional) 
  kamanja update model java input(optional) TENANTID  Properties/PropertiesFile(optional) 
  kamanja update model jtm input(optional) TENANTID  Properties/PropertiesFile(optional) 
  kamanja deactivate model input(optional)
  kamanja activate model input(optional)

Container operations
~~~~~~~~~~~~~~~~~~~~

::

  kamanja add container input(optional) TENANTID  Properties/PropertiesFile(optional) 
  kamanja update container input(optional) TENANTID  Properties/PropertiesFile(optional) 
  kamanja get container input(optional) TENANTID(optional) 
  kamanja get all containers TENANTID(optional) 
  kamanja remove container input(optional)

Congurations operations
~~~~~~~~~~~~~~~~~~~~~~~

::

  kamanja upload cluster config input(optional)
  kamanja upload compile config input(optional)
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

  kamanja start jvmoptions(optional) Properties/PropertiesFile(optional)
  kamanja start -v (for verbose) jvmoptions(optional) Properties/PropertiesFile(optional)

Topic operations
~~~~~~~~~~~~~~~~

::

  kamanja watch status queue
  kamanja push data
  kamanja create queues

Web service
~~~~~~~~~~~

::

  kamanja start jvmoptions(optional) webservice


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

- **MODELOPTIONS** – (optional) contains the active input fields
  from the consumed input message, in JSON format.

Usage
-----

The config file refers to the MetadataAPI configuration properties file
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
The :ref:`ClusterConfig.json<cluster-config-ref>` file
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

See also
--------

- :ref:`MetadataAPIConfig.properties<metadataapiconfig-config-ref>`
  configuration file reference




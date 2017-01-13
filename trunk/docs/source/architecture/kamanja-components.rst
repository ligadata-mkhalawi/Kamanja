
.. _kamanja-components-arch:

Kamanja components
==================

.. image:: /_images/kamanja-arch.png


This figure illustrates the basic elements of Kamanja,
including its inputs and outputs.

- Models – represent the business logic,
  the “rules” that make the decisions within the Kamanja engine.
- Metadata – maintained and managed within Kamanja.
  Metadata consists of all the information that the engine needs
  to process incoming messages, including models,
  information about message types, and configuration information.
- Input/Sources – a variety of sources can serve as input to the Kamanja engine.
  The most common source today is Kafka, an open-source messaging system.
  Kafka takes incoming messages and distributes them on
  publish-subscribe queues,
  from which Kamanja fetches the data and feeds it into the engine.
  Other sources of data, including databases and Hadoop clusters,
  are supported through simple input adapters.
- Input Adapter – Java class that converts input data in some form
  into a serial stream that can be used by the Kamanja engine.
  Kamanja can recognize incoming messages of a variety of types,
  including JSON, XML, MQ, and more.
  Messages are passed from the input adapter into the Kamanja engine,
  where deployed models are applied.
- Stored Data – models often require previous data
  in order to process new messages.
  Kamanja supports access to several data stores,
  including HBase (Hadoop) and Cassandra,
  which may be deployed as part of or external to the Kamanja cluster itself.
- Output Adapter – takes results from a model
  then formats and distributes them as messages
  that may be processed by a consumer.
  In most implementations today,
  output messages are distributed through Kafka queues,
  but its flexible design allows Kamanja to integrate
  with virtually any downstream application or service.
- Output/Consumers – downstream consumers may take
  any number of actions based on messages received from Kamanja,
  including presenting them on a dashboard, generating alerts or triggers,
  passing them on to other applications for further processing,
  or simply storing them in a database for further review or analysis.

Continue reading for a more in-depth discussion of elements found in Kamanja.

Messages
--------

Kamanja ingests input messages and generates output messages
based on the criteria defined by models.

Containers
----------

Containers are essentially the same as messages
but they are used in a slightly different way.
Conceptually, containers hold information
that the user wants to keep versus the input message
that describes the message presented to the model.
Containers are more like descriptions of information
that the model may want to maintain in order to interpret future messages.
Containers have state information that the model needs.

Models
------

One of Kamanja's design intentions is to support
streaming model applications in a technology-agnostic way as possible.
The engine itself is currently written in Scala (that is, JVM-based).
However, the data mining and machine-learning world
is not particularly fixed on JVM-based solutions.
For this reason, Kamanja supports (or, in some cases, plans to support)
models written in the JVM languages
(for example, Java, Scala, Clojure, Jython),
compiled with the builtin compilers, PMML (for example, Kamanja PMML, JPMML),
(C)Python (via IPC client and adapter/server),
DSL (domain-specific language),
and binary models compiled outside Kamanja
but conforming to the Kamanja model interfaces.

In the models section, there are links to the pages
that describe each model type in some detail.
Both the metadata managed by Kamanja for models
as well as the model interfaces that bind all models to the engine
that calls them are also introduced.
These pages are important in order to add a model type
or adapt the Kamanja engine to use a model type.

Model Metadata
~~~~~~~~~~~~~~

For each model cataloged via the add model command in the Kamanja metadata,
the ModelDef instance is created.
In addition to the namespace, name, version, and other information
common to all metadata elements,
the ModelDef has the following instance variables:

- mining model type - describes the sort of model that is represented
  by this ModelDef. Possible values include:

  -  BASELINEMODEL
  -  ASSOCIATIONMODEL
  -  CLUSTERINGMODEL
  -  GENERALREGRESSIONMODEL
  -  MININGMODEL
  -  NAIVEBAYESMODEL
  -  NEARESTNEIGHBORMODEL
  -  NEURALNETWORK
  -  REGRESSIONMODEL
  -  RULESETMODEL
  -  SEQUENCEMODEL
  -  SCORECARD
  -  SUPPORTVECTORMACHINEMODEL
  -  TEXTMODEL
  -  TIMESERIESMODEL
  -  TREEMODEL
  -  SCALA
  -  JAVA
  -  BINARY
  -  PYTHON
  -  UNKNOWN

  For the most part, the types are descriptive only
  and reflect the mining model types specified
  by the Data Mining Group for the PMML specification.
  However, the SCALA, BINARY, and PYTHON values are also possible.
  These are used to direct the metadata API service behavior
  when ingesting a new or changed model of those types.
  The model type for JAVA, SCALA, BINARY, and PYTHON models
  are specified in the ingestion command.
  The PMML type models (both Kamanja and JPMML)
  determine the precise model type based upon
  the PMML source code supplied at ingestion time.

- model representation - description of how the model
  is internally represented in the metadata.
  The possible values are currently {JAR, JPMML, PYTHON}.
  All the JVM-targeted languages,
  including Java, Scala, and Kamanja PMML (compiled to Scala)
  are marked as JAR elements.
  Models interpreted by the JPMML evaluator are marked as JPMML.
  The Python models are marked as PYTHON (not currently supported).
- input variables - array of elements that describe
  the inputs that a given model consumes.
  The input's name and type are specified.
  The principal input is the incoming message.
  However, depending upon the model's complexity,
  it might also consume ancillary dimensional information
  for filtering, validation, and so on.
  Models may also consume outputs from other models,
  common for ensemble models. These input variables are used in two ways:

  - When an input message or container has changed,
    the model is recompiled, if possible,
    to validate the model is still functional and
    can adapt itself to the new input declaration.
  - When a model consumes a message, container, or model field
    computed from the same input message as it consumes,
    the execution of the model is said to be
    dependent on the prior model's computed output
    and its execution is put in abeyance until the computation is complete.
    This information is used to form
    the execution-directed acyclic graph (DAG) for the incoming input message.

- output variables - array of elements
  that describe the outputs produced by the model.
  The output's name and type are specified.
  Outputs of a model can be consumed by other models
  (as described previously in the input variables item).
  Its principal purpose is to aid
  in the formation of the execution graph by the engine.

- is reusable - models so marked are more efficiently managed
  by the engine execution.
  The Kamanja engine caches the model instances of these reusable models,
  avoiding the creation and destruction
  of the model for each incoming message that a given model consumes.
  Depending upon the complexity of the model
  and what it uses to interpret the incoming message,
  this can offer a significant performance advantage
  over the standard create-execute-destroy behavior.

  The JPMML and Kamanja PMML models are both automatically marked as reusable.
  For the custom models built with Java, Scala, and Python,
  the model can be marked as reusable only if it is idempotent.
  That is, given an input message and the same model start state,
  the model always produces the same result
  whenever that model consumes that input.
  There is no automatic way to vet this.
  For this reason, it must be supplied as an input parameter
  at model ingestion time if and only if it is appropriate to mark it as such.
- message consumed - namespace.name.version.
  A '.'-delimited triple that describes the message
  which the model principally consumes.
  This field is appropriate only for JPMML models.
  Other model types determine this information from their build configuration
  (for example, Java, Scala, Python).

- instance serialization support -
  when true, model instances described by the ModelDef
  are serialized and cached at model ingestion time.
  The engine resurrects the cached instance
  instead of dynamically making one.
  This mechanism is useful for any model
  that is relatively expensive to initialize.
  This is NOT IMPLEMENTED YET.
  An additional interface(s) may be required
  to describe the serialization mechanism.

Model Interfaces
~~~~~~~~~~~~~~~~

In addition to the ModelDef itself,
the essential information needed to understand how models work,
regardless of representation or type,
are the interfaces that describe the model's contract with the engine.
There are six abstract classes and interfaces. These are introduced here.

- ModelInstance - this class describes the base behavior
  for an instance of a model.
  It has a reference to its factory, the one that created it.
  Other than providing convenient access
  to basic information such as name and version,
  the method of note is the execute method.
  The execute method is called for each message
  known to be of interest to models of this type.
  The message and other runtime information is supplied.
  The model's execute function is obligated
  to produce a ModelResultBase derivative as its result.
  This may be NULL if the model deems
  that the consumed message was not interesting after all.

  ::

    def Execute(mdlCtxt: ModelContext, outputDefault: Boolean): ModelResultBase

- ModelInstanceFactory - this interface describes
  the behavior of the object that can create model instances.
  As the name suggests, this object exhibits the factory pattern.
  There is one of these objects for every ModelDef instance
  described in the cluster's metadata.
  In fact, each ModelInstanceFactory has its ModelDef as an instance variable.
  Like the ModelInstance,
  it gives standard information about the model
  including its name and version,
  provides access to the persistent storage cache
  via its EnvContext instance variable.
  The money protocol, however, are these three functions:

  ::

    def IsValidMessage(msg: MessageContainerBase): Boolean

  IsValidMessage determines if this message instance is consumable
  by the ModelInstances that this factory can produce.
  If the answer is true, the engine calls CreateNewModel
  to obtain an instance that, in fact, can consume it.

  ::

    def CreateNewModel(): ModelBase

  CreateNewModel is called whenever a ModelInstance
  is required to consume the pending message.
  It is only called when the IsValidMessage method has answered true.
  Once returned, the model's Execute method is called to produce a result.

  ::

    def CreateResultObject(): ModelResultBase

  The ModelInstanceFactory also knows how to create
  a standard output for the ModelInstance
  in a form that is consumable by other objects in Kamanja.
  The ModelResultBase declares behavior
  for converting the result produced by an object to JSON or a map.
  It declares a serialize/deserialize protocol and access methods.
  The ModelInstances all currently use
  a standard implementation of this interface,
  but if it were desirable to do so,
  this behavior can be specialized in ModelInstanceFactory.

- ModelFactoryObject - factory that creates ModelinstanceFactory instances.
  While for many model types,
  there is a one:one relationship between
  the factory of a model and the class of a model,
  that is not the case for some model types.
  For example, JPMML all share the same base model,
  an adapter or shim that intercedes in all engine model interaction
  as described in the ModelInstance and ModelInstanceFactory
  for all JPMML models.
  They differ only in the JPMML model evaluator objects
  that such model instances possess.
  To maintain the one:one simplicity,
  the ModelInstanceFactory generates an individual ModelInstanceFactory
  for each of these JPMML models.
- EnvContext - describes an interface for read and write access
  to a persistent cache that may be used to advantage
  to manage message and container (observation) histories
  for use by the model's execution.
  Elements such as lookup tables can be retrieved and marshalled into the model.
  This interface is also used by the engine itself.
  There are numerous usage examples in SampleApplications.
  See examples of its use in the Java Models section and Kamanja PMML section.
- ModelContext - a simple container object
  that is passed to the ModelInstance at execution time.
  It contains references to the current transaction
  and the incoming message to be processed.
  There is one of these objects for each message processed.
- ModelResultBase - describes a standard interface
  describing behavior that all model output should implement,
  including transformation to JSON, serialization and deserialization,
  and map-like access.
  Currently, there is a standard implementation of this interface
  in MappedModelResults.
  Because this trait is produced by ModelInstanceFactory objects,
  alternate implementations are certainly possible.

Types
-----

It is also possible to catalog a library of types to the Kamanja metadata
so that messages, containers, concepts and models can use them.

Concepts
--------

Concepts and derived concepts are global entities
that can be accessed with a key within the Kamanja engine.
There is no real distinction between a concept and derived concept.

Derived Concepts
----------------

The concept and derived concept is a computed field.
For example, the models run and they accept messages.
They filter messages - they determine that this is a message of interest.
The model does computations on the message that, for example,
compare the message with a table;
they then create some sort of an output and they can publish that output.
When a model is defined,
both the inputs that the model needs and the outputs
that the model offers can be described.
That's part of the metadata.
Those outputs then can be referenced directly by other models in the system.

Non-Stop Processing
-------------------

Adding and subtracting content such as models, messages, types, and UDFs
does not require the engine to be stopped
(dynamic metadata-driven component loading).

DSL (Domain-specific Languages)
-------------------------------

A domain-specific language can be used to generate Kamanja PMML models.

Parallelism
-----------

Threads and the use of DAGs to control execution.

EnvContext
----------

Arbitrary data content can be made available to any model
through Kamanja's EnvContext singleton object.
This is designed as a pluggable component
to each instance of the Kamanja engine running.
It is specified in the Kamanja's configuration file,
which is supplied as an argument when starting Kamanja.
Content can both be read as well as written to EnvContext from a model.

Security
--------

Security in Kamanja is done on each operation being processed.
A security adapter is required to implement the methods.
A user can define his/her own permissions for the CRUD operations
on each of the Kamanja metadata objects
(messages, containers, models, functions, types, concepts, or configuration).

Auditing
--------

Many businesses that may want to use Kamanja
are either heavily regulated or are constrained legally in some fashion
(if not both).
Decisions made in a model may need to be revisited
by either the business’ internal auditors
and/or by legal opponents seeking claims against the business.
For this reason, it is only prudent
to provide enough contextual information as is practical
to easily see why a model prediction was made.

Logging
-------

Standard system logging, implemented with Log4J, is available.



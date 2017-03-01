
.. _pmml-guide-add-remove:

Adding, updating, and removing PMML Models
==========================================

PMML models vary from the standard Kamanja model ingestion mechanism
for Scala, Java, and kPMML models.
The namespace.name model, model version,
and namespace.name message must be supplied on the command-line.
Here are a few Kamanja script examples to illustrate the basic commands.
The value of the KAMANJA_HOME variable in the examples
is the Kamanja install location.
The value is the working directory where the model to be ingested is located.

Adding a PMML Model
-------------------

In this example, an add model command is invoked.
The command is followed by several name/value pairs.
Its version must also be supplied.
When treated as a Long integer,
its value must be greater than any other version for models with this name.
The PMML model text is supplied in the pmml value,
namely $KAMANJA_SRCDIR/Pmml/JpmmlAdapter/src/main/resources/metadata/model/Rattle/DecisionTreeAuto.pmml.
Note that the leading 0s are not required,
but used here to illustrate the maximum size that the Long version can take
(when ‘.’ is eliminated).

::

  $KAMANJA_HOME/bin/kamanja
  $KAMANJA_HOME/config/MetadataAPIConfig.properties
  add model pmml <SomePath>/DecisionTreeAuto.pmml
  MODELNAME com.auto.pmml.DecisionTreeAuto
  MODELVERSION 000000.000001.000001
  MESSAGENAME System.AutoMsg TENANTID tenant1

Note: An important detail is the structure
of the value of the MODELNAME portion of the command.
The detail is that this MUST have both a namespace and a model name part,
separated by a period, as in abc.xyz.
In this example, abc is the namespace and xyz is the model name.
These should be meaningful text,
as they will identify the model in any list of active models;
although they can be ANY text,
although it would be wise NOT to include spaces.

Note: Kamanja has basic multi-tenancy (new in v1.4).
Therefore, every time a container, message, or model is added or updated,
specify the tenant ID.
Multi-tenancy allows deployment of more than one use case to a cluster.
Multiple use cases can exist on the same cluster.

Updating a PMML Model
---------------------

The update model command updates the model added in the prior example.
It deletes the existing latest version of the model
and then effectively adds the new version supplied on the command-line.
Notice the newVersion value abides by the rule
that any new version to be added when other versions of the same model exist
must be greater than the latest version available in the metadata.
Presumably the new source file is different from that it is replacing.

::

  $KAMANJA_HOME/config/MetadataAPIConfig.properties
  update model pmml <SomePath>/DecisionTreeAuto.pmml
  MODELNAME com.auto.pmml.DecisionTreeAuto
  MODELVERSION 0.1.2 TENANTID tenant1

Note that, with the current implementation,
it is not possible to replace any version of a given model.
Only the latest version can be replaced.

Removing a PMML Model
---------------------

The standard remove model command used for the other model types
is also used for PMML. Note the form of the version.
A segmented version is not used. A Long constant is used instead.

::

  $KAMANJA_HOME/bin/kamanja
  $KAMANJA_HOME/config/MetadataAPIConfig.properties
  remove model com.auto.pmml.DecisionTreeAuto.000000000001000002

A Note on Input Message Configuration
-------------------------------------

Kamanja is a cluster-based platform.
Whereever possible and desirable,
work is broken up and distributed
to one (or more) of the nodes in the cluster for execution,
permitting Kamanja to increase throughput
via parallelism endemic to the cluster architecture.
What this means for the PMML model author
is that the messages developed for the datasets
(that is, the input DataField values) need to have a partition key.
This key may not necessarily be needed by the model computation itself,
but one is needed to cause the engine planner
to distribute the work about the cluster
to balance the work load/achieve parallelism.

For this reason, it is suggested that an additional field be added
to the dataset (if appropriate)
that contains a unique ordinal value
relative to the others being received in the same relative time frame.
This value could be the timestamp, an integer counter.
Obviously, if there is some field being used in the model
that would serve this purpose, by all means use that.



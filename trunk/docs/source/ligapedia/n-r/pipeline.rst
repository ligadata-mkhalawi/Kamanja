
.. _pipeline-term:

Pipeline
--------

A pipeline is a set of of modular components
that are executed in sequence to perform an analytical task.
The execution sequence is controlled by a :ref:`DAG<dag-term>`
A typical pipeline contains the following elements:

- Data is :ref:`ingested<ingest-data-model>` to the Kamanja environment
  using either a :ref:`smart file input adapter<smart-input-adapter-term>`
  or the :ref:`kvinit-command-ref` command.
  The new information is written to a :ref:`Kafka<kafka-term>` partition.
- Each record in the new information is written to an
  input :ref:`message<messages-term>`
  that defines a name and type for each field/column.
- The information is processed through an interative set
  of messages, :ref:`containers<container-term>`,
  and :ref:`models<model-term>`:

  - A model takes the data from one or more input messages,
    does some calculations on it, and feeds the results
    into an output message.
  - The output message from one model can be the input message
    to the next model in the pipeline.
  - Reference data that the model needs is read in from
    containers.
  - Most pipelines contain a series of models,
    each of which does a descrete calculation;
    the output from any stage can be persisted
    to a data store from which other processes in the cluster
    can access the information.

-  The output message from the final model in the pipeline
   can be fed to an output adapter to be passed to software
   that displays the information.





.. _simp_scala_models-compile:

Write model compilation configuration
-------------------------------------

Place the model code in the SimpleMathDAG directory
where  the message definitions are located.
Add those models to the metadata.
Not just yet though.
While all the messages and models have been written to execute good logic,
Kamanja doesn’t know how to associate any of the messages
with any of the models.
In order to do that, create some
model compilation configuration files (:ref:`modelcfg-config-ref`).

Here are the contents of DAGAdditionModelCfg.json:

::

  "DAGAddition": {
  "Dependencies": [],
  "MessageAndContainers": [],
  "InputTypesSets": [
  [
    "com.ligadata.test.dag.NumberMessage"
  ]
  ],
  "OutputTypes": [
    "com.ligadata.test.dag.AddedMessage"
  ]
  }
  }

Here is an explanation. The header is DAGAddition.
This is the name of the model configuration.
It is named the same as the model to associate with it.

Dependencies are any JAR files that the model may be dependent on.
With this information,
Kamanja can ensure that those dependencies are included
in the classpath when executing the model.

InputTypesSets is an array of array of messages.
For purposes of this example,
it’s a list of messages that must be presented to the model in order to execute.
In this case, DAGAddition tells the model
that it must look for the com.ligadata.test.dag.NumberMessage classname
(message) and include it as an import statement
(which is generated automatically when adding the model to Kamanja).

OutputTypes is similar to InputTypesSets,
only it tells the model which messages should be produced by the model
(or more specifically, which messages the model should expect to produce,
whether it does or not).

In this case, the expected input message is defined
as com.ligadata.test.dag.NumberMessage
and the expected output message as com.ligadata.test.dag.AddedMessage.

Copy the rest of the following model compilation configurations
into their own files and place them in the SimpleMathDAG directory
created earlier.

DAGMultiplicationModelCfg.json
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

::

  {
  "DAGMultiplication": {
  "Dependencies": [],
  "MessageAndContainers": [],
  "InputTypesSets": [
  [
    "com.ligadata.test.dag.AddedMessage"
  ]
  ],
  "OutputTypes": [
    "com.ligadata.test.dag.MultipliedMessage"
  ]
  }
  }


DAGDivisionModelCfg.json
~~~~~~~~~~~~~~~~~~~~~~~~

::

  {
  "DAGDivision": {
  "Dependencies": [],
  "MessageAndContainers": [],
  "InputTypesSets": [
  [
    "com.ligadata.test.dag.MultipliedMessage"
  ]
  ],
  "OutputTypes": [
    "com.ligadata.test.dag.DividedMessage"
  ]
  }
  }


  

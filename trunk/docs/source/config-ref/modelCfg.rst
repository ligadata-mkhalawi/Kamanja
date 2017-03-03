

.. _modelcfg-config-ref:

modelCfg.json
=============

modelCfg.json files tell Kamanja how to compile your models.
You must create one of these files for each model in your pipeline.
The name of the file is very specific;
it must be the root name of your model followed by "Cfg.json".
For example, if the name of your model is *CountNoses.scala*,
the name of your model compilation file must be
*CountNosesCfg.json*.

File structure
--------------

<model-name>Cfg.json file

::

  "<model-name>": {
      "Dependencies": [],
      "MessageAndContainers": [],
      "InputTypesSets": [
      [
            "<array-of-input-messages>"
      ]
      ],
      "OutputTypes": [
            "<array-of-output-messages>"
      ]
      }
  }

Parameters
----------

- **<model-name>** - file header; it must exactly match
  the root name of the model; for example, CountNoses.
- **Dependencies** - specify and JAR files on which the model is dependent.
  Kamanja uses this information to ensure that those JAR files
  are included in the classpath when executing the model.
  If the model has no dependencies, leave this field blank.
- **MessageAndContainers** -
- **InputTypesSets** - an array of messages.
  This is a list of messages (classnames)
  that must be presented to the model in order to execute.
- **OutputTypes** - tells the model which messages should be produced
  by the model (or more specifically,
  which messages the model should expect to produce, whether it does or not).

Usage
-----

After you create the *modelCfg.json* files,
you can compile and upload them
using the :ref:`kamanja-command-ref` command:

::

  cd /opt/Kamanja-1.4.0_2.11
  bash bin/kamanja upload compile config <path-to-modelCfg.json-file>
  bash bin/kamanja upload compile config  <path-to-modelCfg.json-file>
  bash bin/kamanja upload compile config  <path-to-modelCfg.json-file>




Examples
--------

Example 1
~~~~~~~~~

The first example tells the model that

 - it must look for
   the *com.ligadata.test.dag.NumberMessage* classname (message)
   and include it as an import statement
   (which is generated automatically when adding the model to Kamanja).

  - it must produce the *com.ligadata.test.dag.AddedMessage* message.

::

  "MyAddition": {
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

Example 2
~~~~~~~~~

::

  {
  "MyMultiplication": {
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

See also
--------

- :ref:`simp_scala_models-compile` discusses how to compile
  Scala models





.. _model-term:

Model
-----

Models are programming constructs written in
Python, R, Java, Scala, PMML, etc. to control the input ingestion,
make decisions, and produce output for different use-cases.
Multiple models can be chained into a pipeline,
configured and deployed on a Kamanja cluster.
Models in the pipeline pass data using :ref:`messages<messages-term>`;;
the output message of one model becomes an input message for the next model
in the pipeline.

Kamanja is compatible with many modeling language standards
and can consume many different types of data.

The Kamanja platform enables you to create, run,
and continuously enhance multiple models
that are applied to each new data event that enters the decision layer:

- Models can be developed using a variety of languages, libraries, and IDE’s.
  You can use Kamanja as the platform to develop a model
  or you can import a trained model to Kamanja from some other platform.

- Models can range from simple rules-based decision trees written in Java
  to sophisticated non-linear classifiers
  and neural networks implemented in Python, R, Java, or Scala.

- Models can continuously leverage the most recent
  and all past data to make arbitrarily complex decisions at any given moment.

- By adding nodes, Kamanja scales to meet
  virtually any volume of data or number and complexity of models.

- The output of one model run can be exported
  and then used as input to another model run;
  this means that applications can be modularized
  into a number of discrete models
  that run in a pre-defined order as a pipeline.

- Models created and trained on other platforms
  can be run as PMML models on the Kamanja platform.

.. :note::   in the Machine Learning and Data Mining worlds,
    "model" usually refers to the code that runs the ML algorithms
    which may also ingest the data, preprocess the data
    and do some feature engineering on it,
    and perhaps produce some sort of visualization 
    of the output of running the model.
    Kamanja runs this sort of model
    but also expands the "model" term to refer to
    executable modules written in Java or Python.

For more information:

- Use the :ref:`kamanja-command-ref` command
  to add, remove, update, get, activate, and deactivate a model.
- See :ref:`java-scala-guide-java` for information
  about creating a Java model.


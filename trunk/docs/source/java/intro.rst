
.. _java-scala-guide-intro:

Introduction to Java and Scala native models
============================================

Native models are models that are written directly
in the Java or Scala programming languages.
Using these models can significantly simplify the model design,
because of the difficulties of using PMML.
However, using native models adds a few extra steps
that are required because Java or Scala code must handle dependencies
that cannot be easily defined in a native source code.

In addition to PMML, Kamanja allows writing of rules
using the Java and Scala programming languages
and the Kamanja metadata API supports generating
appropriate messages and containers
as well as deploying correct model objects for execution.

An execution environment for a Kamanja model on the Kamanja server
requires JAR files, containing all the execution logic,
to be loaded into a metadata storage.
This can be accomplished by using the metadata API services
provided with the Kamanja distribution.
The metadata API takes JSON-formatted files and creates JAR files.

Here is a demonstration of this process by using a simple example.
Assume a developer needs to create, test, and deploy a rule
for the Kamanja engine.
The engine is running at a financial institution
that processes a flow of customer transactions
and determines if a customer that initiated a transaction needs
to be notified if an account becomes too low as the result of this transaction.
What is too low is determined and set up by a customer,
and the decision to send out a low balance alert
is made on a per customer basis.

The pseudocode for such a rule would be:

::

  IF account balance < $100 (configurable)
  AND an alert hasn't been issued in 48 hours (configurable)
  AND this customer wants to be alerted, THEN send out an alert


How can this rule be implemented, tested, and deployed
using either the Java or Scala programming language?
The following sections provide information
such as instructions on generating the code
that works with models and tips on how to create a Scala or Java model.

These sections describe how to carry out the following instructions:

- If using Kafka, create the topics to hold the messages and containers.
  Add the appropriate JSON containers to the engine
  to generate source code and JARs.
- Add the appropriate JSON message to the engine to generate source code and JARs.
- Create the Java or Scala model using the tips provided.
- Upload the model configuration file to the engine.
- Add the model to the engine.
- Finish the compile-time activity.
- Complete the run-time activity.

This section provides the JSON files and models created
according to the above rule.
In real life, the developer has to create his/her own JSON files and models.
The developer does not have to create the JAR files.
Those are generated when the JSON files are added to the engine.

Note: The assumption is that all the necessary Kamanja components
have been installed on the system. See here for those components.


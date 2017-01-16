
.. _intro-simple-scala-tut:

Introduction
============

This tutorial illustrates how to create
a :ref:`model<model-term>` ensemble
that leverages :ref:`DAG<dag-term>` to control the order of execution.
The characteristics of this example are:

This application is a collection of three models,
written in Scala and executed sequentially.

- The first model performs a mathematical operation
  and writes the results to an output message.
- The second model takes that message as an input message
  and performs another mathematical operation on that content,
  then creates an output message to pass its results to the third model.
- The third model takes that message as input and performs
  another mathmatical operation.


Before performing any commands on this page,
install and set up a Kamanja environment
following the instructions in the Quick Start Guide.

To download the files used in this tutorial,
click on https://s3.amazonaws.com/kamanja/samples/1.4.0/SimpleMathDAG.zip.

Before actually creating any of the pieces to the ensemble,
create a directory (in any directory)
called SimpleMathDAG to store everything to be written.


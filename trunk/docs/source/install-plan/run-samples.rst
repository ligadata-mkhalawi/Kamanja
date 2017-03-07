
.. _run-samples-install:

Run Samples
===========

The following samples are provided:

- **HelloWorld** – demonstrates the basic functionality of Kamanja.
- **Medical** – simulates a diagnostic tool
  to discover those patients who are at risk for COPD.
- **Telecom** – discovers when a cell phone has been used
  beyond the number of minutes that are allowed each month.
- **Finance** – discovers when an account balance is too low.
- **Building a Simple Model Ensemble** – builds a simple model ensemble.
  To download the files that are used in this tutorial,
  click on https://s3.amazonaws.com/kamanja/samples/1.4.0/SimpleMathDAG.zip.

Much information is necessary to make Kamanja work
and for it to do anything useful.
The system needs to know what types of messages there are,
where things are (IP addresses, queues, and much more),
and what type of processing to do.
This is called metadata and it is loaded through the metadata API.


Run the HelloWorld sample
-------------------------

HelloWorld is a basic demonstration of how Kamanja works.
The Kamanja engine takes data from the input queue,
processes that data using a model,
and outputs data that follows the criteria in the model to the output queue.


Medical sample
--------------

The medical use case uses the Kamanja engine
to discover which patients are at risk for COPD.


Telecom sample
--------------

The telecom use case uses the Kamanja engine
to generate an alert when the specified threshold
for the number of minutes talking on the cell phone has been exceeded.


Finance sample
--------------

The finance sample uses the Kamanja engine to generate
an alert when a bank balance gets below a certain threshold
so a penalty is not incurred.


Simple model ensemble
---------------------



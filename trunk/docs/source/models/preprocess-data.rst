
.. _preprocess-data:

Data Transformation and Enrichment (Pre-processing)
---------------------------------------------------

After the data is ingested to the system,
it may require some transformation and enrichment
(what is often called ETL for Extract, Transform, and Load
in the database world) before it can be fed into your model.
The pre-processing code runs after the data is ingested
but before any calculations or algorithms are executed.
By incorporating pre-processing models into the pipeline,
you ensure that new data streaming into Kamanja
is valid and structured appropriately for the models that do the analysis.

This includes tasks such as the following:

- Extract just the columns/fields you need from each data set
- Correct or discard records with missing or invalid information
- Correlate the data between all the data stores you are using.
  Data that is correlated between different data sources
  must be in the same format.

  - For example, one data source might represent
    the first three months of the year as January, February, March
    while another uses Jan, Feb, Mar
    and yet another uses 01, 02, 03;
    days of the week have similar discrepancies.
  - As another example, phone numbers can be represented
    as (xxx)xxx-xxxx or xxx.xxx.xxxx or just xxxxxxxxxx.

- Bin the data to group granular information into larger groups.

  - For example, you data source may store
    the exact age of each person listed
    but you just want to break down the ages by
    0-18 years; 18-50 years; 50-70 years; and over 70.
  - You could do categorical binning.
    For example, your data source lists each fruit sold
    and you want to bin the results into citrus fruits,
    stone fruits, berries, and so forth.

- Aggregate data into summary form.

  - For example, you might have a record for every purchase made
    in a retail outlet;
    you could aggregate this data to
    show the total number and value of purchases
    in each department or each branch.

The Wikipedia `Extract, Transform, load
<https://en.wikipedia.org/wiki/Extract,_transform,_load>`_ article
discusses additional data preparation tasks that might be required.


How it works
------------

- The data flows into each model in the pipeline as an input message,
  then the model creates an output message
  that is fed to the next model in the pipeline as an input stream.
- The model that implements the final preprocessing step
  then creates an output message that is fed to the first model
  that analyzes the data.
- You can create the preprocessing models using any of the
  languages supported on Kamanja
  but :ref:`JTM<jtm-term>` (JSON Transformation Model) is particularly useful
  when working with Java and Scala;
  it converts all incoming messages to class member variables
  and starts manipulating them, then pushes out the clean structure and format.
- R and the Python libraries for machine learning
  contain a rich set of functions for preprocessing data.

To pre-process raw data that has been ingested:

- Create models to do the preprocessing
  after the Kafka message is ingested into Kamanja
  but before passing the data to the models that analyze the data.
- Create a separate model for each pre-processing step that is used;
  for example, you might have one model that
  extracts the fields/column required by the application;
  then another model that cleans NA (missing) data;
  then another model that transforms those
  January, February, March fields/columns to 01, 02, 03; and so forth.
- Create appropriate input and output messages for each stage of preprocessing.

You can instead pre-process the data outside of Kamanja,
before it is ingested into the cluster,
either by writing code in the language of your choice
(such as Java, R, Python, C, C++) or using an ETL suite.

To use the preprocessed data in Kamanja:

- The preprocessed data is fed into the system as a Kafka message.
- An input adapter reads and writes the messages
  (that is, record structures).

- The :ref:`FileDataConsumer<filedataconsumer-command-ref>` tool
  populates Kafka message queues from the files
  based on information in its configuration file.

- The data associated with any message that has the "persist" attribute
  is also written to the cluster's data warehouse,
  where it is available to all models executing on the cluster
  that are authorized to access that data.



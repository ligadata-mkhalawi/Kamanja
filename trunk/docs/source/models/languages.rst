
.. _lang-model:

Analytical languages supported on Kamanja
=========================================

Kamanja can consume models that are written in virtually any language.
Users can select the language they prefer
or that has special capabilities they need,
depending on their preferences and the capabilities of each language.
Models written in different languages can coexist
and work together in a single application.

A quick summary of the modeling languages currently supported by Kamanja:

- Kamanja runs models that are written in native Java or Scala.
- Kamanja can run Python servers on which Python models can be trained and run.
- Kamanja also runs PMML models.
- Models written in many other languages can be transformed into PMML and then consumed in Kamanja.

A single Kamanja application can use models written in different languages
so you can choose the best language
for each calculation your application requires.
For example, you might use JTM for the "pre-processing" model(s)
because it is simple and straightforward,
then use a more powerful language for the analytical models
that require more sophisticated analytical capabilities. 

The following table gives more details about the supported modeling language,
with links to guide material for each:

.. list-table::
   :widths: 20 50 30
   :header-rows: 1

   * - Language
     - Notes
     - Guide
   * - :ref:`Python<python-term>`
     - Python is a general purpose computer language
       that supports a rich set of statistical libraries
       for rapid prototyping and all sorts of machine learning.
       Kamanja builds a parallel network of Python servers
       where the Kamanja cluster nodes interact
       with IP communication mechanisms.
     - :ref:`python-guide-top`
   * - :ref:`Java<java-term>`
     - Java is a popular general-purpose programming language
       that works well for simple rules-based models
       and is especially useful for data preprocessing.
     - :ref:`java-guide-top`
   * - :ref:`PMML<pmml-term>`
     - PMML (Predictive Model Markup Language) is an XML-based language
       for specifying data mining model types.
       On Kamanja, all models that are saved or passed to another model
       are stored as PMML, no matter the language originally used
       to create the model.  PMML models execute as Java.
     - :ref:`pmml-guide-top`
   * - Scala
     - Scala is a general-purpose programming language
       that runs on a JVM and has complete language interoperability with Java.
       Models written in Scala are multi-threaded
       and so give better performance than the other options.
       Scala includes a number of other features
       that make up for deficiencies in Java.
       Like Java, it is most appropriate for simple, rule-based models.
     - :ref:`scala-models-top`
   * - JTM
     - JTM (JSON Transformation Model) is useful
       for simple data transformation and enrichment models.
       JTM executes as native Scala code.
     - :ref:`jtm-guide-top`
   * - R
     - R is a language customized for statistical programming
       and machine learning.  RStudio is the most popular IDE for R;
       a number of libraries and tools are available through CRAN.
       You can train a model using R outside the Kamanja platform
       and write the results to PMML, then run the PMML model on Kamanja.
       Rattle is a GUI that can be used
       to generate R code by selecting
       the tasks to be performed on the Rattle console.
     -

Note that Kamanja cannot currently produce models
written in R, Spark/mllib, and similar languages
but trained models written in these languages can be run on Kamanja as PMML.
To take best advantage of the Kamanja platform,
you should modularize your models,
putting the code that prepares and pre-processes the data
in models that are separate from the code
that executes the actual algorithms; this is discussed more below.

For a list of machine learning models and libraries that have been validated on the Kamanja platform, see http://kamanja.org/wiki/model-validation-process/ .

For instructions about setting up a development environment on Kamanja,
see :ref:`develop-install-top`.


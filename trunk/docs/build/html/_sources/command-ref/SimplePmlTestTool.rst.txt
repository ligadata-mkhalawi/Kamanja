
.. _simplepmltesttool-command-ref:

SimplePmlTestTool
=================

The **SimplePmlTestTool** can be used to quickly test
a generated PMML model from the model development environment.


This tool does not use Kamanja, but allows the user to quickly test his/her generated PMML model from his/her model development environment.


Syntax
------

::

  java -jar SimplePmlTestTool -dataset <path> [-omitInputs] [-output <path>]
      -pmmlSrc <path> [-separator:<char>] [-version]

Options and arguments
---------------------

- **-dataset <path>** -- Specify the full pathname of the CSV dataset file.
  For example:

  ::

    –dataset /home/john/omml/irisraw.csv

- **-omitInputs** -- Only emit the output fields specified in the PMML;
  if this flat is not supplied, both the input and output fields
  in the PMML are emitted.

- **-output <path>** -- Specify a file to which the output is written.
  For example:

  ::

    –output /home/john/pmml/results.csv

  By default, output is written to *stdout*.

- **-pmmlSrc** -- Path to thePMML file being tested.  For example:

  ::

    –pmmlSrc /home/john/pmml/KNIME/DecisionTreeEnsembleIris.pmml

- **-separator** -- Specify the delimiter character to use;
  by default, comma is the delimiter character.
  For example, to set the semi-colon as the delimiter character:

  ::

    -separator ;

- **-version** -- Print version of the command and exit.

Usage
-----

PMML source providers (such as R/Rattle, KNIME, RapidMiner, SAS)
and consumers (KNIME, Zementis’ Adapa).
See :ref:`pmml-guide-produce` for a complete list of supported producers.

Examples
--------

To run any of these examples, **cd** to $INSTALL_DIR:

::

  cd /path/to/Kamanja/install/dir

The following example runs the DecisionTreeEnsembleIris model
using the iris.raw.csv dataset
and prints out the input and output to the console:

::

  java -jar <InstallDir>/bin/PmmlTestTool-1.0
  –pmmlSrc /home/john/pmml/KNIME/DecisionTreeEnsembleIris.pmml
  –dataset /home/john/pmml/iris.raw.csv

The next example prints out ONLY the output fields to the console:

::

  java -jar <InstallDir>/bin/PmmlTestTool-1.0
  –pmmlSrc /home/john/pmml/KNIME/DecisionTreeEnsembleIris.pmml
  –dataset /home/john/pmml/iris.raw.csv
  –omitInputs

To output the results to a particular file, run:

::

  java -jar <InstallDir>/bin/PmmlTestTool-1.0
  –pmmlSrc /home/john/pmml/KNIME/DecisionTreeEnsembleIris.pmml
  –dataset /home/john/pmml/iris.raw.csv
  –output /home/john/pmml/results.csv

Finally, if the input data has a delimiter that is not a comma
(a ‘:’ for example), run:

::

  java -jar <InstallDir>/bin/PmmlTestTool-1.0
  –pmmlSrc /home/john/pmml/KNIME/DecisionTreeEnsembleIris.pmml
  –dataset /home/john/pmml/iris.raw.csv
  –separator :

See also
--------

:ref:`Introduction to PMML<pmml-guide-top>`



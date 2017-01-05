


.. _jsonchecker-command-ref:

JsonChecker.sh
==============

Syntax
------

::

  bash $KAMANJA_HOME/bin/JsonChecker.sh --inputfile $KAMANJA_HOME/config/ClusterConfig.json

Options and arguments
---------------------

- **inputfile** is the path of the JSON file to check.

Usage
-----

Output
------

1. Suppose that the command was run without the --inputfile option:

   ::

     bash $KAMANJA_HOME/bin/JsonChecker.sh 

   The error message is:

   ::

     ERROR [main] - Please pass the input file after –inputfile optionWARN [main] – Usage: bash $KAMANJA_HOME/bin/JsonChecker.sh –inputfile $KAMANJA_HOME/config/ClusterConfig.json

2. Suppose that the command was run without providing the file after the --inputfile option:

   ::

     bash $KAMANJA_HOME/bin/JsonChecker.sh --inputfile

   The error message is:

   ::

     ERROR [main] - Unkown option –inputfileWARN [main] – Usage: bash $KAMANJA_HOME/bin/JsonChecker.sh –inputfile $KAMANJA_HOME/config/ClusterConfig.json

3. Suppose that the command was run but the incorrect path or incorrect file
   was provided:

   ::

     bash $KAMANJA_HOME/bin/JsonChecker.sh --inputfile $KAMANJA_HOME/bin/ClusterConfig.json

   The error message is:

   ::

     ERROR [main] – The file /opt/Kamanja/bin/ClusterConfig.json does not exist.       WARN [main] – Usage: bash $KAMANJA_HOME/bin/JsonChecker.sh –inputfile $KAMANJA_HOME/config/ClusterConfig.json

4. Suppose that the command was run but a file was provided without data:

   :: bash $KAMANJA_HOME/bin/JsonChecker.sh --inputfile test.json

   The error message is:

   ::

     ERROR [main] – The does not include data. Check your file please.

5. Suppose that the command was run and everything is ok:

   ::

     bash $KAMANJA_HOME/bin/JsonChecker.sh
       --inputfile $KAMANJA_HOME/config/ClusterConfig.json

   No error is raised and the following message is seen:

   ::

     WARN [main] – Json file parsed successfully

6. Suppose that the command was run and there is the wrong JSON format
   inside the file:

   ::

     bash $KAMANJA_HOME/bin/JsonChecker.sh --inputfile $KAMANJA_HOME/config/ClusterConfig.json

   The error message is:

::

  ERROR [main] – There is an error in the format of fileErrorMsg: “here you will see the error with stack trace”



See also
--------



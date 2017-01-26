
.. simp_scala_models-compile:

Add model compilation configuration and models
=================================================

It’s time to add the compilation configuration along with the models.
To add all the configurations, run the following:

::

  cd <KamanjaInstallDirectory>

For example:

::

  cd /opt/Kamanja-1.4.0_2.11

  bash bin/kamanja upload compile config <path-to :ref:`modelcfg-config-ref` file>

  bash bin/kamanja upload compile config  <path-to :ref:`modelcfg-config-ref` file>

  bash bin/kamanja upload compile config  <path-to :ref:`modelcfg-config-ref` file>

Now that the compilation configurations have been properly uploaded, add the models.

::

  bash bin/kamanja add model scala /path/to/SimpleMathDAG/DAGAddition.scala DEPENDSON DAGAddition TENANTID tenant1

  bash bin/kamanja add model scala /path/to/SimpleMathDAG/DAGMultiplication.scala DEPENDSON DAGMultiplication TENANTID tenant1

  bash bin/kamanja add model scala /path/to/SimpleMathDAG/DAGDivision.scala DEPENDSON DAGDivision TENANTID tenant1


Notice the DEPENDSON clause in the commands when adding models.
This is required to tell Kamanja which model configuration
should be used in the compilation of the model.





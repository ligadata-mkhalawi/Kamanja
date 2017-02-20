
.. _pkgs-prereqs-install:

Install Kamanja Prerequisites
=============================

Kamanja requires some third-party software
that must be installed before Kamanja is installed.

- If you are installing Kamanja on newly-installed servers,
  you must install these packages.
- If you are installing on a Hadoop stack or existing servers,
  you may already have some of these packages installed;
  be sure that you are running a compatible version of each package;
  see :ref:`component-versions` for a list of supported versions.
- After you install the packages,
  :ref:`define environment variables<env-variables-install>`
  that identify the installation path for each package.

.. _java-install:

Install Java JDK 1.8
--------------------

Download the latest Oracle Java binaries from
`Oracle Java download site
<http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html>`_.
We do not recommend running Flare on top of OpenJDK.

We recommend choosing the most recent release.
Java 1.8 is necessary to support the password encryption feature;

.. _scala-install:

Install Scala
-------------

Download and install :ref:`Scala<scala-term>` 2.11 from the
`Scala 2.11 download site <http://www.scala-lang.org/download/2.11.7.html>`-.

Kamanja can instead use Scala 2.10, which can be downloaded from
`the Scala 2.10 download site <http://www.scala-lang.org/download/2.10.2.html>`-.

.. _zookeeper-install:

Install ZooKeeper
-----------------

Download and install :ref:`ZooKeeper<zookeeper-term>` 3.4.6 from the
`Zookeeper download page <http://www.apache.org/dyn/closer.cgi/zookeeper/>`_.

After adding $ZOOKEEPER_HOME to your .bashrc or .bash_profile file,
issue the following commands to complete the ZooKeeper installation:

::

  cd $ZOOKEEPER_HOME/conf
  cp zoo_sample.cfg zoo.cfg

.. _kafka-install:

Install Kafka
-------------

Download :ref:`Kafka<kafka-term>` 0.10 from the
`Kafka download site <http://kafka.apache.org/downloads.html>`_.
Note that separate files are provided to run with Scala 2.11 or 2.10;
be sure to get the version that matches the version of Scala you installed.

Note that Kamanja 1.6.x has not been tested with Kafka 0.10.* .

Untar the .tgz file to install the software.  NEED TO CHECK THIS!!!

.. _env-variables-install:

Define environment variables
----------------------------

Before proceeding, you must define the environment variables
for the root directory of each add-on software component.
Do this by editing the *.bashrc* file (Linux) or the *.bash_profile* (Mac):

- Issue the **sudo -v command** to confirm that you have admin privileges.
  If you get a Password prompt and it accepts your password,
  you can edit the file.
- Issue the **sudo vim ~/.bash_profile** (Mac) command
  or **sudo vim ~/.bashrc** (Linux) command to edit the file.

  Alternately, you can make yourself (instead of root)
  the owner of the file by issuing the
  **sudo chown user_name ~/.bash_profile** (Mac)
  or **sudo chown user_name ~/.bashrc** (Linux) command.
- Add the appropriate lines to the *.bash_profile* or *.bashrc* file.
- Run the ** source ~/bash_profile** (Mac)
  or **source ~/.bashrc** (Linux) command.
- Verify that the environment variables have been set
  by issuing commands such as echo $SCALA_HOME.

Use the following lines to define the root path for each component
and then export that information into $PATH:

::

  export JAVA_HOME=<Java-install-path>
  export PATH=$JAVA_HOME/bin:$PATH

  export SCALA_HOME=<Scala-install-path>
  export PATH=$SCALA_HOME/bin:$PATH

  export ZOOKEEPER_HOME=<ZooKeeper-install-path>
  export PATH=$ZOOKEEPER_HOME/bin:$PATH

  export KAFKA_HOME=<Kafka-install-path>
  export PATH=$KAFKA_HOME/bin:$PATH

 

For example:

::

  export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_72.jdk/Contents/Home
  export PATH=$JAVA_HOME/bin:$PATH

  export SCALA_HOME=/usr/local/scala-2.11.7
  export PATH=$SCALA_HOME/bin:$PATH

  export ZOOKEEPER_HOME=/usr/local/zookeeper-3.4.6
  export PATH=$ZOOKEEPER_HOME/bin:$PATH

  export KAFKA_HOME=/usr/local/kafka_2.11-0.9.0.0
  export PATH=$KAFKA_HOME/bin:$PATH

.. _kafka-install:

Download and install Kafka
--------------------------

Download Kafka 0.9.0.1 from `the Kafka download page
<http://kafka.apache.org/downloads.html>`_.
Note that separate files are provided to run with Scala 2.11 or 2.10;
be sure to get the version that matches the version of Scala you installed.

Note that Kamanja has not been tested with Kafka 0.10.* .

Untar the .tgz file to install the software.

.. _elastic-install:

Download and install ElasticSearch
----------------------------------

Install ElasticSearch 2.4.3 from the
`Elastic download page
<https://www.elastic.co/blog/elasticsearch-2-4-3-released website>`_.


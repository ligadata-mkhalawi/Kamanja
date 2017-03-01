
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

- Download the latest Oracle Java 1.8 binaries from
  `Oracle Java download site
  <http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html>`_.
  We do not recommend running Kamanja and Flare on top of OpenJDK.

- Set the `alternatives <https://linux.die.net/man/8/alternatives>`_
  for the Java commands to ensure
  that Kamanja is pointing to the correct version of each command.
  You must run these commmands on each node in the cluster:

  ::

    alternatives --install /usr/bin/java java $JAVA_HOME/bin/java 2
    alternatives --install /usr/bin/javac javac $JAVA_HOME/bin/javac 2
    alternatives --install /usr/bin/jps jps $JAVA_HOME/bin/jps 2
    alternatives --install /usr/bin/jar jar $JAVA_HOME/bin/jar 2

    alternatives --config java
    alternatives --config javac
    alternatives --config jar
    alternatives --config jps


To ensure that all the components are installed and set up,
issue the `jps
<http://docs.oracle.com/javase/6/docs/technotes/tools/share/jps.html>`_
command in a separate window:

::

  jps

**jps** is the Java Process Tool,
a built-in Unix command to list Java processes running.

Sample output:

::

  3969 Jps
  3947 QuorumPeerMain
  3964 Kafka

Note: QuorumPeerMain is ZooKeeper.


.. _scala-install:

Install Scala
-------------

Download and install :ref:`Scala<scala-term>` 2.11_7 from the
`Scala 2.11 download site <http://www.scala-lang.org/download/2.11.7.html>`_.

Kamanja can instead use Scala 2.10_4, which can be downloaded from
`the Scala 2.10 download site <http://www.scala-lang.org/download/2.10.2.html>`_.

Note that the Scala version must be specified
when you issue the **ClusterInstallerDriver** command

Note that the Scala version must be specified
when you issue the **ClusterInstallerDriver** command.

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

Download and install Kafka
--------------------------

Download :ref:`Kafka<kafka-term>` 0.10 from the
`Kafka download site <http://kafka.apache.org/downloads.html>`_.
Note that separate files are provided to run with Scala 2.11 or 2.10;
be sure to get the version that matches the version of Scala you installed.

Note that Kamanja 1.6.x has not been tested with Kafka 0.10.* .

Untar the .tgz file to install the software.  NEED TO CHECK THIS!!!

.. _hbase-install:

Download and install HBase (optional)
-------------------------------------

:ref:`HBase<hbase-term>` must be installed if you want to use it
as a storage format on the cluster.

Go to the `Apache Download Mirrors
<http://www.apache.org/dyn/closer.cgi/hbase/>`_ page.

See something such as:

::

  We suggest the following mirror site for your download:
  http://shinyfeather.com/hbase/

Click on the suggested mirror site
or another site listed on the page.

You will see a list of releases;
we suggest downloading the current stable release,
so click on **stable**. An index is shown.
Choose the *bin.tar.gz* file.

After HBase has downloaded, untar the file.


.. _cassandra-install:

Download and install Cassandra (optional)
-----------------------------------------

:ref:`Cassandra<cassandra-term>` must be installed if you want to use it
as a storage format on the cluster.

Go to the `Cassandra dowload page
<http://www.apache.org/dyn/closer.lua/cassandra/3.10/apache-cassandra-3.10-bin.tar.gz>`_.

See something such as:

::

  We suggest the following mirror site for your download:
  http://mirror.cc.columbia.edu/pub/software/apache/cassandra/3.0.10/apache-cassandra-3.0.10-bin.tar.gz
  
Click on the suggested link or choose a different mirror.

After Cassandra has downloaded, untar the file.


.. _elastic-install:

Download and install ElasticSearch (optional)
---------------------------------------------

Install ElasticSearch 2.4.3 from the
`Elastic download page
<https://www.elastic.co/blog/elasticsearch-2-4-3-released website>`_.


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

  export HBASE_HOME=<HBase-install-path>
  export PATH=$HBASE_HOME/bin:$PATH

  export CASSANDRA_HOME=<Cassandra-install-path>
  export PATH=$CASSANDRA_HOME/bin:$PATH

 

For example:

::

  export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_72.jdk/Contents/Home
  export PATH=$JAVA_HOME/bin:$PATH

  export SCALA_HOME=/usr/local/scala-2.11.7
  export PATH=$SCALA_HOME/bin:$PATH

  export ZOOKEEPER_HOME=/usr/local/zookeeper-3.4.6
  export PATH=$ZOOKEEPER_HOME/bin:$PATH

  export KAFKA_HOME=/usr/local/kafka_2.11-0.10.0.0
  export PATH=$KAFKA_HOME/bin:$PATH

  export HBASE_HOME=/usr/local/hbase
  export PATH=$HBASE_HOME/bin:$PATH

  export CASSANDRA_HOME=/usr/local/cassandra
  export PATH=$CASSANDRA_HOME/bin:$PATH


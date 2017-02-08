

.. _cassandra-install:

Install Cassandra
=================

Go to the `Cassandra dowload page
<http://www.apache.org/dyn/closer.lua/cassandra/3.10/apache-cassandra-3.10-bin.tar.gz>`_.

See something such as:

::

  We suggest the following mirror site for your download:
  http://mirror.cc.columbia.edu/pub/software/apache/cassandra/3.0.10/apache-cassandra-3.0.10-bin.tar.gz
  
Click on the suggested link or choose a different mirror.

After Cassandra has downloaded, untar the file.

Now add $CASSANDRA_HOME to $PATH, if it is not already there.
On Linux, this means editing the *.bashrc* file;
on Mac, edit the *.bash_profile* file.
For example:

For example:

::

  export CASSANDRA_HOME=
  export PATH=$CASSANDRA_HOME/bin:$PATH



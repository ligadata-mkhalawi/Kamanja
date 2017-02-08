
.. _hbase-install:

Install HBase
=============

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

Now add $HBASE_HOME to $PATH, if it is not already there.
On Linux, this means editing the *.bashrc* file;
on Mac, edit the *.bash_profile* file.
For example:

::

  export HBASE_HOME=
  export PATH=$HBASE_HOME/bin:$PATH



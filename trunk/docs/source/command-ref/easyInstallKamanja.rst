

.. _easyinstallkamanja-command-ref:

easyInstallKamanja.sh
=====================

Build a Java or Scala model.

Syntax
------

::

  cd /Users/userid/Downloads
  mkdir installKamanja

  cd /Users/userid/Downloads/Kamanja-version_number/trunk/SampleApplication/EasyInstall

  ./easyInstallKamanja.sh <install path> <src tree trunk directory> \
    <ivy directory path for dependencies> <kafka installation path>

Options and arguments
---------------------

Usage
-----

Note that **wget** is required to run **easyInstallKamanja.sh**.
See :ref:`develop-install-top` for more details
about setting up the Kamanja development environment.


Error messages
--------------

- [error] java.util.concurrent.ExecutionException:
  java.lang.OutOfMemoryError: GC overhead limit exceeded error

  Add the following line to the *sbtopts* file in
  the */etc/sbt-launcher-packaging/*:

  ::

    -mem 6144

  You can instead set the $SBT_OPT environment variable.
  


Example
-------

::

  bash /Users/<userid>/Downloads/Kamanja-<version-number>/trunk/SampleApplication/
      EasyInstall/easyInstallKamanja.sh \
      /Users/<userid>/Downloads/installKamanja \
      /Users/<userid>/Downloads/Kamanja-<version-number>/trunk ~/.ivy2 \
      /Users/<userid>/Downloads/kafka_2.11-0.9.0.0

The expected output is similar to the following:

::

  /Users/userid/Downloads/installKamanja
  /Users/userid/Downloads/Kamanja-version-number/trunk
  /Users/userid/Downloads/installKamanja/bin
  ...
  [success] Total time: 11 s, completed Nov 19, 2015 4:02:09 PM
  copy the fat jars to /Users/userid/Downloads/installKamanja ...
  copy all Kamanja jars and the jars upon which they depend
        to the /Users/userid/Downloads/installKamanja/lib/system
  Prepare test messages and copy them into place...
  Setting up paths
  Kamanja install complete...


See also
--------

- :ref:`dir-struct-install` describes the directory tree that
  is created by the **easyInstallKamanja.sh** command.


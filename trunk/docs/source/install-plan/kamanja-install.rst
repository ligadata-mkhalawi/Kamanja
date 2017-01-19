
.. _kamanja-install-top:

Install Kamanja software
========================

The installation process can be summarized as:

#. Install external software components that are required
   for the Kamanja engine and set environment variables to identify
   the root directory where each is installed.
#. Install the Kamanja engine
#. Install additional external software components that are required
   for the Flare Email Surveillance product.
#. Install the Flare Email Surveillance product.

Create the kamanja user
-----------------------

These instructions use the **sudo** command for a number of steps.
If you prefer, you can instead set up the kamanja user
and use that ID to install and configure the Kamanja software.
The commands to do this are:

::

  sudo useradd kamanja
  sudo passwd kamanja

For non-production systems, you can delete the password for the kamanja user:

::

  sudo passwd --delete kamanja

For production systems, you should always use a password for the kamanja user.

Install Kamanja Prerequisites
-----------------------------

A number of third-party products must be installed
before installing the Flare software.

Install Java 1.8
~~~~~~~~~~~~~~~~

Download the latest Oracle Java binaries from
`Oracle Java download site
<http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html>`_.
We do not recommend running Flare on top of OpenJDK.

We recommend choosing the most recent release.
Java 1.8 is necessary to support the password encryption feature;
if you are not using that feature, you can use Java 1.7.


Install Scala
~~~~~~~~~~~~~

Download and install :ref:`Scala<scala-term>` 2.11 from the
`Scala 2.11 download site <http://www.scala-lang.org/download/2.11.7.html>`-.

Kamanja can instead use Scala 2.10, which can be downloaded from
`the Scala 2.10 download site <http://www.scala-lang.org/download/2.10.2.html>`-.

.. _zookeeper-install:

Install ZooKeeper
~~~~~~~~~~~~~~~~~

Download and install :ref:`ZooKeeper<zookeeper-term>` 3.4.6 from the
`Zookeeper download page <http://www.apache.org/dyn/closer.cgi/zookeeper/>`_.

After adding $ZOOKEEPER_HOME to your .bashrc or .bash_profile file,
issue the following commands to complete the ZooKeeper installation:

::

  cd $ZOOKEEPER_HOME/conf
  cp zoo_sample.cfg zoo.cfg

.. _kafka-install:

Install Kafka
~~~~~~~~~~~~~

Download :ref:`Kafka<kafka-term>` 0.10 from the
`Kafka download site <http://kafka.apache.org/downloads.html>`_.
Note that separate files are provided to run with Scala 2.11 or 2.10;
be sure to get the version that matches the version of Scala you installed.

Note that Kamanja 1.6.x has not been tested with Kafka 0.10.* .

Untar the .tgz file to install the software.  NEED TO CHECK THIS!!!


Define environment variables
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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


Install Kamanja engine
----------------------

The Kamanja software is packaged using the RedHat RPM utility.
It can be installed with the **sudo** utility
or you can create the kamanja user and install the software as the kamanja user.

Download the RPM that corresponds to the version of Scala you are using:

- URL for 2.10-1.6.1
- URL for 2.11-1.6.1

Basic installation
~~~~~~~~~~~~~~~~~~

To do a fresh install of Kamanja 1.6.1, issue the following command:

::

  sudo rpm –ivh <kamanja rpm>

The Kamanja binaries are installed in */usr* by default.

Basic upgrade to 1.6.1
~~~~~~~~~~~~~~~~~~~~~~

To upgrade to Release 1.6.1 from an earlier Kamanja release,
issue the following command:

  sudo rpm –Uvh <kamanja rpm>

Installing in a different location
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The Kamanja binaries are installed in */usr* by default.
Use the **--prefix** option to specify an alternate installation location:

::

  sudo rpm –ivh <kamanja rpm> [--prefix <install-path>] \
       [--dbpath <new-rpmdb-path>] [--nodeps]

For example:

::

  sudo rpm -ivh kamanja_1.6.0_2.11.rpm --prefix /usr/local \
       --dbpath /tmp/rpmdb/ -nodeps

When you do this,
a soft link is created at */usr/kamanja* that points to
the actual location where the software is located.

The **--dbpath** option tells the **rpm** command to use
a different RPM database.
This is useful when testing a complete system install,
where the RPM database needs to be changed.
 
You can use a similar command to upgrade from an earlier Kamanja release
but install into a location other the */usr*:

::

  sudo pm –Uvh <kamanja rpm> --prefix <custom location> \
       --dbpath <location of new rpmdb> --nodeps

For example:

::

  sudo pm -Uvh kamanja_1.6.0_2.11.rpm --prefix /usr/local \
       --dbpath /tmp/rpmdb/ --nodeps

Running multiple Kamanja releases on one system
-----------------------------------------------

TODO: rewrite

Type:

::

  sudo rpm –ivh <kamanja version 1>

This installs the Kamanja version 1 at /usr (or the prefix location)
and the /usr/bin/kamanja soft link (or the prefix where Kamanja is installed)
points to the Kamanja version.

To install a higher version without removing the old one,
run the following command:

::

  sudo rpm –ivh <kamanja version 2>

This creates another Kamanja version 2 directory at the location /usr
(or the prefix location) without removing the old directory.

However, the /usr/kamanja soft link
(or the prefix path where Kamanja is installed)
now points to the latest Kamanja version 2.
Also, if a custom database and prefix is used for a previous version,
use the same custom database location and the prefix path.

Note that if a higher version of Kamanja is present,
a lower version is not be installed. It gives the following error:

::

  package kamanja-version2 (which is newer than kamanja-version1) is already installed


More about the --dbpath option
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The **--dbpath** option tells the **rpm** command
to use a different RPM database.
This is useful when testing a complete system install,
where the RPM database needs to be changed.

In this case, use a different RPM database.

The basic syntax for this option is:

::

  --dbpath directory_name

For example:

::

  <sudo> rpm -ivh <kamanja.rpm> --dbpath <location of new rpmdb>

Some systems may give the following error:

::

  error: Failed dependencies:
  /bin/sh is needed by XXX

This happens because bash rpm is not available in the new rpmdb path.
Use the following to install RPM:

::

  <sudo> rpm --dbpath <location of new rpmdb> -nodeps -ivh <kamanja.rpm>

This is recommended for non-sudo users and to be used
with the custom installation path. The command for non-sudo users is:

::

  rpm --dbpath <location of new rpmdb> -nodeps -ivh <kamanja.rpm> --prefix /usr/local

If dbpath is used for previous versions,
use the same dbpath for the higher version installation.

Refer to `Fedora documentation
<https://docs.fedoraproject.org/en-US/Fedora_Draft_Documentation/0.1/html/RPM_Guide/ch04s05s03.html>`_
for information about creating a custom database.

Post-installation Steps and Verification
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

TODO: Verify and rewrite

To set all the paths to the current installed location of Kamanja,
run the following command:

::

    cd $KAMANJA_HOME/bin
    SetPaths.sh $KAFKA_HOME

Query the RMP database to verify the installation:

::

  rpm –qa | grep –i <package name>

This lists all package names; check this list
to ensure that it contains the installed packages (old and new version).

::

  rpm -q <package name>

This checks if the package is installed.

::

  rpm -ql <package name>

This lists the files in the installed package.

::

  rpm –qs <package name>

The **–s** option to the **rpm –q** command lists the state of each file
in a package:

.. list-table::
   :widths: 25 75
   :header-rows: 1

   * - State
     - Usage
   * - Normal
     - The file has been installed
   * - Not installed
     - The file from the package is not installed.
   * - Replaced
     - The file has been replaced


**ls –l /usr/kamanja** (or the prefix location where Kamanja is installed)
points to <path to the latest kamanja folder/bin/kamanja>.

Uninstall or Rollback
~~~~~~~~~~~~~~~~~~~~~

The uninstall process has the following scenarios:

::

  sudo rpm –e <kamanja.rpm> <--dbpath (if it is a custom database)>

If there is only one version of Kamanja, this command uninstalls
the Kamanja software from the system entirely.
However, if a higher version is uninstalled,
then the /usr/kamanja Kamanja soft link now points to
the lower version of Kamanja and deletes the higher version.
Also, if a lower version is uninstalled,
then it removes that lower version from the system
and the Kamanja soft link keeps pointing to the higher version.
If the custom database option, --dbpath, is used,
add it for the delete command as well.

During uninstall, Kamanja is completely removed from the system
along with the working directory, configuration, and storage.
Back up Kamanja before deleting, if it is required for future reference.



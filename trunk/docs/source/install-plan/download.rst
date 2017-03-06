

.. _kamanja-download:

Download Kamanja software
=========================

The Kamanja software is released as a tarball.

Download the 1.6.2 tarball that corresponds
to the version of Scala you are using
from the `Kamanja download page <http://kamanja.org/about/releasenotes/>`_.

We recommend that you download the tarball to a location
(such as */tmp* or *Downloads*)
other than where you want to install the software.
In this documentation,
we refer to this location as $KAMANJA_INSTALL.

Unzip the package.

.. note::  In most cases, using the tarballs provided by LigaData
           are the best alternative.
           However, Kamanja is completely open sourced and you can
           clone the `kamanja <https://github.com/LigaData/Kamanja>`_
           github repository and run **sbt build command**
           to generate binaries and a tar.gz file
           named *Kamanja-<ver>.<scala_ver>.tar.gz*
           that can be used to
           :ref:`install the cluster<clusterinstallerdriver-install>`.
           This also creates important folders required for installation:
           */bin*, */ClusterInstall*, and */config*.
           



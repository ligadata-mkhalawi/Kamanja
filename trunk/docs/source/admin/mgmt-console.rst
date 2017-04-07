
.. _mgmt-console-top:

Flare Management Console
========================

The Flare Management Console simplifies the task of
creating and maintaining metadata objects.
A prototype of this tool is included in this release.
This prototype supports the following activities:

- Connect to a data source
- Import an existing data source DDL
- Add, remove, update, and browse existing
  :ref:`message definitions<message-def-config-ref>`

Additional functionality is under development
for possible inclusion in future releases.

See :ref:`mgmt-console-install-config`
to set up the software.

.. _run-mgmt-console:

Opening the Management Console
------------------------------

Run the tool:

::

  > bin/start.sh

.. image:: /_images/fmc-dashboard.png

.. _new-data-mgmt-console:

Accessing data
--------------

Connecting to a new data source
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


CSV
~~~

XML
~~~

Database
~~~~~~~~

Importing an existing data source DDL
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. _message-mgmt-console:

Working with messages
---------------------

Messages contain the data that flows into and out of
the models in a pipeline that executes on Kamanja.
The message data can be formatted as CSV, JSON, XML, or KV.
The message that is output from one model
may be the input message for another model in the pipeline.

A :ref:`message definition<message-def-config-ref>` is a JSON file
that specifies the content, format, and handling of the information.

Message definitions can be created manually
by editing a message definition file
and then calling the :ref:`kamanja<kamanja-command-ref>` command
to add the message to the Kamanja metadata.
The **kamanja** command is also used to update and delete a message.

The Flare Management Console can be used to create and manage messages
more easily.

Display existing messages on the system:

.. image:: /_images/fmc-messages-definition.png

Create and Update Message Definition
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. image:: /_images/fmc-add-update-message.png




.. image:: /_images/fmc-add-update-message-2.png



.. image:: /_images/fmc-add-update-message-3.png



Delete Message Definition
~~~~~~~~~~~~~~~~~~~~~~~~~

To delete a message, click on the red trash can icon
at the right end of the row for the message to be deleted.
A pop-up appears to confirm that you want to delete the message:


.. image:: /_images/fmc-delete-message.png



Search Message Definition
~~~~~~~~~~~~~~~~~~~~~~~~~


.. _container-mgmt-console:

Working with containers
-----------------------


Create container
~~~~~~~~~~~~~~~~

Update container
~~~~~~~~~~~~~~~~

Delete container
~~~~~~~~~~~~~~~~

Search container
~~~~~~~~~~~~~~~~


.. _inspector-mgmt-console:

Inspector
---------


.. image:: /_images/fmc-inspector-datasource.png

.. _mgmt-console-install-config:

Install and configure the Management Console
--------------------------------------------

Download Apache Maven from the
`Maven download site <https://maven.apache.org/download.cgi>`_.

Be sure that Java 7 or Jave 8 is installed on your system.

Install nodejs:

::

  > yum install nodejs

Install bower:

::

  > npm install bower -g

Install Oracle ojdbc6, which is located inside the *lib* folder.
You need to install it in the local Maven repository:

::

  > cd lib
  > mvn install:install-file -Dfile=ojdbc6.jar -DgroupId=com.oracle \
       -DartifactId=ojdbc6 -Dversion=11.2.0.4 -Dpackaging=jar

Compile:

::

  > mvn assembly assembly


.. _oracle-output-adapter-ref:

Oracle Output Adapter
=====================


ClassName
---------


Oracle Output Adapter structure
--------------------------------------

::

  {
    "Name" = "<adapter-name>";
    "adapterSpecificCfg" =
         "{"hostname": "vm002.ligadata.com",
         "instancename":"KAMANJA",
         "portnumber":"1521",
         "user":"<user-name>",
         "password":"Carribean2",
         "SchemaName":"<schema-name>",
         "jarpaths":"/media/home2/jdbc",
         "jdbcJar":"ojdbc6.jar",
         "autoCreateTables":"YES",
         "appendOnly":"NO"}"
  }



Parameters
----------

The first few parameters are the standard ones
used for all adapters and described on the
:ref:`adapter-def-config-ref` page.
In addition, the **AdapterSpecificCfg** parameter
has the following attributes:

- **hostname** -
- **instancename** -
- **portnumber** - port used to connect to the Oracle database
- **user**, **password** - user name and password used
  to connect to the Oracle database
- **SchemaName** - name of the Oracle schema to which the data is being output.
  The names and types specified
  in the :ref:`message definition<message-def-config-ref>`
  must match what is defined in the Oracle schema.
- **jarpaths** -
- **jdbcJar** -
- **autoCreateTables** -
- **appendOnly**
  



Usage
-----


See also
--------

- :ref:`adapter-binding-config-ref`
- :ref:`adapter-def-config-ref`


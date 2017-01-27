

.. _jdbcdatacollector-command-ref:

JdbcDataCollector.sh
====================

Extract data from an external relational database
into a local file using a JDBC driver.
This tool can be used with any database that supports JDBC.
The local file is loaded into a :ref:`container<container-term>`
using :ref:`KVInit<kvinit-command-ref>`.

Syntax
------

::

  JdbcDataCollector.sh

Configuration file structure
----------------------------
The JDBC data collector is controlled by the *Config.json* file,
which can be found at
`<https://github.com/LigaData/Kamanja/blob/dev/trunk/Utils/JdbcDataCollector/src/main/resources/Config.json>`_.


::

  {
   "DriverName": "oracle.jdbc.driver.OracleDriver",
   "URLString": "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=myserver)
       (PORT=7896))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=myserver)))",
   "UserId": "",
   "Password": "",
   "Timeout": "86400",
   "Query": "SELECT A,B,C FROM TBL1",
   "WriteHeader": "true",
   "FieldDelimiter": "u0001",
   "RowDelimiter": "n",
   "CompressionString": "gz",
   "FileName": "/tmp/output.dat.gz",
   "DriverJars": [
   "/tmp/ojdbc6.jar"
   ]
  }

Options and arguments
---------------------

Configuration file parameters
-----------------------------

- **DriverName** - name of the JDBC driver.
- **URLString** - connection string for the database.
  Defines connection information such as the database and the port.
- **UserId/Password** - authentication information.
- **Timeout** - if the data cannot be extracted within the Timeout limit,
  then extraction ceases.
- **Query** - query to execute.
- **WriteHeader** - whether a header should be written to the file.
- **FieldDelimiter** - format of the delimiter between the fields.
- **RowDelimiter** - format of the delimiter between the rows.
- **CompressionString** - format to compress the data.
- **FileName** - name of the file to write to.
- **DriverJars** - corresponding JARs for the JDBC driver.


Usage
-----

The tool can be called once or as often as necessary.

Return values
-------------

Examples
--------

Files
-----

- $KAMANJA_HOME/bin/ JdbcDataCollector.sh
- `<https://github.com/LigaData/Kamanja/blob/dev/trunk/Utils/JdbcDataCollector/src/main/resources/Config.json>`_.


See also
--------


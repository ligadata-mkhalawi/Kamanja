

.. _containerutility-command-ref:

ContainerUtility.sh
===================

Select, delete, and truncate data from a container
using a key and/or timerange.
Only one container can be passed each time.

Syntax
------

::

  bash $KAMANJA_HOME/bin/ContainerUtility.sh --containername \
    com.ligadata.kamanja.sample.spumcodes \
    --config $KAMANJA_HOME/config/Engine1Config.properties \
    --outputpath /tmp/Kamanja --operation select --filter /opt/filter.json \
    --serializer “com.ligadata.kamanja.serializer.csvserdeser” \
    --serializeroptionjson \
    ‘{“alwaysQuoteFields”:false,”fieldDelimiter”:”,”,”valueDelimiter”:”~”}’ \
    --compressionstring “gz”

Options and arguments
---------------------

- **containername** - name of container.
  The containername option should not include a version of it.
- **config** - file config that includes storage type.
- **operation** - process type.
  Valid values are select, truncate, and delete.
- **filter** - JSON file that includes time ranges and/or keys
  to select/delete from the container.
  Valid values are:

    - Timeranges - beginTime and endTime.
      Currently, this can only be specified as 0 and 0.
    - Keys is a bucket key.
      It is the first column in the file
      that is used to push data to the container.

      This argument is mandatory for the delete and select operations
      but is not necessary for the truncate operation.
      See the "Usage" section below for more information.

- **outputpath** - path to put the data after selected.
  It is mandatory for the select operation
  and not necessary for the delete and truncate operations
- **serializer** - how to see the data in the select operation.
  Valid values are:

  - com.ligadata.kamanja.serializer.csvserdeser
  - com.ligadata.kamanja.serializer.jsonserdeser

  It is mandatory for the select operation
  and not necessary for the delete and truncate operations.
- **serializeroptionjson** - how to delimit the value field and key field.
  It is mandatory for the select operation
  and not necessary for the delete and truncate operations.
- **compressionstring** - format of file when using the select operation.
  The only supported value is gz.
  It is mandatory for the select operation
  and not necessary for the delete and truncate operation.


Usage
-----

  If the *filter.json* file contains more than one criteria,
  each criteria acts individually and gives results
  independent of the other.
  As a result, duplicate values may be seen in the output file.
  For example, if the filter.json specifies the following select criteria:

  ::

    [{ "begintime": "0", "endtime": "0"}, { "begintime": "0", "endtime": "0"}]


  Each returns results independently and, in this case,
  the same result is shown twice:

  ::

     "4843 - Pneumonia in whooping cough"7868, "7868 - Hiccough"33983,
     "33983 - Primary cough headache"0331,
     "0331 - Whooping cough due to bordetella parapertussis [B. parapertussis]"0330,
     "0330 - Whooping cough due to bordetella pertussis [B. pertussis]"0338,
     "0338 - Whooping cough due to other specified organism"0339,
     "0339 - Whooping cough4843,
     "4843 - Pneumonia in whooping cough"7868,
     "7868 - Hiccough"33983, "33983 - Primary cough headache"0331,
     "0331 - Whooping cough due to bordetella parapertussis [B. parapertussis]"0330,
     "0330 - Whooping cough due to bordetella pertussis [B. pertussis]"0338,
     "0338 - Whooping cough due to other specified organism"0339, "0339 - Whooping cough

Error returns
-------------

If an invalid path is specified for the **outputpath** argument,
the error message is:

::

  ERROR [main] - this path does not exist: tmp/kamanja

If an invalid value is specified for the **operation** argument
the error message is:

::

  ERROR [main] - you should pass truncate or delete or select in operation
    option WARN [main] - Usage: $KAMANJA_HOME/bin/ContainerUtility.sh
    --config <config file while has jarpaths, metadata store information
      & data store information>
    $KAMANJA_HOME/config/Engine1config.properties
    --container name <full package qualified name of a Container without
    version> test.kamanja.container --operation <truncate, select, delete>
    --filter <a json file that includes timeranges and keys>
    --outputpath <a path where you want put a selected rows *mandatory for
      select and not necessary for truncate and delete*>
    --serializer <how you need to see selected data *mandatory for select
      and not necessary for truncate and delete*>
    --serializeroptionsjson <*mandatory for select
      and not necessary for truncate and delete*>
    --compression string <the extension of file gz or dat *mandatory for select
      and not necessary for truncate and delete*>Sample uses:
     bash $KAMANJA_HOME/bin/ContainerUtility.sh
       --containername System.TestContainer
    --config $KAMANJA_HOME/config/Engine1Config.properties --operation truncate

If an incorrect or invalid filter file is provided, the error message is:

::

  ERROR [main] - this path does not exist: /opt/tesjson.json WARN [main]
    - Usage: $KAMANJA_HOME/bin/ContainerUtility.sh
    --config <config file while has jarpaths, metadata store information
        & data store information>
      $KAMANJA_HOME/config/Engine1config.properties
    --container name <full package qualified name of a Container without version>
      test.kamanja.container
    --operation <truncate, select, delete>
    --filter <a json file that includes timeranges and keys>
    --outputpath <a path where you want put a selected rows *mandatory for select
      and not necessary for truncate and delete*>
    --serializer <how you need to see selected data *mandatory for select
      and not necessary for truncate and delete*>
    --serializeroptionsjson <*mandatory for select and not necessary for truncate and delete*>
    --compression string <the extension of file gz or dat *mandatory for select
      and not necessary for truncate and delete*>Sample uses:
     bash $KAMANJA_HOME/bin/ContainerUtility.sh --containername System.TestContainer
      --config $KAMANJA_HOME/config/Engine1Config.properties --operation truncate

If the specified filter file has no content or keys
and time ranges are are not specified, the error message is:

::

  ERROR [main] - Failed to select data from com.ligadata.kamanja.samples.containers.CoughCodes
    container,at least one item (keys, timerange) should not be null for select operation

If a container that is not in the data store is specified, the error message is:

::

  ERROR [main] - Not found valid type for com.ligadata.kamanja.samples.containers.coughcodes1
    ERROR [main] - Not found tenantInfo for tenantId 

If a delete or select operation is used without providing the filter file,
the error message is:

::

  ERROR [main] - you should pass a filter file which includes keys and/or timeranges
   in filter option WARN [main] - Usage: $KAMANJA_HOME/bin/ContainerUtility.sh
    --config <config file while has jarpaths, metadata store information
      & data store information> $KAMANJA_HOME/config/Engine1config.properties
    --container name <full package qualified name of a Container without version>
      test.kamanja.container
    --operation <truncate, select, delete>
    --filter <a json file that includes timeranges and keys>
    --outputpath <a path where you want put a selected rows
      *mandatory for select and not necessary for truncate and delete*>
    --serializer <how you need to see selected data *mandatory for select
      and not necessary for truncate and delete*>
    --serializeroptionsjson <*mandatory for select and not necessary for truncate and delete*>
    --compression string <the extension of file gz or dat *mandatory for select
      and not necessary for truncate and delete*>Sample uses:
     bash $KAMANJA_HOME/bin/ContainerUtility.sh
       --containername System.TestContainer
       --config $KAMANJA_HOME/config/Engine1Config.properties
       --operation truncate

If the select operation is used and the serializer option is not provided,
the error message is:

::

  ERROR [main] - you should pass a serializer option for select operation WARN [main]
    - Usage: $KAMANJA_HOME/bin/ContainerUtility.sh
    --config <config file while has jarpaths, metadata store information
        & data store information>
      $KAMANJA_HOME/config/Engine1config.properties
    --container name <full package qualified name of a Container without version>
        test.kamanja.container
    --operation <truncate, select, delete>
    --filter <a json file that includes timeranges and keys>
    --outputpath <a path where you want put a selected rows *mandatory for select
      and not necessary for truncate and delete*>
    --serializer <how you need to see selected data *mandatory for select
      and not necessary for truncate and delete*>
    --serializeroptionsjson <*mandatory for select
      and not necessary for truncate and delete*>
    --compression string <the extension of file gz or dat *mandatory for select
      and not necessary for truncate and delete*>Sample uses:
      bash $KAMANJA_HOME/bin/ContainerUtility.sh
      --containername System.TestContainer
      --config $KAMANJA_HOME/config/Engine1Config.properties --operation truncate

Examples
--------

The com.ligadata.kamanja.samples.containers.CoughCodes container
is used for all testing scenarios below.
Add the container to the storage and push some data to it
before performing the following testing.

Truncating a container
~~~~~~~~~~~~~~~~~~~~~~

After pushing some data to the container,
there are seven records inserted in the storage
and when running the following command,
there is not any data in the storage for the container:

::

  bash $KAMANJA_HOME/bin/ContainersUtility.sh --containername \
    com.ligadata.kamanja.samples.containers.CoughCodes \
    --config /opt/Kamanja/config/Engine1Config.properties \
    --operation truncate:

::

  [RESULT] - Truncate com.ligadata.kamanja.samples.containers.CoughCodes
     container [RESULT] - Truncate com.ligadata.kamanja.samples.containers.CoughCodes
     container successfully

Deleting data from a container
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Use a command like the following to delete data from a container:

::

  bash $KAMANJA_HOME/bin/ContainersUtility.sh \
    --containername com.ligadata.kamanja.samples.containers.CoughCodes \
    --config /opt/Kamanja/config/Engine1Config.properties \
    --operation delete --filter /opt/testjson.json

The filter file can specify any of three methods
of selecting the data to be deleted:


#. Delete by time ranges.

   ::

     [{ "begintime": "0", "endtime": "0"}]

#. Delete by keys.

   ::

     [{ "keys": [ ["0330"], ["0331"] ]}]

#. Delete by timeranges and keys.

   ::

     [{ "begintime": "0", "endtime": "0", "keys": [ ["0330"], ["0331"] ]}]

Expected output:

::

  [RESULT] - The data deleted successfully

Selecting data from a container
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Use a command like the following to select data from a container:

::

  bash $KAMANJA_HOME/bin/ContainersUtility.sh --containername \
    com.ligadata.kamanja.samples.containers.CoughCodes \
    --config /opt/Kamanja/config/Engine1Config.properties \
    --outputpath /tmp/kamanja --operation select \
    --filter /opt/testjson.json \
    --serializer "com.ligadata.kamanja.serializer.csvserdeser" \
    --serializeroptionsjson '{"alwaysQuoteFields":false,"fieldDelimiter":",",
      "valueDelimiter":"~"}' \
    --compressionstring "gz"

The contents of the filter file determine how the data is selected:

#. Select by time ranges.

   ::

     [{ "begintime": "0", "endtime": "0"}]

#. Select by keys.

   ::

     [{ "keys": [ ["0330"], ["0331"] ]}]

#. Select by timeranges and keys.

   ::

     [{ "begintime": "0", "endtime": "0", "keys": [ ["0330"], ["0331"] ]}]

#. Select all records from the container (special case):

   ::

     [{ "begintime": "-1”, "endtime": "-1"}]

Expected output:

::

  [RESULT] - 1 rows fetched successfully [RESULT] -
    You can find data in this file:
    /tmp/kamanja/com_ligadata_kamanja_samples_containers_CoughCodes_result_29042016074131.dat



See also
--------

- :ref:`container-def-config-ref`



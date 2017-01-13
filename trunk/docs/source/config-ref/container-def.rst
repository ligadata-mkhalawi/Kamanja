

.. _container-def-config-ref:

Container definition
====================

All :ref:`container<container-term>` objects
used in the cluster are defined in the
:ref:`ClusterConfig.json<clusterconfig-config-ref>` JSON file.
Each container used in the cluster
has its own "Container" section,
identified by a unique "Name".


File structure
--------------

::

  {
      "Container": {
           "NameSpace": "<namespace>",
           "Name": "<name>",
           "Version": "<version>",
           "Description": "<description of product",
           "Persist": "true" | "false",
           "Fixed": "true" | "false",
           "CaseSensitive" : "true" | "false",
           "Fields": [{
                   "Name": "<attribute-name>",
                   "Type": "<type>"
           }, {
                   ...
           ],
          "PartitionKey": ["Id"],


Parameters
----------

- **NameSpace** – namespace of the message.
- **Name** – name of the message.
- **Version** – version of the message.
- **Description** – (optional) description of the message.
- **Persist** – (optional) If set to TRUE, data processed as this message type
  is saved to the data store.  See :ref:`persist-term`.
- **Fixed** – if set to TRUE, this is a fixed message;
  if set to FALSE, it is a mapped messages.
  See :ref:`Fixed and mapped messages<messages-fix-map-term>`.
- **CaseSensitive** -- if set to TRUE, fields in the message definition
  are case-sensitive.
  The variables in the generated message are the same case
  as given in the message definition.
  If set to FALSE, the fields in the message definition
  are considered lower case
  and the field variables in the message are generated as lower case.
  Default value is FALSE.
- **Fields/elements** – schema definition for the data included
  in this message.  This is a list of attribute names
  and the :ref:`type<types-term>` of each attribute.
- **PartitionKey** – (optional) partition keys for the message.

Usage
-----


Examples
--------

::

  {
      "Container": {
          "NameSpace": "System",
          "Name": "CustAlertHistory",
          "Version": "00.01.00",
          "Description": "Customer Alert History",
          "Fixed": "true",
          "Persist": "true",
          "Fields": [
              {
                  "Name": "custId",
                  "Type": "System.Long"
              },
              {
                  "Name": "branchId",
                  "Type": "System.Int"
              },
              {
                  "Name": "accNo",
                  "Type": "System.Long"
              },
              {
                  "Name": "alertDtTmInMs",
                  "Type": "System.Long"
              },
              {
                  "Name": "alertType",
                  "Type": "System.String"
              },
              {
                  "Name": "numDaysWithLessBalance",
                  "Type": "System.Int"
              }
          ],
          "PartitionKey": [
              "custId"
          ]
      }
  }



See also
--------



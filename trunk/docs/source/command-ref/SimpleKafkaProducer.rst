

.. _simplekafkaproducer-command-ref:

SimpleKafkaProducer.sh
======================

Syntax
------

::

  java -jar SimpleKafkaProducer-0.1.0
  --gz false
  --format json
  --topics “testin_1”
  --threads 1 --topicpartitions 8
  --brokerlist “localhost:9092”
  --files /location/of/CustomerInfo.json


Options and arguments
---------------------

- **gz** -
— **format** - indicates the format of the data (either CSV or JSON).
- **topics** -
- **threads** -
- **topicpartitions** -
- **brokerlist** -
- **files** - 
— **partitionkeyidxs <System.MessageName>:<PartitionKey>**
  indicates the message name and the partition key
  by which to aggregate other messages in the message history.

- **partitionkeyidxs** if the file contains multiple message types,
  list each type here, separated with commas.
  For example:

  ::

    -partitionkeyidxs “System.Customer:CustomerID,
    System.CustomerAddress:CustomerID,
    System.CustomerPreferences:CustomerID”.


Usage
-----

See also
--------



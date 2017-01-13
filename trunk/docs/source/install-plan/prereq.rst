
.. _prereqs-install-top:

Prerequisites
=============

Port 7800

Kamanja uses port 7800 for JGroups
to replicate and distribute cached Ehcache data over TCP.
Port 7800 is restricted for Kamanja/Kafka processes only.

Prerequisites for installation
------------------------------

Here are the prerequisites.
There are details later regarding how to check
whether these prerequisites have been met.

- CentOS/RedHat/OS X (virtual machine for Windows)
- ~ 400 MB for installation (3 GB if building from source)
- sudo access if using a Mac
- JDK 1.7+ (preferable 1.8)
- Scala v2.11.7
- Kafka 2.11_0.9
- ZooKeeper 3.4.6+
- Ensure ZooKeeper, Kafka, and Cassandra OR HBase (optional)
  services are running.

Supported Operating Systems
---------------------------

- Linux OS: Redhat, CentOS
- MAC OS: 10.9 and 10.10

Hardware Specifications

The following table shows the system requirement recommendations
for production performance.
Development systems can have a minimum required hardware capacity of 16GB RAM.

.. list-table::
   :widths: 20 30 20 30
   :header-rows: 1

   * - Platform
     - Minimum (Dev)
     - Minimum (Production)
     - Minimum (Optimal)
   * - Architecture
     - 32-bit, 64-bit
     - 32-bit, 64-bit
     - 64-bit
   * - Hard Drive
     - 200 GB or as per needs of the test data      
     - 500 GB, SSD, or non-SSD      
     - 1 Terabyte, SSD recommended
   * - Non-Windows Platforms processors
     - 8 GB RAM
     - 1.14 GHz, 32 GB RAM      
     - 2x six-core, 2-GHz CPU, 128-192 GB RAM,
       Redundant Array of Independent Disks (RAID) 0 or 1-0,
       with a 64-bit OS installed
   * - VMs
       (Ubuntu/Debian, CentOS, RedHat, Fedora)
     - 8 GB RAM
     - Intel Nehalem CPU or equivalent at 2 GHz, 32 GB RAM
     - 2x six-core, 2-GHz CPU, 128-192 GB RAM, RAID 0 or 1-0,
       with a 64-bit OS installed
   * - Available HD space
     - 5 GB
     - 5 GB (not including any database indexes)
     - 100 GB (not including any database indexes)

Data Formats
------------

LigaData Kamanja currently imports .json, and .csv data.

Types That the Engine Supports
------------------------------

See :ref:`types-term` for information about the types
supported by the Kamanja engine.



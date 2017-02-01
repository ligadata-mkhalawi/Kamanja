
.. _prereqs-install-top:

Planning your Kamanja environment
=================================

A Kamanja environment contains one or more
:ref:`clusters<cluster-term>`,
each of which contains one or more :ref:`nodes<node-term>`.
A node is a single server or VM.
The following installation scenarios are supported:

- :ref:`Install a single-node cluster<kamanja-install-top>`.
  This is appropriate for development, unit-testing,
  and demonstration purposes.

- :ref:`Install developer tools<develop-install-top>`
  if you will be developing software on this system.

- :ref:`Replicate<replicate-install-top>` a configured node
  to other nodes in the cluster.

- :ref:`Configure a multi-node cluster<cluster-install>`
  for systems that will be used for production.

- :ref:`Upgrade<upgrade-install-top>` from an earlier version
  of Kamanja.

.. _platform-reqs:

Platform requirements
---------------------

Before installing the Kamanja software,
you must install either an operating system
or a Hadoop stack.

Kamanja has been tested on the following operating systems:

- Linux OS: Redhat, CentOS
- MAC OS: 10.9 and 10.10

Kamanja has also been tested on the following Hadoop stacks:

- Cloudera
- HortonWorks 2.5.3

.. _hardware-reqs:

Hardware Requirements for a Node
--------------------------------

The following table shows the system requirement recommendations
for production performance.
Development systems can have a minimum required hardware capacity of 16GB RAM.

.. list-table::
   :widths: 20 30 20 30
   :header-rows: 1

   * - Platform
     - Minimum (Dev)
     - Minimum (Production)
     - Recommended
   * - Architecture
     - 32-bit, 64-bit
     - 32-bit, 64-bit
     - 64-bit
   * - Hard Drive
     - 200 GB
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



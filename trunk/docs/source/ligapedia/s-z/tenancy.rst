
.. _tenancy-term:

Tenancy
-------

Kamanja supports basic multi-tenancy.
Each business unit or division is known as a tenant or use case.
So, for example, if a company has two use cases,
both use cases can be deployed on the same cluster
without have access to each others resources..
To handle this, the engine has to balance how much processing time
to give each use case.
Each tenant has its own input adapter, output adapter,
and storage adapter, as well as default storage.
This allows multiple use cases on the same cluster.

Basic multi-tenancy is defined in the :ref:`clusterconfig-config-ref` file.

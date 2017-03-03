
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

For details about implementing multi-tenancy:

- Each tenant in the system is defined in a
  :ref:`tenant definition<tenant-def-config-ref>` section
  of the the :ref:`clusterconfig-config-ref` file.

- Each :ref:`adapter<adapter-term>` can be assigned to a tenant
  in the :ref:`adapter definition<adapter-def-config-ref>` section
  of the the :ref:`clusterconfig-config-ref` file.
  Only users who are members of that tenant
  can access the :ref:`models<model-term>`,
  :ref:`messages<messages-term>`, and :ref:`containers<container-term>`
  that are controlled by that adapter.

- You must specify a TENANTID every time you add or update
  a :ref:`container<container-term>`, :ref:`message<messages-term>`,
  :ref:`model<model-term>` using either the
  :ref:`kamanja-command-ref` command or a REST service.


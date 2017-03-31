

.. _dir-struct-install:

Directory structure after installation
======================================

Running the :ref:`easyInstallKamanja.sh<easyinstallkamanja-command-ref>` command
creates the following directory structure under the
$KAMANJA_HOME directory that represents the top-level directory
that is installed.
See :ref:`bashrc and bash_profile<bashrc-term>`
for instructions about defining $KAMANJA_HOME.

- bin - contains command scripts such as
  :ref:`CreateQueues.sh<createqueues-command-ref>`
  and :ref:`WatchInputQueue.sh<watchqueue-command-ref>`.
- config - contains configuration files such as
  :ref:`ClusterConfig.json<clusterconfig-config-ref>`
  and :ref:`MetadataAPIConfig.properties<metadataapiconfig-config-ref>`
- documentation - contains the KamanjaAPIdocs directory
  that contains the Readme.txt file
  and the built HTML files for the API documentation;
  point your browser to the index.html file in this directory
  to view those documents.
- input

  - HelpMenu.txt
  - SampleApplications

    - bin - command scripts for the sample applications
      such as InitKvStores_Medical.sh and PushSampleDataToKafka_Medical.sh
    - data
    - metadata
      - config
      - container
      - function
      - message
      - model
      - script
      - type
    - template

    - lib
    - logs
    - output
    - storage
    - template
    - workingdir - default working directory.

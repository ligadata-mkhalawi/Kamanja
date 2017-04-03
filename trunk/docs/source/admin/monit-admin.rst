
.. _monit-admin:

Using Monit
===========

The :ref:`monit-term` utility
can be used to monitor the status of the Kamanja cluster
and the configured nodes
and to restart any node that has failed.
For full information about using **monit**, see the
`Monit documentation <https://mmonit.com/monit/documentation/monit.html>`_.

Run monit
---------

Monit is run as a daemon that is started from the command line;
results can be viewed in a browser or on the command line.

Before running monit, you must configure it for your environment;
see :ref:`monit-config-admin` at the end of this chapter.

To start/stop/reload monit, use the following commands:

::

  cd /home/kamanjadev/monit-5.20.0/bin 

  ./monit             # Start monit
  ./monit quit        # Stop monit
  ./monit reload      # Reload monit
  ./monit -help       # Display monit help message

When Monit starts,
it uses the configuration files under the
*/home/kamanjadev/monit-5.20.0* directory
to define the services, clusters, and nodes
to be monitored.
This location can be modified  by editing the
:ref:`monitrc<monitrc-admin>` file.
Each file defines the shell scripts to be used to check for status
or to start/stop the service.
The shell script that checks the nodes exits with status=0 if successful;
if the return status is not 0,
Monit raises an alert.

Note the following:

- If the :ref:`SSH<ssh-term>` connection to the main node fails,
  it is not possible to start/stop nodes from Monit
  but the status of the nodes can still be pulled.
- Status checks are done periodically.
  When turning a node on or off,
  it may take a few seconds for Monit to display the correct status.
  The interval between checks is set by a **sleep** statement
  at the end of each :ref:`shell script<monit-scripts-admin>`.
- The KamanjaCluster tab in monit prints out the last ERROR line
  in the *KamanjaLog.log*.
  this may not completely explain the cause for failure
  but may give an indication as to where to start looking.
- Monit logs are written to the *logs/monit.log* directory
  under the */home/kamanjadev/monit-5.20.0* directory.

Monit dashboard
---------------

Use your browser to open the monit dashboard:

- URL: http://172.27.24.159:5000/
- Default user: admin
- Default password: monit

The user ID and password can be reset in the
:ref:`monitrc<monitrc-admin>` file.

You will see a display similar to the following:

.. image:: /_images/monit-nodes.png

The **System:** lines at the top
reports the status and some basic statistics
for the server on which Monit is running;
in this case, jbd1node16.digicelgroup.local:

- **Status**: see below
- **Load**: system load averages as displayed in the top line
  of the w(1) command or by the uptime(1) command.
- **CPU**: CPU usage as displayed in the top(1) command.

The remaining lines include the following fields:

- **Program**: refers to the entity that is being monitored.
  For the current Kamanja implementation,
  it can be a KamanjaNode, a KamanjaCluster, or a remoteHostSystem
  (the physical or virtual machine on which the  Kamanja Nodes is installed).
- **Status**: refers to the ability to poll
  the Status of the Monitored Service:

  - Green (Status ok) indicates that Monit
    was able to poll the status of the corresponding service.
  - Red (Status failed) means that Monit could not poll the status
    of this service.
    In other words, this indicates that the service
    is unable to give status, not that the service is down.
    This rarely happens and, in most cases,
    "Status ok" is displayed the next time status is polled.

- **Output**: gives the status of this service:

  - UP means that the service is running.
  - DOWN means the service is not running.

- **Last started**: gives the time and date
  when the last status poll was been issued for this node.
- **Exit value**: gives the exit value of the shell script
  that polled the status:

  - 0 means the program exited correctly.
  - Other Exit codes can be checked online.

.. :note:: the status checks are done once every specific number of seconds,
           so, when turning off/on a node,
           there is a lag until the correct status is reflected.

You can click on the name of a service to open a new screen
that displays more information about that service:

.. image:: /_images/monit-program-details.png


This screen has four buttons on the bottom: Start Service, Stop Service,
Restart Service, and Disable Monitoring/Enable Monitoring.
Note that stopping the service automatically disables monitoring it;
you can start monitoring it again by clicking "Enable Monitoring"
or starting the service again.

KamanjaCluster is considered to be up if any of the KamanjaNodes are up.
Check the status of the individual nodes
to determine how many nodes are up or down.

Use F5 to refresh the page if necessary.


.. _monit-config-admin:

Configure monit
---------------

Monit is installed on the system you will use to monitor the cluster.
In production environments, this should be a server
that does not host any processes in the analytic pipeline;
for demonstration purposes, Monit can co-exist with other processes.

By default, the configuration files are located in the
*/home/kamanjadev/monit-5.20.0/monitFiles/* directory.
To configure Monit:

- Create a file for each configured cluster,
  system node, and instance of the cluster
  by copying the appropriate template file.
- The name given to each of these files
  is the string that is displayed in the "Program" column
  of the dashboard;
  choose a name that is meaningful and is easy to associate
  with the underlying configuration that is defined in the
  :ref:`clusterconfig-config-ref` file.
- Modify each file to show the correct IP address, paths,
  and so forth.

.. list-table::
   :class: ld-wrap-fixed-table
   :widths: 25 18 52
   :header-rows: 1

   * - File
     - Permissions
     - Description
   * - :ref:`monitrc<monitrc-admin>`
     - `- r w - - - - - - -`
       (chmod 600)
     - Monit control file
   * - :ref:`KamanjaCluster<KamanjaCluster-admin>`
     - `- r w - - - - - - -`
       (chmod 600)
     - One copy for each configured Kamanja cluster;
       edit it for your configuration.
   * - :ref:`remoteHostSystem-admin`
     - `- r w - - - - - - -`
       (chmod 600)
     - One copy for each physical or virtual system node;
       edit it for your configuration.
   * - :ref:`kamanja1-admin`
     - `- r w - - - - - - -`
       (chmod 600)
     - One copy for each instance of the Kamanja cluster;
       edit it for your configuration.
   * - :ref:`shell scripts<monit-scripts-admin>`
     - `- r w x r - x r - x`
       (chmod 755)
     - Shell scripts are used to check the status.
       These must be located in the directory specified
       by the other files and be edited for your configuration.


.. _monitrc-admin:

monitrc file
~~~~~~~~~~~~

*monitrc* is the control file for Monit.
It contains fields for monitoring many activities
beyond what we are using here.

If you modify *monitrc*,
you can check that the syntax is good with the following command:

::

  $ monit -t
  $ Control file syntax OK


See the :ref:`monitrc-config-ref` reference page
for details about the full file.
In this section,
we just discuss the parameters
that relate to monitoring the Kamanja cluster:

::

   set daemon  30              # check services at 30 seconds intervals
   #  with start delay 5       # optional: delay the first check by 5 minutes (by
   #                           # default Monit check immediately after Monit start)
   set logfile /home/kamanjadev/monit-5.20.0/logs/monit.log
   
   set httpd port 5000
   #
       use address 0.0.0.0    # only accept connection from localhost
       allow 0.0.0.0/0.0.0.0  # allow localhost to connect to the server and
       allow admin:monit      # require user 'admin' with password 'monit'
   #          ####
   
   
   include /home/kamanjadev/monit-5.20.0/monitFiles/*

These parameters are defined as:


- **set daemon** -- Specifies the interval, in seconds, 
  between checks of services.
  Default value is 30 seconds.

- **with start delay** -- If set, specifies the lag between when Monit starts
  and the first check of services.
  By default, this parameter is not enabled
  and Monit checks the services immediately after it starts.

- **set logfile** -- specify the directory where
  `Syslog <https://linux.die.net/man/8/syslogd>`_ writes Monit logs.

- **set httpd port** -- specify the port to use for HTTP access.
  This is followed by **allow** lines that provide an access control list
  and set the user name and password used to access the dashboard.
  You can add additional lines to enable other users to log in.
  You can also set up an external file to manage the user names and passwords.
  See `MONIT HTTPD <https://mmonit.com/monit/documentation/#MONIT-HTTPD>`_
  in the Monit documentation for more information.

- **include** -- specify the location of the Monit configuration files.

Other configurations you may want to implement:

- Configure SSL so you can access the dashboard using https.
  See `SSL settings <https://mmonit.com/monit/documentation/#SSL-settings>`_
  in the Monit documentation for more information.

- Configure alert handling.  See `Alert Messages
  <https://mmonit.com/monit/documentation/#ALERT-MESSAGES>`_
  in the Monit documentation for more information.

- Set up email notification for alerts.
  See `Setting a mail server for alert delivery
  <https://mmonit.com/monit/documentation/#Setting-a-mail-server-for-alert-delivery>`_
  in the Monit documentation for more information.



.. _KamanjaCluster-admin:

KamanjaCluster
~~~~~~~~~~~~~~

For the Kamanja cluster itself,
make a copy of the *KamanjaCluster* file,
giving it a name that makes sense in your configuration.
The name of this file is the name displayed under the "Program" column
on the Monit dashboard.

::

  CHECK PROGRAM KamanjaCluster PATH ${PATH_TO}/kamanjaClusterStatusCheck.sh
    ${NODES_IPS} TIMEOUT 17 SECONDS
  if status != 0 then alert
  
  start program "/usr/bin/ssh -i ${PATH_TO}/Key.pem ${USER}@${LEADER_NODE_IP}
    '${PATH_TO}/StartKamanjaCluster.sh --ClusterId {ClusterId}
    --MetadataAPIConfig ${PATH_TO}/MetadataAPIConfig.properties'"

  stop program  "/usr/bin/ssh -i {PATH_TO}/Key.pem ${USER}@${LEADER_NODE_IP}
    '${PATH_TO}/StopKamanjaCluster.sh --ClusterId {ClusterId}
    --MetadataAPIConfig ${PATH_TO}/MetadataAPIConfig.properties'"


This file must then be edited to reflect your configuration.
The strings that need to be supplied
are represented in curly brackets:

- {PATH_TO} - replace with the full path for the specified file.

  - For the Monit scripts, this is typically
    the */home/kamanjadev/monit-5.20.0/scripts* directory.
  - For the :ref:`metadataapiconfig-config-ref` file,
    this is typically the *$KAMANJA_HOME/config* directory.
  - For the Key.pem file, this could be *$HOME/.ssh*
    or a location under $KAMANJA_HOME.

- {NODES_IPS} - IP addresses of each node in the cluster,
  separated with commas.  For example, if this is a four-node cluster,
  this list might be:

  ::

    127.0.0.1,127.0.0.2,127.0.0.3,127.0.0.4


- {USER} - system from which the **ssh** command is issued.
- {LEADER_NODE_IP} - IP address for Kamanja node 1.
- {ClusterId} - name of the cluster as defined in the
  :ref:`clusterconfig-config-ref` file.


.. _remoteHostSystem-admin:

remoteHostSystem
~~~~~~~~~~~~~~~~

For each physical or virtual system node,
copy an instance of the *remoteHostSystem* file,
giving it a name that makes sense for your configuration.

::

  SET DAEMON 15

  CHECK PROGRAM remoteHostSystem PATH ${PATH_TO}/remoteHostServerStatusCheck.sh
    ${NODE_IP} TIMEOUT 13 SECONDS
  if status != 0 then alert


Each of these files must then be edited
to reflect your configuration.
The strings that need to be supplied
are represented in curly brackets:

- {PATH_TO} - replace with the full path for the specified file.
  For the Monit scripts, this is typically
  the */home/kamanjadev/monit-5.20.0/scripts* directory.
- {NODE_IP} - IP address of this node

.. _kamanja1-admin:

kamanja1
~~~~~~~~

Copy the *kamanja1* file to create files
for each Kamanja instance in the cluster.
The names of the files are the names used for the items
when they are listed under the "Programs" header on the dashboard.
We recommend choosing names that are meaningful in your configuration.

::

  CHECK PROGRAM kamanja1 PATH ${PATH_TO}/kamanjaStatusCheck.sh
      ${NODE_IP} TIMEOUT 17 SECONDS
  if status != 0 then alert

  start program "/usr/bin/ssh -i ${PATH_TO}/Key.pem ${USER}@${LEADER_NODE_IP}
      '${PATH_TO}/StartKamanjaCluster.sh --ClusterId {ClusterId}
      --MetadataAPIConfig ${PATH_TO}/MetadataAPIConfig.properties --NodeIds 1'"

  stop program  "/usr/bin/ssh -i ${PATH_TO}/Key.pem ${USER}@${LEADER_NODE_IP}
      '${PATH_TO}/StopKamanjaCluster.sh --ClusterId {ClusterId}
      --MetadataAPIConfig ${PATH_TO}/MetadataAPIConfig.properties --NodeIds {NodeId}'"

Each of these files must then be edited
to reflect your configuration.
The strings that need to be supplied
are represented in curly brackets:

- {PATH_TO} - replace with the full path for the specified file.

  - For the Monit scripts, this is typically
    the */home/kamanjadev/monit-5.20.0/scripts* directory.
  - For the :ref:`metadataapiconfig-config-ref` file,
    this is typically the *$KAMANJA_HOME/config* directory.
  - For the Key.pem file, this could be *$HOME/.ssh*
    or a location under *$KAMANJA_HOME*.

- {NODE_IP} - IP address of this node
- {USER} - system from which the **ssh** command is issued
- {LEADER_NODE_IP} - IP address for Kamanja node 1. 
- {ClusterId} - ID of the cluster as defined in the
  :ref:`clusterconfig-config-ref` file.
- {NodeIds} - NodeId defined for this node in the
  :ref:`clusterconfig-config-ref` file.


.. _monit-scripts-admin:

Shell scripts
~~~~~~~~~~~~~

Three shell scripts are used to check for status.
These are:

- kamanjaClusterStatusCheck.sh
- kamanjaStatusCheck.sh
- remoteHostServerStatusCheck.sh

These scripts are typically located in the
*/home/kamanjadev/monit-5.20.0/scripts* directory
and must have the correct file permissions (-rwxr-xr-x).
You can set the file permissions with the following commands:

::

  chmod 755 kamanjaClusterStatusCheck.sh
  chmod 755 kamanjaStatusCheck.sh
  chmod 755 remoteHostServerStatusCheck.sh
  

You then need to edit each file to have
the correct paths and permissions set.

kamanjaClusterStatusCheck.sh
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

::

  #!/bin/bash

  Nodes=$1
  atLeastOneNodeUp='1'
  nodesStatus=''

  IFS=',' read -ra nodesIPs <<< "$Nodes"

  for i in "${nodesIPs[@]}"; do
     operations=`/usr/bin/ssh -i ${PATH_TO}/Key.pem ${USER}@$i 'ps aux | grep java | grep com.ligadata.KamanjaManager.KamanjaManager | grep -v "grep" | wc -l'`
     if [ $operations -gt 0 ]
     then
        atLeastOneNodeUp='0'
     fi
  done

  if [ $atLeastOneNodeUp -eq 0 ]
  then
  echo "Cluster is UP"
  exit $?
  else
  ErrorCode=`/usr/bin/ssh -i ${PATH_TO}/Key.pem ${USER}@${NODE_1_IP} 'cat ${PATH_TO}/KamanjaLog.log | grep ERROR | tail -n 1'`
  echo "Cluster is DOWN: $ErrorCode"
  sleep 29
  fi


kamanjaStatusCheck.sh
^^^^^^^^^^^^^^^^^^^^^

::

  #!/bin/bash

  nodeIP=$1
  operations=`/usr/bin/ssh -i ${PATH_TO}/Key.pem ${USER}@$nodeIP
      'ps aux | grep java | grep com.ligadata.KamanjaManager.KamanjaManager |
      grep -v "grep" | wc -l'`
  if [ $operations -gt 0 ]
  then
  echo "Node is UP"
  exit $?
  else
  echo "Node is DOWN"
  sleep 30
  fi


remoteHostServerStatusCheck.sh
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

::

  #!/bin/bash

  nodeIP=$1
  operations=`/usr/bin/ssh -i ${PATH_TO}/Key.pem ${USER}@$nodeIP 'ls / | wc -l'`
  if [ $operations -gt 1 ]
  then
  echo "Machine is UP"
  exit $?
  else
  echo "Machine is DOWN"
  sleep 30
  fi




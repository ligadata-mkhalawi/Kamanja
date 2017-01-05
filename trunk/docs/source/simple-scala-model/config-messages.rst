
.. simp-scala-msg-config:

Add message definitions to Kamanja
----------------------------------

Now that the messages have been written for use by Kamanja,
they must be added to Kamanja.
For each message definition written above,
run a command that takes the message definition,
generates Scala code out of it,
and compiles it into a JAR to be used by Kamanja.
To do this, on the command-line, run the following:

::

  cd <KamanjaInstallDirectory>

For example:
  
::

  cd /opt/Kamanja-1.4.0_2.11/

You must also upload the cluster configuration from the
:ref:`clusterconfig-config-ref` file
if you have not previously done this:

::

  bash bin/kamanja upload cluster config /config/ClusterConfig.json

Then, call the **kamanja add message** command to add the messages.
Note that all of these commands define :ref:`tenancy<tenancy-term>`
for the message.

::

  bash bin/kamanja add message /path/to/SimpleMathDAG/NumberMessage.json \
     TENANTID tenant1

  bash bin/kamanja add message /path/to/SimpleMathDAG/AddedMessage.json \
     TENANTID tenant1

  bash bin/kamanja add message /path/to/SimpleMathDAG/MultipliedMessage.json \
     TENANTID tenant1

  bash bin/kamanja add message /path/to/SimpleMathDAG/DividedMessage.json \
     TENANTID tenant1

After each of the above commands executes,
you can look at the *APIResult* file for that command
to see whether the command was successful.
The file looks something like this:

::

  Result: {
    "APIResults": {
        "Status Code": 0,
      	"Function Name": "AddMessageDef",
        "Result Data": null,
        "Result Description": "Message Added 	Successfully:com.ligadata.test.dag.numbermessage.000000000001000000"
      }
   }


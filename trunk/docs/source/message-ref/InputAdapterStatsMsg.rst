
.. _inputadaptersstatssg-msg-ref:

InputAdapterStatsMsg
====================

This message records statistical information
about data being ingested to the Kamanja cluster.
It can be supplied as the attribute to the
**StatusMsgTypeName** parameter in the
:ref:`smart-input-config-ref` file
to have the input adapter send a status message
for each file it successfully processes.

InputAdapterStatsMsg structure
------------------------------

::

  {
    "Message": {
      "NameSpace": "com.ligadata.messages",
      "Name": "InputAdapterStatsMsg",
      "Version": "00.01.00",
      "Description": "",
      "Fixed": "true",
      "Fields": [
          {
          "Name": "msgtype",
          "Type": "String"
        },
        {
          "Name": "source",
          "Type": "String"
        },
        {
          "Name": "filename",
          "Type": "String"
        },
	    {
          "Name": "recordscount",
          "Type": "Long"
        },
	    {
		  "Name":"starttime",
		  "Type": "String"
	    },
        {
          "Name": "endtime",
          "Type": "String"
        },
        {
          "Name": "bytesRead",
          "Type": "Long"
        },
        {
          "Name": "nodeId",
          "Type": "String"
        },
        {
          "Name": "status",
          "Type": "String"
        }
	    
      ]
    }
  }

Usage
-----

Before running your application with this feature enabled,
you must run the :ref:`kamanja-command-ref` command
with the **add message** argument to add the message
to the Kamanja metadata.

This message can be bound to any output adapter;
here are some examples of the logs produced
when it is enabled:

::

  { "msgtype": "FileInputAdapterStatusMsg","source": "SFTP",
      "filename": "\/home\/bigdata\/billing_20170121T173821.csv.tgz",
      "recordscount": 678973,"starttime": "2017-02-23 04:29:38.612 EST",
      "endtime": "2017-02-23 04:39:20.878 EST","bytesread": 204810240,
      "nodeid": "6","status": "Success" }

  { "msgtype": "FileInputAdapterStatusMsg","source": "SFTP",
      "filename": "\/home\/bigdata\/mygroup.local.http.20170218.10482135.csv.tgz",      "recordscount": 1873,"starttime": "2017-02-23 04:39:19.496 EST",
      "endtime": "2017-02-23 04:39:21.458 EST","bytesread": 961665,
      "nodeid": "3","status": "Corrupt" }

Differences between versions
----------------------------

This message type is new in Release 1.6.2.

See also
--------

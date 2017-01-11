
.. _properties-param-term:

Properties parameter
--------------------

The add/update :ref:`kamanja<kamanja-command-ref>` commands
have the properties parameter used for ??.
This additional parameter can be provided through the PropertiesFile file
or through a command string in JSON format – Properties.

For example:

::

  kamanja add message $KAMANJA_HOME/input/SampleApplication/metadata/message/Message.json TENANTID hello PropertiesFile $KAMANJA_HOME/config/HelloProp.json

or

::

  kamanja add message $KAMANJA_HOME/input/SampleApplication/metadata/message/Message.json TENANTID hello Properties “{ “Comment” : “This is a comment”, “Tag” : “134kjlsdf” , “Description” : “This is description”, “OtherParams: “These are other params” }

The added properties can be seen when the objects are retrieved
using the get command.

When PropertiesFile and Properties are provided at the same time,
PropertiesFile has precedence over Properties and Properties is ignored.

HelloProp.json looks something like this:

::

  {
  "description" : "This is description \n This is description \n This is description.",
  "comment" : "this is comment",
  "tag" : "1n343434"
  }

Here is an example of adding a Message_Definition_HelloWorld.json message
with the PropertiesFile parameter:

::

  $KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/Message_Definition_HelloWorld.json TENANTID hello PropertiesFile $KAMANJA_HOME/config/HelloProp.json

Expected output:

::

  Result: {
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "AddMessageDef",
      "Result Data" : null,
      "Result Description" : "Message Added Successfully:com.ligadata.kamanja.samples.messages.msg1.000000000001000000"
    }
  }

Here is an example of getting that last message to see if it was added:

::

  kamanja get message com.ligadata.kamanja.samples.messages.msg1.000000000001000000

Expected output:

::

  Result: {
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "GetMessageDefFromCache",
      "Result Data" : "{\"Message\":{\"NameSpace\":\"com.ligadata.kamanja.samples.messages\",\"Name\":\"msg1\",\"FullName\":\"com.ligadata.kamanja.samples.messages.msg1\",\"Version\":\"000000000001000000\",\"TenantId\":\"hello\",\"Description\":\"hello there howdy \\n hello there howdy\\n hello there howdy.\",\"Comment\":\"this is comment\",\"Author\":null,\"Tag\":\"1n343434\",\"OtherParams\":\"{\\\"b\\\":\\\"there\\\",\\\"a\\\":\\\"hello\\\"}\",\"CreatedTime\":1466538621856,\"UpdatedTime\":1466538621856,\"ElementId\":2000024,\"ReportingId\":2000062,\"SchemaId\":2000020,\"AvroSchema\":\"{ \\\"type\\\": \\\"record\\\",  \\\"namespace\\\" : \\\"com.ligadata.kamanja.samples.messages\\\" , \\\"name\\\" : \\\"msg1\\\" , \\\"fields\\\":[{ \\\"name\\\" : \\\"id\\\" , \\\"type\\\" : \\\"int\\\"},{ \\\"name\\\" : \\\"name\\\" , \\\"type\\\" : \\\"string\\\"},{ \\\"name\\\" : \\\"score\\\" , \\\"type\\\" : \\\"int\\\"}]}\",\"JarName\":\"com.ligadata.kamanja.samples.messages_msg1_1000000_1466538617144.jar\",\"PhysicalName\":\"com.ligadata.kamanja.samples.messages.V1000000.msg1\",\"ObjectDefinition\":\"{\\n  \\\"Message\\\": {\\n    \\\"NameSpace\\\": \\\"com.ligadata.kamanja.samples.messages\\\",\\n    \\\"Name\\\": \\\"msg1\\\",\\n    \\\"Version\\\": \\\"00.01.00\\\",\\n    \\\"Description\\\": \\\"Hello World Processing Message\\\",\\n    \\\"Fixed\\\": \\\"true\\\",\\n    \\\"Fields\\\": [\\n      {\\n        \\\"Name\\\": \\\"Id\\\",\\n        \\\"Type\\\": \\\"System.Int\\\"\\n      },\\n      {\\n        \\\"Name\\\": \\\"Name\\\",\\n        \\\"Type\\\": \\\"System.String\\\"\\n      },\\n      {\\n        \\\"Name\\\": \\\"Score\\\",\\n        \\\"Type\\\": \\\"System.Int\\\"\\n      }\\n    ]\\n  }\\n}\",\"ObjectFormat\":\"JSON\",\"DependencyJars\":[],\"MsgAttributes\":[{\"NameSpace\":\"system\",\"Name\":\"id\",\"TypNameSpace\":\"system\",\"TypName\":\"int\",\"Version\":1000000,\"CollectionType\":\"None\"},{\"NameSpace\":\"system\",\"Name\":\"name\",\"TypNameSpace\":\"system\",\"TypName\":\"string\",\"Version\":1000000,\"CollectionType\":\"None\"},{\"NameSpace\":\"system\",\"Name\":\"score\",\"TypNameSpace\":\"system\",\"TypName\":\"int\",\"Version\":1000000,\"CollectionType\":\"None\"}],\"PrimaryKeys\":[],\"ForeignKeys\":[],\"TransactionId\":34}}",
      "Result Description" : "Successfully fetched message from cache"
    }
  } 


Here is an example of updating the Message_Definition_HelloWorld.json message
with the Properties parameter:

::

  kamanja update message $KAMANJA_HOME/input/SampleApplicationmetadata/message/Message_Definition_HelloWorld.json
  TENANTID hello Properties ‘{“Description” : “This is the new description”,
  “Comment” : “The update is done to test the new feature”, \
  “Tag” : “NEWTAG”, “OtherParams” : “The test was executed by QA department to verify”}’

Expected output:

::

  Result: {
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "AddMessageDef",
      "Result Data" : null,
      "Result Description" : "Message Added Successfully:com.ligadata.kamanja.samples.messages.msg1.000000000001000001"
    }
  }

  RecompileModel results for com.ligadata.kamanja.samples.models.helloworldmodel.1
  {
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "RemoveModel",
      "Result Data" : null,
      "Result Description" : "Deleted Model Successfully:com.ligadata.kamanja.samples.models.helloworldmodel.000000000000000001"
    }
  }{
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "AddModel",
      "Result Data" : null,
      "Result Description" : "Model Added Successfully:com.ligadata.kamanja.samples.models.helloworldmodel.000000000000000001"
    }
  }

Here is an example of getting that last message to see if it was updated:

::

  kamanja get message com.ligadata.kamanja.samples.messages.msg1.000000000001000001

Expected output:

::

  Result: {
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "GetMessageDefFromCache",
      "Result Data" : "{\"Message\":{\"NameSpace\":\"com.ligadata.kamanja.samples.messages\",\"Name\":\"msg1\",\"FullName\":\"com.ligadata.kamanja.samples.messages.msg1\",\"Version\":\"000000000001000001\",\"TenantId\":\"hello\",\"Description\":\"this is the new description\",\"Comment\":\"the update is done to test the new feature\",\"Author\":null,\"Tag\":\"newtag\",\"OtherParams\":\"{\\\"otherparams\\\":\\\"the test was executed by qa department to verify\\\"}\",\"CreatedTime\":1466541054527,\"UpdatedTime\":1466541063645,\"ElementId\":2000024,\"ReportingId\":2000065,\"SchemaId\":2000021,\"AvroSchema\":\"{ \\\"type\\\": \\\"record\\\",  \\\"namespace\\\" : \\\"com.ligadata.kamanja.samples.messages\\\" , \\\"name\\\" : \\\"msg1\\\" , \\\"fields\\\":[{ \\\"name\\\" : \\\"id\\\" , \\\"type\\\" : \\\"int\\\"},{ \\\"name\\\" : \\\"name\\\" , \\\"type\\\" : \\\"string\\\"},{ \\\"name\\\" : \\\"score\\\" , \\\"type\\\" : \\\"int\\\"}]}\",\"JarName\":\"com.ligadata.kamanja.samples.messages_msg1_1000001_1466541059070.jar\",\"PhysicalName\":\"com.ligadata.kamanja.samples.messages.V1000001.msg1\",\"ObjectDefinition\":\"{\\n  \\\"Message\\\": {\\n    \\\"NameSpace\\\": \\\"com.ligadata.kamanja.samples.messages\\\",\\n    \\\"Name\\\": \\\"msg1\\\",\\n    \\\"Version\\\": \\\"00.01.01\\\",\\n    \\\"Description\\\": \\\"Hello World Processing Message\\\",\\n    \\\"Fixed\\\": \\\"true\\\",\\n    \\\"Fields\\\": [\\n      {\\n        \\\"Name\\\": \\\"Id\\\",\\n        \\\"Type\\\": \\\"System.Int\\\"\\n      },\\n      {\\n        \\\"Name\\\": \\\"Name\\\",\\n        \\\"Type\\\": \\\"System.String\\\"\\n      },\\n      {\\n        \\\"Name\\\": \\\"Score\\\",\\n        \\\"Type\\\": \\\"System.Int\\\"\\n      }\\n    ]\\n  }\\n}\\n\",\"ObjectFormat\":\"JSON\",\"DependencyJars\":[\"com.ligadata.kamanja.samples.messages_msg1_1000000_1466538617144.jar\"],\"MsgAttributes\":[{\"NameSpace\":\"system\",\"Name\":\"id\",\"TypNameSpace\":\"system\",\"TypName\":\"int\",\"Version\":1000001,\"CollectionType\":\"None\"},{\"NameSpace\":\"system\",\"Name\":\"name\",\"TypNameSpace\":\"system\",\"TypName\":\"string\",\"Version\":1000001,\"CollectionType\":\"None\"},{\"NameSpace\":\"system\",\"Name\":\"score\",\"TypNameSpace\":\"system\",\"TypName\":\"int\",\"Version\":1000001,\"CollectionType\":\"None\"}],\"PrimaryKeys\":[],\"ForeignKeys\":[],\"TransactionId\":37}}",
      "Result Description" : "Successfully fetched message from cache"
    }
  }



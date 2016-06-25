
**Python Proxy**

... my punch list as to what is going to be done to screw in the python server feature to kamanja:

_1)_ The NodeContext will contain several new pieces of information needed by the 
model factories and the instances they create:

	The host, port, and Kamanja python module path for the pythonserver to which to establish a connection has been established will be avaialble in the proxy server connection object state.

	The server will have been started by the engine before the 
	PythonFactoryOfModelInstanceFactory's getModelInstanceFactory
	is called to get the PythonAdapterFactory instance for the 
	supplied model. 

	The PythonAdapterFactory for the model will pull out this proxy pyserver connection  from the NodeContext based upon the current thread id.  

	Since there is only one PythonAdapterFactory for any given python model, it means that the NodeContext (at least I am thinking that is the place for this info) will need to have a map perhaps with the threadId/connection (see 4 below for more description).  For example,

		val low_level_proxy_server_connection_object : PyServerConnectObject = 
			nodeContext.getValue(Thread.currentThread().getId())
				.asInstanceOf[PyServerConnectObject]

	In any event the proxy python server connection object needs to be available in that NodeContext if we don't want to redo the model interface again for this.

_2)_ The server messages final wrapper will be done by the proxy pyserver connection object just before sending.  The proxy model instances will only package the payload message data I think and the rest of the message can be done by the proxy pyserver connection for each call type (i.e., addModel, removeModel, serverStatus, stopServer) 

_3)_ Logging. There are currently two pieces of information that the server requires to effect the logging:
	a) the log configuration file
	b) the log file path

We probably want separate log files for each server running on the node? The portId could be used to suffix the log file name which will be found in 
	
		s"$pythonPath/logs/pythonserver$port.log

It may be possible to use our current log4j configuration to configure the logging on each server, but since there are going to be a number of python servers running on each node, the config would get much more complicated with possibly different logger specs for each python server log in addition to our normal logs.

Then again, if the formatters are sufficiently detailed, we could just run all of it together.  Small changes in the server code is needed to remove the dynamically created logger in this case.

We can probably go with what is there now, and make these logging changes more integrated with our current kamanja approach on a future sprint.  For the most part, this log feature is inward focused with very little customer configurable.  The python path should be part of the installation.  The logs can be kept inside that python path in the logs directory....

As to whether the python side can respond or be setup to respond to refresh its log parameters periodically (as is done on kamanja side), I don't know that.  Investigation is needed to see if this is available in 2.7.x.

_4)_ There is one dedicated low level proxy object per thread/partition that is to support communication with the models dedicated to support messages that have been allocated to that thread/partition .   Communication to models that are running on a given server will use the same low level connection object.   

A class will be built that has the connection and the client side loop, decode and encode elements in it.  Since there is but one factory per model the NodeContext needs to have a map that tells the factory of factory which low level communication object to give to the ModelFactory so that it in turn can choose the right low level object to give to each model instance. See (1) above. OR The NodeContext has unique thread specific content only.  In other words, no map is needed of the connection objects... just a string key and the proxy pyserver connection object value.

All this suggests that this low level proxy object is instantiated/ initialized (startServer cmd) for each thread in the kamanja manager and a map[ThreadId,low lev proxy inst] of them is passed to the factory of factory factory instantiator in the NodeContext OR a thread specific NodeContext is prepared and passed... whatever is right.

_5)_ The other python server commands (besides startServer and addModel) need to be integrated.  Commands are:
	a) server status (currently returns the models running on server)
	... this is a server wide command.  Command typically sent to all servers
	b) stop server (server wide command). Command typically sent to all servers.
	c) removeModel (server wide command). Command sent to all servers.

In order to serialize the commands to the server s.t. the reply is obtained before the next command is sent (as we discussed, to keep the server simple, it was suggested to not try to contend with back to back commands without intervening replies).

To do this, the server wide commands (status, stop, remove model) need to be invoked from same thread as the addModel and executeModel for each respective proxy thread/server connection object.

_6)_ New classes:

	a) low level server connection object that makes the call to startserver as part of its initialization.
	b) possibly separate class for mapping/packaging outgoing message to json command and construction of the serialized request message wrapper.  This has two aspects... 
	- the mapping in the outgoing message to the fields that the models said it needs as part of its reply to the addModel command,
	- the construction of the add wrapper, and full remove, status, and stop commands are all the same regardless of the thread running.
	c) similarly a reply decode object that decodes the payload reply, prepares the ResultMap for return to the engine. Here there is an initial unwrapping of the actual result (removal of wrapper cruft used to transmit reply) and the generation of the map or list or whatever it is that is known only by the proxy model object.

_7)_ The engine in the metadata module will do the per thread proxy/pyserver connection object.  The port used for each connection will be unique.  Once built a map is prepared with the thread ids as the key.  Alternatively if the thread id is not there yet, the port is used and there is a map from threadid to port to the connection object lash up.

-*-

_I am sure of two things... there are other ways to organize this and there are no doubt issues I have not broached above that will need to be considered during development._


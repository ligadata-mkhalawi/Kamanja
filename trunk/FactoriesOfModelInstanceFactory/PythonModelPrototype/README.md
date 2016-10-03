**------------------------------**
**Scala Client and Python Server**
**------------------------------**

_Status_

The code you find in the checkin is "feature complete" but not fully tested.  I have unit tested different aspects as I tried to discover what I would need for this effort in terms of the Python language.  

The server starts without issue.  Multiple servers can be run on each machine by varying the listen port.

_Description_

Server related commands found in the Scala client script include _startServer_, _stopServer_, and _serverStatus_.  The model related command are _addModel_, _removeModel_, and _executeModel_. The commands one can use from the Scala client are: 

	  SocketClient.scala <named args...> 
	  where named args are:

	    --cmd startServer  
	              	[--host <hostname or ip> ... default = localhost]
	              	[--port <user port no.> ... default=9999]
	    --cmd stopServer  
	    			[--host <hostname or ip> ... default = localhost] 
	              	[--port <user port no.> ... default=9999]
	    --cmd addModel    
	    			--filePath <filePath>
	    --cmd removeModel   
	    			--modelName '<modelName>'
	    --cmd serverStatus
	    --cmd executeModel  
	    			--modelName '<modelName>' 
	              	--msg '<msg data>'
	    --cmd executeModel 
	    			--modelName '<modelName>'
	                --filePath '<msg file path>'


Multiple servers can be run concurrently on the same machine by listening on a different port.  Provisions for starting the server on another machine have been coded (not tested). 

With the exception of the startServer command, all of the commands you see above all go across the connection as one string with linefeed delimiters.  For example, the _addModel_ command goes in a formatted string like this:

		s"$cmd\n$modelName\n$modelSrc"

That is, the _addModel_ command on the first line, followed by the _model name_ to be added, followed by the actual python _model source_ file content on subsequent lines.  Python has rich string and collection manipulation like Scala, so lists can be "popped" by line index which will remove the line for that index.  You will see a lot of pop(0) in the code.

The python files are added to a subdirectory of one that is declared in the PYTHONPATH environment variable.  This must be set whereever you test.  For example, if you were to copy the Python folder to your home directory...

	export PYTHONPATH=$HOME/python

This is accomplished by having the scala client invoke a script that sets this pre-planned/apriori known location of the Kamanja Python packages before loading the server.  See the notes below for more details.

_Notes_

1) _The Files_

	rich@pepper:~/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/src/main$ ls -lR      
	.:
	total 12
	drwxrwxr-x 6 rich rich 4096 May 29 15:35 python
	drwxrwxr-x 2 rich rich 4096 May 25 21:54 resources
	drwxrwxr-x 2 rich rich 4096 May 30 00:46 scala

	./python:
	total 20
	drwxrwxr-x 2 rich rich 4096 May 29 16:05 bin
	drwxrwxr-x 2 rich rich 4096 May 28 23:21 commands
	-rw-rw-r-- 1 rich rich    0 May 28 22:32 __init__.py
	drwxrwxr-x 2 rich rich 4096 May 28 22:35 models
	drwxrwxr-x 2 rich rich 4096 May 28 22:35 modelsToLoad
	-rwx------ 1 rich rich 4061 May 30 00:08 pythonserver.py

	./python/bin:
	total 4
	-rwx------ 1 rich rich 1187 May 29 17:25 StartPythonServer.sh

	./python/commands:
	total 44
	-rw-rw-r-- 1 rich rich 1187 May 28 23:20 addModel.py
	-rw-rw-r-- 1 rich rich 1854 May 28 23:21 addModel.pyc
	-rw-rw-r-- 1 rich rich  396 May 28 13:48 executeModel.py
	-rw-rw-r-- 1 rich rich  609 May 28 22:19 executeModel.pyc
	-rw-rw-r-- 1 rich rich    0 May 28 22:34 __init__.py
	-rw-rw-r-- 1 rich rich  112 May 28 22:35 __init__.pyc
	-rw-rw-r-- 1 rich rich  269 May 28 13:48 removeModel.py
	-rw-rw-r-- 1 rich rich  546 May 28 22:19 removeModel.pyc
	-rw-rw-r-- 1 rich rich  197 May 28 13:48 serverStatus.py
	-rw-rw-r-- 1 rich rich  555 May 28 22:19 serverStatus.pyc
	-rw-rw-r-- 1 rich rich   95 May 28 00:49 stopServer.py
	-rw-rw-r-- 1 rich rich  445 May 28 22:19 stopServer.pyc

	./python/models:
	total 4
	-rw-rw-r-- 1 rich rich   0 May 28 22:34 __init__.py
	-rw-rw-r-- 1 rich rich 107 May 28 22:35 __init__.pyc

	./python/modelsToLoad:
	total 20
	-rwx------ 1 rich rich 150 May 30 00:13 add.py
	-rwx------ 1 rich rich 168 May 28 00:49 divide.py
	-rw-rw-r-- 1 rich rich   0 May 28 22:34 __init__.py
	-rw-rw-r-- 1 rich rich 113 May 28 22:35 __init__.pyc
	-rwx------ 1 rich rich 162 May 28 00:49 multiply.py
	-rwx------ 1 rich rich 246 May 28 00:49 subtract.py

	./resources:
	total 0

	./scala:
	total 20
	-rwx------ 1 rich rich 17702 May 30 00:08 SockClient.scala

2) _Scala Test Script_ 

The scala script is the client serving as the proxy for the python stub model, python factory, and python factory of factories and is found in the src/main/scala folder.

3) _The Server Loading Mechanism_

The python server program is located in the python folder.  The python commands that handle command messages sent from the scala client are found in the python commands directory.  They are loaded by this function called from the pythonserver.py program:

	def importPackageByName(moduleName, name):
		"""
		Import the class 'name' found in package 'moduleName'.  The moduleName
		may be a sub-package (e.g., the module is found in the 'commands' 
		sub-package in the use below).
		"""
		try:
			print "load moduleName = " + moduleName 
			module = __import__(moduleName, globals(), locals(), [name])
		except ImportError:
			return None
		return getattr(module, name)

4) _The Server Commands_

The server commands are: 'addModel', 'removeModel', 'serverStatus', 'executeModel', 'stopServer'.  Notice that the names of these commands _**EXACTLY**_ match the main stem of the python source files in the _commands_ subdirectory.  This is important.  As written the file name is used as the command name when building the function dispatch dictionary in the server.

5) _Test Models_

The _modelsToLoad_ directory contain the sample models to test the server.  It is these files that you will add to the server.  Currently there is an add, divide, multiply and subtract "model" available.  They all take a list of numbers as their principal argument and return result only from it.  The _models_ directory is where the model source will land from an invocation of the addModel command.  The first path in PYTHONPATH's value is assumed writable and used to construct the path for the path/_models/$modelName_.py file.

6) _Sparse Semantic Checking_

There is _some_ (but not enough) semantic checking for arguments.  On the server side and in the model implementations, it will be quite easy to cause the python to throw exceptions.  Any demo (if it is your intention to do that) should have pre-prepared and tested commands with admonishment that this is just a prototype, blah blah.  Of special note is that there are no state transition type checks.  For example, if you failed to add a server before trying to add a model... o well.

7) _Python Server Commands_

All server commands share the argument list. For example, here is the executeModel.py command:

	# ExecuteModelCmd message is formatted like this Scala string : s"$cmd\n$modelName\n$msg"
	class Handler(object): 
		def handler(self, modelDict, host, port, cmdList):
			modelName = cmdList.pop(0).strip()
			# assumption for this revision is that the message is one line of CSV data.
			msg = cmdList.pop(0).strip().split(',')
			cmd = modelDict[modelName]
			results = cmd.handler(msg)
			return results

The _$cmd_ in the comment is the executeModel key that causes the server command dispatcher to send control here.  The _modelName_ is the one that will execute.  Its name is used to find the appropriate model in the model dispatch map.  The _msg_, for this version at least, assumes just one line to be consumed.  That could be relaxed for real use.  The goal here is simply to demonstrate the patterns of communication.

8) _Python Models_

Like the server commands, the models are dispatched by name lookup where the key is the model file name.  Here is an example of one of our test models, add.py:

	class Handler(object): 
		def handler(self, numbers):
			sumofTup = int(numbers.pop(0))
			for v in numbers
				sumofTup += int(v)
			return str(sumofTup)

As you can see, there currently is very little in the way of type checking.  The parameter, _numbers_, is a list of strings.  Again, this is to demonstrate the patterns of communication, not provide a robust example that manages malformed inputs and the rest.

The current implemementation will try to load the class named _Handler_ from the module file.  In other words, it is the model file name that is loaded that distinguishes one model versus another.  The stem of the file name (the part sans .py) effectively becomes part of the model's namespace with the package directory (in our example, _models_) being the parent namespace part.  

Obviously the fully qualified name of the model could be passed to the server and the appropriate directory in a multi-level tree could be updated with the file and loaded.  Checking for directory existence and creating one if it is not there plus checking if **__init__.py** in that directory would be come part of the procedure for adding/loading new python models.

To verify that the c-python models are compilable, this command can be used

	python -m compileall .

It will compile all of the python files in a given folder, creating the .pyc files from it.

9) _Model loading_

The loading of the models is done in the same way as the server commands are loaded... only from a different package directory, namely _models_ instead of _commands_.  This happens in the _addModel_command.

10) _Message Format_

This version of the python server simply accepts a list of strings as the input to the model.  A kamanja message might be represented differently.  A mapped representation can be easily "serialized" to a list of tuple2 and back again.  This might be the interim solution until full type support, server access to the metadata, etc. is completed and integrated.

11) _Important note about Python package directories_

If you don't have an __init__.py file in your package directories, the dynamic loader will __fail__.  Here is a good explanation from [http://python-notes.curiousefficiency.org/en/latest/python_concepts/import_traps.html#the-missing-init-py-trap]

The missing __init__.py trap

This particular trap applies to 2.x releases, as well as 3.x releases up to and including 3.2.

Prior to Python 3.3, filesystem directories, and directories within zipfiles, had to contain an __init__.py in order to be recognised as Python package directories. Even if there is no initialisation code to run when the package is imported, an empty __init__.py file is still needed for the interpreter to find any modules or subpackages in that directory.

This has changed in Python 3.3: now any directory on sys.path with a name that matches the package name being looked for will be recognised as contributing modules and subpackages to that package.

12) _Starting the Server_

When the client script is used to start a server, it invokes the StartPythonServer.sh script that should be on the command path (i.e., the PATH env var) where the python server is installed.  It looks like this:

	#!/bin/bash

	# set the PYTHONPATH to whereever the python root directory will be. 
	# The python server is in the $PYTHONSERVER_HOME and is also the root of 
	# the python server package directory that has modules in the commands 
	# and models sub-directories of $PYTHONSERVER_HOME

	# This script should be placed on the PATH of the account that is running 
	# kamanja (or the test script that is ued to test it.

	# FIXME: add more checks... user port, valid host name, etc..

	host=$1
	port=$2
	if [ -z "$host" -o -z "$port" ]; then
		echo "supply host and listen port for the pythonserver... script failed"
		exit 1
	fi

	# this directory should be set to the "conventional" directory, not rich's
	# home directory tree.  For example, /var/Kamanja/<homedir>/python places it
	# in the KAMANJA_HOME tree.
	export PYTHONSERVER_HOME="/home/rich/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/src/main/python"
	export PYTHONPATH="$PYTHONSERVER_HOME"

	python $PYTHONSERVER_HOME/pythonserver.py --host $host --port $port &

	echo "The python server is running on host $host and is listening on port $port"
	echo "The pythonserver.py is being run from the $PYTHONSERVER_HOME directory"
	echo "The pythonserver commands are found in $PYTHONPATH/commands/ directory"
	echo "The pythonserver models will be added to $PYTHONPATH/models/ directory"
	exit 0

The script simplifies the command process execution from the scala client script.   Other than actually starting the server, it establishes the location of the PYTHONPATH that contains the Kamanja Python Server packages.  For example the subdirectory called _models_ is where the models will be added.

The script expects both the host (e.g., localhost, 192.168.149.2, pepsi.fishpuppy.com) and a listen port value.  Multiple servers can be run on the same host if the port is different.


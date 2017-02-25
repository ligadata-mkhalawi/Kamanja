#!/bin/bash

# set the PYTHONPATH to whereever the python root directory will be. The python server is in 
# the $PYTHONSERVER_HOME and is also the root of the python server package directory that has modules in the commands and models sub-directories of $PYTHONSERVER_HOME

# This script should be placed on the PATH of the account that is running kamanja (or the 
# test script that is ued to test it.

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
export PYTHONSERVER_HOME="/home/rich/github1/dev/r1.6.2/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/src/main/python"
export PYTHONPATH="$PYTHONSERVER_HOME"

python $PYTHONSERVER_HOME/pythonserver.py --host $host --port $port &

echo "The python server is running on host $host and is listening on port $port"
echo "The pythonserver.py is being run from the $PYTHONSERVER_HOME directory"
echo "The pythonserver commands are found in $PYTHONPATH/commands/ directory"
echo "The pythonserver models will be added to $PYTHONPATH/models/ directory"
exit 0

rich@pepper:~/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/src/main/scala$  ./SockClient.scala

**Script Setup**
export ipport=8998
export CLASSPATH=/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar
export METADATA=~/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/src/main/resources/metadata
export KAMANJAPYPATH=~/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/src/main/python
export DATA=~/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/src/main/resources/data

export CLASSPATH=/home/rich/.ivy2/cache/org.json4s/json4s-ast_2.11/jars/json4s-ast_2.11-3.2.11.jar:/home/rich/.ivy2/cache/org.json4s/json4s-core_2.11/jars/json4s-core_2.11-3.2.11.jar:/home/rich/.ivy2/cache/org.json4s/json4s-native_2.11/jars/json4s-native_2.11-3.2.11.jar:/home/rich/.ivy2/cache/org.json4s/json4s-jackson_2.11/jars/json4s-jackson_2.11-3.2.11.jar:/home/rich/.ivy2/cache/com.fasterxml.jackson.core/jackson-core/bundles/jackson-core-2.7.5.jar:/home/rich/.ivy2/cache/com.fasterxml.jackson.core/jackson-databind/bundles/jackson-databind-2.7.5.jar:/home/rich/.ivy2/cache/com.fasterxml.jackson.core/jackson-annotations/bundles/jackson-annotations-2.7.0.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar
export METADATA=~/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/src/main/resources/metadata
export KAMANJAPYPATH=~/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/src/main/python
export DATA=~/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/src/main/resources/data

**Start Server Commands**
SockClient.scala --cmd startServer --user kamanaja --host localhost --port 9999 --pyPath $KAMANJAPYPATH --log4jConfig $KAMANJAPYPATH/bin/pythonlog4j.cfg --fileLogPath $KAMANJAPYPATH/logs/pythonserver.log
SockClient.scala --cmd startServer --user kamanaja --host localhost --port 9998 --pyPath $KAMANJAPYPATH --log4jConfig $KAMANJAPYPATH/bin/pythonlog4j.cfg --fileLogPath $KAMANJAPYPATH/logs/pythonserver.log

**START SERVER DIRECTLY**
./pythonserver.py --host localhost --port 9999 --pythonPath $KAMANJAPYPATH --log4jConfig $KAMANJAPYPATH/bin/pythonlog4j.cfg  --fileLogPath $KAMANJAPYPATH/logs/pythonserver.log
./pythonserver.py --host localhost --port 9998 --pythonPath $KAMANJAPYPATH --log4jConfig $KAMANJAPYPATH/bin/pythonlog4j.cfg --fileLogPath $KAMANJAPYPATH/logs/pythonserver.log


**Stop Server Commands**
SockClient.scala --cmd stopServer --user kamanaja --host localhost --port 9999

SockClient.scala --cmd stopServer --user kamanaja --host localhost --port 9998

**Add Model Commands**
_port 9999_
SockClient.scala --cmd addModel --filePath $METADATA/model/add.py --modelName AddTuple --user kamanaja --modelOptions '{"a": "Int", "b": "Int"}'  --host localhost --port 9999 --pyPath $KAMANJAPYPATH
SockClient.scala --cmd addModel --filePath $METADATA/model/subtract.py --modelName SubtractTuple --user kamanaja --modelOptions '{"a": "Int", "b": "Int"}'  --host localhost --port 9999 --pyPath $KAMANJAPYPATH
SockClient.scala --cmd addModel --filePath $METADATA/model/multiply.py --modelName MultiplyTuple --user kamanaja --modelOptions '{"a": "Int", "b": "Int"}' --host localhost --port 9999 --pyPath $KAMANJAPYPATH
SockClient.scala --cmd addModel --filePath $METADATA/model/divide.py --modelName DivideTuple --user kamanaja --modelOptions '{"a": "Int", "b": "Int"}' --host localhost --port 9999 --pyPath $KAMANJAPYPATH

_port 9998_
SockClient.scala --cmd addModel --filePath $METADATA/model/add.py --modelName AddTuple --user kamanaja --modelOptions '{"a": "Int", "b": "Int"}'  --host localhost --port 9998 --pyPath $KAMANJAPYPATH
SockClient.scala --cmd addModel --filePath $METADATA/model/subtract.py --modelName SubtractTuple --user kamanaja --modelOptions '{"a": "Int", "b": "Int"}'  --host localhost --port 9998 --pyPath $KAMANJAPYPATH
SockClient.scala --cmd addModel --filePath $METADATA/model/multiply.py --modelName MultiplyTuple --user kamanaja --modelOptions '{"a": "Int", "b": "Int"}' --host localhost --port 9998 --pyPath $KAMANJAPYPATH
SockClient.scala --cmd addModel --filePath $METADATA/model/divide.py --modelName DivideTuple --user kamanaja --modelOptions '{"a": "Int", "b": "Int"}' --host localhost --port 9998 --pyPath $KAMANJAPYPATH


**Remove Model Commands**
SockClient.scala --cmd removeModel --modelName AddTuple --user kamanaja --host localhost --port 9999
SockClient.scala --cmd removeModel --modelName SubtractTuple --user kamanaja --host localhost --port 9999
SockClient.scala --cmd removeModel --modelName MultiplyTuple --user kamanaja --host localhost --port 9999
SockClient.scala --cmd removeModel --modelName DivideTuple --user kamanaja --host localhost --port 9999

SockClient.scala --cmd removeModel --modelName AddTuple --user kamanaja --host localhost --port 9998
SockClient.scala --cmd removeModel --modelName SubtractTuple --user kamanaja --host localhost --port 9998
SockClient.scala --cmd removeModel --modelName MultiplyTuple --user kamanaja --host localhost --port 9998
SockClient.scala --cmd removeModel --modelName DivideTuple --user kamanaja --host localhost --port 9998

**Server Status Commands**
SockClient.scala --cmd serverStatus --user kamanaja --host localhost --port 9999

SockClient.scala --cmd serverStatus --user kamanaja --host localhost --port 9998

**Execute Model Commands (data from csv file with header record)**
SockClient.scala --cmd executeModel --modelName AddTuple --filePath $DATA/arithmeticData.txt --user kamanaja --host localhost --port 9999

SockClient.scala --cmd executeModel --modelName AddTuple --filePath $DATA/arithmeticData.txt --user kamanaja --host localhost --port 9998

**------------------------------------------------------------**
**Retrieving pid from server on port 9998 from 'ps aux' output**
**------------------------------------------------------------**

ps aux | grep python | grep '\-\-port 9998' | sed 's/^[A-Za-z0-9][A-Za-z0-9]*[\t ][\t ]*\([0-9][0-9]*\).*$/\1/g'

**-----------------------------------**
**INFORMATION ABOUT MESSAGES AND DATA**
**-----------------------------------**

**The Arithmetic message metadata supplied to the AddModel commands**
{"InputMsgs": [{"name": "org.kamanja.arithmetic.arithmeticMsg", "Fields": {"a": "Int", "b": "Int"} } ], "OutputMsgs": [{"name": "org.kamanja.arithmetic.arithmeticOutMsg", "Fields": {"a": "Int", "b": "Int", "result": "Int"} } ] }

**Sample Arithmetic message data supplied to the ExecuteModel commands**
[{"name": "org.kamanja.arithmetic.arithmeticMsg", "fields": {"a": 1, "b": 2 } } 

**Sample Arithmetic message data in the $DATA/arithmeticData.txt**
__csv data with header whose values match input msg_
a,b
1,1
2,2
3,3
4,4
5,5
6,6
7,7
8,8
9,9


_**************************************************_
_Add model debug command using the application form_
_**************************************************_
java -Xdebug -Xrunjdwp:transport=dt_socket,address="$ipport",server=y -Dlog4j.configurationFile=file:/tmp/drdigital/Kamanja-1.4.1_2.11/config/log4j2.xml -cp /tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/kamanjamanager_2.11-1.4.1.jar:/home/rich/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/target/scala-2.11/pythonmodelprototype_2.11-1.0.jar SockClient --cmd startServer --user kamanaja --host localhost --port 9999 --pyPath $KAMANJAPYPATH

_*****************************************************_
_Start Server debug command using the application form_
_*****************************************************_
java -Xdebug  -cp /tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/kamanjamanager_2.11-1.4.1.jar:/home/rich/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/target/scala-2.11/pythonmodelprototype_2.11-1.0.jar SockClient --cmd startServer --user kamanaja --host localhost --port 9999 --pyPath $KAMANJAPYPATH

_******************************************************_
_Current status message returned from SockClient script_
_******************************************************_

{
  "result": "Server started successfully",
  "pid": 22537
}

_*********************************************_
_Add model commands using the application form_
_*********************************************_

java  -cp /tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/kamanjamanager_2.11-1.4.1.jar:/home/rich/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/target/scala-2.11/pythonmodelprototype_2.11-1.0.jar SockClient --cmd addModel --filePath $METADATA/model/add.py --modelName AddTuple --user kamanaja --modelOptions '{"a": "Int", "b": "Int"}'  --host localhost --port 9999 --pyPath $KAMANJAPYPATH
java  -cp /tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/kamanjamanager_2.11-1.4.1.jar:/home/rich/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/target/scala-2.11/pythonmodelprototype_2.11-1.0.jar SockClient --cmd addModel --filePath $METADATA/model/subtract.py --modelName SubtractTuple --user kamanaja --modelOptions '{"a": "Int", "b": "Int"}'  --host localhost --port 9999 --pyPath $KAMANJAPYPATH
java  -cp /tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/kamanjamanager_2.11-1.4.1.jar:/home/rich/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/target/scala-2.11/pythonmodelprototype_2.11-1.0.jar SockClient --cmd addModel --filePath $METADATA/model/multiply.py --modelName MultiplyTuple --user kamanaja --modelOptions '{"a": "Int", "b": "Int"}'  --host localhost --port 9999 --pyPath $KAMANJAPYPATH
java  -cp /tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/kamanjamanager_2.11-1.4.1.jar:/home/rich/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/target/scala-2.11/pythonmodelprototype_2.11-1.0.jar SockClient --cmd addModel --filePath $METADATA/model/divide.py --modelName DivideTuple --user kamanaja --modelOptions '{"a": "Int", "b": "Int"}'  --host localhost --port 9999 --pyPath $KAMANJAPYPATH

_**************************************************************_
_debug an addModel command_
_**************************************************************_

java -Xdebug -Xrunjdwp:transport=dt_socket,address="$ipport",server=y -Dlog4j.configurationFile=file:/tmp/drdigital/Kamanja-1.4.1_2.11/config/log4j2.xml -cp /tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/kamanjamanager_2.11-1.4.1.jar:/home/rich/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/target/scala-2.11/pythonmodelprototype_2.11-1.0.jar SockClient --cmd addModel --filePath $METADATA/model/add.py --modelName AddTuple --user kamanaja --modelOptions '{"a": "Int", "b": "Int"}'  --host localhost --port 9999 --pyPath $KAMANJAPYPATH

_**************************************************************_
_executeModel single unit tests_
_**************************************************************_

java  -cp /tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/kamanjamanager_2.11-1.4.1.jar:/home/rich/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/target/scala-2.11/pythonmodelprototype_2.11-1.0.jar SockClient --cmd executeModel --modelName MultiplyTuple --user kamanaja --msg '{"a": 2, "b": 2 }'

java  -Xdebug -Xrunjdwp:transport=dt_socket,address="$ipport",server=y -Dlog4j.configurationFile=file:/tmp/drdigital/Kamanja-1.4.1_2.11/config/log4j2.xml  -cp /tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/kamanjamanager_2.11-1.4.1.jar:/home/rich/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/target/scala-2.11/pythonmodelprototype_2.11-1.0.jar SockClient --cmd executeModel --modelName MultiplyTuple --user kamanaja --msg '{"a": 2, "b": 2 }'

_**************************************************************_
_executeModel multiple unit tests via filePath content AS APPLICATION_
_**************************************************************_

java -cp /tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/kamanjamanager_2.11-1.4.1.jar:/home/rich/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/target/scala-2.11/pythonmodelprototype_2.11-1.0.jar SockClient --cmd executeModel --modelName AddTuple --user kamanaja --filePath $DATA/arithmeticData.txt

java -cp /tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/kamanjamanager_2.11-1.4.1.jar:/home/rich/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/target/scala-2.11/pythonmodelprototype_2.11-1.0.jar SockClient --cmd executeModel --modelName SubtractTuple --user kamanaja --filePath $DATA/arithmeticData.txt

java -cp /tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/kamanjamanager_2.11-1.4.1.jar:/home/rich/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/target/scala-2.11/pythonmodelprototype_2.11-1.0.jar SockClient --cmd executeModel --modelName MultiplyTuple --user kamanaja --filePath $DATA/arithmeticData.txt

java -cp /tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/kamanjamanager_2.11-1.4.1.jar:/home/rich/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/target/scala-2.11/pythonmodelprototype_2.11-1.0.jar SockClient --cmd executeModel --modelName DivideTuple --user kamanaja --filePath $DATA/arithmeticData.txt

**executeModel debug setup**
java  -Xdebug -Xrunjdwp:transport=dt_socket,address="$ipport",server=y -Dlog4j.configurationFile=file:/tmp/drdigital/Kamanja-1.4.1_2.11/config/log4j2.xml  -cp /tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/kamanjamanager_2.11-1.4.1.jar:/home/rich/github1/dev/r1.5.0/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonModelPrototype/target/scala-2.11/pythonmodelprototype_2.11-1.0.jar SockClient --cmd executeModel --modelName MultiplyTuple --user kamanaja --filePath $DATA/arithmeticData.txt

_**************************************************************_
_executeModel multiple unit tests via filePath content AS SCRIPT_
_**************************************************************_

SockClient.scala --cmd executeModel --modelName AddTuple --filePath $DATA/arithmeticData.txt --user kamanaja --host localhost --port 9999
SockClient.scala --cmd executeModel --modelName SubtractTuple --filePath $DATA/arithmeticData.txt --user kamanaja --host localhost --port 9999
SockClient.scala --cmd executeModel --modelName MultiplyTuple --filePath $DATA/arithmeticData.txt --user kamanaja --host localhost --port 9999
SockClient.scala --cmd executeModel --modelName DivideTuple --filePath $DATA/arithmeticData.txt --user kamanaja --host localhost --port 9999


SockClient.scala --cmd executeModel --modelName AddTuple --filePath $DATA/arithmeticData.txt --user kamanaja --host localhost --port 9998
SockClient.scala --cmd executeModel --modelName SubtractTuple --filePath $DATA/arithmeticData.txt --user kamanaja --host localhost --port 9998
SockClient.scala --cmd executeModel --modelName MultiplyTuple --filePath $DATA/arithmeticData.txt --user kamanaja --host localhost --port 9998
SockClient.scala --cmd executeModel --modelName DivideTuple --filePath $DATA/arithmeticData.txt --user kamanaja --host localhost --port 9998


{
  "Cmd": "stopServer",
  "Port": "9998",
  "Result": "Host pepper.fishpuppy.com listening on port 9998 to be stopped by user command",
  "Server": "pepper.fishpuppy.com"
}

{
  "InputFields": {
    "a": "Int",
    "b": "Int"
  },
  "Cmd": "addModel",
  "Server": "pepper.fishpuppy.com",
  "OutputFields": {
    "a": "Int",
    "b": "Int",
    "result": "Int"
  },
  "Result": "model add.AddTuple added",
  "Port": "9998"
}

{
  "Cmd": "removeModel",
  "Port": "9998",
  "Result": "model AddTuple removed",
  "Server": "pepper.fishpuppy.com"
}

Map("name" -> "john doe","age" -> 18,"hasChild" -> true)


{
  "Cmd": "serverStatus",
  "Port": "9998",
  "Result": "Active models are: ['MultiplyTuple', 'SubtractTuple', 'DivideTuple', 'PythonInstallPath']",
  "Server": "pepper.fishpuppy.com"
}

{
  "a": 2,
  "b": 2,
  "result": 4
}

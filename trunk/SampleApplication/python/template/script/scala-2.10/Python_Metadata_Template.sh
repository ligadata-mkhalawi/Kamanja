#!/usr/bin/env bash

# Install the metadata required to run the models that exercise the python
# server(s). The models added are add.py, multiply.py, subtract.py, and divide.py...
# admittedly simple minded "models" that add/multiply/subtract/divide two numbers
# producing a result and the inputs that were used to obtain the result.
#
# Any of these models show the basic structure needed to build a more substantial
# model.  Consult the online documentation for more information about python models.

KAMANJA_HOME={InstallDirectory}

# cluster config

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties upload cluster config $KAMANJA_HOME/config/ClusterConfig.json

# messages

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/arithmeticMsg.json TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/arithmeticOutMsg.json TENANTID tenant1

# models

$KAMANJA_HOME/bin/kamanja debug add model python $PY_METADATA/model/add.py MODELNAME AddTuple MESSAGENAME org.kamanja.arithmetic.arithmeticMsg OUTMESSAGE org.kamanja.arithmetic.arithmeticOutMsg MODELOPTIONS '{"InputTypeInfo": {"a": "Int", "b": "Int"}, "OutputTypeInfo": {"a": "Int", "b": "Int", "result": "Int"} }' TENANTID tenant1

$KAMANJA_HOME/bin/kamanja debug add model python $PY_METADATA/model/mutliply.py MODELNAME MultiplyTuple MESSAGENAME org.kamanja.arithmetic.arithmeticMsg OUTMESSAGE org.kamanja.arithmetic.arithmeticOutMsg MODELOPTIONS '{"InputTypeInfo": {"a": "Int", "b": "Int"}, "OutputTypeInfo": {"a": "Int", "b": "Int", "result": "Int"} }' TENANTID tenant1

$KAMANJA_HOME/bin/kamanja debug add model python $PY_METADATA/model/subtract.py MODELNAME SubtractTuple MESSAGENAME org.kamanja.arithmetic.arithmeticMsg OUTMESSAGE org.kamanja.arithmetic.arithmeticOutMsg MODELOPTIONS '{"InputTypeInfo": {"a": "Int", "b": "Int"}, "OutputTypeInfo": {"a": "Int", "b": "Int", "result": "Int"} }' TENANTID tenant1

$KAMANJA_HOME/bin/kamanja debug add model python $PY_METADATA/model/divide.py MODELNAME DivideTuple MESSAGENAME org.kamanja.arithmetic.arithmeticMsg OUTMESSAGE org.kamanja.arithmetic.arithmeticOutMsg MODELOPTIONS '{"InputTypeInfo": {"a": "Int", "b": "Int"}, "OutputTypeInfo": {"a": "Int", "b": "Int", "result": "Int"} }' TENANTID tenant1


# Add the input adapter (CSV) binding

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "TestIn_1", "MessageName": "org.kamanja.arithmetic.arithmeticMsg", "Serializer": "com.ligadata.kamanja.serializer.csvserdeser", "Options": {"alwaysQuoteFields": false, "fieldDelimiter": ","} }'

# Add the output adapter (JSON) binding

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "TestOut_1", "MessageNames": ["org.kamanja.arithmetic.arithmeticOutMsg"], "Serializer": " com.ligadata.kamanja.serializer.JsonSerDeser"}'

# system adapter message binding

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMFILE $KAMANJA_HOME/config/SystemMsgs_Adapter_Binding.json


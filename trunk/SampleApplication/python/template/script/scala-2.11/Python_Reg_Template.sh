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

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/loanMsg.json TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/loanOutMsg.json TENANTID tenant1

# models

$KAMANJA_HOME/bin/kamanja  add model python $KAMANJA_HOME/input/SampleApplications/metadata/model/loan.py MODELNAME loan.LoanTuple MESSAGENAME org.kamanja.arithmetic.loanMsg OUTMESSAGE org.kamanja.arithmetic.loanOutMsg TENANTID tenant1 MODELVERSION 0.00001


# Add the input adapter (CSV) binding

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "LoanInput", "MessageName": "org.kamanja.arithmetic.loanMsg", "Serializer": "com.ligadata.kamanja.serializer.csvserdeser", "Options": {"alwaysQuoteFields": false, "fieldDelimiter": ","} }'

# Add the output adapter (JSON) binding

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "TestOut_1", "MessageNames": ["org.kamanja.arithmetic.loanOutMsg"], "Serializer": " com.ligadata.kamanja.serializer.JsonSerDeser"}'

# system adapter message binding

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMFILE $KAMANJA_HOME/config/SystemMsgs_Adapter_Binding.json


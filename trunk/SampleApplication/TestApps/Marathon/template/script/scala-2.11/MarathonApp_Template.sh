#!/usr/bin/env bash

KAMANJA_HOME={InstallDirectory}

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties upload cluster config $KAMANJA_HOME/config/ClusterConfig_kafka_v10.json

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/Message_Definition_Marathon.json TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/OutMessage_Definition_Marathon.json TENANTID tenant1

# $KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add model kpmml $KAMANJA_HOME/input/SampleApplications/metadata/model/KPMML_Model_HelloWorld.xml TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties upload compile config $KAMANJA_HOME/config/Model_Config_Marathon.json

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add model java $KAMANJA_HOME/input/SampleApplications/metadata/model/Marathon.java DEPENDSON marathonmodel TENANTID tenant1 

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add adaptermessagebinding FROMFILE $KAMANJA_HOME/config/Marathon_Adapter_Binding.json

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add adaptermessagebinding FROMFILE $KAMANJA_HOME/config/SystemMsgs_Adapter_Binding.json

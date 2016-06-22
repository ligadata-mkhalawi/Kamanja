#!/usr/bin/env bash

KAMANJA_HOME={InstallDirectory}

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties upload cluster config $KAMANJA_HOME/config/ClusterConfig.json

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add container $KAMANJA_HOME/input/SampleApplications/metadata/container/CustAlertHistory_Finance.json TENANTID low211 PropertiesFile $KAMANJA_HOME/config/LowBalanceProp.json

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add container $KAMANJA_HOME/input/SampleApplications/metadata/container/CustPreferences_Finance.json TENANTID low211 PropertiesFile $KAMANJA_HOME/config/LowBalanceProp.json

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add container $KAMANJA_HOME/input/SampleApplications/metadata/container/CustomerInfo_Finance.json TENANTID low211 PropertiesFile $KAMANJA_HOME/config/LowBalanceProp.json

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add container $KAMANJA_HOME/input/SampleApplications/metadata/container/GlobalPreferences_Finance.json TENANTID low211 PropertiesFile $KAMANJA_HOME/config/LowBalanceProp.json

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/TransactionMsg_Finance.json TENANTID low211 PropertiesFile $KAMANJA_HOME/config/LowBalanceProp.json

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/LowBalanceAlertOutputMsg.json TENANTID low211 PropertiesFile $KAMANJA_HOME/config/LowBalanceProp.json

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties upload compile config $KAMANJA_HOME/config/LBCompileCfg_Finance.json

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add model scala $KAMANJA_HOME/input/SampleApplications/metadata/model/LowBalanceAlert_Finance.scala DEPENDSON lowbalancealert TENANTID low211 PropertiesFile $KAMANJA_HOME/config/LowBalanceProp.json

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add adaptermessagebinding FROMFILE $KAMANJA_HOME/config/Finance_Adapter_Binding.json

$KAMANJA_HOME/bin/kamanja $KAMANJA_HOME/config/MetadataAPIConfig.properties add adaptermessagebinding FROMFILE $KAMANJA_HOME/config/SystemMsgs_Adapter_Binding.json

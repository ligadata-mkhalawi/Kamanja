#!/usr/bin/env bash
KAMANJA_HOME={InstallDirectory}

currentKamanjaVersion=1.6.2

java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/kvinit_2.10-${currentKamanjaVersion}.jar com.ligadata.tools.kvinit.KVInit --typename com.ligadata.kamanja.samples.containers.AccountAggregatedUsage          --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/AccountAggregatedUsage_Telecom.dat --ignorerecords 1 --deserializer "com.ligadata.kamanja.serializer.csvserdeser" --optionsjson '{"alwaysQuoteFields":false,"fieldDelimiter":",","valDelimiter":"~"}'
java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/kvinit_2.10-${currentKamanjaVersion}.jar com.ligadata.tools.kvinit.KVInit --typename com.ligadata.kamanja.samples.containers.AccountInfo                     --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/AccountInfo_Telecom.dat            --ignorerecords 1 --deserializer "com.ligadata.kamanja.serializer.csvserdeser" --optionsjson '{"alwaysQuoteFields":false,"fieldDelimiter":",","valDelimiter":"~"}'
java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/kvinit_2.10-${currentKamanjaVersion}.jar com.ligadata.tools.kvinit.KVInit --typename com.ligadata.kamanja.samples.containers.SubscriberAggregatedUsage       --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/SubscriberAggregatedUsage_Telecom.dat --ignorerecords 1 --deserializer "com.ligadata.kamanja.serializer.csvserdeser" --optionsjson '{"alwaysQuoteFields":false,"fieldDelimiter":",","valDelimiter":"~"}'
java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/kvinit_2.10-${currentKamanjaVersion}.jar com.ligadata.tools.kvinit.KVInit --typename com.ligadata.kamanja.samples.containers.SubscriberGlobalPreferences     --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/SubscriberGlobalPreferences_Telecom.dat --ignorerecords 1 --deserializer "com.ligadata.kamanja.serializer.csvserdeser" --optionsjson '{"alwaysQuoteFields":false,"fieldDelimiter":",","valDelimiter":"~"}'
java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/kvinit_2.10-${currentKamanjaVersion}.jar com.ligadata.tools.kvinit.KVInit --typename com.ligadata.kamanja.samples.containers.SubscriberInfo                  --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/SubscriberInfo_Telecom.dat            --ignorerecords 1 --deserializer "com.ligadata.kamanja.serializer.csvserdeser" --optionsjson '{"alwaysQuoteFields":false,"fieldDelimiter":",","valDelimiter":"~"}'
java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/kvinit_2.10-${currentKamanjaVersion}.jar com.ligadata.tools.kvinit.KVInit --typename com.ligadata.kamanja.samples.containers.SubscriberPlans                 --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/SubscriberPlans_Telecom.dat --ignorerecords 1 --deserializer "com.ligadata.kamanja.serializer.csvserdeser" --optionsjson '{"alwaysQuoteFields":false,"fieldDelimiter":",","valDelimiter":"~"}'

#!/usr/bin/env bash

KAMANJA_HOME={InstallDirectory}

currentKamanjaVersion=1.6.0

java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/extractdata_2.11-${currentKamanjaVersion}.jar com.ligadata.ExtractData.ExtractData "$@"

#!/usr/bin/env bash

KAMANJA_HOME={InstallDirectory}

currentKamanjaVersion=1.5.3

java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/jsonchecker_2.10-${currentKamanjaVersion}.jar com.ligadata.jsonutility.JsonChecker "$@"


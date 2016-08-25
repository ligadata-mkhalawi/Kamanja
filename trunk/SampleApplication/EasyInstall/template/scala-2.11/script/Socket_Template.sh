#!/usr/bin/env bash

KAMANJA_HOME={InstallDirectory}

currentKamanjaVersion=1.5.3
java -cp /opt/KAM/Kamanja-${currentKamanjaVersion}_2.11/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:/opt/KAM/Kamanja-1.5.3_2.11/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:/opt/KAM/Kamanja-1.5.3_2.11/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:/opt/KAM/Kamanja-${currentKamanjaVersion}_2.11/lib/system/kamanjamanager_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/socket_2.11-${currentKamanjaVersion}.jar com.ligadata.MetaDataApiClient.KamanjaService

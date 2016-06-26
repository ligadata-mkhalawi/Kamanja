#!/bin/sh
KAMANJA_HOME={InstallDirectory}

cd {InstallDirectory}/bin

java -Dh2.baseDir=../storage -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.10-1.5.0.jar org.h2.tools.Server -tcp -tcpPort 9100 -tcpAllowOthers
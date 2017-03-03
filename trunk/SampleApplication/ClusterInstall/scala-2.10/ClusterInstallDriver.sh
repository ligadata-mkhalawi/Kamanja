#!/bin/bash

# ClusterInstallDriver.sh
#

currentKamanjaVersion=1.6.2
KAMANJA_INSTALL_HOME=$(dirname "$0")

java -Dlog4j.configurationFile=file:$KAMANJA_INSTALL_HOME/log4j2.xml -jar $KAMANJA_INSTALL_HOME/ClusterInstallerDriver-${currentKamanjaVersion} "$@"


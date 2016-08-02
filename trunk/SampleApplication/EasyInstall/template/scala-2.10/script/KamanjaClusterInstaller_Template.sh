#!/usr/bin/env bash

java -Dlog4j.configurationFile=file:{KAMANJA_CLUSTER_INSTALL_HOME}/ClusterInstall/log4j2.xml -jar {KAMANJA_CLUSTER_INSTALL_HOME}/ClusterInstall/ClusterInstallerDriver-* $@
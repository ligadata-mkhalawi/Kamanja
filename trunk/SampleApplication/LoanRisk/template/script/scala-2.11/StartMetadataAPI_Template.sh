#!/usr/bin/env bash

currentKamanjaVersion=1.5.3

java -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/metadataapi_2.11-${currentKamanjaVersion}.jar com.ligadata.MetadataAPI.StartMetadataAPI --config {InstallDirectory}/input/SampleApplications/metadata/config/MetadataAPIConfig.properties

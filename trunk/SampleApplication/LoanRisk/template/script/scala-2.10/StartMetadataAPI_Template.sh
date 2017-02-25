#!/usr/bin/env bash

currentKamanjaVersion=1.6.2

java -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/metadataapi_2.10-${currentKamanjaVersion}.jar com.ligadata.MetadataAPI.StartMetadataAPI --config {InstallDirectory}/input/SampleApplications/metadata/config/MetadataAPIConfig.properties

#!/usr/bin/env bash
scalaver=2.11
release=1.6.1
java -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_$scalaver-$release.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_$scalaver-$release.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_$scalaver-$release.jar:{InstallDirectory}/lib/system/metadataapi_$scalaver-$release.jar com.ligadata.MetadataAPI.StartMetadataAPI --config {InstallDirectory}/config/MetadataAPIConfig.properties

#!/usr/bin/env bash
java -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.11-1.4.0.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.11-1.4.0.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.11-1.4.0.jar:{InstallDirectory}/lib/system/metadataapi_2.11-1.4.0.jar com.ligadata.MetadataAPI.StartMetadataAPI --config {InstallDirectory}/input/SampleApplications/metadata/config/MetadataAPIConfig.properties
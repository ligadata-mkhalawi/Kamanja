#!/usr/bin/env bash
# Sample shell script to execute GenerateAdapterBindings
sbt '++2.11.7 GenerateAdapterBindings/assembly'
cp /media/home2/Kamanja/trunk/Utils/Migrate/GenerateAdapterBindings/target/scala-2.11/GenerateAdapterBindings_2.11-1.4.1.jar $KAMANJA_HOME/lib/system
java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:$KAMANJA_HOME/lib/system/GenerateAdapterBindings_2.11-1.4.1.jar com.ligadata.Migrate.GenerateAdapterBindings --config /media/home2/Kamanja/trunk/MetadataAPI/src/test/resources/Metadata/config/sample_adapters.json --outfile /tmp/AdapterMessageBindings.json
echo result = $?

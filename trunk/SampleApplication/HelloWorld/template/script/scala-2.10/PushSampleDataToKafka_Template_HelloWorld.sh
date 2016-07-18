#!/usr/bin/env bash
KAMANJA_HOME={InstallDirectory}

java -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-1.4.3.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-1.4.3.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-1.4.3.jar:$KAMANJA_HOME/lib/system/simplekafkaproducer_2.10-1.4.3.jar com.ligadata.tools.SimpleKafkaProducer --gz true --topics "helloworldinput" --threads 1 --topicpartitions 8 --brokerlist "localhost:9092" --files "$KAMANJA_HOME/input/SampleApplications/data/Input_Data_HelloWorld.csv.gz" --partitionkeyidxs "0" --format CSV

#!/bin/bash
if [ "$KAFKA_HOME" = "" ]; then
    echo "Please set KAFKA_HOME"
    exit 1
fi

partitions=8 # by default
if [ "$#" -gt 1 ]; then
	tag="$1"
	val="$2"
	if [ $tag == "--partitions" ]; then ## FIXME:  check for valid value nonEmpty and numeric
		partitions=$val
	fi
	echo "partitions value = $partitions"
fi

$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions "$partitions" --topic testin_1
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions "$partitions" --topic testout_1
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions "$partitions" --topic testout_hw
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic teststatus_1
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions "$partitions" --topic helloworldinput
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions "$partitions" --topic loaninput
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions "$partitions" --topic medicalinput
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions "$partitions" --topic financeinput
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions "$partitions" --topic telecominput
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions "$partitions" --topic testfailedevents_1
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions "$partitions" --topic testmessageevents_1
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions "$partitions" --topic velocitymetrics

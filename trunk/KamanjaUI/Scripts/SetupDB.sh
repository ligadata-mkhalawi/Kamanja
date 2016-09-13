#!/bin/bash

# SetupDB.sh

Usage()
{
    echo
    echo "Create and populate KamanjaViews table."
    echo
    echo "Usage:"
    echo "      SetupDB.sh --HostName <Host Name> "
    echo "                 --DbName  <Database Name>  "
    echo "                 --UserId <Database UserId>"
    echo "                 --Password <Database Password> "
    echo "                 --OrientdbHome <Orientdb Home> "
    echo 
}

if [[ "$#" -eq 10 ]]; then
    echo 
else 
    echo 
    echo "Problem: Incorrect number of arguments."
    Usage
    exit 1
fi

name1=$1

if [[ "$name1" != "--HostName" && "$name1" != "--DbName" && "$name1" != "--UserId" && "$name1" != "--Password" && "$name1" != "--OrientdbHome" ]]; then
    echo 
	echo "Problem: Not found valid arguments."
    Usage
	exit 1
fi

# Collect the named parameters 
hostName=""
dbName=""
userId=""
password=""
ORIENTDB_HOME="."

while [ "$1" != "" ]; do
    case $1 in
        --HostName )       shift
                           hostName=$1
                           ;;
        --DbName )         shift
                           dbName=$1
                           ;;
        --UserId )         shift
                           userId=$1
                           ;;
        --Password )       shift
                           password=$1
                           ;;
        --OrientdbHome )   shift
                           ORIENTDB_HOME=$1
                           ;;
        --help )           Usage
                           exit 0
                           ;;
        * )                echo
                           echo "Problem: Argument $1 is invalid named parameter."
                           Usage
                           exit 1
                           ;;
    esac
    shift
done

pwdnm=$(pwd -P)

dirnm=$(dirname "$0")
cd $dirnm

install_dir=$(pwd -P)
# install_dir_repl=$(echo $install_dir | sed 's/\//\\\//g')

sed "s/{HostName}/$hostName/g;s/{DbName}/$dbName/g;s/{UserId}/$userId/g;s/{Password}/$password/g" $install_dir/SetupDB_template.sql > $install_dir/SetupDB.sql

$ORIENTDB_HOME/bin/console.sh $install_dir/SetupDB.sql

cd $pwdnm


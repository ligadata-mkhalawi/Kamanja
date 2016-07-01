/bin/bash

# PopulateKamanjaViews.sh

Usage()
{
    echo
    echo "Create and populate KamanjaViews table."
    echo
    echo "Usage:"
    echo "      PopulateKamanjaViews.sh --HostName <Host Name> "
    echo "                              --DbName  <Database Name>  "
    echo "                              --UserId <Database UserId>"
    echo "                              --Password <Database Password> "
    echo "                              --OrientdbHome <Orientdb Home> "
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

$ORIENTDB_HOME/bin/console.sh 'CONNECT REMOTE:'"${hostName}/${dbName} ${userId} ${password}"';DROP CLASS KamanjaViews ;CREATE CLASS KamanjaViews ;Create Property KamanjaViews.ViewName STRING ;Create Property KamanjaViews.MainQuery EMBEDDED  ;Create Property KamanjaViews.DepthQuery EMBEDDED  ;Create Property KamanjaViews.PropertiesQuery EMBEDDED  ;Create Property KamanjaViews.SymbolClasses EMBEDDEDSet  ;Create Property KamanjaViews.FilterClasses EMBEDDEDSet ;Create Property KamanjaViews.isDefault BOOLEAN ;Create Property KamanjaViews.isActive BOOLEAN ;'

$ORIENTDB_HOME/bin/console.sh 'CONNECT REMOTE:'"${hostName}/${dbName} ${userId} ${password}"';INSERT INTO KamanjaViews content {"ViewName": "Universal", "MainQuery": {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select @rid as @rid, @class as @class, @version as @version, ID, Type, Name, out, in from (Traverse * from V while @class in (select FilterClasses from KamanjaViews where ViewName = '${ViewName}')))"], "parameters":{"ViewName": "ViewName"}},"DepthQuery": {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select @rid as @rid, @class as @class, @version as @version, ID, Type, Name, out, in from (Traverse * from ${RID} while $depth <= 2 and @class in (select FilterClasses from KamanjaViews where ViewName = '${ViewName}')))"], "parameters":{"ViewName": "ViewName", "RID":"@rid"}},"PropertiesQuery" : {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select ID, Name, Namespace, Version, CreatedBy, CreatedTime, LastModifiedTime, Tenant, Description, Author, Active from ${RID})"], "parameters":{"ViewName": "ViewName", "RID":"@rid"}},"SymbolClasses": ["Input", "Output", "Storage", "Message", "Model", "Container", "System"], "FilterClasses": ["Input", "Output", "Storage", "Message", "Model", "Container", "Produces", "ConsumedBy", "StoredBy", "SentTo", "System"], "isDefault": true, "isActive": true} ;'

$ORIENTDB_HOME/bin/console.sh 'CONNECT REMOTE:'"${hostName}/${dbName} ${userId} ${password}"';INSERT INTO KamanjaViews content {"ViewName": "DAG", "MainQuery": {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select @rid as @rid, @class as @class, @version as @version, ID, Type, Name, out, in from (Traverse * from V while @class in (select FilterClasses from KamanjaViews where ViewName = '${ViewName}')))"], "parameters":{"ViewName": "ViewName"}},"DepthQuery": {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select @rid as @rid, @class as @class, @version as @version, ID, Type, Name, out, in from (Traverse * from ${RID} while $depth <= 2 and @class in (select FilterClasses from KamanjaViews where ViewName = '${ViewName}')))"], "parameters":{"ViewName": "ViewName", "RID":"@rid"}},"PropertiesQuery" : {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select ID, Name, Namespace, Version, CreatedBy, CreatedTime, LastModifiedTime, Tenant, Description, Author, Active from ${RID})"], "parameters":{"ViewName": "ViewName", "RID":"@rid"}},"SymbolClasses": ["Input", "Output", "Storage", "Model", "System"], "FilterClasses": ["Input", "Output", "Storage", "Model", "MessageE", "System"], "isDefault": false, "isActive": true} ;'

$ORIENTDB_HOME/bin/console.sh 'CONNECT REMOTE:'"${hostName}/${dbName} ${userId} ${password}"';INSERT INTO KamanjaViews content {"ViewName": "DAG Input", "MainQuery": {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select @rid as @rid, @class as @class, @version as @version, ID, Type, Name, out, in from (Traverse * from V while @class in ['Input']))"], "parameters":{"ViewName": "ViewName"}},"DepthQuery": {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select @rid as @rid, @class as @class, @version as @version, ID, Type, Name, out, in from (Traverse * from ${RID} while $depth <= 2 and @class in (select FilterClasses from KamanjaViews where ViewName = '${ViewName}')))"], "parameters":{"ViewName": "ViewName", "RID":"@rid"}},"PropertiesQuery" : {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select ID, Name, Namespace, Version, CreatedBy, CreatedTime, LastModifiedTime, Tenant, Description, Author, Active from ${RID})"], "parameters":{"ViewName": "ViewName", "RID":"@rid"}},"SymbolClasses": ["Input", "Output", "Storage", "Model", "System"], "FilterClasses": ["Input", "Output", "Storage", "Model", "MessageE", "System"], "isDefault": false, "isActive": true} ;'


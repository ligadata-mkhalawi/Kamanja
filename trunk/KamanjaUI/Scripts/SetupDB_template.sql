SET echo TRUE ;

CONNECT REMOTE:{HostName}/{DbName} {UserId} {Password};

DROP CLASS KamanjaViews ;
CREATE CLASS KamanjaViews ;

Create Property KamanjaViews.ViewName STRING ;
Create Property KamanjaViews.MainQuery EMBEDDED  ;
Create Property KamanjaViews.DepthQuery EMBEDDED  ;
Create Property KamanjaViews.PropertiesQuery EMBEDDED  ;
Create Property KamanjaViews.SymbolClasses EMBEDDEDSet  ;
Create Property KamanjaViews.FilterClasses EMBEDDEDSet ;
Create Property KamanjaViews.isDefault BOOLEAN ;
Create Property KamanjaViews.isActive BOOLEAN ;

INSERT INTO KamanjaViews content {"ViewName": "Universal", "MainQuery": {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select @rid as @rid, @class as @class, @version as @version, ID, Type, Name, out, in from (Traverse * from V while @class in (select FilterClasses from KamanjaViews where ViewName = '${ViewName}')))"], "parameters":{"ViewName": "ViewName"}},"DepthQuery": {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select @rid as @rid, @class as @class, @version as @version, ID, Type, Name, out, in from (Traverse * from ${RID} while $depth <= 2 and @class in (select FilterClasses from KamanjaViews where ViewName = '${ViewName}')))"], "parameters":{"ViewName": "ViewName", "RID":"@rid"}},"PropertiesQuery" : {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select ID, Name, Namespace, Version, CreatedBy, CreatedTime, LastModifiedTime, Tenant, Description, Author, Active from ${RID})"], "parameters":{"ViewName": "ViewName", "RID":"@rid"}},"SymbolClasses": ["Input", "Output", "Storage", "Message", "Model", "System"], "FilterClasses": ["Input", "Output", "Storage", "Message", "Model", "Produces", "ConsumedBy", "StoredBy", "SentTo", "System"], "isDefault": true, "isActive": true}

INSERT INTO KamanjaViews content {"ViewName": "DAG", "MainQuery": {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select @rid as @rid, @class as @class, @version as @version, ID, Type, Name, out, in from (Traverse * from V while @class in (select FilterClasses from KamanjaViews where ViewName = '${ViewName}')))"], "parameters":{"ViewName": "ViewName"}},"DepthQuery": {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select @rid as @rid, @class as @class, @version as @version, ID, Type, Name, out, in from (Traverse * from ${RID} while $depth <= 2 and @class in (select FilterClasses from KamanjaViews where ViewName = '${ViewName}')))"], "parameters":{"ViewName": "ViewName", "RID":"@rid"}},"PropertiesQuery" : {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select ID, Name, Namespace, Version, CreatedBy, CreatedTime, LastModifiedTime, Tenant, Description, Author, Active from ${RID})"], "parameters":{"ViewName": "ViewName", "RID":"@rid"}},"SymbolClasses": ["Input", "Output", "Storage", "Model", "System"], "FilterClasses": ["Input", "Output", "Storage", "Model", "MessageE", "System"], "isDefault": false, "isActive": true}

INSERT INTO KamanjaViews content {"ViewName": "DAG Input", "MainQuery": {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select @rid as @rid, @class as @class, @version as @version, ID, Type, Name, out, in from (Traverse * from V while @class in ['Input']))"], "parameters":{"ViewName": "ViewName"}},"DepthQuery": {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select @rid as @rid, @class as @class, @version as @version, ID, Type, Name, out, in from (Traverse * from ${RID} while $depth <= 2 and @class in (select FilterClasses from KamanjaViews where ViewName = '${ViewName}')))"], "parameters":{"ViewName": "ViewName", "RID":"@rid"}},"PropertiesQuery" : {"@type":"d", "commands":["select @this.toJSON() as jsonrec from (select ID, Name, Namespace, Version, CreatedBy, CreatedTime, LastModifiedTime, Tenant, Description, Author, Active from ${RID})"], "parameters":{"ViewName": "ViewName", "RID":"@rid"}},"SymbolClasses": ["Input", "Output", "Storage", "Model", "System"], "FilterClasses": ["Input", "Output", "Storage", "Model", "MessageE", "System"], "isDefault": false, "isActive": true}


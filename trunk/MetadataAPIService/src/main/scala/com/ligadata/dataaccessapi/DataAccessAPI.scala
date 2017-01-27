package com.ligadata.dataaccessapi

case class AttributeDef(name: String, typeStr: String) 
case class AttributeGroupDef(name: String, attributes: Array[AttributeDef]) 
case class DataContainerDef(name: String, fullContainerName: String, attributes: Array[AttributeDef], attributeGroups: Map[String, AttributeGroupDef]) 
//case class Attribute(name: String, value: Any) 
//case class DataRecord(values: Array[Attribute]) 

trait DataAccessAPI {
  def get(fullContainerName: String, select : Array[AttributeDef], keys: Array[String]): Array[Map[String, Any]]
  
  //def get(fullContainerName: String, select : Array[AttributeDef], filter: Array[String]): Array[DataRecord]
}
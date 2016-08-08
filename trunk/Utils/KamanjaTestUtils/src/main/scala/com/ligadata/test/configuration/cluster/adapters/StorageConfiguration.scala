/*
 * Copyright 2015 ligaDATA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ligadata.test.configuration.cluster.adapters

import com.ligadata.test.configuration.cluster.adapters.interfaces.{H2DBStore, StorageAdapter, StoreType}

case class StorageConfiguration(storeType: StoreType,
                                schemaName: String,
                                hostname: String,
                                name: String = "",
                                typeString: String = "",
                                tenantId: String = ""
                               ) extends StorageAdapter {
  override def toString: String = {
    val builder = new StringBuilder
    builder.append("{\n")
    if(name != "" && name != null) {
      builder.append(s""""Name": "$name",""" + "\n")
    }
    storeType match {
      case s @ H2DBStore => builder.append(s""""connectionMode": "${s.connectionMode}",""" + "\n")
    }

    if(typeString != "" && typeString != null) {
      builder.append(s""""TypeString": "${typeString}",""" + "\n")
    }
    builder.append(s""""StoreType": "${storeType.name.toLowerCase}",""" + "\n")
    if(tenantId != "" && tenantId != null) {
      builder.append(s""""TenantId": "$tenantId",""")
    }
    builder.append(s""""SchemaName": "$schemaName",""")
    builder.append(s""""Location": "$hostname"}""")
    builder.toString()
  }

  override def jarName: String = ""

  override def className: String = ""

  override def dependencyJars: List[String] = null
}

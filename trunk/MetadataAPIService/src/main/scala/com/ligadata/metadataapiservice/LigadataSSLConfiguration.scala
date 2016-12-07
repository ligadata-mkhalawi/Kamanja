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

package com.ligadata.metadataapiservice

import org.apache.camel.util.jsse._
import javax.net.ssl.SSLContext
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import spray.io._
import com.ligadata.MetadataAPI.MetadataAPI

// Must be enabled in the applicatin.conf
trait LigadataSSLConfiguration {

    // if there is no SSLContext in scope implicitly the HttpServer uses the default SSLContext,
  // since we want non-default settings in this example we make a custom SSLContext available here
  implicit def sslContext: SSLContext = {
    val getMetadataAPI = MetadataAPI.getMetadataApiInterface()
    if(getMetadataAPI.isSslEnabled) {
      val keyStoreResource = getMetadataAPI.getSSLCertificatePath
      val kspass = getMetadataAPI.getSSLCertificatePasswd

      val ksp = new KeyStoreParameters()
      ksp.setResource(keyStoreResource);
      ksp.setPassword(kspass)

      val kmp = new KeyManagersParameters()
      kmp.setKeyStore(ksp)
      kmp.setKeyPassword(kspass)

      val scp = new SSLContextParameters()
      scp.setKeyManagers(kmp)

      val context = scp.createSSLContext()
      context
    }
    else
      SSLContext.getDefault
  }
}

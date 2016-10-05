/*
 * Copyright 2016 ligaDATA
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
package com.ligadata.runtime

import org.apache.logging.log4j.{ Logger, LogManager }

class Log(configuration: String) {

  private val log = LogManager.getLogger(configuration)

  def Trace(str: String) = if(log.isTraceEnabled())  log.trace(str)
  def Warning(str: String) = if(log.isWarnEnabled()) log.warn(str)
  def Info(str: String) = if(log.isInfoEnabled())    log.info(str)
  def Error(str: String) = if(log.isErrorEnabled())  log.error(str)
  def Debug(str: String) = if(log.isDebugEnabled())  log.debug(str)

  def Trace(str: String, e: Throwable) = if(log.isTraceEnabled())  log.trace(str, e)
  def Warning(str: String, e: Throwable) = if(log.isWarnEnabled()) log.warn(str, e)
  def Info(str: String, e: Throwable) = if(log.isInfoEnabled())    log.info(str, e)
  def Error(str: String, e: Throwable) = if(log.isErrorEnabled())  log.error(str, e)
  def Debug(str: String, e: Throwable) = if(log.isDebugEnabled())  log.debug(str, e)

  def Trace(e: Throwable) = if(log.isTraceEnabled())  log.trace("", e)
  def Warning(e: Throwable) = if(log.isWarnEnabled()) log.warn("", e)
  def Info(e: Throwable) = if(log.isInfoEnabled())    log.info("", e)
  def Error(e: Throwable) = if(log.isErrorEnabled())  log.error("", e)
  def Debug(e: Throwable) = if(log.isDebugEnabled())  log.debug("", e)

  def isTraceEnabled = log.isTraceEnabled()
  def isWarnEnabled = log.isWarnEnabled()
  def isInfoEnabled = log.isInfoEnabled()
  def isErrorEnabled = log.isErrorEnabled()
  def isDebugEnabled = log.isDebugEnabled()
}

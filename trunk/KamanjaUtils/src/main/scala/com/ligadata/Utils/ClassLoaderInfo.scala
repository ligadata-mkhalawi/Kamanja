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

package com.ligadata.Utils

import scala.collection.mutable.TreeSet
import scala.reflect.runtime.{universe => ru}
import java.net.{URL, URLClassLoader}
import org.apache.logging.log4j.{Logger, LogManager}
import scala.collection.mutable.ArrayBuffer
import java.io.{ File }

/*
 * Kamanja custom ClassLoader. We can use this as Parent first (default, which is default for java also) and Parent Last.
 *   So, if same class loaded multiple times in the class loaders hierarchy, 
 *     with parent first it gets the first loaded class.
 *     with parent last it gets the most recent loaded class.
 */

class KamanjaClassLoader(val systemClassLoader: URLClassLoader, val parent: KamanjaClassLoader,
                         val currentClassClassLoader: ClassLoader, val parentLast: Boolean, var findInSystemLast: Boolean = false, val preprendedJars: Array[String] = Array[String]())
  extends URLClassLoader(preprendedJars.map(fl => new File(fl.trim)).map(fl => fl.toURI().toURL()) ++ (if ((findInSystemLast || parentLast == false) && systemClassLoader != null) systemClassLoader.getURLs() else Array[URL]()),
    if (parentLast == false && parent != null) parent else if (parentLast == false) currentClassClassLoader else null) {
  private val LOG = LogManager.getLogger(getClass)

  if (LOG.isDebugEnabled() || findInSystemLast || parentLast) {
    // Printing invokation stack trace
    try {
      val s: String = null
      s.length
    } catch {
      case e: Throwable => {
        val urls = if (parentLast == false && parent == null && systemClassLoader != null) systemClassLoader.getURLs() else Array[URL]()
        if (LOG.isDebugEnabled())
          LOG.debug("Created KamanjaClassLoader. this:" + this + ", systemClassLoader:" + systemClassLoader + ", currentClassClassLoader:" + currentClassClassLoader + ", parentLast:" + parentLast + ", findInSystemLast:" + findInSystemLast + ", URLS:" + urls.map(u => u.getFile()).mkString(","), e)
        else if (LOG.isWarnEnabled)
          LOG.warn("Created KamanjaClassLoader. this:" + this + ", systemClassLoader:" + systemClassLoader + ", currentClassClassLoader:" + currentClassClassLoader + ", parentLast:" + parentLast + ", findInSystemLast:" + findInSystemLast + ", URLS:" + urls.map(u => u.getFile()).mkString(","), e)
      }
    }
  }

  override def addURL(url: URL) {
    if (LOG.isDebugEnabled()) LOG.debug("Adding URL:" + url.getPath + " to default class loader for this:" + this)
    super.addURL(url)
  }

  protected override def loadClass(className: String, resolve: Boolean): Class[_] = this.synchronized {
    if (LOG.isDebugEnabled()) LOG.debug("Trying to load class:" + className + ", resolve:" + resolve + ", parentLast:" + parentLast + ", parent:" + parent + ", classloader:" + this)

    var exp: Throwable = null

    if (parentLast) {
      val fndInSystemAtTheEnd = findInSystemLast && (className.startsWith("com.google.common.") || className.startsWith("com.google.thirdparty."))
      var clz = findLoadedClass(className);
      if (clz == null) {
        if (systemClassLoader != null && !fndInSystemAtTheEnd) {
          try {
            clz = systemClassLoader.loadClass(className);
          }
          catch {
            case e: Throwable => {
            }
          }
        }

        if (currentClassClassLoader != null && !fndInSystemAtTheEnd) {
          try {
            clz = currentClassClassLoader.loadClass(className);
          }
          catch {
            case e: Throwable => {
            }
          }
        }

        if (clz == null) {
          try {
            clz = findClass(className);
          } catch {
            case e: ClassNotFoundException => {
              try {
                clz = super.loadClass(className, resolve);
              } catch {
                case e: ClassNotFoundException => {
                  try {
                    if (parent != null) {
                      clz = parent.loadClass(className, resolve);
                    }
                    else {
                      // Not doing anything. Just fall-thru
                      exp = e
                    }
                  } catch {
                    case e: ClassNotFoundException => {
                      // Not doing anything. Just fall-thru
                      exp = e
                    }
                  }
                }
              }
            }
          }
        }

        if (clz == null && currentClassClassLoader != null && fndInSystemAtTheEnd) {
          try {
            clz = currentClassClassLoader.loadClass(className);
          }
          catch {
            case e: Throwable => {
              if (exp == null)
                exp = e
            }
          }
        }

        if (clz == null && systemClassLoader != null && fndInSystemAtTheEnd) {
          try {
            clz = systemClassLoader.loadClass(className);
          }
          catch {
            case e: Throwable => {
              if (exp == null)
                exp = e
            }
          }
        }
      }

      if (exp != null && clz == null) {
        if (fndInSystemAtTheEnd) {
          val curURLs = getURLs()
          val sysURLs = systemClassLoader.getURLs()
          LOG.error("Class:" + className + " not found in classloader:" + this + " and also in systemClassLoader:" + systemClassLoader + " and also in currentClassClassLoader:" + currentClassClassLoader
                     + ", curURLs:{" + curURLs.map(url => url.getFile).mkString(",") + "}, sysURLs:{" + sysURLs.map(url => url.getFile).mkString(",") + "}")
        }
        throw exp
      }
      if (resolve) {
        resolveClass(clz);
      }
      return clz
    } else {
      return super.loadClass(className, resolve)
    }
  }

  override def getResource(name: String): URL = {
    var url: URL = null;
    if (LOG.isDebugEnabled()) LOG.debug("Trying to getResource:" + name)

    if (parentLast) {
      val fndInSystemAtTheEnd = findInSystemLast && (name.startsWith("com.google.common.") || name.startsWith("com.google.thirdparty."))
      if (systemClassLoader != null && url == null && !fndInSystemAtTheEnd) {
        url = systemClassLoader.getResource(name);
      }
      if (currentClassClassLoader != null && url == null && !fndInSystemAtTheEnd) {
        url = currentClassClassLoader.getResource(name);
      }
      if (url == null) {
        url = findResource(name);
        if (url == null) {
          url = super.getResource(name);
        }
        if (url == null && parent != null) {
          url = parent.getResource(name);
        }
      }
      if (systemClassLoader != null && url == null && fndInSystemAtTheEnd) {
        url = systemClassLoader.getResource(name);
      }
      if (currentClassClassLoader != null && url == null && fndInSystemAtTheEnd) {
        url = currentClassClassLoader.getResource(name);
      }
    } else {
      url = super.getResource(name)
    }

    if (LOG.isDebugEnabled()) LOG.debug("URL is:" + url)
    url
  }

  override def getResources(name: String): java.util.Enumeration[URL] = {
    if (LOG.isDebugEnabled()) LOG.debug("Trying to getResources:" + name)

    var urls = ArrayBuffer[URL]()

    var systemUrls: java.util.Enumeration[URL] = null
    val superUrls = findResources(name)
    var curClassLoaderUrls: java.util.Enumeration[URL] = null
    var parentUrls: java.util.Enumeration[URL] = null
    var parent1Urls: java.util.Enumeration[URL] = null

    if (systemClassLoader != null) {
      systemUrls = systemClassLoader.getResources(name);
    }

    if (currentClassClassLoader != null) {
      curClassLoaderUrls = currentClassClassLoader.getResources(name);
    }

    if (parentLast && getParent() != null) {
      parentUrls = getParent().getResources(name)
    }

    if (parentLast && getParent() != parent && parent != null) {
      parent1Urls = parent.getResources(name)
    }

    if (systemUrls != null) {
      while (systemUrls.hasMoreElements()) {
        urls += systemUrls.nextElement()
      }
    }

    if (curClassLoaderUrls != null) {
      while (curClassLoaderUrls.hasMoreElements()) {
        urls += curClassLoaderUrls.nextElement()
      }
    }

    if (superUrls != null) {
      while (superUrls.hasMoreElements()) {
        urls += superUrls.nextElement()
      }
    }

    if (parentUrls != null) {
      while (parentUrls.hasMoreElements()) {
        urls += parentUrls.nextElement()
      }
    }

    if (parent1Urls != null) {
      while (parent1Urls.hasMoreElements()) {
        urls += parent1Urls.nextElement()
      }
    }

    if (LOG.isDebugEnabled()) LOG.debug("Found %d URLs".format(urls.size))
    new java.util.Enumeration[URL]() {
      var iter = urls.iterator

      def hasMoreElements(): Boolean = iter.hasNext

      def nextElement(): URL = iter.next()
    }
  }
}

/*
 * KamanjaLoaderInfo is just wrapper for ClassLoader to maintain already loaded jars.
 */
class KamanjaLoaderInfo(val parent: KamanjaLoaderInfo = null, val useParentloadedJars: Boolean = false, val parentLast: Boolean = false, var findInSystemLast: Boolean = false, val preprendedJars: Array[String] = Array[String]()) {
  // Parent class loader
  val parentKamanLoader: KamanjaClassLoader = if (parent != null) parent.loader else null

  // Class Loader
  val loader = new KamanjaClassLoader(ClassLoader.getSystemClassLoader().asInstanceOf[URLClassLoader], parentKamanLoader, getClass().getClassLoader(), parentLast, findInSystemLast, preprendedJars)

  // Loaded jars
  val loadedJars: TreeSet[String] = if (useParentloadedJars && parent != null) parent.loadedJars else new TreeSet[String]

  // Get a mirror for reflection
  val mirror: reflect.runtime.universe.Mirror = ru.runtimeMirror(loader)
}


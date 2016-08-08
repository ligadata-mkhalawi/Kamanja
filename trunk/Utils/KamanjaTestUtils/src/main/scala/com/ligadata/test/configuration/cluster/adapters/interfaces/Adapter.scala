package com.ligadata.test.configuration.cluster.adapters.interfaces

/**
  * Created by william on 4/11/16.
  */
trait Adapter {
  def name: String
  def className: String
  def jarName: String
  def dependencyJars: List[String]
}
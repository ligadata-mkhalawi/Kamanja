#!groovy

node {
    stage 'build'  {
        checkout scm
        sh "sbt '++2.11.7 package'"
    }
}
 

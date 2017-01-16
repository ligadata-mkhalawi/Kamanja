#!groovy

node {
    stage('Build')  {
        checkout scm
        sh "sbt '++ 2.11.7 package'"
    }
}
 

#!groovy

node {
    stage('Build')  {
        dir('trunk') {
            checkout scm
            sh "sbt '++ 2.11.7 package'"
        }
    }
}
 

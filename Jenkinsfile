#!groovy

node {
    stage('Build')  {
        checkout scm
        sh "cd trunk; sbt '++ 2.11.7 package'"
    }
}
 

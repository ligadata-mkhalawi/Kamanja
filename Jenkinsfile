#!groovy

node {
    stage('Build')  {
        checkout scm
	sh "cd trunk"
        sh "sbt '++ 2.11.7 package'"
    }
}
 

#!groovy

node {
    echo "Running ${env.BUILD_ID} on ${env.JENKINS_URL}"
    stage('Build')  {
        checkout scm
        sh "cd trunk; sbt '++ 2.11.7 package' 'makeSite'"
    }
}
 

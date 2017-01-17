#!groovy

node {
    stage('Build')  {
        checkout scm
        sh "cd trunk; sbt '++ 2.11.7 package' makeSite"
        publishHTML([
            allowMissing: false, 
            alwaysLinkToLastBuild: false, 
            keepAll: false, 
            reportDir: 'trunk/target/site', 
            reportFiles: 'index.html', 
            reportName: 'Documentation'
        ])
    }
}
 

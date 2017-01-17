#!groovy

node {
    stage('Build')  {
        try {
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
	finally {
	    emailext attachLog: true, body: '', compressLog: true, replyTo: 'Jenkins Daemon <no-reply@ligadata.com>', subject: 'Build Failure ${env.BUILD_ID}', to: 'william@ligadata.com'
	}
	
    }
}
 

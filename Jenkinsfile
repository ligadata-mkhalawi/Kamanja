#!groovy

node {
    try {
        notifyBuild('STARTED')
        stage('Build')  {
            checkout scm
            // Navigating to the trunk directory, building the package and generating the documentation.
            sh "cd trunk; sbt '++ 2.11.7 package' makeSite"
            // This publishes the documentation generated on that branch so anyone with Jenkins access may review it.
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
    catch(e) {
        // If there was an exception, the build failed
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        // Success or fail, always send email
        notifyBuild(currentBuild.result)
    }
}

def notifyBuild(String buildStatus = "STARTED") {
    // build status of null means successful
    buildStatus = buildStatus ?: 'SUCCESSFUL'

    // Default Values
    def colorName ='RED'
    def colorCode = '#FF0000'
    def subject = "${buildStatus}: Jenkins Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':"
    def summary = "${subject} (${env.BUILD_URL})"
    def details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
        <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""

    // Override default values based on build status
    if (buildStatus == 'STARTED') {
        color = 'YELLOW'
        colorCode = '#FFFF00'
    }
    else if (buildStatus == 'SUCCESSFUL') {
        color = 'GREEN'
        colorCode = '#00FF00'
    }
    else {
        color = 'RED'
        colorCode = '#FF0000'
    }

    // Send email
    emailext (
        subject: subject,
        body: details,
        recipientProviders: [[$class: 'DevelopersRecipientProvider']],
        to: "william@ligadata.com"
    )
}

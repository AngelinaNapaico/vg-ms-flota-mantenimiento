pipeline {
    agent any

    environment {
        SONARQUBE_SERVER = 'SonarQube'
        SLACK_CHANNEL = '#prs-ci'
    }

    stages {
        stage('Build and unit tests') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'chmod +x mvnw && ./mvnw --batch-mode clean verify'
                    } else {
                        bat 'mvnw.cmd --batch-mode clean verify'
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube analysis') {
            steps {
                withSonarQubeEnv("${SONARQUBE_SERVER}") {
                    script {
                        if (isUnix()) {
                            sh 'chmod +x mvnw && ./mvnw --batch-mode sonar:sonar'
                        } else {
                            bat 'mvnw.cmd --batch-mode sonar:sonar'
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            slackSend(
                channel: "${SLACK_CHANNEL}",
                color: 'good',
                message: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${env.BUILD_URL}"
            )
        }
        failure {
            slackSend(
                channel: "${SLACK_CHANNEL}",
                color: 'danger',
                message: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${env.BUILD_URL}"
            )
        }
    }
}

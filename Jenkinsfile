pipeline {
    agent any

    tools {
        maven 'M3'
    }

    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-cred')
        // Đổi <dockerhub-user>/<repo> thành tài khoản Docker Hub thật của bạn.
        IMAGE_NAME = "bachdx202156/cicd-demo"
    }

    options {
        skipDefaultCheckout(false)
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean compile'
            }
        }

        stage('Unit Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh '''
                        mvn -B sonar:sonar \
                          -Dsonar.projectKey=cicd-demo \
                          -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Build & Push Image') {
            when {
                branch 'main'
            }
            steps {
                sh '''
                    mvn -B compile jib:build \
                      -Djib.to.image=${IMAGE_NAME}:${BUILD_NUMBER} \
                      -Djib.to.auth.username=${DOCKERHUB_CREDENTIALS_USR} \
                      -Djib.to.auth.password=${DOCKERHUB_CREDENTIALS_PSW}
                '''
            }
        }

        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                sh '''
                    docker pull ${IMAGE_NAME}:${BUILD_NUMBER}
                    docker stop demo-app || true
                    docker rm demo-app || true
                    docker run -d --name demo-app -p 8080:8080 ${IMAGE_NAME}:${BUILD_NUMBER}
                '''
            }
        }
    }

    post {
        always {
            echo "Pipeline finished with status: ${currentBuild.currentResult}"
        }
    }
}

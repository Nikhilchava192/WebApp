pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }

    environment {
        DOCKER_IMAGE   = "yourdockerhubusername/java-tomcat-app"
        DOCKER_TAG     = "${BUILD_NUMBER}"
        DOCKER_CREDS   = "docker-hub-credentials"
        DEPLOY_HOST    = "ec2-user@<YOUR_EC2_PUBLIC_IP>"
        S3_BACKUP_DEST = "s3://app-server-backups-prod-2026/configs/"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Maven Test & SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=java-web-app'
                }
            }
        }

        stage('Quality Gate Evaluation') {
            steps {
                timeout(time: 3, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', "${DOCKER_CREDS}") {
                        def customImage = docker.build("${DOCKER_IMAGE}:${DOCKER_TAG}", "-f Dockerfile .")
                        customImage.push()
                        customImage.push('latest')
                    }
                }
            }
        }

        stage('Deploy to EC2 via Docker') {
            steps {
                sshagent(['ec2-ssh-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_HOST} "
                            docker pull ${DOCKER_IMAGE}:latest &&
                            docker stop tomcat-app || true &&
                            docker rm tomcat-app || true &&
                            docker run -d --name tomcat-app -p 8080:8080 ${DOCKER_IMAGE}:latest
                        "
                    """
                }
            }
        }

        stage('S3 Configuration Backup') {
            steps {
                sshagent(['ec2-ssh-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_HOST} "
                            aws s3 cp /etc/docker/daemon.json ${S3_BACKUP_DEST} || true
                        "
                    """
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        failure {
            echo "CI/CD Pipeline failed! Check SonarQube Quality Gate or Docker logs."
        }
    }
}
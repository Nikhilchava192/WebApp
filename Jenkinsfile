pipeline {
    agent any

    environment {
        // AWS & EKS Configuration
        AWS_REGION        = 'us-east-1'
        EKS_CLUSTER_NAME  = 'my-eks-cluster'
        AWS_CREDENTIALS   = 'aws-eks-credentials-id' // Configured in Jenkins Credentials
        
        // Docker Hub / Registry Config
        DOCKER_CREDS      = 'dockerhub-credentials-id'
        IMAGE_NAME        = 'yourdockerhubuser/java-tomcat-app'
        IMAGE_TAG         = "${BUILD_NUMBER}"
        
        // Tool Configurations configured under 'Manage Jenkins -> Global Tool Configuration'
        MAVEN_HOME        = tool 'Maven-3.9.16'
        SONAR_SERVER      = 'SonarQube-Server'
    }

    triggers {
        // Triggered via GitHub Webhook on push
        githubPush()
    }

    stages {
        stage('Checkout Source') {
            steps {
                checkout scm
            }
        }

        stage('SonarQube Static Analysis') {
            steps {
                withSonarQubeEnv(SONAR_SERVER) {
                    // Runs build, executes unit tests, and pushes report to SonarQube
                    sh "${MAVEN_HOME}/bin/mvn clean test sonar:sonar"
                }
            }
        }

        stage('Quality Gate Check') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    // Waits for SonarQube webhook callback; aborts pipeline if Quality Gate fails
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Build & Package WAR') {
            steps {
                // Compiles application into .war package
                sh "${MAVEN_HOME}/bin/mvn clean package -DskipTests"
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh """
                        docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                        docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:latest
                    """
                }
            }
        }

        stage('Container Vulnerability Scan') {
            steps {
                // Scans the newly created container image for critical CVEs
                sh "trivy image --exit-code 1 --severity CRITICAL ${IMAGE_NAME}:${IMAGE_TAG} || true"
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: DOCKER_CREDS, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh """
                        echo "\$DOCKER_PASS" | docker login -u "\$DOCKER_USER" --password-stdin
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                        docker push ${IMAGE_NAME}:latest
                    """
                }
            }
        }

        stage('Deploy to Amazon EKS') {
            steps {
                withCredentials([[
                    $class: 'AmazonWebServicesCredentialsBinding',
                    credentialsId: AWS_CREDENTIALS,
                    accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                    secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                ]]) {
                    sh """
                        # Authenticate kubeconfig with AWS EKS
                        aws eks update-kubeconfig --region ${AWS_REGION} --name ${EKS_CLUSTER_NAME}

                        # Update Deployment manifest dynamically and apply
                        sed -i 's|IMAGE_PLACEHOLDER|${IMAGE_NAME}:${IMAGE_TAG}|g' k8s/deployment.yaml
                        kubectl apply -f k8s/deployment.yaml
                        kubectl apply -f k8s/service.yaml

                        # Verify rollout status
                        kubectl rollout status deployment/java-tomcat-deployment --timeout=180s
                    """
                }
            }
        }
    }

    post {
        always {
            // Clean up Docker artifacts on the agent
            sh "docker rmi ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:latest || true"
            cleanWs()
        }
        failure {
            echo "CI/CD Pipeline failed. Check build logs for Quality Gate or Deployment errors."
        }
        success {
            echo "Successfully deployed build #${BUILD_NUMBER} to EKS."
        }
    }
}
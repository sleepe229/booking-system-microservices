pipeline {
    agent {
        docker {
            image 'maven:3.9.9-eclipse-temurin-21'
            args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

    stages {
        stage('Build Parent POM') {
            steps {
                sh '''
                    echo "Building parent POM..."
                    mvn -B clean install -DskipTests -f pom.xml
                '''
            }
        }

        stage('Build All Services') {
            parallel {
                stage('Build Hotel Service') {
                    steps {
                        sh 'mvn -B clean package -f hotel/pom.xml'
                    }
                }
                stage('Build Audit Service') {
                    steps {
                        sh 'mvn -B clean package -f hotel-audit-service/pom.xml'
                    }
                }
                stage('Build Discount Service') {
                    steps {
                        sh 'mvn -B clean package -f discount-analytics-service/pom.xml'
                    }
                }
                stage('Build Orchestrator Service') {
                    steps {
                        sh 'mvn -B clean package -f booking-orchestrator-service/pom.xml'
                    }
                }
                stage('Build Notification Service') {
                    steps {
                        sh 'mvn -B clean package -f booking-notification-service/pom.xml'
                    }
                }
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn -B test -f pom.xml'
            }
        }
    }

    post {
        always {
            echo "Build finished!"
        }
        success {
            echo "Build successful!"
        }
        failure {
            echo "Build failed!"
        }
    }
}

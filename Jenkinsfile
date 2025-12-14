pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'localhost:5000'
        DOCKER_NETWORK = 'backend'
    }

    stages {
        stage('Build Parent POM') {
            steps {
                sh '''
                    echo "Building parent POM..."
                    mvn clean install -DskipTests -f pom.xml
                '''
            }
        }

        stage('Build All Services') {
            parallel {
                stage('Build Hotel Service') {
                    steps {
                        sh '''
                            echo "Building Hotel Service..."
                            mvn clean package -f hotel/pom.xml
                        '''
                    }
                }
                stage('Build Audit Service') {
                    steps {
                        sh '''
                            echo "Building Audit Service..."
                            mvn clean package -f hotel-audit-service/pom.xml
                        '''
                    }
                }
                stage('Build Discount Service') {
                    steps {
                        sh '''
                            echo "Building Discount Service..."
                            mvn clean package -f discount-analytics-service/pom.xml
                        '''
                    }
                }
                stage('Build Orchestrator Service') {
                    steps {
                        sh '''
                            echo "Building Booking Orchestrator Service..."
                            mvn clean package -f booking-orchestrator-service/pom.xml
                        '''
                    }
                }
                stage('Build Notification Service') {
                    steps {
                        sh '''
                            echo "Building Notification Service..."
                            mvn clean package -f booking-notification-service/pom.xml
                        '''
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    echo "Building Docker images..."
                    docker-compose build --no-cache
                '''
            }
        }

        stage('Unit Tests') {
            steps {
                sh '''
                    echo "Running unit tests..."
                    mvn test -f pom.xml
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    echo "Deploying services..."
                    docker-compose down -v || true
                    docker-compose up -d
                    sleep 30
                    echo "Services deployed!"
                '''
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                    echo "Checking service health..."
                    curl -f http://localhost:8080/actuator/health || exit 1
                    curl -f http://localhost:8082/actuator/health || exit 1
                    curl -f http://localhost:8083/actuator/health || exit 1
                    curl -f http://localhost:8084/actuator/health || exit 1
                    curl -f http://localhost:8085/actuator/health || exit 1
                    echo "All services are healthy!"
                '''
            }
        }
    }

    post {
        always {
            echo "Pipeline finished!"
            sh 'docker-compose logs | tail -100'
        }
        success {
            echo " Build successful!"
        }
        failure {
            echo " Build failed!"
            sh 'docker-compose logs'
        }
    }
}

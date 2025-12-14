pipeline {
    agent any

    stages {
        stage('Build Parent POM') {
            steps {
                sh 'mvn -B clean install -DskipTests -f pom.xml'
            }
        }

        stage('Build All Services') {
            parallel {
                stage('Hotel') {
                    steps { sh 'mvn -B clean package -f hotel/pom.xml' }
                }
                stage('Audit') {
                    steps { sh 'mvn -B clean package -f hotel-audit-service/pom.xml' }
                }
                stage('Discount') {
                    steps { sh 'mvn -B clean package -f discount-analytics-service/pom.xml' }
                }
                stage('Orchestrator') {
                    steps { sh 'mvn -B clean package -f booking-orchestrator-service/pom.xml' }
                }
                stage('Notification') {
                    steps { sh 'mvn -B clean package -f booking-notification-service/pom.xml' }
                }
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn -B test -f pom.xml'
            }
        }
    }
}

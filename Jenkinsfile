pipeline {
    agent {
        docker {
            image 'wadoon/key-test-docker:jdk11'
        }
    }

    stages {
        stage('Build') {
            steps {
                sh "./mvnw clean package -DskipTests"
            }
        }
        stage('Test') {
            steps {
                sh "./mvnw test"
            }
        }
        stage('Deploy') {
            steps {
                sh "./mvnw deploy"
            }
        }
    }
}

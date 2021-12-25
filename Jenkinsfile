pipeline {
    agent {
                docker { image 'node:16.13.1-alpine' }

    }

    stages {
        stage('Build') {
            steps {
                sh "mvn clean package -DskipTests"
            }
        }
        stage('Test') {
            steps {
                sh "mvn test"
            }
        }
        stage('Deploy') {
            steps {
                sh "mvn deploy"
            }
        }
    }
}

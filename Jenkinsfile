pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                mvn clean package -DskipTests
            }
        }
        stage('Test') {
            steps {
                mvn test
            }
        }
        stage('Deploy') {
            steps {
                mvn deploy
            }
        }
    }
}

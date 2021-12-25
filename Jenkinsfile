pipeline {
    agent {
        docker {
            image 'wadoon/key-test-docker:jdk11'
        }
    }

    stages {
        stage('Build') {
            steps {
                sh "mvn clean package -DskipTests"
            }
        }
        stage('Test') {
            steps {
                //sh "mvn test"
            }
        }
        stage('Deploy') {
            steps {
                //sh "mvn deploy"
            }
        }
    }
}

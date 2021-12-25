pipeline {
    agent {
        docker {
            image 'wadoon/key-test-docker:jdk11'
        }
    }

    stages {
        stage('Env') {
            steps {
                sh "echo $PATH"
            }
        }

        stage('Build') {
            steps {
                sh "mvn clean package -DskipTests"
            }
        }
        stage('Test') {
            steps {
                sh "echo 1"
            }
        }
        stage('Deploy') {
            steps {
                sh "echo 1"
            }
        }
    }
}

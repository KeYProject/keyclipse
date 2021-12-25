pipeline {
    agent {
        docker {
            image 'wadoon/key-test-docker:jdk11'
        }
    }

    environment {
        PATH = "/root/.sdkman/candidates/maven/current/bin:/root/.sdkman/candidates/gradle/current/bin:${env.PATH}"
    }


    stages {
        stage('Env') {
            steps {
                sh "echo $PATH"
                sh "ll /root/.sdkman/candidates/maven/current/bin/mvn"
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

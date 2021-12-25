pipeline {
    agent {
        docker {
            //image 'maven:3.8.1-adoptopenjdk-11'
            args '-v $HOME/.m2:/root/.m2'
            image 'wadoon/key-test-docker:jdk11'
        }
    }

    environment {
        PATH = "/root/.sdkman/candidates/maven/current/bin:/root/.sdkman/candidates/gradle/current/bin:${env.PATH}"
    }


    stages {
        stage('Env') {
            steps {
                sh 'echo $PATH'
                sh 'echo $USER $USERNAME $UID'
                sh "ls -l /root/.sdkman/candidates/"
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

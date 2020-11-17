pipeline {
    agent any
    stages {
        stage('Configure execution') {
            steps {
                echo 'Configure execution'
            }
        }
        stage('Build') {
            steps {
                echo 'Build'
            }
        }
        stage('Unit Test') {
            steps {
                echo 'Unit Test'
            }
        }
        stage('Static Analysis') {
            steps {
                echo 'Static Analysis'
            }
        }
        stage('Integration Test') {
            steps {
                echo 'Integration Test'
            }
        }
        stage('Acceptance Test') {
            steps {
                echo 'Integration Test'
            }
        }
    }
}
@Library('standard_library') _

pipeline {
    agent any
    stages {
        stage('Configure Execution') {
            steps {
                script {
                    currentBuild.displayName = "${BUILD_NUMBER}. ${params.branchName}"

                    echo 'Build'
                    sh "rm -rf ./mvnwrapper"
                    sh "rm -rf ./.mvn"
                    sh "rm -rf ./mvnw"

                    sh "git clone https://github.com/takari/maven-wrapper.git ./mvnwrapper"
                    sh "rm -rf ./mvnwrapper/.git"
                    sh "mv ./mvnwrapper/mvnw ./"
                    sh "mv ./mvnwrapper/.mvn ./"
                    sh "rm -rf ./mvnwrapper"
                    sh "chmod +x ./mvnw"
                }
            }
        }
        stage('Checkout Source') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: params.commitHash ]],
                          userRemoteConfigs: [[url: params.gitUrl]]])
                bitbucket_status_notify(params.commitHash, "INPROGRESS", "Build", params.bitBucketServerUrl, params.bitBucketCredentialsId, params.bitBucketUsernameVariableName, params.bitBucketPasswordVariableName)
            }
        }
        stage('Build') {
            steps {
                script {
                    sh "./mvnw versions:set -DnewVersion=0.0.0-${params.branchName.split('/')[1].toUpperCase().replaceAll('-', '_')}-SNAPSHOT"
                }
                maven_clean_install()
                archiveArtifacts artifacts: "target/*"
                bitbucket_status_notify(params.commitHash, "SUCCESSFUL", "Build", params.bitBucketServerUrl, params.bitBucketCredentialsId, params.bitBucketUsernameVariableName, params.bitBucketPasswordVariableName)
            }
        }
        stage('Unit Test') {
            steps {
                script {
                    sh "./mvnw test"
                }
                bitbucket_status_notify(params.commitHash, "SUCCESSFUL", "Unit Testing", params.bitBucketServerUrl, params.bitBucketCredentialsId, params.bitBucketUsernameVariableName, params.bitBucketPasswordVariableName)
            }
        }
        stage('Static Analysis') {
            steps {

            }
        }
        stage('Integration Test') {
            steps {
                script {
                    sh './mvnw verify'
                }
            }
        }
        stage('Acceptance Test') {
            steps {
                echo 'Acceptance Tests'
            }
        }
    }
}
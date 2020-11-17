@Library('standard_library') _

pipeline {
    agent any

    stages {
        stage("Start job for event") {
            steps {
                build job: "EDP - X286 LoanCalcApp - Feature updated", parameters: [[$class: 'StringParameterValue', name: 'gitUrl', value: params.gitUrl], [$class: 'StringParameterValue', name: 'branchName', value: params.branchName]]
            }
        }
    }
}
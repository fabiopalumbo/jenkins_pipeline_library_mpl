def call(String commitHash, String status, String buildStage, String bitBucketServerUrl, String bitbucketCredentialsId, String bitBucketUsernameVariableName, String bitBucketPasswordVariableName) {
    env.status="${status}"
    env.buildStage="${buildStage}"
    withCredentials([usernamePassword( credentialsId: bitbucketCredentialsId, usernameVariable: bitBucketUsernameVariableName, passwordVariable: bitBucketPasswordVariableName)]) {
        sh """
            curl --insecure -u $USERNAME:$PASSWORD -H "Content-Type: application/json" -X POST $bitBucketServerUrl/rest/build-status/1.0/commits/${commitHash} -d '{ "state": "'"${status}"'", "key": "'"${buildStage}"'", "name": "'"${buildStage}"'", "url": "'"${BUILD_URL}"'", "description": "Built by Jenkins" }'           
        """
    }
}
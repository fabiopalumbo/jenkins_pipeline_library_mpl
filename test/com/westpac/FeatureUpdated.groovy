package com.westpac


import com.offbytwo.jenkins.JenkinsServer
import com.offbytwo.jenkins.model.BuildResult
import groovy.test.GroovyAssert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockserver.integration.ClientAndServer
import org.mockserver.verify.VerificationTimes

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.StringBody.exact;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpResponse.response;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

class FeatureUpdated {
    private ClientAndServer mockBitbucketServer;

    @Before
    void createFeatureUpdatedJobBeforeEachTest() {
        String jobName = Util.createFeatureUpdatedJenkinsJob("X286", "Loan Calc App")
        def jobs = new JenkinsServer(new URI("http://localhost:8080"), "api", "api").getJobs()
        GroovyAssert.assertTrue(jobs.containsKey(jobName))
        this.mockBitbucketServer = startClientAndServer(50820)
    }

    @After
    void deleteFeatureUpdatedJobAfterEachTest() {
        new JenkinsServer(new URI("http://localhost:8080"), "api", "api")
                .deleteJob(Util.getJobName("X286", "Loan Calc App", "Feature updated"), true)
        this.mockBitbucketServer.stop()
    }

    @Test
    void shouldExecuteFeatureBuildJobSuccessfully() {
        def buildDetails = Util.runJob("X286",
                "Loan Calc App",
                "Feature updated",
                [
                        "gitUrl": "https://github.com/narent/sample_maven_project.git",
                        "branchName": "feature/X286-1234-a_new_feature",
                        "commitHash" : "ed9a8a8f7cdae673ec9913f30693be1ff173997f",
                        "bitBucketServerUrl": "http://docker.for.mac.host.internal:50820",
                        "bitBucketCredentialsId": "bitbucketcredentials",
                        "bitBucketUsernameVariableName": "USERNAME",
                        "bitBucketPasswordVariableName": "PASSWORD",
                ])

        GroovyAssert.assertEquals(BuildResult.SUCCESS, buildDetails.result)
    }

    @Test
    void shouldCheckoutTheCommitThatTriggeredTheBuild() {
        def buildDetails = Util.runJob("X286",
                "Loan Calc App",
                "Feature updated",
                [
                        "gitUrl": "https://github.com/narent/sample_maven_project.git",
                        "branchName": "feature/X286-1234-a_new_feature",
                        "commitHash" : "ed9a8a8f7cdae673ec9913f30693be1ff173997f",
                        "bitBucketServerUrl": "http://docker.for.mac.host.internal:50820",
                        "bitBucketCredentialsId": "bitbucketcredentials",
                        "bitBucketUsernameVariableName": "USERNAME",
                        "bitBucketPasswordVariableName": "PASSWORD",
                ])

        // Check the workspace for a change that only exists in the feature branch
        GroovyAssert.assertEquals(BuildResult.SUCCESS, buildDetails.result)
    }

    @Test
    void shouldSetDisplayNameToNameOfFeatureBranchBeingBuilt() {
        def buildDetails = Util.runJob("X286",
                "Loan Calc App",
                "Feature updated",
                [
                        "gitUrl": "https://github.com/narent/sample_maven_project.git",
                        "branchName": "feature/X286-1234-a_new_feature",
                        "commitHash" : "ed9a8a8f7cdae673ec9913f30693be1ff173997f",
                        "bitBucketServerUrl": "http://docker.for.mac.host.internal:50820",
                        "bitBucketCredentialsId": "bitbucketcredentials",
                        "bitBucketUsernameVariableName": "USERNAME",
                        "bitBucketPasswordVariableName": "PASSWORD",
                ])

        GroovyAssert.assertEquals("${buildDetails.number}. feature/X286-1234-a_new_feature".toString(), buildDetails.displayName)
    }

    @Test
    void shouldNotifyBitBucketRepoThatBuildHasStarted(){
        this.mockBitbucketServer.when(
                request()
                    .withMethod("POST")
                    .withPath("/rest/build-status/1.0/commits/1b300faacdd5519f537a16e70cff16abb418531b"), exactly(1))
                .respond(response().withStatusCode(200))

        def buildDetails = Util.runJob("X286",
                "Loan Calc App",
                "Feature updated",
                [
                        "gitUrl": "https://github.com/narent/sample_maven_project.git",
                        "branchName": "feature/X286-1234-a_new_feature",
                        "commitHash" : "ed9a8a8f7cdae673ec9913f30693be1ff173997f",
                        "bitBucketServerUrl": "http://docker.for.mac.host.internal:50820",
                        "bitBucketCredentialsId": "bitbucketcredentials",
                        "bitBucketUsernameVariableName": "USERNAME",
                        "bitBucketPasswordVariableName": "PASSWORD",
                ])

        GroovyAssert.assertEquals(BuildResult.SUCCESS, buildDetails.result)
        this.mockBitbucketServer.verify(
                request()
                        .withMethod("POST")
                        .withPath("/rest/build-status/1.0/commits/1b300faacdd5519f537a16e70cff16abb418531b")
                        .withBody(exact('{ "state": "INPROGRESS", "key": "Build", "name": "Build", "url": "'  + buildDetails.url +  '", "description": "Built by Jenkins" }')),
                VerificationTimes.exactly(1)
        )
    }


    @Test
    void shouldBuildMavenProjectAndProduceArtifactsWithCorrectVersioning() {
        def buildDetails = Util.runJob("X286",
                "Loan Calc App",
                "Feature updated",
                [
                        "gitUrl": "https://github.com/narent/sample_maven_project.git",
                        "branchName": "feature/X286-1234-a_new_feature",
                        "commitHash" : "ed9a8a8f7cdae673ec9913f30693be1ff173997f",
                        "bitBucketServerUrl": "http://docker.for.mac.host.internal:50820",
                        "bitBucketCredentialsId": "bitbucketcredentials",
                        "bitBucketUsernameVariableName": "USERNAME",
                        "bitBucketPasswordVariableName": "PASSWORD"
                ])

        GroovyAssert.assertEquals(BuildResult.SUCCESS, buildDetails.result)
        GroovyAssert.assertEquals(1, buildDetails.artifacts.size())
        GroovyAssert.assertEquals("sample-app-0.0.0-X286_1234_A_NEW_FEATURE-SNAPSHOT.jar", buildDetails.artifacts[0].fileName)
        // Download the artifact and check that it is the expected application
    }

    @Test
    void shouldFailWhenGitBranchDoesNotExist() {
        def buildDetails = Util.runJob("X286",
                "Loan Calc App",
                "Feature updated",
                [
                        "gitUrl": "https://github.com/narent/sample_maven_project.git",
                        "branchName": "feature/X286-1234-a_non_existant_feature",
                        "commitHash" : "a fake commit hash",
                        "bitBucketServerUrl": "http://docker.for.mac.host.internal:50820",
                        "bitBucketCredentialsId": "bitbucketcredentials",
                        "bitBucketUsernameVariableName": "USERNAME",
                        "bitBucketPasswordVariableName": "PASSWORD",
                ])

        GroovyAssert.assertEquals(BuildResult.FAILURE, buildDetails.result)
    }
}


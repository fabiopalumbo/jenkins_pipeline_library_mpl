package com.westpac

import com.offbytwo.jenkins.JenkinsServer
import com.offbytwo.jenkins.model.Build
import com.offbytwo.jenkins.model.BuildWithDetails
import com.offbytwo.jenkins.model.QueueReference
import groovy.json.JsonSlurper
import groovy.test.GroovyAssert
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.nio.file.Files
import java.nio.file.Paths

class Webhook {
    @Before
    void createFeatureUpdatedJobBeforeEachTest() {
        def controllerJobName = Util.createControllerJob("X286", "LoanCalcApp", "a_git_url")
        def featureUpdatedJobName = Util.createFeatureUpdatedJenkinsJob("X286", "LoanCalcApp")

        def jobs = new JenkinsServer(new URI("http://localhost:8080"), "api", "api").getJobs()
        GroovyAssert.assertTrue(jobs.containsKey(controllerJobName))
        GroovyAssert.assertTrue(jobs.containsKey(featureUpdatedJobName))
    }

    @After
    void deleteFeatureUpdatedJobAfterEachTest() {
        def jenkins = new JenkinsServer(new URI("http://localhost:8080"), "api", "api")
        jenkins.deleteJob(Util.getJobName("X286", "LoanCalcApp", "Controller"), true)
        jenkins.deleteJob(Util.getJobName("X286", "LoanCalcApp", "Feature updated"), true)
    }

    @Test
    void shouldTriggerControllerJobWhenCalledWithTokenValue() {
        def response = simulateWebhookTriggerWithPayload("featureUpdatedWebhookPayload.json", "X286_LoanCalcApp")

        def webhookTriggerResponse = new JsonSlurper().parseText(response)
        def controllerJobName = Util.getJobName("X286", "LoanCalcApp", "Controller")

        GroovyAssert.assertTrue(webhookTriggerResponse.jobs[controllerJobName].triggered)
        GroovyAssert.assertEquals("feature/EDP-123-feature-name", webhookTriggerResponse.jobs[controllerJobName].resolvedVariables.branchName)
        GroovyAssert.assertTrue(webhookTriggerResponse.jobs[controllerJobName].id > 0)

        def queueRef = new QueueReference(webhookTriggerResponse.jobs[controllerJobName].url)

        def buildDetails = Util.awaitQueueItemCompletion(queueRef, controllerJobName)
        GroovyAssert.assertNotNull(buildDetails)
    }

    @Test
    void shouldStartFeatureUpdatedJobWhenBranchNameStartsWithFeatureSlash() {
        def (String controllerJobName, BuildWithDetails buildDetails) = triggerControllerAndGetBuildDetails("featureUpdatedWebhookPayload.json", "X286_LoanCalcApp")

        def controllerBuildNumber = buildDetails.number
        def featureUpdatedJobName = Util.getJobName("X286", "LoanCalcApp", "Feature updated")

        def build = waitForBuildWithCauseMatching(featureUpdatedJobName) { c ->
            c.upstreamBuild == controllerBuildNumber && c.upstreamProject == controllerJobName
        }

        GroovyAssert.assertTrue(build.details().parameters.containsKey("gitUrl"))
        GroovyAssert.assertEquals("a_git_url", build.details().parameters["gitUrl"])

        GroovyAssert.assertTrue(build.details().parameters.containsKey("branchName"))
        GroovyAssert.assertEquals("feature/EDP-123-feature-name", build.details().parameters["branchName"])
    }

    Build waitForBuildWithCauseMatching(String jobName, Closure causeChecker) {
        def jenkins = new JenkinsServer(new URI("http://localhost:8080"), "api", "api")
        while(jenkins.getJob(jobName).builds.find { b -> (b.details().causes.find(causeChecker) != null) } == null) {}
        return jenkins.getJob(jobName).builds.find { b -> (b.details().causes.find(causeChecker) != null) }
    }

    private List triggerControllerAndGetBuildDetails(String payloadFilename, String token) {
        def response = simulateWebhookTriggerWithPayload(payloadFilename, token)
        def webhookTriggerResponse = new JsonSlurper().parseText(response)
        def controllerJobName = Util.getJobName("X286", "LoanCalcApp", "Controller")
        def queueRef = new QueueReference(webhookTriggerResponse.jobs[controllerJobName].url)
        def buildDetails = Util.awaitQueueItemCompletion(queueRef, controllerJobName)
        [controllerJobName, buildDetails]
    }

    private String simulateWebhookTriggerWithPayload(String payloadFileName, String token) {
        def baseUrl = ("http://localhost:8080/generic-webhook-trigger/invoke?token=" + token).toURL()
        def webhookPayload = Files.readString(Paths.get(new File("jobs/" + payloadFileName).getAbsolutePath()))

        def connection = (HttpURLConnection) baseUrl.openConnection()
        connection.doOutput = true
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.withWriter { writer ->
            writer << webhookPayload
        }

        String response = connection.inputStream.withReader { reader -> reader.text }
        GroovyAssert.assertEquals(200, connection.getResponseCode())
        response
    }
}

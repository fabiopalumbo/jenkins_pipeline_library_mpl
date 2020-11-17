package com.westpac

import com.offbytwo.jenkins.JenkinsServer
import com.offbytwo.jenkins.model.BuildResult
import com.offbytwo.jenkins.model.BuildWithDetails
import com.offbytwo.jenkins.model.QueueReference

import java.nio.file.Files
import java.nio.file.Paths

class Util {
    static BuildWithDetails runJob(String appId, String projectName, String jobType, Map params = [:]) {
        def jenkins = new JenkinsServer(new URI("http://localhost:8080"), "api", "api")
        def jobName = Util.getJobName(appId, projectName, jobType)

        def job = jenkins.getJob(jobName)
        def queueRef = job.build(params, true)

        def buildDetails = awaitQueueItemCompletion(queueRef, jobName)
        return buildDetails
    }

    static BuildWithDetails awaitQueueItemCompletion(QueueReference queueRef, String jobName) {
        def jenkins = new JenkinsServer(new URI("http://localhost:8080"), "api", "api")

        while (jenkins.getQueueItem(queueRef).executable == null) {
        }
        int buildNumber = jenkins.getQueueItem(queueRef).executable.number

        while (jenkins.getJob(jobName).getBuildByNumber(buildNumber) == null) {
        }
        while (jenkins.getJob(jobName).getBuildByNumber(buildNumber).details() == null) {
        }
        while (jenkins.getJob(jobName).getBuildByNumber(buildNumber).details().result == null) {
        }
        while (jenkins.getJob(jobName).getBuildByNumber(buildNumber).details().result == BuildResult.BUILDING) {
        }

        def buildDetails = jenkins.getJob(jobName).getBuildByNumber(buildNumber).details()
        buildDetails
    }

    static String createFeatureUpdatedJenkinsJob(String appID, String projectName) {
        def jobName = Util.getJobName(appID, projectName, "Feature updated")
        String jobXml = Util.getJobXml("featureUpdatedJenkinsfile.groovy", "featureUpdated.xml", jobName)

        def jenkins = new JenkinsServer(new URI("http://localhost:8080"), "api", "api")
        jenkins.createJob(jobName, jobXml, true)
        jobName
    }

    static String createControllerJob(String appID, String projectName, String gitUrl) {
        def jobName = Util.getJobName(appID, projectName, "Controller")
        String jobXml = Util.getControllerJobXml(jobName, gitUrl)

        def jenkins = new JenkinsServer(new URI("http://localhost:8080"), "api", "api")
        jenkins.createJob(jobName, jobXml, true)
        jobName
    }

    static String getJobXml(String jenkinsFileName, String xmlFileName, String jobName) {
        def jenkinsFileAbsolutePath = Paths.get(new File("jobs/" + jenkinsFileName).getAbsolutePath())
        def xmlFileAbsolutePath = Paths.get(new File("jobs/" + xmlFileName).getAbsolutePath())

        def scriptString = Files.readString(jenkinsFileAbsolutePath)
        def jobXMLString = Files.readString(xmlFileAbsolutePath)

        def jobProtoXml = new XmlParser().parseText(jobXMLString)

        jobProtoXml.definition.script[0].value = scriptString
        jobProtoXml.description[0].value = jobName

        def stringWriter = new StringWriter()
        def printer = new XmlNodePrinter(new PrintWriter(stringWriter))
        printer.setPreserveWhitespace(true)
        printer.print(jobProtoXml)

        def jobXml = stringWriter.toString()
        jobXml
    }

    static String getControllerJobXml(String jobName, String gitUrl) {
        def jenkinsFileAbsolutePath = Paths.get(new File("jobs/controllerJenkinsfile.groovy").getAbsolutePath())
        def xmlFileAbsolutePath = Paths.get(new File("jobs/controller.xml").getAbsolutePath())

        def scriptString = Files.readString(jenkinsFileAbsolutePath)
        def jobXMLString = Files.readString(xmlFileAbsolutePath)

        def jobProtoXml = new XmlParser().parseText(jobXMLString)

        jobProtoXml.definition.script[0].value = scriptString
        jobProtoXml.description[0].value = jobName
        jobProtoXml.properties["hudson.model.ParametersDefinitionProperty"]["parameterDefinitions"]["hudson.model.StringParameterDefinition"][0]["defaultValue"][0].value = gitUrl

        def stringWriter = new StringWriter()
        def printer = new XmlNodePrinter(new PrintWriter(stringWriter))
        printer.setPreserveWhitespace(true)
        printer.print(jobProtoXml)
        def jobXml = stringWriter.toString()
        jobXml
    }

    static String getJobName(String appID, projectName, String jobType) {
        return "EDP - $appID ${projectName} - ${jobType}"
    }
}
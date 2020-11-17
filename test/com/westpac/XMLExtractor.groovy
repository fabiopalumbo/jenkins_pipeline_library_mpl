package com.westpac

import com.offbytwo.jenkins.JenkinsServer
import org.junit.Test

class XMLExtractor {
    @Test
    void shouldGetJobXML() {
        def jenkins = new JenkinsServer(new URI("http://localhost:8080"), "api", "api")
        def jobXml = jenkins.getJobXml("EDP - X086_CalculatorApp Controller")
        println jobXml
    }
}

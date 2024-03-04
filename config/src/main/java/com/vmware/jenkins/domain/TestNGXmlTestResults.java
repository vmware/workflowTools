package com.vmware.jenkins.domain;

import com.vmware.util.StringUtils;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * Parses test results from a testng-results.xml file
 */
public class TestNGXmlTestResults extends TestResults {
    private final SimpleDateFormat DATE_FORMAT_WITH_TIME_ZONE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss ZZZ");

    public TestNGXmlTestResults(JobBuild build, String text) {
        this.build = build;
        parseTestResultsFromTestNGXmlFile(text);
    }

    private void parseTestResultsFromTestNGXmlFile(String text) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(new StringReader(text)));
            NodeList testngResultsNode = doc.getElementsByTagName("testng-results");
            NodeList classes = doc.getElementsByTagName("class");
            List<TestResult> results = parseResultsFromClassesTags(classes);
            if (testngResultsNode.getLength() > 0) {
                Node testngResults = testngResultsNode.item(0);
                NamedNodeMap attributes = testngResults.getAttributes();
                int total = Integer.parseInt(attributes.getNamedItem("total").getNodeValue());
                Node ignoredNode = attributes.getNamedItem("ignored");
                this.total = ignoredNode != null && StringUtils.isInteger(ignoredNode.getNodeValue())
                        ? total - Integer.parseInt(ignoredNode.getNodeValue()) : total;
                this.failCount = Integer.parseInt(attributes.getNamedItem("failed").getNodeValue());
                this.skipCount = Integer.parseInt(attributes.getNamedItem("skipped").getNodeValue());
            } else {
                LoggerFactory.getLogger(this.getClass()).info("testng-results node not found, computing totals manually");
                this.total = results.size();
                this.skipCount = (int) results.stream().filter(result -> !Boolean.TRUE.equals(result.configMethod) && result.status == TestResult.TestStatus.SKIP).count();
                this.failCount = (int) results.stream().filter(result -> !Boolean.TRUE.equals(result.configMethod) && result.status == TestResult.TestStatus.FAIL).count();
            }

            this.skipConfig = (int) results.stream().filter(result -> Boolean.TRUE.equals(result.configMethod) && result.status == TestResult.TestStatus.SKIP).count();
            this.failConfig = (int) results.stream().filter(result -> Boolean.TRUE.equals(result.configMethod) && result.status == TestResult.TestStatus.FAIL).count();
            this.loadedTestResults = results;
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private List<TestResult> parseResultsFromClassesTags(NodeList classes) {
        List<TestResult> results = new ArrayList<>();
        Map<String, String[]> usedUrls = new HashMap<>();
        for (int i = 0; i < classes.getLength(); i++) {
            Node classNode = classes.item(i);
            for (Node test : getChildNodes(classNode, "test-method")) {
                NamedNodeMap attributes = test.getAttributes();
                TestResult testResult = new TestResult();
                Node isConfigAttribute = attributes.getNamedItem("is-config");
                testResult.configMethod = isConfigAttribute != null && "true".equals(isConfigAttribute.getNodeValue());
                String packageAndClassName = classNode.getAttributes().getNamedItem("name").getNodeValue();
                double durationInMs = Long.parseLong(attributes.getNamedItem("duration-ms").getNodeValue());
                testResult.duration = durationInMs > 0 ? durationInMs / 1000 : 0;
                testResult.className = StringUtils.substringAfterLast(packageAndClassName,  ".");
                testResult.packagePath = StringUtils.substringBefore(packageAndClassName, "." + testResult.className);
                testResult.name = attributes.getNamedItem("name").getNodeValue();
                testResult.status = TestResult.TestStatus.valueOf(attributes.getNamedItem("status").getNodeValue());
                testResult.startedAt = parseStartedAtTime(attributes.getNamedItem("started-at"));

                Optional<Node> paramsNode = getChildNode(test, "params");
                if (paramsNode.isPresent()) {
                    List<Node> paramNodes = getChildNodes(paramsNode.get(), "param");
                    testResult.parameters = paramNodes.stream().map(node -> getChildNode(node, "value")).filter(Optional::isPresent)
                            .map(node -> StringUtils.trim(node.get().getTextContent())).toArray(String[]::new);
                }

                if (testResult.status == TestResult.TestStatus.FAIL || testResult.status == TestResult.TestStatus.SKIP) {
                    Optional<Node> exceptionNode = getChildNode(test, "exception");
                    exceptionNode.ifPresent(node -> testResult.exception = getChildNode(node, "full-stacktrace")
                            .map(Node::getTextContent).map(StringUtils::trim).orElse(null));
                }

                testResult.buildNumber = build.buildNumber;
                testResult.commitId = build.commitId;
                testResult.setUrlForTestMethod(build.getTestReportsUIUrl(), usedUrls);
                usedUrls.put(testResult.url, testResult.parameters);

                if (Boolean.TRUE.equals(testResult.configMethod) && testResult.status == TestResult.TestStatus.PASS && durationInMs < 300) {
                    continue; // don't store test results that pass really quickly
                }
                results.add(testResult);
            }
        }
        return results;
    }

    private long parseStartedAtTime(Node startedAtNode) {
        String startedAt = startedAtNode.getNodeValue();
        try {
            Date startedAtDate = DATE_FORMAT_WITH_TIME_ZONE.parse(startedAt);
            return startedAtDate.getTime();
        } catch (ParseException pe) {
            return TimeUnit.SECONDS.toMillis(Instant.parse(startedAt).getEpochSecond());
        } catch (NumberFormatException nfe) {
            throw new RuntimeException(nfe);
        }
    }

    private List<Node> getChildNodes(Node parentNode, String childNodeName) {
        NodeList childNodes = parentNode.getChildNodes();
        return IntStream.range(0, childNodes.getLength()).mapToObj(childNodes::item)
                .filter(node -> childNodeName.equals(node.getNodeName())).collect(toList());
    }

    private Optional<Node> getChildNode(Node parentNode, String childNodeName) {
        NodeList childNodes = parentNode.getChildNodes();
        return IntStream.range(0, childNodes.getLength()).mapToObj(childNodes::item)
                .filter(node -> childNodeName.equals(node.getNodeName())).findFirst();
    }
}

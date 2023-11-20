package com.vmware.jenkins.domain;

import com.vmware.util.StringUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class TestNGResultsFile {
    private final JobBuild build;
    private final List<TestResult> testResults = new ArrayList<>();

    public TestNGResultsFile(JobBuild build, String text) throws SAXException {
        this.build = build;
        parseTestResultsFromTestNGXmlFile(text);
    }

    public List<TestResult> getTestResults() {
        return testResults;
    }

    private void parseTestResultsFromTestNGXmlFile(String text) throws SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        SimpleDateFormat dateFormatWithTimeZone = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss ZZZ");
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(new StringReader(text)));
            Map<String, String[]> usedUrls = new HashMap<>();
            NodeList classes = doc.getElementsByTagName("class");
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
                    String startedAt = attributes.getNamedItem("started-at").getNodeValue();
                    try {
                        testResult.startedAt = dateFormatWithTimeZone.parse(startedAt).getTime();
                    } catch (ParseException pe) {
                        testResult.startedAt = TimeUnit.SECONDS.toMillis(Instant.parse(startedAt).getEpochSecond());
                    }

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
                    testResults.add(testResult);
                }
            }
        } catch (ParserConfigurationException | IOException e) {
            throw new RuntimeException(e);
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

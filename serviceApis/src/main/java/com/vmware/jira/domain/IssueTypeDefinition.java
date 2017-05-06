package com.vmware.jira.domain;

import com.vmware.util.StringUtils;
import com.vmware.util.complexenum.ComplexEnum;
import com.vmware.util.exception.InvalidDataException;

import java.util.ArrayList;
import java.util.List;

public enum IssueTypeDefinition implements ComplexEnum<Integer> {
    Bug(1),
    NewFeature(2),
    Task(3),
    Improvement(4),
    SubTask(6),
    Story(8),
    TechnicalTask(9),
    PlanningBoardMarker(10),
    Feature(11),
    TestingTask(12),
    StoryBug(13),
    LegalTask(14),
    DevTask(15),
    QETask(16),
    UXTask(17),
    DocTask(18),
    Other(19),
    CustomerIssue(20),
    CodeBug(22),
    PerformanceTesting(23),
    TechComm(24),
    UnknownValue(-1);

    private int code;

    private IssueTypeDefinition(int code) {
        this.code = code;
    }

    @Override
    public Integer getValue() {
        return code;
    }

    @Override
    public String toString() {
        return this.name();
    }

    public static IssueTypeDefinition[] fromValues(String[] values) {
        List<IssueTypeDefinition> definitionList = new ArrayList<>();
        for (String value : values) {
            if (!StringUtils.isInteger(value)) {
                throw new InvalidDataException("Issue definition type value {} must be an integer." +
                        "\nValid values are {}", value, typesWithIntValues());
            }
            definitionList.add(fromValue(Integer.parseInt(value)));
        }
        return definitionList.toArray(new IssueTypeDefinition[definitionList.size()]);
    }

    public static IssueTypeDefinition fromValue(int value) {
        for (IssueTypeDefinition definition : IssueTypeDefinition.values()) {
            if (value == definition.getValue()) {
                return definition;
            }
        }
        throw new InvalidDataException("No Jira issue type matches int value {}\nValid values are {}",
                String.valueOf(value), typesWithIntValues());
    }

    private static String typesWithIntValues() {
        String value = "";
        for (IssueTypeDefinition definition : IssueTypeDefinition.values()) {
            if (!value.isEmpty()) {
                value += ",";
            }
            value += definition.name() + "[" + definition.getValue() + "]";
        }
        return value;
    }
}

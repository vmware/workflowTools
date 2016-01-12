package com.vmware.jira.domain;

import com.vmware.util.complexenum.ComplexEnum;

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
}

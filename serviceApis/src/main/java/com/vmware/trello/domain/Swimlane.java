/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.trello.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.annotations.Expose;

public class Swimlane {
    public static String STORY_POINTS_SUFFIX = " Story Point(s)";

    public String idBoard;
    public String name;
    public String pos = "bottom";

    @Expose(serialize = false)
    public String id;
    @Expose(serialize = false)
    public boolean closed;

    private Swimlane() {}

    public Swimlane(Board board, String name) {
        this.idBoard = board.id;
        this.name = name;
    }

    public Double getStoryPointValue() {
        Matcher matcher = Pattern.compile("([\\d.]+)\\Q" + STORY_POINTS_SUFFIX + "\\E").matcher(name);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return null;
    }
}

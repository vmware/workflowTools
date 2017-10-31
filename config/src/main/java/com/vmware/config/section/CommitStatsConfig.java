package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

/**
 * Commit size configuration for stats calculation.
 */
public class CommitStatsConfig {

    @ConfigurableProperty(commandLine = "--last-number-of-days", help = "Last number of days to collect stats for")
    public int lastNumberOfDaysForStats;

    @ConfigurableProperty(commandLine = "--file-count-ranges", help = "File count ranges for grouping commits / reviews when generating stats")
    public int[] fileCountRanges;

    @ConfigurableProperty(commandLine = "--line-count-ranges", help = "Line count ranges for grouping commits / reviews when generating stats")
    public int[] lineCountRanges;

    @ConfigurableProperty(commandLine = "-ms,--max-summary", help = "Sets max line length for the one line summary")
    public int maxSummaryLength;

    @ConfigurableProperty(commandLine = "--authors", help = "Show commits for specified authors, should be comman separated")
    public String authorsForCommits;
}

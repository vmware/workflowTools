package com.vmware.action.review;

import com.vmware.action.base.BaseReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.section.CommitStatsConfig;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestDiff;
import com.vmware.reviewboard.domain.ReviewRequests;
import com.vmware.reviewboard.domain.ReviewStatType;
import com.vmware.reviewboard.domain.UserReview;
import com.vmware.util.DateUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.Padder;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;

@ActionDescription("Generate review board statistics for specified groups.")
public class GenerateReviewStats extends BaseReviewBoardAction {

    public GenerateReviewStats(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        reviewBoard.setupAuthenticatedConnectionWithLocalTimezone(reviewBoardConfig.reviewBoardDateFormat);
        String groupsToUse = "";
        CommitStatsConfig statsConfig = this.statsConfig;
        if (reviewBoardConfig.targetGroups == null || reviewBoardConfig.targetGroups.length == 0) {
            log.info("No target groups selected");
            groupsToUse = InputUtils.readValueUntilNotBlank("Please enter target groups");
        } else {
            groupsToUse = StringUtils.join(Arrays.asList(reviewBoardConfig.targetGroups));
        }

        log.info("Generating stats for groups {} with file count ranges {}",
                groupsToUse, Arrays.toString(statsConfig.fileCountRanges));
        long aMonthAgo = new Date().getTime() - TimeUnit.DAYS.toMillis(30);

        ReviewRequests recentReviews =
                reviewBoard.getReviewRequestsWithShipItsForGroups(groupsToUse, new Date(aMonthAgo));
        if (recentReviews.review_requests == null || recentReviews.review_requests.length == 0) {
            log.info("No review requests with ship its found for groups {}", groupsToUse);
            return;
        }
        int totalReviewsCount = recentReviews.review_requests.length;
        log.info("Retrieved {} review requests for the last 30 days", totalReviewsCount);

        fetchReviewInfoForRequests(recentReviews.review_requests);
        fetchFileCountsForReviewRequests(recentReviews.review_requests);

        int filteredCount = 0;
        for (int i = 0; i < statsConfig.fileCountRanges.length; i ++) {
            int startRange = i == 0 ? 1 : statsConfig.fileCountRanges[i-1] + 1;
            int endRange = statsConfig.fileCountRanges[i];
            List<ReviewRequest> filteredRequests = filterReviewRequestsByMaxFileCount(recentReviews.review_requests,
                    startRange, endRange);
            Padder padder = new Padder("File count range {} -> {}, ({} requests)", startRange, endRange,
                    filteredRequests.size());
            if (filteredRequests.isEmpty()) {
                padder.infoTitle();
                padder.infoTitle();
                continue;
            }
            filteredCount += filteredRequests.size();
            Map<ReviewStatType, Double> averageStats = generateAverageStats(filteredRequests);
            Map<ReviewStatType, ReviewRequest> highestStats = generateHighestStats(filteredRequests);


            padder.infoTitle();
            printAverageStats(averageStats);
            log.info("");
            printHighestStats(highestStats);
            padder.infoTitle();
        }
        log.info("Processed {} requests", filteredCount);
        if (filteredCount < totalReviewsCount) {
            log.info("{} review requests were not included in ranges {}",
                    (totalReviewsCount - filteredCount), Arrays.toString(statsConfig.fileCountRanges));
        }
    }

    private void printHighestStats(Map<ReviewStatType, ReviewRequest> highestStats) {
        for (ReviewStatType statType : ReviewStatType.values()) {
            String label = StringUtils.splitOnCapitalization(statType.name());
            ReviewRequest reviewRequest = highestStats.get(statType);
            long statValue = reviewRequest.stats.get(statType);
            log.info("Highest {} - {} (Request {})", label, formatStatValue(statType, statValue), reviewRequest.id);
        }
    }

    private void printAverageStats(Map<ReviewStatType, Double> averageStats) {
        for (ReviewStatType statType : ReviewStatType.values()) {
            String label = StringUtils.splitOnCapitalization(statType.name());
            log.info("Average {} - {}", label, formatStatValue(statType, averageStats.get(statType)));
        }
    }

    private String formatStatValue(ReviewStatType statType, double value) {
        if (statType.isBasedOnDiffCount()) {
            DecimalFormat df = new DecimalFormat("#.##");
            return df.format(value);
        }

        long timeInHours = HOURS.convert((long) value, TimeUnit.MINUTES);
        if (timeInHours >= 24) {
            long timeInDays = DAYS.convert((long) value, TimeUnit.MINUTES);
            long fullDaysInHours = HOURS.convert(timeInDays, DAYS);
            return timeInDays + " days, " + (timeInHours - fullDaysInHours) + " hours";
        }
        return timeInHours + " hours";
    }

    private List<ReviewRequest> filterReviewRequestsByMaxFileCount(ReviewRequest[] reviewRequests,
                                                                   int startRange, int endRange) {
        // allow for stupidity
        startRange = Math.min(startRange, endRange);
        endRange = Math.max(startRange, endRange);

        List<ReviewRequest> filteredRequests = new ArrayList<ReviewRequest>();
        for (ReviewRequest reviewRequest : reviewRequests) {
            if (reviewRequest.fileCount >= startRange && reviewRequest.fileCount <= endRange) {
                filteredRequests.add(reviewRequest);
            }
        }
        return filteredRequests;
    }

    private Map<ReviewStatType, Double> generateAverageStats(List<ReviewRequest> reviewRequests) {
        Map<ReviewStatType, Double> aggregateStatTypes = new HashMap<ReviewStatType, Double>();
        for (ReviewStatType statType : ReviewStatType.values()) {
            long totalValue = 0;
            for (ReviewRequest reviewRequest : reviewRequests) {
                long statValue = reviewRequest.stats.get(statType);
                totalValue += statValue;
            }
            double averageValue = reviewRequests.isEmpty() ? 0 : (double) totalValue / (double) reviewRequests.size();
            aggregateStatTypes.put(statType, averageValue);
        }
        return aggregateStatTypes;
    }

    private Map<ReviewStatType, ReviewRequest> generateHighestStats(List<ReviewRequest> reviewRequests) {
        Map<ReviewStatType, ReviewRequest> aggregateStatTypes = new HashMap<ReviewStatType, ReviewRequest>();
        for (ReviewStatType statType : ReviewStatType.values()) {
            long highestValue = 0;
            ReviewRequest requestToUse = null;
            for (ReviewRequest reviewRequest : reviewRequests) {
                Long requestValue = reviewRequest.stats.get(statType);
                if (highestValue < requestValue) {
                    highestValue = requestValue;
                    requestToUse = reviewRequest;
                }
            }
            aggregateStatTypes.put(statType, requestToUse);
        }
        return aggregateStatTypes;
    }

    private void fetchReviewInfoForRequests(ReviewRequest[] recentReviews) {
        log.info("Retrieving user review information for review requests");
        for (ReviewRequest recentReview : recentReviews) {
            Map<ReviewStatType, Long> stats = recentReview.stats;
            UserReview[] userReviews = reviewBoard.getReviewsForReviewRequest(recentReview.getReviewsLink());
            recentReview.userReviews = userReviews;
            for (UserReview userReview : userReviews) {
                long duration = DateUtils.workWeekMinutesBetween(recentReview.timeAdded, userReview.timestamp);
                for (ReviewStatType statType : ReviewStatType.values()) {
                    if (statType.isBasedOnDiffCount()) {
                        continue;
                    }
                    if (statType.isBasedOnShipIt() && !userReview.ship_it) {
                        continue;
                    }
                    Long existingValue = stats.get(statType);
                    if (existingValue == null) {
                        stats.put(statType, duration);
                    } else if (statType.isHigherValueBetter() && duration > existingValue) {
                        stats.put(statType, duration);
                    } else if (!statType.isHigherValueBetter() && duration < existingValue) {
                        stats.put(statType, duration);
                    }
                }
            }
        }
    }

    private void fetchFileCountsForReviewRequests(ReviewRequest[] reviewRequests) {
        log.info("Retrieving diff file counts for review requests");
        for (ReviewRequest reviewRequest : reviewRequests) {
            ReviewRequestDiff[] diffs = reviewBoard.getDiffsForReviewRequest(reviewRequest.getDiffsLink());
            reviewRequest.diffs = diffs;
            reviewRequest.fileCount = reviewBoard.getFilesCountForReviewRequestDiff(diffs[diffs.length -1].getFilesLink());
            reviewRequest.stats.put(ReviewStatType.diffCount, Long.valueOf(reviewRequest.diffs.length));
        }
    }


}

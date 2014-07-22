package com.vmware.reviewboard.domain;

public enum ReviewStatType {

    firstReviewWaitTime,
    firstShipItWaitTime(true, false),
    lastShipItWaitTime(true, true),
    diffCount(false,true, true);

    private boolean basedOnShipIt;
    private boolean higherValueBetter;
    private boolean basedOnDiffCount;

    private ReviewStatType() {
    }

    private ReviewStatType(boolean basedOnShipIt, boolean higherValueBetter) {
        this.basedOnShipIt = basedOnShipIt;
        this.higherValueBetter = higherValueBetter;
    }

    private ReviewStatType(boolean basedOnShipIt, boolean higherValueBetter, boolean basedOnDiffCount) {
        this.basedOnShipIt = basedOnShipIt;
        this.higherValueBetter = higherValueBetter;
        this.basedOnDiffCount = basedOnDiffCount;
    }

    public boolean isBasedOnShipIt() {
        return basedOnShipIt;
    }

    public boolean isHigherValueBetter() {
        return higherValueBetter;
    }

    public boolean isBasedOnDiffCount() {
        return basedOnDiffCount;
    }
}

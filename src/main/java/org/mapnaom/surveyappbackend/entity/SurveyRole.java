package org.mapnaom.surveyappbackend.entity;

public enum SurveyRole {
    MANAGERS(false),
    BOARD(true),
    CUSTOMERS(true),
    SUPPLIERS(true);

    private final boolean allowSkip;

    SurveyRole(boolean allowSkip) {
        this.allowSkip = allowSkip;
    }

    public boolean allowsSkipping() {
        return allowSkip;
    }
}

package com.josie.tripstar.entity.enums;

/** 与 trip_task 的 CHECK 约束保持一致。 */
public enum TaskStatus {
    PROCESSING("processing"), COMPLETED("completed"), FAILED("failed");
    private final String value;
    TaskStatus(String value) { this.value = value; }
    public String getValue() { return value; }
}

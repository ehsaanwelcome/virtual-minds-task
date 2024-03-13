package com.learn.vm.models;

public record StatsResponse(CustomerDayStats customerDayStats, DayStats dayStats) {
    public record CustomerDayStats(long customerId, long request_count, long invalid_count) {}
    public record DayStats(long request_count, long invalid_count) {}
}

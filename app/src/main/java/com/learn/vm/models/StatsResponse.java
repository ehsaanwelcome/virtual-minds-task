package com.learn.vm.models;

import java.util.Date;

public record StatsResponse(long customerId, int day, Date date, CustomerDayStats customerDayStats, DayStats dayStats) {
    public record CustomerDayStats(long request_count, long invalid_count) {}
    public record DayStats(long request_count, long invalid_count) {}
}

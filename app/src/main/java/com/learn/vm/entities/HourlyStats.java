package com.learn.vm.entities;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "hourly_stats")
public record HourlyStats(@Id @GeneratedValue(strategy = GenerationType.AUTO)long id, long customerId, Date time, long request_count, long invalid_count) {

}

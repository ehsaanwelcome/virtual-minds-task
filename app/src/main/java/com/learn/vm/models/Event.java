package com.learn.vm.models;

//public record Event(int customerID, int tagID, String userID, String remoteIP, long timestamp) {
//
//}

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class Event {
    private long customerID;
    private String remoteIP;
    @JsonIgnore
    private long ip;
    private String userID;
    @JsonIgnore
    private String ua;
    @JsonIgnore
    private long timestamp;
}

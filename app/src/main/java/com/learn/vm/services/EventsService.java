package com.learn.vm.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.vm.common.Helpers;
import com.learn.vm.models.Event;
import com.learn.vm.models.EventError;
import com.learn.vm.models.StatsResponse;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

@Service
public class EventsService {
    private static final Logger LOG = LoggerFactory.getLogger(EventsService.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Value(value = "${kafka.topic.name}")
    private String topicName;
    @Value(value = "${spring.datasource.url}")
    private String postgresConn;
    @Value(value = "${spring.datasource.username}")
    private String postgresUser;
    @Value(value = "${spring.datasource.password}")
    private String postgresPass;

    //init kafka producer under the hood, check KafkaProducerConfig
    @Autowired
    private final KafkaTemplate<Long, Boolean> kafkaTemplate;

    //services to check if customer if active
    @Autowired final CustomerService customerService;
    //services to check if ip if active
    @Autowired final IPBlacklistService ipBlacklistService;
    //services to check if ua if active
    @Autowired final UABlacklistService uaBlacklistService;

    public EventsService(KafkaTemplate<Long, Boolean> kafkaTemplate, CustomerService customerService, IPBlacklistService ipBlacklistRepository, UABlacklistService uaBlacklistRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.customerService = customerService;
        this.ipBlacklistService = ipBlacklistRepository;
        this.uaBlacklistService = uaBlacklistRepository;
    }

    private Quartet<Event, Boolean, Boolean, EventError> getTaskResult(Event event, boolean isValidReq, boolean isLogableReq, EventError eventError) {
        return Quartet.with(event, isValidReq, isLogableReq, eventError);
    }

    //send event to kafka
    @Async
    public void logEvent(Event event, boolean isValid) {
        //kafkaTemplate.send(topicName, 0, 1710100000025L, event.getCustomerID() , isValid);
        kafkaTemplate.send(topicName, event.getCustomerID() , isValid);
    }

    //check if req is valid then send event to kafka
    public Quartet<Event, Boolean, Boolean, EventError> processRequest(String uaHeader, InputStream inputStream) {
        var validityResponse = isValidRequest(uaHeader, inputStream);
        var isLogableRequest = validityResponse.getValue2();
        if(isLogableRequest) {
            var event = validityResponse.getValue0();
            var isValidRequest = validityResponse.getValue1();

            //post kafka event
            this.logEvent(event, isValidRequest);
        }
        return validityResponse;
    }

    //check for req validity
    public Quartet<Event, Boolean, Boolean, EventError> isValidRequest(String uaHeader, InputStream inputStream) {
        //parse event
        var event  = parseEvent(uaHeader, inputStream);
        if(event == null)
            return getTaskResult(event, false, false, EventError.Parse);

        //chek for missing fields
        var areFieldsMissing = checkforMissingFields(event);
        if(areFieldsMissing) {
            if(event.getCustomerID() > 0)
                return getTaskResult(event, false, true, EventError.MissingFields);
            return getTaskResult(event, false, false, EventError.MissingFields);
        }

        //check if customer exists
        var isActiveCustomer = this.customerService.findIfCustomerIsActive(event.getCustomerID());
        if(isActiveCustomer == null)
            return getTaskResult(event, false, false, EventError.InvalidCustomer);
        else if(!isActiveCustomer) //is it disable
            return getTaskResult(event, false, true, EventError.DisabledCustomer);

        //check if ip is in block list
        var isBlockedIp = this.ipBlacklistService.findIfIpIsBlocked(event.getIp());
        if(isBlockedIp)
            return getTaskResult(event, false, true, EventError.BlockedIP);

        //check if ua is in blocklist
        var isBlockedUa = this.uaBlacklistService.findIfUaIsBlocked(event.getUa());
        if (isBlockedUa)
            return getTaskResult(event, false, true, EventError.BlockUA);

        return getTaskResult(event, true, true, EventError.None);
    }

    //required fields check
    private boolean checkforMissingFields(Event event) {
        return Helpers.isNullOrEmpty(event.getUserID()) || event.getCustomerID() <= 0 || Helpers.isNullOrEmpty(event.getRemoteIP());
    }

    //simple json parse
    public Event parseEvent(String uaHeader, InputStream inputStream) {
        try {
            Event event =  jsonMapper.readValue(inputStream, Event.class);
            //event.setUa(Helpers.getUA(uaHeader));
            event.setUa(event.getUserID());
            event.setIp(Helpers.ipToNum(event.getRemoteIP()));
            return event;
        } catch (Exception exception) {
            LOG.error("malformed json: {}", exception.getMessage());
            return null;
        }
    }

    public StatsResponse stats(long customerId, int day, Date date) {
        try (var conn = DriverManager.getConnection(postgresConn, postgresUser, postgresPass)) {
            var customerDayStatsQuery = conn.prepareStatement("""
                    select SUM(request_count) request_count, Sum(invalid_count) invalid_count
                    from hourly_stats
                    where customer_id=? and Extract(DOY from time)=?;
                    """);
            customerDayStatsQuery.setLong(1, customerId);
            customerDayStatsQuery.setInt(2, day);

            ResultSet rs = customerDayStatsQuery.executeQuery();
            rs.next();
            var customerDayStats = new StatsResponse.CustomerDayStats(rs.getLong("request_count"), rs.getLong("invalid_count"));

            var dayStatsQuery = conn.prepareStatement("""
                    select SUM(request_count) request_count, Sum(invalid_count) invalid_count
                    from hourly_stats
                    where Extract(DOY from time)=?;
                    """);

            dayStatsQuery.setInt(1, day);

            rs = dayStatsQuery.executeQuery();
            rs.next();
            var dayStats = new StatsResponse.DayStats(rs.getLong("request_count"), rs.getLong("invalid_count"));

            return new StatsResponse(customerId, day, date, customerDayStats, dayStats);
        } catch (SQLException e) {
            return null;
        }
    }
}

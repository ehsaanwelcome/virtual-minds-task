package com.learn.vm.api;

import com.learn.vm.models.EventError;
import com.learn.vm.models.StatsResponse;
import com.learn.vm.services.EventsService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")

public class EventsController {
    private static final Logger LOG = LoggerFactory.getLogger(EventsController.class);
    private static final ResponseEntity<String> badRequest = new ResponseEntity<>("bad request", HttpStatus.BAD_REQUEST);
    private static final ResponseEntity<String> okRequest = new ResponseEntity<>("event logged", HttpStatus.OK);

    @Autowired
    private final EventsService evenetsService;

    public EventsController(EventsService evenetsService) {
        this.evenetsService = evenetsService;
    }

    //meaningful error response
    private String getLogEventErrorMessage(EventError eventError) {
        return switch (eventError) {
            case Parse -> "Parse Error";
            case MissingFields -> "Missing Fields";
            case InvalidCustomer -> "Invalid Customer";
            case DisabledCustomer -> "Disabled Customer";
            case BlockedIP -> "Blocked IP";
            case BlockUA -> "Block UA";
            default -> "Unknown Error";
        };
    }

    //receive request {"customerID":1,"tagID":2,"userID":"aaaaaaaa-bbbb-cccc-1111-222222222222","remoteIP":"123.234.56.78","timestamp":1500000000}
    @PostMapping(value = "/log_event", consumes = "application/json")
    ResponseEntity<String> log_event(HttpServletRequest request) {
        try {
            //pass through validity and log event
            var parsedEvent = evenetsService.processRequest(request.getHeader("User-Agent"), request.getInputStream());

            var isValidRequest = parsedEvent.getValue1();
            var eventError = parsedEvent.getValue3();

            LOG.info("{} {}", "processed", eventError);
            if (!isValidRequest)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(getLogEventErrorMessage(eventError));

            return okRequest;
        } catch (Exception e) {
            LOG.error("unknown error: {}", e.getMessage());
        }
        return badRequest;
    }

    @GetMapping(value = "/stats")
    ResponseEntity<StatsResponse> stats(@RequestParam long customerId, @RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") Optional<Date> date, @RequestParam Optional<Integer> day) {
        int dayOfYear = day.orElse(0);
        if (date.isPresent()) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date.get());

            dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        }
        if (dayOfYear == 0 || customerId <= 0) {
            return ResponseEntity.badRequest().build();
        }

        var dayStats = this.evenetsService.stats(customerId, dayOfYear);
        if (dayStats == null)
            return ResponseEntity.badRequest().build();
        else
            return ResponseEntity.ok(dayStats);
    }
}
package com.learn.vm;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.vm.api.EventsController;
import com.learn.vm.common.Helpers;
import com.learn.vm.models.Event;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest()
@AutoConfigureMockMvc
public class EventsControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Before
    public void settup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(EventsController.class).build();
    }

    @Test
    public void EventsController_LogTests() throws Exception {
        var event = new Event();
        event.setRemoteIP("10.10.10.10");
        event.setUserID("A6-Indexer_Valid");

        //missing fields error
        mockMvc.perform(MockMvcRequestBuilders.post("/api/log_event")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(Helpers.asJsonString(event))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing Fields"))
                .andDo(MockMvcResultHandlers.print());


        //invalid customer error
        event.setCustomerID(5);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/log_event")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(Helpers.asJsonString(event))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid Customer"))
                .andDo(MockMvcResultHandlers.print());

        //disabled customer error
        event.setCustomerID(3);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/log_event")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(Helpers.asJsonString(event))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Disabled Customer"))
                .andDo(MockMvcResultHandlers.print());

        //blocked ip error
        event.setCustomerID(1);
        event.setRemoteIP("0.0.0.0");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/log_event")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(Helpers.asJsonString(event))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Blocked IP"))
                .andDo(MockMvcResultHandlers.print());

        //blocked ua error
        event.setUserID("A6-Indexer");
        event.setRemoteIP("10.10.10.10");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/log_event")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(Helpers.asJsonString(event))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Blocked UA"))
                .andDo(MockMvcResultHandlers.print());

        //event logged case
        event.setUserID("A6-Indexer_Valid");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/log_event")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(Helpers.asJsonString(event))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("event logged"))
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void EventsController_StatTests() throws Exception {
        //missing field exception
        mockMvc.perform(MockMvcRequestBuilders.get("/api/stats?customerId=&date=13.03.2024").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(r -> assertTrue(r.getResolvedException() instanceof MethodArgumentTypeMismatchException))
                .andDo(MockMvcResultHandlers.print());

        //missing field error
        mockMvc.perform(MockMvcRequestBuilders.get("/api/stats?customerId=0&date=13.03.2024").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing Field"))
                .andDo(MockMvcResultHandlers.print());

        //stats response
        mockMvc.perform(MockMvcRequestBuilders.get("/api/stats?customerId=1&date=13.03.2024").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.customerId").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.day").value(73))
                .andDo(MockMvcResultHandlers.print());
    }
}

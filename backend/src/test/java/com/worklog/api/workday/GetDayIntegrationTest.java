package com.worklog.api.workday;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GetDayIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final String TIMEZONE = "Europe/Stockholm";

    @Test
    void getDay_neverStarted_returnsNotWorking() throws Exception {
        final UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/days/2020-06-01").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_WORKING"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.activeTimeBlock").doesNotExist())
                .andExpect(jsonPath("$.totalWorkedMinutes").value(0));
    }

    @Test
    void getDay_afterStartWork_returnsWorking() throws Exception {
        final UUID userId = UUID.randomUUID();
        final UUID timeBlockId = UUID.randomUUID();
        final String date = "2020-06-02";

        final String body = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "timeBlockId", timeBlockId,
                "requestId", UUID.randomUUID(),
                "timestamp", "2020-06-02T07:00:00Z",
                "timezone", TIMEZONE,
                "expectedVersion", 0
        ));
        mockMvc.perform(post("/days/" + date + "/start-work")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/days/" + date).param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WORKING"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.activeTimeBlock.id").value(timeBlockId.toString()))
                .andExpect(jsonPath("$.activeTimeBlock.startedAt").value("2020-06-02T07:00:00Z"))
                .andExpect(jsonPath("$.completedTimeBlocks").isEmpty())
                .andExpect(jsonPath("$.totalWorkedMinutes").value(0));
    }

    @Test
    void getDay_afterStopWork_returnsNotWorkingWithCompletedBlock() throws Exception {
        final UUID userId = UUID.randomUUID();
        final UUID timeBlockId = UUID.randomUUID();
        final String date = "2020-06-03";

        final String startBody = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "timeBlockId", timeBlockId,
                "requestId", UUID.randomUUID(),
                "timestamp", "2020-06-03T07:00:00Z",
                "timezone", TIMEZONE,
                "expectedVersion", 0
        ));
        mockMvc.perform(post("/days/" + date + "/start-work")
                        .contentType(MediaType.APPLICATION_JSON).content(startBody))
                .andExpect(status().isCreated());

        final String stopBody = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "timeBlockId", timeBlockId,
                "requestId", UUID.randomUUID(),
                "timestamp", "2020-06-03T09:00:00Z",
                "timezone", TIMEZONE,
                "expectedVersion", 1
        ));
        mockMvc.perform(post("/days/" + date + "/stop-work")
                        .contentType(MediaType.APPLICATION_JSON).content(stopBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/days/" + date).param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_WORKING"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.activeTimeBlock").doesNotExist())
                .andExpect(jsonPath("$.completedTimeBlocks[0].minutes").value(120))
                .andExpect(jsonPath("$.totalWorkedMinutes").value(120));
    }
}

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StartWorkIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    // 2020-01-15 is a safe past date; Stockholm is CET (UTC+1) in January.
    // 2020-01-15T08:00:00Z = 09:00:00+01:00 → January 15 in Stockholm.
    private static final String DATE = "2020-01-15";
    private static final String TIMESTAMP = "2020-01-15T08:00:00Z";
    private static final String TIMEZONE = "Europe/Stockholm";

    @Test
    void startWork_happyPath_returns201() throws Exception {
        final UUID userId = UUID.randomUUID();
        final UUID timeBlockId = UUID.randomUUID();
        final UUID requestId = UUID.randomUUID();

        final String body = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "timeBlockId", timeBlockId,
                "requestId", requestId,
                "timestamp", TIMESTAMP,
                "timezone", TIMEZONE,
                "expectedVersion", 0
        ));

        mockMvc.perform(post("/days/" + DATE + "/start-work")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.timeBlockId").value(timeBlockId.toString()))
                .andExpect(jsonPath("$.startedAt").value(TIMESTAMP));
    }

    @Test
    void startWork_duplicateRequestId_returns200() throws Exception {
        final UUID userId = UUID.randomUUID();
        final UUID timeBlockId = UUID.randomUUID();
        final UUID requestId = UUID.randomUUID();

        final String body = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "timeBlockId", timeBlockId,
                "requestId", requestId,
                "timestamp", TIMESTAMP,
                "timezone", TIMEZONE,
                "expectedVersion", 0
        ));

        mockMvc.perform(post("/days/" + DATE + "/start-work")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/days/" + DATE + "/start-work")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void startWork_openBlockExists_returns409() throws Exception {
        final UUID userId = UUID.randomUUID();
        final String date = "2020-02-10";
        final String timestamp = "2020-02-10T08:00:00Z";

        final String firstBody = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "timeBlockId", UUID.randomUUID(),
                "requestId", UUID.randomUUID(),
                "timestamp", timestamp,
                "timezone", TIMEZONE,
                "expectedVersion", 0
        ));

        mockMvc.perform(post("/days/" + date + "/start-work")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstBody))
                .andExpect(status().isCreated());

        final String secondBody = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "timeBlockId", UUID.randomUUID(),
                "requestId", UUID.randomUUID(),
                "timestamp", "2020-02-10T09:00:00Z",
                "timezone", TIMEZONE,
                "expectedVersion", 1
        ));

        mockMvc.perform(post("/days/" + date + "/start-work")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("open time block")));
    }

    @Test
    void startWork_futureTimestamp_returns422() throws Exception {
        final String body = objectMapper.writeValueAsString(Map.of(
                "userId", UUID.randomUUID(),
                "timeBlockId", UUID.randomUUID(),
                "requestId", UUID.randomUUID(),
                "timestamp", "2099-01-01T00:00:00Z",
                "timezone", TIMEZONE,
                "expectedVersion", 0
        ));

        mockMvc.perform(post("/days/2099-01-01/start-work")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("future")));
    }

    @Test
    void startWork_versionConflict_returns409() throws Exception {
        final UUID userId = UUID.randomUUID();
        final String date = "2020-03-10";

        final String body = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "timeBlockId", UUID.randomUUID(),
                "requestId", UUID.randomUUID(),
                "timestamp", "2020-03-10T08:00:00Z",
                "timezone", TIMEZONE,
                "expectedVersion", 99
        ));

        mockMvc.perform(post("/days/" + date + "/start-work")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }
}

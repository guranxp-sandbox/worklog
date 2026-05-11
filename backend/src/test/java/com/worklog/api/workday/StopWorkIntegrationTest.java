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
class StopWorkIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private static final String TIMEZONE = "Europe/Stockholm";

    private void startWork(String date, String timestamp, UUID userId, UUID timeBlockId) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "timeBlockId", timeBlockId,
                "requestId", UUID.randomUUID(),
                "timestamp", timestamp,
                "timezone", TIMEZONE,
                "expectedVersion", 0
        ));
        mockMvc.perform(post("/days/" + date + "/start-work")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void stopWork_happyPath_returns201() throws Exception {
        String date = "2020-04-01";
        UUID userId = UUID.randomUUID();
        UUID timeBlockId = UUID.randomUUID();

        startWork(date, "2020-04-01T07:00:00Z", userId, timeBlockId);

        String body = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "timeBlockId", timeBlockId,
                "requestId", UUID.randomUUID(),
                "timestamp", "2020-04-01T15:00:00Z",
                "timezone", TIMEZONE,
                "expectedVersion", 1
        ));

        mockMvc.perform(post("/days/" + date + "/stop-work")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.timeBlockId").value(timeBlockId.toString()))
                .andExpect(jsonPath("$.endedAt").value("2020-04-01T15:00:00Z"));
    }

    @Test
    void stopWork_duplicateRequestId_returns200() throws Exception {
        String date = "2020-04-02";
        UUID userId = UUID.randomUUID();
        UUID timeBlockId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        startWork(date, "2020-04-02T07:00:00Z", userId, timeBlockId);

        String body = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "timeBlockId", timeBlockId,
                "requestId", requestId,
                "timestamp", "2020-04-02T15:00:00Z",
                "timezone", TIMEZONE,
                "expectedVersion", 1
        ));

        mockMvc.perform(post("/days/" + date + "/stop-work")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/days/" + date + "/stop-work")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void stopWork_noOpenBlock_returns422() throws Exception {
        String date = "2020-04-03";
        UUID userId = UUID.randomUUID();

        String body = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "timeBlockId", UUID.randomUUID(),
                "requestId", UUID.randomUUID(),
                "timestamp", "2020-04-03T15:00:00Z",
                "timezone", TIMEZONE,
                "expectedVersion", 0
        ));

        mockMvc.perform(post("/days/" + date + "/stop-work")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("no open time block")));
    }

    @Test
    void stopWork_wrongTimeBlockId_returns422() throws Exception {
        String date = "2020-04-04";
        UUID userId = UUID.randomUUID();
        UUID timeBlockId = UUID.randomUUID();

        startWork(date, "2020-04-04T07:00:00Z", userId, timeBlockId);

        String body = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "timeBlockId", UUID.randomUUID(),
                "requestId", UUID.randomUUID(),
                "timestamp", "2020-04-04T15:00:00Z",
                "timezone", TIMEZONE,
                "expectedVersion", 1
        ));

        mockMvc.perform(post("/days/" + date + "/stop-work")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("does not match")));
    }

    @Test
    void stopWork_futureTimestamp_returns422() throws Exception {
        String date = "2020-04-05";
        UUID userId = UUID.randomUUID();
        UUID timeBlockId = UUID.randomUUID();

        startWork(date, "2020-04-05T07:00:00Z", userId, timeBlockId);

        String body = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "timeBlockId", timeBlockId,
                "requestId", UUID.randomUUID(),
                "timestamp", "2099-01-01T00:00:00Z",
                "timezone", TIMEZONE,
                "expectedVersion", 1
        ));

        mockMvc.perform(post("/days/" + date + "/stop-work")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("future")));
    }

    @Test
    void stopWork_versionConflict_returns409() throws Exception {
        String date = "2020-04-06";
        UUID userId = UUID.randomUUID();
        UUID timeBlockId = UUID.randomUUID();

        startWork(date, "2020-04-06T07:00:00Z", userId, timeBlockId);

        String body = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "timeBlockId", timeBlockId,
                "requestId", UUID.randomUUID(),
                "timestamp", "2020-04-06T15:00:00Z",
                "timezone", TIMEZONE,
                "expectedVersion", 99
        ));

        mockMvc.perform(post("/days/" + date + "/stop-work")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }
}

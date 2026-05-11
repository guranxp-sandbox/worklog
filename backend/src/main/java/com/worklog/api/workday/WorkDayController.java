package com.worklog.api.workday;

import com.worklog.application.workday.CurrentDayView;
import com.worklog.application.workday.DuplicateRequestException;
import com.worklog.application.workday.WorkDayCommandHandler;
import com.worklog.application.workday.WorkDayQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/days")
public class WorkDayController {

    private final WorkDayCommandHandler commandHandler;
    private final WorkDayQueryService queryService;

    public WorkDayController(final WorkDayCommandHandler commandHandler, final WorkDayQueryService queryService) {
        this.commandHandler = commandHandler;
        this.queryService = queryService;
    }

    @GetMapping("/{date}")
    public ResponseEntity<CurrentDayView> getDay(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate date,
            @RequestParam final UUID userId) {
        return ResponseEntity.ok(queryService.getCurrentDayView(userId, date));
    }

    @PostMapping("/{date}/start-work")
    public ResponseEntity<StartWorkResponse> startWork(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate date,
            @RequestBody final StartWorkRequest request) {
        final Instant timestamp = resolveTimestamp(request.timestamp());
        try {
            commandHandler.handle(request.toCommand(date, timestamp));
            return ResponseEntity.status(201).body(new StartWorkResponse(request.timeBlockId(), timestamp));
        } catch (final DuplicateRequestException e) {
            return ResponseEntity.ok(new StartWorkResponse(request.timeBlockId(), timestamp));
        }
    }

    @PostMapping("/{date}/stop-work")
    public ResponseEntity<StopWorkResponse> stopWork(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate date,
            @RequestBody final StopWorkRequest request) {
        final Instant timestamp = resolveTimestamp(request.timestamp());
        try {
            commandHandler.handle(request.toCommand(date, timestamp));
            return ResponseEntity.status(201).body(new StopWorkResponse(request.timeBlockId(), timestamp));
        } catch (final DuplicateRequestException e) {
            return ResponseEntity.ok(new StopWorkResponse(request.timeBlockId(), timestamp));
        }
    }

    private static Instant resolveTimestamp(final Instant requested) {
        return requested != null ? requested : Instant.now();
    }
}

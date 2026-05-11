package com.worklog.api.workday;

import com.worklog.application.workday.CurrentDayView;
import com.worklog.application.workday.DuplicateRequestException;
import com.worklog.application.workday.StartWorkCommand;
import com.worklog.application.workday.StopWorkCommand;
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

    public WorkDayController(WorkDayCommandHandler commandHandler, WorkDayQueryService queryService) {
        this.commandHandler = commandHandler;
        this.queryService = queryService;
    }

    @GetMapping("/{date}")
    public ResponseEntity<CurrentDayView> getDay(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(queryService.getCurrentDayView(userId, date));
    }

    @PostMapping("/{date}/start-work")
    public ResponseEntity<StartWorkResponse> startWork(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody StartWorkRequest request) {

        Instant timestamp = request.timestamp() != null ? request.timestamp() : Instant.now();

        StartWorkCommand command = new StartWorkCommand(
                request.userId(),
                date,
                request.timeBlockId(),
                request.requestId(),
                timestamp,
                request.timezone(),
                request.projectId(),
                request.expectedVersion()
        );

        try {
            commandHandler.handle(command);
            return ResponseEntity.status(201)
                    .body(new StartWorkResponse(request.timeBlockId(), timestamp));
        } catch (DuplicateRequestException e) {
            return ResponseEntity.ok(new StartWorkResponse(request.timeBlockId(), timestamp));
        }
    }

    @PostMapping("/{date}/stop-work")
    public ResponseEntity<StopWorkResponse> stopWork(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody StopWorkRequest request) {

        Instant timestamp = request.timestamp() != null ? request.timestamp() : Instant.now();

        StopWorkCommand command = new StopWorkCommand(
                request.userId(),
                date,
                request.timeBlockId(),
                request.requestId(),
                timestamp,
                request.timezone(),
                request.projectId(),
                request.note(),
                request.expectedVersion()
        );

        try {
            commandHandler.handle(command);
            return ResponseEntity.status(201)
                    .body(new StopWorkResponse(request.timeBlockId(), timestamp));
        } catch (DuplicateRequestException e) {
            return ResponseEntity.ok(new StopWorkResponse(request.timeBlockId(), timestamp));
        }
    }
}

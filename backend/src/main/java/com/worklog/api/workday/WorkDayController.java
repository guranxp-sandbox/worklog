package com.worklog.api.workday;

import com.worklog.application.workday.DuplicateRequestException;
import com.worklog.application.workday.StartWorkCommand;
import com.worklog.application.workday.WorkDayCommandHandler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;

@RestController
@RequestMapping("/days")
public class WorkDayController {

    private final WorkDayCommandHandler commandHandler;

    public WorkDayController(WorkDayCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
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
}

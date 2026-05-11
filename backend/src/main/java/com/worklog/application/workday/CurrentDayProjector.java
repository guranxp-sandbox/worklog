package com.worklog.application.workday;

import com.worklog.domain.DomainEvent;
import com.worklog.domain.workday.WorkDayId;
import com.worklog.domain.workday.events.TimeBlockEnded;
import com.worklog.domain.workday.events.TimeBlockStarted;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CurrentDayProjector {

    private final CurrentDayProjectionRepository projectionRepository;

    public CurrentDayProjector(CurrentDayProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    public void project(WorkDayId workDayId, int newVersion, List<DomainEvent> events) {
        CurrentDayView current = projectionRepository
                .find(workDayId.userId(), workDayId.date())
                .orElseGet(() -> new CurrentDayView(
                        workDayId.date(), CurrentDayView.NOT_WORKING, 0, null, List.of(), 0));

        for (DomainEvent event : events) {
            current = apply(current, event);
        }

        CurrentDayView updated = new CurrentDayView(
                current.date(), current.status(), newVersion,
                current.activeTimeBlock(), current.completedTimeBlocks(), current.totalWorkedMinutes());

        projectionRepository.save(workDayId.userId(), updated);
    }

    private CurrentDayView apply(CurrentDayView view, DomainEvent event) {
        if (event instanceof TimeBlockStarted e) {
            TimeBlockView active = new TimeBlockView(e.timeBlockId(), e.startedAt(), null, e.projectId(), null);
            return new CurrentDayView(view.date(), CurrentDayView.WORKING, view.version(),
                    active, view.completedTimeBlocks(), view.totalWorkedMinutes());
        }

        if (event instanceof TimeBlockEnded e) {
            int minutes = (int) ChronoUnit.MINUTES.between(
                    view.activeTimeBlock().startedAt(), e.endedAt());
            UUID projectId = e.projectId() != null ? e.projectId()
                    : (view.activeTimeBlock() != null ? view.activeTimeBlock().projectId() : null);
            TimeBlockView closed = new TimeBlockView(
                    e.timeBlockId(), view.activeTimeBlock().startedAt(), e.endedAt(), projectId, minutes);

            List<TimeBlockView> completed = new ArrayList<>(view.completedTimeBlocks());
            completed.add(closed);

            return new CurrentDayView(view.date(), CurrentDayView.NOT_WORKING, view.version(),
                    null, completed, view.totalWorkedMinutes() + minutes);
        }

        return view;
    }
}

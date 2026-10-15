package vn.edu.qlhs.timetable.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TimetableReplaceRequest {
    @NotNull
    @Valid
    private List<TimetableSlotRequest> entries = new ArrayList<>();
}

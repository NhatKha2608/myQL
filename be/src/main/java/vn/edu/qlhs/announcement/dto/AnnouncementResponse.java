package vn.edu.qlhs.announcement.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AnnouncementResponse {
    private Long id;
    private Long classroomId;
    private String title;
    private String content;
    private Instant publishedAt;
    private String status;
    private Long createdBy;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
}

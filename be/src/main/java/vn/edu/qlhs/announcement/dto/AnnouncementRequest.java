package vn.edu.qlhs.announcement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnouncementRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    /** DRAFT or PUBLISHED */
    private String status;
}

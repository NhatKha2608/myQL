package vn.edu.qlhs.announcement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qlhs.announcement.dto.AnnouncementRequest;
import vn.edu.qlhs.announcement.dto.AnnouncementResponse;
import vn.edu.qlhs.announcement.entity.AnnouncementEntity;
import vn.edu.qlhs.announcement.repository.AnnouncementRepository;
import vn.edu.qlhs.audit.service.AuditLogService;
import vn.edu.qlhs.common.exception.BusinessException;
import vn.edu.qlhs.common.exception.ResourceNotFoundException;
import vn.edu.qlhs.common.service.ClassroomAccessService;
import vn.edu.qlhs.security.SecurityUtils;
import vn.edu.qlhs.security.UserPrincipal;
import vn.edu.qlhs.user.repository.UserRepository;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final ClassroomAccessService classroomAccessService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listByClassroom(Long classroomId) {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        return announcementRepository.findByClassroomIdOrderByCreatedAtDesc(classroomId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AnnouncementResponse create(Long classroomId, AnnouncementRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        classroomAccessService.requireAccessibleClassroom(classroomId, principal);
        String status = normalizeStatus(request.getStatus());
        AnnouncementEntity entity = AnnouncementEntity.builder()
                .classroomId(classroomId)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .status(status)
                .publishedAt("PUBLISHED".equals(status) ? Instant.now() : null)
                .createdBy(principal.getId())
                .build();
        entity = announcementRepository.save(entity);
        auditLogService.log(principal.getId(), "CREATE", "ANNOUNCEMENT", entity.getId(), entity.getTitle(), true);
        return toResponse(entity);
    }

    @Transactional
    public AnnouncementResponse update(Long id, AnnouncementRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        AnnouncementEntity entity = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo"));
        classroomAccessService.requireAccessibleClassroom(entity.getClassroomId(), principal);
        String status = normalizeStatus(request.getStatus() != null ? request.getStatus() : entity.getStatus());
        entity.setTitle(request.getTitle().trim());
        entity.setContent(request.getContent().trim());
        if ("PUBLISHED".equals(status) && entity.getPublishedAt() == null) {
            entity.setPublishedAt(Instant.now());
        }
        entity.setStatus(status);
        entity = announcementRepository.save(entity);
        auditLogService.log(principal.getId(), "UPDATE", "ANNOUNCEMENT", entity.getId(), entity.getTitle(), true);
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        UserPrincipal principal = SecurityUtils.currentUser();
        AnnouncementEntity entity = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo"));
        classroomAccessService.requireAccessibleClassroom(entity.getClassroomId(), principal);
        announcementRepository.delete(entity);
        auditLogService.log(principal.getId(), "DELETE", "ANNOUNCEMENT", id, entity.getTitle(), true);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "DRAFT";
        }
        String normalized = status.trim().toUpperCase();
        if (!"DRAFT".equals(normalized) && !"PUBLISHED".equals(normalized)) {
            throw new BusinessException("Trạng thái thông báo không hợp lệ");
        }
        return normalized;
    }

    private AnnouncementResponse toResponse(AnnouncementEntity entity) {
        String createdByName = userRepository.findByIdAndDeletedFalse(entity.getCreatedBy())
                .map(u -> u.getFullName())
                .orElse(null);
        return AnnouncementResponse.builder()
                .id(entity.getId())
                .classroomId(entity.getClassroomId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .publishedAt(entity.getPublishedAt())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .createdByName(createdByName)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

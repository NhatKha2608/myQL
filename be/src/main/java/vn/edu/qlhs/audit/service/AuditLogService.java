package vn.edu.qlhs.audit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qlhs.audit.entity.AuditLogEntity;
import vn.edu.qlhs.audit.repository.AuditLogRepository;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(Long userId, String actionType, String entityType, Long entityId, String detail, boolean success) {
        auditLogRepository.save(AuditLogEntity.builder()
                .userId(userId)
                .actionType(actionType)
                .entityType(entityType)
                .entityId(entityId)
                .detail(detail)
                .success(success)
                .build());
    }
}

package vn.edu.qlhs.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.qlhs.audit.entity.AuditLogEntity;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
}

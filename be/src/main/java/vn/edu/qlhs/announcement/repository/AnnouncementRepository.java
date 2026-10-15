package vn.edu.qlhs.announcement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.qlhs.announcement.entity.AnnouncementEntity;

import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, Long> {
    List<AnnouncementEntity> findByClassroomIdOrderByCreatedAtDesc(Long classroomId);

    List<AnnouncementEntity> findByClassroomIdInOrderByCreatedAtDesc(List<Long> classroomIds);

    Optional<AnnouncementEntity> findByIdAndClassroomId(Long id, Long classroomId);
}

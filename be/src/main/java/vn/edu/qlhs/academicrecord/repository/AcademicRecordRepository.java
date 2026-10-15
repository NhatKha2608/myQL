package vn.edu.qlhs.academicrecord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.qlhs.academicrecord.entity.AcademicRecordEntity;

import java.util.List;
import java.util.Optional;

public interface AcademicRecordRepository extends JpaRepository<AcademicRecordEntity, Long> {
    List<AcademicRecordEntity> findByStudentIdOrderByEvaluatedAtDesc(Long studentId);

    List<AcademicRecordEntity> findByClassroomId(Long classroomId);

    Optional<AcademicRecordEntity> findByIdAndClassroomId(Long id, Long classroomId);
}

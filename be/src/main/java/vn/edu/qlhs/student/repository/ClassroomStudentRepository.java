package vn.edu.qlhs.student.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.qlhs.student.entity.ClassroomStudentEntity;
import vn.edu.qlhs.student.entity.StudentEntity;

import java.util.List;
import java.util.Optional;

public interface ClassroomStudentRepository extends JpaRepository<ClassroomStudentEntity, Long> {

    List<ClassroomStudentEntity> findByClassroomIdAndStatusAndDeletedFalse(Long classroomId, String status);

    List<ClassroomStudentEntity> findByStudentIdAndDeletedFalse(Long studentId);

    Optional<ClassroomStudentEntity> findByClassroomIdAndStudentIdAndStatusAndDeletedFalse(
            Long classroomId, Long studentId, String status);

    Optional<ClassroomStudentEntity> findFirstByStudentIdAndStatusAndDeletedFalseOrderByEnrolledAtDesc(
            Long studentId, String status);

    @Query("""
            SELECT COALESCE(MAX(cs.sortOrder), 0) FROM ClassroomStudentEntity cs
            WHERE cs.classroomId = :classroomId
            """)
    Integer findMaxSortOrder(@Param("classroomId") Long classroomId);

    @Query("""
            SELECT s FROM StudentEntity s
            JOIN ClassroomStudentEntity cs ON cs.studentId = s.id
            WHERE cs.classroomId = :classroomId
              AND cs.status = 'ACTIVE'
              AND cs.deleted = false
              AND s.deleted = false
            ORDER BY cs.sortOrder ASC, cs.id ASC
            """)
    List<StudentEntity> findActiveStudentsByClassroom(@Param("classroomId") Long classroomId);
}

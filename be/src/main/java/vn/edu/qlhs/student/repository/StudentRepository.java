package vn.edu.qlhs.student.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.qlhs.student.entity.StudentEntity;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {

    Optional<StudentEntity> findByIdAndDeletedFalse(Long id);

    Optional<StudentEntity> findByStudentCodeAndDeletedFalse(String studentCode);

    boolean existsByStudentCodeAndDeletedFalse(String studentCode);

    boolean existsByStudentCode(String studentCode);

    @Query("""
            SELECT s FROM StudentEntity s
            WHERE s.deleted = false
              AND (:keyword IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR s.status = :status)
              AND (:classroomId IS NULL OR s.id IN (
                    SELECT cs.studentId FROM ClassroomStudentEntity cs
                    WHERE cs.classroomId = :classroomId AND cs.status = 'ACTIVE' AND cs.deleted = false))
            """)
    Page<StudentEntity> search(@Param("keyword") String keyword,
                               @Param("classroomId") Long classroomId,
                               @Param("status") String status,
                               Pageable pageable);

    @Query(value = """
            SELECT s FROM StudentEntity s
            JOIN ClassroomStudentEntity cs ON cs.studentId = s.id
            WHERE s.deleted = false
              AND cs.classroomId = :classroomId
              AND cs.status = 'ACTIVE'
              AND cs.deleted = false
              AND (:keyword IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR s.status = :status)
            ORDER BY cs.sortOrder ASC, cs.id ASC
            """,
            countQuery = """
            SELECT COUNT(s) FROM StudentEntity s
            JOIN ClassroomStudentEntity cs ON cs.studentId = s.id
            WHERE s.deleted = false
              AND cs.classroomId = :classroomId
              AND cs.status = 'ACTIVE'
              AND cs.deleted = false
              AND (:keyword IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR s.status = :status)
            """)
    Page<StudentEntity> searchByClassroomOrder(@Param("keyword") String keyword,
                                               @Param("classroomId") Long classroomId,
                                               @Param("status") String status,
                                               Pageable pageable);
}

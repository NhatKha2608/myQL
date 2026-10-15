package vn.edu.qlhs.classroom.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.qlhs.classroom.entity.ClassroomEntity;

import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<ClassroomEntity, Long> {

    Optional<ClassroomEntity> findByIdAndDeletedFalse(Long id);

    Page<ClassroomEntity> findByDeletedFalse(Pageable pageable);

    List<ClassroomEntity> findByHomeroomTeacherIdAndDeletedFalse(Long teacherId);

    Page<ClassroomEntity> findByHomeroomTeacherIdAndDeletedFalse(Long teacherId, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(cs) > 0 THEN true ELSE false END
            FROM ClassroomStudentEntity cs
            JOIN StudentEntity s ON s.id = cs.studentId
            WHERE cs.classroomId = :classroomId
              AND cs.studentId = :studentId
              AND cs.status = 'ACTIVE'
              AND cs.deleted = false
              AND s.deleted = false
            """)
    boolean isActiveStudentInClassroom(@Param("classroomId") Long classroomId,
                                       @Param("studentId") Long studentId);

    @Query("""
            SELECT COUNT(cs)
            FROM ClassroomStudentEntity cs
            JOIN StudentEntity s ON s.id = cs.studentId
            WHERE cs.classroomId IN :classroomIds
              AND cs.status = 'ACTIVE'
              AND cs.deleted = false
              AND s.deleted = false
            """)
    long countActiveStudentsInClassrooms(@Param("classroomIds") List<Long> classroomIds);
}

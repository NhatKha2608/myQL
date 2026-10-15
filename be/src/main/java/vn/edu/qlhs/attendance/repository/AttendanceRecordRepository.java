package vn.edu.qlhs.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.qlhs.attendance.entity.AttendanceRecordEntity;
import vn.edu.qlhs.common.enums.AttendanceStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecordEntity, Long> {

    List<AttendanceRecordEntity> findByClassroomIdAndAttendanceDate(Long classroomId, LocalDate date);

    Optional<AttendanceRecordEntity> findByClassroomIdAndStudentIdAndAttendanceDate(
            Long classroomId, Long studentId, LocalDate date);

    List<AttendanceRecordEntity> findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
            Long studentId, LocalDate from, LocalDate to);

    @Query("""
            SELECT ar.status, COUNT(ar)
            FROM AttendanceRecordEntity ar
            WHERE ar.classroomId = :classroomId
              AND ar.attendanceDate BETWEEN :from AND :to
            GROUP BY ar.status
            """)
    List<Object[]> countByStatus(@Param("classroomId") Long classroomId,
                                 @Param("from") LocalDate from,
                                 @Param("to") LocalDate to);

    long countByClassroomIdInAndAttendanceDateAndStatus(
            List<Long> classroomIds, LocalDate date, AttendanceStatus status);

    List<AttendanceRecordEntity> findByClassroomIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long classroomId, LocalDate from, LocalDate to);

    List<AttendanceRecordEntity> findByClassroomIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
            Long classroomId, LocalDate from, LocalDate to);
}

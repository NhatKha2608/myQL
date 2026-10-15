package vn.edu.qlhs.score.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.qlhs.score.entity.ScoreTransactionEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScoreTransactionRepository extends JpaRepository<ScoreTransactionEntity, Long> {

    List<ScoreTransactionEntity> findByStudentIdAndClassroomIdOrderByCreatedAtDesc(Long studentId, Long classroomId);

    Page<ScoreTransactionEntity> findByClassroomIdOrderByCreatedAtDesc(Long classroomId, Pageable pageable);

    @Query("""
            SELECT st.studentId, COALESCE(SUM(st.pointsDelta), 0)
            FROM ScoreTransactionEntity st
            WHERE st.classroomId = :classroomId
              AND st.reversedTransactionId IS NULL
              AND NOT EXISTS (
                    SELECT 1 FROM ScoreTransactionEntity rev
                    WHERE rev.reversedTransactionId = st.id
              )
            GROUP BY st.studentId
            ORDER BY COALESCE(SUM(st.pointsDelta), 0) DESC
            """)
    List<Object[]> leaderboard(@Param("classroomId") Long classroomId);

    @Query("""
            SELECT st.studentId, COALESCE(SUM(st.pointsDelta), 0)
            FROM ScoreTransactionEntity st
            WHERE st.classroomId = :classroomId
              AND st.transactionDate BETWEEN :from AND :to
              AND st.reversedTransactionId IS NULL
              AND NOT EXISTS (
                    SELECT 1 FROM ScoreTransactionEntity rev
                    WHERE rev.reversedTransactionId = st.id
              )
            GROUP BY st.studentId
            ORDER BY COALESCE(SUM(st.pointsDelta), 0) DESC
            """)
    List<Object[]> leaderboardInRange(@Param("classroomId") Long classroomId,
                                      @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);

    boolean existsByReversedTransactionId(Long reversedTransactionId);

    @Query("""
            SELECT COALESCE(SUM(st.pointsDelta), 0)
            FROM ScoreTransactionEntity st
            WHERE st.classroomId = :classroomId
              AND st.studentId = :studentId
              AND st.reversedTransactionId IS NULL
              AND NOT EXISTS (
                    SELECT 1 FROM ScoreTransactionEntity rev
                    WHERE rev.reversedTransactionId = st.id
              )
            """)
    Integer sumActivePoints(@Param("classroomId") Long classroomId, @Param("studentId") Long studentId);

    @Query("""
            SELECT COALESCE(SUM(st.pointsDelta), 0)
            FROM ScoreTransactionEntity st
            WHERE st.classroomId = :classroomId
              AND st.studentId = :studentId
              AND st.transactionDate BETWEEN :from AND :to
              AND st.reversedTransactionId IS NULL
              AND NOT EXISTS (
                    SELECT 1 FROM ScoreTransactionEntity rev
                    WHERE rev.reversedTransactionId = st.id
              )
            """)
    Integer sumPointsInRange(@Param("classroomId") Long classroomId,
                             @Param("studentId") Long studentId,
                             @Param("from") LocalDate from,
                             @Param("to") LocalDate to);

    Optional<ScoreTransactionEntity> findByIdAndClassroomId(Long id, Long classroomId);
}

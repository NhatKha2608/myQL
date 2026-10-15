package vn.edu.qlhs.score.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.qlhs.score.entity.ScoreRuleEntity;

import java.util.List;
import java.util.Optional;

public interface ScoreRuleRepository extends JpaRepository<ScoreRuleEntity, Long> {
    List<ScoreRuleEntity> findByClassroomIdOrderByNameAsc(Long classroomId);

    List<ScoreRuleEntity> findByClassroomIdAndActiveTrue(Long classroomId);

    Optional<ScoreRuleEntity> findByIdAndClassroomId(Long id, Long classroomId);
}

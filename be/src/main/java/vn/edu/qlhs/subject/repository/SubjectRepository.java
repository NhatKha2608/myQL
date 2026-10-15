package vn.edu.qlhs.subject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.qlhs.subject.entity.SubjectEntity;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<SubjectEntity, Long> {
    List<SubjectEntity> findByActiveTrueOrderByNameAsc();

    Optional<SubjectEntity> findFirstByNameIgnoreCase(String name);

    boolean existsByCode(String code);
}

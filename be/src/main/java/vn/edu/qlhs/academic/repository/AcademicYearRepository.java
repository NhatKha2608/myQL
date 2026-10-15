package vn.edu.qlhs.academic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.qlhs.academic.entity.AcademicYearEntity;

import java.util.List;

public interface AcademicYearRepository extends JpaRepository<AcademicYearEntity, Long> {
    List<AcademicYearEntity> findByStatusOrderByStartDateDesc(String status);

    List<AcademicYearEntity> findAllByOrderByStartDateDesc();
}

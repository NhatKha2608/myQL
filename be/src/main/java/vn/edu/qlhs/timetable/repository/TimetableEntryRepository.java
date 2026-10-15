package vn.edu.qlhs.timetable.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.qlhs.timetable.entity.TimetableEntryEntity;

import java.util.List;
import java.util.Optional;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntryEntity, Long> {
    List<TimetableEntryEntity> findByClassroomIdOrderByDayOfWeekAscPeriodNumberAsc(Long classroomId);

    List<TimetableEntryEntity> findByClassroomIdAndDayOfWeekOrderByPeriodNumberAsc(Long classroomId, Integer dayOfWeek);

    Optional<TimetableEntryEntity> findByIdAndClassroomId(Long id, Long classroomId);

    void deleteByClassroomIdAndDayOfWeekBetween(Long classroomId, Integer fromDay, Integer toDay);
}

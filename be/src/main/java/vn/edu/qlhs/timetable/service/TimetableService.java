package vn.edu.qlhs.timetable.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qlhs.audit.service.AuditLogService;
import vn.edu.qlhs.common.exception.BusinessException;
import vn.edu.qlhs.common.exception.ResourceNotFoundException;
import vn.edu.qlhs.common.service.ClassroomAccessService;
import vn.edu.qlhs.security.SecurityUtils;
import vn.edu.qlhs.security.UserPrincipal;
import vn.edu.qlhs.subject.entity.SubjectEntity;
import vn.edu.qlhs.subject.repository.SubjectRepository;
import vn.edu.qlhs.subject.service.SubjectService;
import vn.edu.qlhs.timetable.dto.TimetableEntryRequest;
import vn.edu.qlhs.timetable.dto.TimetableEntryResponse;
import vn.edu.qlhs.timetable.dto.TimetableReplaceRequest;
import vn.edu.qlhs.timetable.dto.TimetableSlotRequest;
import vn.edu.qlhs.timetable.entity.TimetableEntryEntity;
import vn.edu.qlhs.timetable.repository.TimetableEntryRepository;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableEntryRepository timetableEntryRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectService subjectService;
    private final ClassroomAccessService classroomAccessService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> listByClassroom(Long classroomId) {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        return timetableEntryRepository.findByClassroomIdOrderByDayOfWeekAscPeriodNumberAsc(classroomId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<TimetableEntryResponse> replace(Long classroomId, TimetableReplaceRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        classroomAccessService.requireAccessibleClassroom(classroomId, principal);

        List<TimetableSlotRequest> slots = request.getEntries() == null ? List.of() : request.getEntries();
        Set<String> seen = new HashSet<>();
        for (TimetableSlotRequest slot : slots) {
            String key = slot.getDayOfWeek() + "-" + slot.getPeriodNumber();
            if (!seen.add(key)) {
                throw new BusinessException("Trùng tiết trong cùng một ngày");
            }
            validateTimes(slot.getStartTime(), slot.getEndTime());
        }

        timetableEntryRepository.deleteByClassroomIdAndDayOfWeekBetween(classroomId, 1, 5);
        timetableEntryRepository.flush();

        for (TimetableSlotRequest slot : slots) {
            SubjectEntity subject = resolveSubject(slot);
            timetableEntryRepository.save(TimetableEntryEntity.builder()
                    .classroomId(classroomId)
                    .subjectId(subject.getId())
                    .dayOfWeek(slot.getDayOfWeek())
                    .periodNumber(slot.getPeriodNumber())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .build());
        }

        auditLogService.log(principal.getId(), "REPLACE", "TIMETABLE", classroomId,
                "Lớp " + classroomId + ": " + slots.size() + " tiết", true);
        return listByClassroom(classroomId);
    }

    @Transactional
    public TimetableEntryResponse create(Long classroomId, TimetableEntryRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        classroomAccessService.requireAccessibleClassroom(classroomId, principal);
        validateTimes(request);
        subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        TimetableEntryEntity entity = TimetableEntryEntity.builder()
                .classroomId(classroomId)
                .subjectId(request.getSubjectId())
                .dayOfWeek(request.getDayOfWeek())
                .periodNumber(request.getPeriodNumber())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .room(request.getRoom())
                .note(request.getNote())
                .build();
        entity = timetableEntryRepository.save(entity);
        auditLogService.log(principal.getId(), "CREATE", "TIMETABLE", entity.getId(),
                "Lớp " + classroomId + " tiết " + request.getPeriodNumber(), true);
        return toResponse(entity);
    }

    @Transactional
    public TimetableEntryResponse update(Long id, TimetableEntryRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        TimetableEntryEntity entity = timetableEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tiết học"));
        classroomAccessService.requireAccessibleClassroom(entity.getClassroomId(), principal);
        validateTimes(request);
        subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        entity.setSubjectId(request.getSubjectId());
        entity.setDayOfWeek(request.getDayOfWeek());
        entity.setPeriodNumber(request.getPeriodNumber());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        entity.setRoom(request.getRoom());
        entity.setNote(request.getNote());
        entity = timetableEntryRepository.save(entity);
        auditLogService.log(principal.getId(), "UPDATE", "TIMETABLE", entity.getId(),
                "Tiết " + request.getPeriodNumber(), true);
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        UserPrincipal principal = SecurityUtils.currentUser();
        TimetableEntryEntity entity = timetableEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tiết học"));
        classroomAccessService.requireAccessibleClassroom(entity.getClassroomId(), principal);
        timetableEntryRepository.delete(entity);
        auditLogService.log(principal.getId(), "DELETE", "TIMETABLE", id, null, true);
    }

    private void validateTimes(TimetableEntryRequest request) {
        validateTimes(request.getStartTime(), request.getEndTime());
    }

    private void validateTimes(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new BusinessException("Giờ kết thúc phải sau giờ bắt đầu");
        }
    }

    private SubjectEntity resolveSubject(TimetableSlotRequest slot) {
        if (slot.getSubjectId() != null) {
            return subjectRepository.findById(slot.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));
        }
        String name = slot.getSubjectName() == null ? "" : slot.getSubjectName().trim();
        if (name.isEmpty()) {
            throw new BusinessException("Mỗi tiết đã điền cần có tên môn học");
        }
        return subjectService.findOrCreate(name);
    }

    private TimetableEntryResponse toResponse(TimetableEntryEntity entity) {
        SubjectEntity subject = subjectRepository.findById(entity.getSubjectId()).orElse(null);
        return TimetableEntryResponse.builder()
                .id(entity.getId())
                .classroomId(entity.getClassroomId())
                .subjectId(entity.getSubjectId())
                .subjectName(subject != null ? subject.getName() : null)
                .subjectCode(subject != null ? subject.getCode() : null)
                .dayOfWeek(entity.getDayOfWeek())
                .periodNumber(entity.getPeriodNumber())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .room(entity.getRoom())
                .note(entity.getNote())
                .build();
    }
}

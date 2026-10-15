package vn.edu.qlhs.academicrecord.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qlhs.academicrecord.dto.AcademicRecordRequest;
import vn.edu.qlhs.academicrecord.dto.AcademicRecordResponse;
import vn.edu.qlhs.academicrecord.entity.AcademicRecordEntity;
import vn.edu.qlhs.academicrecord.repository.AcademicRecordRepository;
import vn.edu.qlhs.audit.service.AuditLogService;
import vn.edu.qlhs.common.enums.UserRole;
import vn.edu.qlhs.common.exception.BusinessException;
import vn.edu.qlhs.common.exception.ResourceNotFoundException;
import vn.edu.qlhs.common.service.ClassroomAccessService;
import vn.edu.qlhs.security.SecurityUtils;
import vn.edu.qlhs.security.UserPrincipal;
import vn.edu.qlhs.student.entity.StudentEntity;
import vn.edu.qlhs.student.repository.ClassroomStudentRepository;
import vn.edu.qlhs.student.repository.StudentRepository;
import vn.edu.qlhs.subject.entity.SubjectEntity;
import vn.edu.qlhs.subject.repository.SubjectRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcademicRecordService {

    private final AcademicRecordRepository academicRecordRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final ClassroomAccessService classroomAccessService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<AcademicRecordResponse> listByClassroom(Long classroomId) {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        return academicRecordRepository.findByClassroomId(classroomId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcademicRecordResponse> listByStudent(Long studentId) {
        assertStudentAccess(studentId);
        return academicRecordRepository.findByStudentIdOrderByEvaluatedAtDesc(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AcademicRecordResponse create(AcademicRecordRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        classroomAccessService.requireAccessibleClassroom(request.getClassroomId(), principal);
        classroomAccessService.assertStudentInClassroom(request.getClassroomId(), request.getStudentId());
        subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        AcademicRecordEntity entity = AcademicRecordEntity.builder()
                .classroomId(request.getClassroomId())
                .studentId(request.getStudentId())
                .subjectId(request.getSubjectId())
                .evaluationPeriod(request.getEvaluationPeriod().trim())
                .evaluationType(request.getEvaluationType().trim())
                .scoreValue(request.getScoreValue())
                .gradeLevelText(request.getGradeLevelText())
                .commentText(request.getCommentText())
                .evaluatedAt(request.getEvaluatedAt())
                .createdBy(principal.getId())
                .build();
        entity = academicRecordRepository.save(entity);
        auditLogService.log(principal.getId(), "CREATE", "ACADEMIC_RECORD", entity.getId(),
                "HS " + request.getStudentId(), true);
        return toResponse(entity);
    }

    @Transactional
    public AcademicRecordResponse update(Long id, AcademicRecordRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        AcademicRecordEntity entity = academicRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học bạ"));
        classroomAccessService.requireAccessibleClassroom(entity.getClassroomId(), principal);
        classroomAccessService.requireAccessibleClassroom(request.getClassroomId(), principal);
        classroomAccessService.assertStudentInClassroom(request.getClassroomId(), request.getStudentId());
        subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        entity.setClassroomId(request.getClassroomId());
        entity.setStudentId(request.getStudentId());
        entity.setSubjectId(request.getSubjectId());
        entity.setEvaluationPeriod(request.getEvaluationPeriod().trim());
        entity.setEvaluationType(request.getEvaluationType().trim());
        entity.setScoreValue(request.getScoreValue());
        entity.setGradeLevelText(request.getGradeLevelText());
        entity.setCommentText(request.getCommentText());
        entity.setEvaluatedAt(request.getEvaluatedAt());
        entity = academicRecordRepository.save(entity);
        auditLogService.log(principal.getId(), "UPDATE", "ACADEMIC_RECORD", entity.getId(),
                "HS " + request.getStudentId(), true);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> classroomStatistics(Long classroomId) {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        List<AcademicRecordEntity> records = academicRecordRepository.findByClassroomId(classroomId);
        Map<Long, List<AcademicRecordEntity>> bySubject = records.stream()
                .filter(r -> r.getScoreValue() != null)
                .collect(Collectors.groupingBy(AcademicRecordEntity::getSubjectId));

        List<Map<String, Object>> subjectStats = new ArrayList<>();
        for (Map.Entry<Long, List<AcademicRecordEntity>> entry : bySubject.entrySet()) {
            List<BigDecimal> scores = entry.getValue().stream()
                    .map(AcademicRecordEntity::getScoreValue)
                    .toList();
            BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = scores.isEmpty()
                    ? BigDecimal.ZERO
                    : sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
            String subjectName = subjectRepository.findById(entry.getKey())
                    .map(SubjectEntity::getName)
                    .orElse(null);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("subjectId", entry.getKey());
            row.put("subjectName", subjectName);
            row.put("count", scores.size());
            row.put("averageScore", avg);
            row.put("maxScore", scores.stream().max(BigDecimal::compareTo).orElse(null));
            row.put("minScore", scores.stream().min(BigDecimal::compareTo).orElse(null));
            subjectStats.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("classroomId", classroomId);
        result.put("totalRecords", records.size());
        result.put("bySubject", subjectStats);
        return result;
    }

    private void assertStudentAccess(Long studentId) {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }
        var enrollment = classroomStudentRepository.findFirstByStudentIdAndStatusAndDeletedFalseOrderByEnrolledAtDesc(studentId, "ACTIVE");
        if (enrollment.isEmpty()) {
            throw new BusinessException("Không tìm thấy lớp của học sinh");
        }
        classroomAccessService.requireAccessibleClassroom(enrollment.get().getClassroomId(), principal);
    }

    private AcademicRecordResponse toResponse(AcademicRecordEntity entity) {
        StudentEntity student = studentRepository.findById(entity.getStudentId()).orElse(null);
        String subjectName = subjectRepository.findById(entity.getSubjectId())
                .map(SubjectEntity::getName)
                .orElse(null);
        return AcademicRecordResponse.builder()
                .id(entity.getId())
                .classroomId(entity.getClassroomId())
                .studentId(entity.getStudentId())
                .studentName(student != null ? student.getFullName() : null)
                .studentCode(student != null ? student.getStudentCode() : null)
                .subjectId(entity.getSubjectId())
                .subjectName(subjectName)
                .evaluationPeriod(entity.getEvaluationPeriod())
                .evaluationType(entity.getEvaluationType())
                .scoreValue(entity.getScoreValue())
                .gradeLevelText(entity.getGradeLevelText())
                .commentText(entity.getCommentText())
                .evaluatedAt(entity.getEvaluatedAt())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

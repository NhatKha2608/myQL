package vn.edu.qlhs.score.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qlhs.audit.service.AuditLogService;
import vn.edu.qlhs.common.exception.BusinessException;
import vn.edu.qlhs.common.exception.ResourceNotFoundException;
import vn.edu.qlhs.common.service.ClassroomAccessService;
import vn.edu.qlhs.score.dto.*;
import vn.edu.qlhs.score.entity.ScoreRuleEntity;
import vn.edu.qlhs.score.entity.ScoreTransactionEntity;
import vn.edu.qlhs.score.repository.ScoreRuleRepository;
import vn.edu.qlhs.score.repository.ScoreTransactionRepository;
import vn.edu.qlhs.security.SecurityUtils;
import vn.edu.qlhs.security.UserPrincipal;
import vn.edu.qlhs.student.entity.StudentEntity;
import vn.edu.qlhs.student.repository.ClassroomStudentRepository;
import vn.edu.qlhs.student.repository.StudentRepository;
import vn.edu.qlhs.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final ScoreRuleRepository scoreRuleRepository;
    private final ScoreTransactionRepository scoreTransactionRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ClassroomAccessService classroomAccessService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<ScoreTransactionResponse> listTransactions(Long classroomId, Pageable pageable) {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        return scoreTransactionRepository.findByClassroomIdOrderByCreatedAtDesc(classroomId, pageable)
                .map(this::toTransactionResponse);
    }

    @Transactional(readOnly = true)
    public List<StudentScoreSummaryResponse> listClassroomScores(Long classroomId) {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        List<StudentEntity> students = classroomStudentRepository.findActiveStudentsByClassroom(classroomId);
        List<StudentScoreSummaryResponse> result = new ArrayList<>();
        for (StudentEntity student : students) {
            Integer total = scoreTransactionRepository.sumActivePoints(classroomId, student.getId());
            result.add(StudentScoreSummaryResponse.builder()
                    .studentId(student.getId())
                    .studentName(student.getFullName())
                    .studentCode(student.getStudentCode())
                    .totalPoints(total != null ? total : 0)
                    .build());
        }
        result.sort((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()));
        return result;
    }

    @Transactional(readOnly = true)
    public List<ScoreRuleResponse> listRules(Long classroomId) {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        return scoreRuleRepository.findByClassroomIdOrderByNameAsc(classroomId).stream()
                .map(this::toRuleResponse)
                .toList();
    }

    @Transactional
    public ScoreRuleResponse createRule(Long classroomId, ScoreRuleRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        classroomAccessService.requireAccessibleClassroom(classroomId, principal);
        ScoreRuleEntity entity = ScoreRuleEntity.builder()
                .classroomId(classroomId)
                .name(request.getName().trim())
                .description(request.getDescription())
                .pointValue(request.getPointValue())
                .category(request.getCategory())
                .active(request.getActive() == null || request.getActive())
                .createdBy(principal.getId())
                .build();
        entity = scoreRuleRepository.save(entity);
        auditLogService.log(principal.getId(), "CREATE", "SCORE_RULE", entity.getId(), entity.getName(), true);
        return toRuleResponse(entity);
    }

    @Transactional
    public ScoreRuleResponse updateRule(Long id, ScoreRuleRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        ScoreRuleEntity entity = scoreRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc điểm"));
        classroomAccessService.requireAccessibleClassroom(entity.getClassroomId(), principal);
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        entity.setPointValue(request.getPointValue());
        entity.setCategory(request.getCategory());
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        entity = scoreRuleRepository.save(entity);
        auditLogService.log(principal.getId(), "UPDATE", "SCORE_RULE", entity.getId(), entity.getName(), true);
        return toRuleResponse(entity);
    }

    @Transactional
    public void deactivateRule(Long id) {
        UserPrincipal principal = SecurityUtils.currentUser();
        ScoreRuleEntity entity = scoreRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc điểm"));
        classroomAccessService.requireAccessibleClassroom(entity.getClassroomId(), principal);
        entity.setActive(false);
        scoreRuleRepository.save(entity);
        auditLogService.log(principal.getId(), "DEACTIVATE", "SCORE_RULE", entity.getId(), entity.getName(), true);
    }

    @Transactional
    public ScoreTransactionResponse createTransaction(Long classroomId, ScoreTransactionRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        classroomAccessService.requireAccessibleClassroom(classroomId, principal);
        classroomAccessService.assertStudentInClassroom(classroomId, request.getStudentId());

        Integer pointsDelta;
        Long ruleId = request.getScoreRuleId();
        if (ruleId != null) {
            ScoreRuleEntity rule = scoreRuleRepository.findByIdAndClassroomId(ruleId, classroomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc điểm"));
            if (!rule.isActive()) {
                throw new BusinessException("Quy tắc điểm đã bị vô hiệu hóa");
            }
            pointsDelta = rule.getPointValue();
        } else {
            if (request.getPointsDelta() == null) {
                throw new BusinessException("Vui lòng nhập điểm khi không chọn quy tắc");
            }
            pointsDelta = request.getPointsDelta();
        }

        ScoreTransactionEntity entity = ScoreTransactionEntity.builder()
                .classroomId(classroomId)
                .studentId(request.getStudentId())
                .scoreRuleId(ruleId)
                .pointsDelta(pointsDelta)
                .reason(request.getReason().trim())
                .note(request.getNote())
                .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now())
                .createdBy(principal.getId())
                .build();
        entity = scoreTransactionRepository.save(entity);
        auditLogService.log(principal.getId(), "CREATE", "SCORE_TRANSACTION", entity.getId(),
                "HS " + request.getStudentId() + ": " + pointsDelta, true);
        return toTransactionResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ScoreTransactionResponse> studentHistory(Long studentId, Long classroomId) {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (classroomId == null) {
            throw new BusinessException("Vui lòng chọn lớp học (classroomId)");
        }
        classroomAccessService.requireAccessibleClassroom(classroomId, principal);
        classroomAccessService.assertStudentInClassroom(classroomId, studentId);
        return scoreTransactionRepository
                .findByStudentIdAndClassroomIdOrderByCreatedAtDesc(studentId, classroomId)
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    @Transactional
    public ScoreTransactionResponse reverse(Long id) {
        UserPrincipal principal = SecurityUtils.currentUser();
        ScoreTransactionEntity original = scoreTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch điểm"));
        classroomAccessService.requireAccessibleClassroom(original.getClassroomId(), principal);

        if (original.getReversedTransactionId() != null) {
            throw new BusinessException("Không thể đảo giao dịch đảo chiều");
        }
        if (scoreTransactionRepository.existsByReversedTransactionId(original.getId())) {
            throw new BusinessException("Giao dịch này đã được đảo chiều");
        }

        ScoreTransactionEntity reverse = ScoreTransactionEntity.builder()
                .classroomId(original.getClassroomId())
                .studentId(original.getStudentId())
                .scoreRuleId(original.getScoreRuleId())
                .pointsDelta(-original.getPointsDelta())
                .reason("Đảo chiều: " + original.getReason())
                .note(original.getNote())
                .transactionDate(LocalDate.now())
                .createdBy(principal.getId())
                .reversedTransactionId(original.getId())
                .build();
        reverse = scoreTransactionRepository.save(reverse);
        auditLogService.log(principal.getId(), "REVERSE", "SCORE_TRANSACTION", reverse.getId(),
                "Đảo GD #" + original.getId(), true);
        return toTransactionResponse(reverse);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> leaderboard(Long classroomId, LocalDate from, LocalDate to) {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        List<Object[]> rows;
        if (from != null && to != null) {
            rows = scoreTransactionRepository.leaderboardInRange(classroomId, from, to);
        } else {
            rows = scoreTransactionRepository.leaderboard(classroomId);
        }
        Map<Long, StudentEntity> students = classroomStudentRepository.findActiveStudentsByClassroom(classroomId)
                .stream()
                .collect(Collectors.toMap(StudentEntity::getId, s -> s));
        List<LeaderboardEntryResponse> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            Long studentId = (Long) row[0];
            Integer points = row[1] instanceof Number n ? n.intValue() : 0;
            StudentEntity student = students.get(studentId);
            if (student == null) {
                student = studentRepository.findById(studentId).orElse(null);
            }
            result.add(LeaderboardEntryResponse.builder()
                    .rank(rank++)
                    .studentId(studentId)
                    .studentName(student != null ? student.getFullName() : null)
                    .studentCode(student != null ? student.getStudentCode() : null)
                    .totalPoints(points)
                    .build());
        }
        return result;
    }

    private ScoreRuleResponse toRuleResponse(ScoreRuleEntity entity) {
        return ScoreRuleResponse.builder()
                .id(entity.getId())
                .classroomId(entity.getClassroomId())
                .name(entity.getName())
                .description(entity.getDescription())
                .pointValue(entity.getPointValue())
                .category(entity.getCategory())
                .active(entity.isActive())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ScoreTransactionResponse toTransactionResponse(ScoreTransactionEntity entity) {
        StudentEntity student = studentRepository.findById(entity.getStudentId()).orElse(null);
        String ruleName = null;
        if (entity.getScoreRuleId() != null) {
            ruleName = scoreRuleRepository.findById(entity.getScoreRuleId())
                    .map(ScoreRuleEntity::getName)
                    .orElse(null);
        }
        String createdByName = userRepository.findByIdAndDeletedFalse(entity.getCreatedBy())
                .map(u -> u.getFullName())
                .orElse(null);
        boolean reversed = scoreTransactionRepository.existsByReversedTransactionId(entity.getId());
        return ScoreTransactionResponse.builder()
                .id(entity.getId())
                .studentId(entity.getStudentId())
                .studentName(student != null ? student.getFullName() : null)
                .pointsDelta(entity.getPointsDelta())
                .reason(entity.getReason())
                .note(entity.getNote())
                .transactionDate(entity.getTransactionDate())
                .scoreRuleId(entity.getScoreRuleId())
                .scoreRuleName(ruleName)
                .createdBy(entity.getCreatedBy())
                .createdByName(createdByName)
                .createdAt(entity.getCreatedAt())
                .reversedTransactionId(entity.getReversedTransactionId())
                .reversed(reversed)
                .build();
    }
}

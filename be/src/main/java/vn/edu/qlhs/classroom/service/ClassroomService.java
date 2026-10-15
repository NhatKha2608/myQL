package vn.edu.qlhs.classroom.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qlhs.academic.entity.AcademicYearEntity;
import vn.edu.qlhs.academic.repository.AcademicYearRepository;
import vn.edu.qlhs.audit.service.AuditLogService;
import vn.edu.qlhs.classroom.dto.ClassroomRequest;
import vn.edu.qlhs.classroom.dto.ClassroomResponse;
import vn.edu.qlhs.classroom.entity.ClassroomEntity;
import vn.edu.qlhs.classroom.repository.ClassroomRepository;
import vn.edu.qlhs.common.enums.UserRole;
import vn.edu.qlhs.common.exception.ResourceNotFoundException;
import vn.edu.qlhs.common.service.ClassroomAccessService;
import vn.edu.qlhs.security.SecurityUtils;
import vn.edu.qlhs.security.UserPrincipal;
import vn.edu.qlhs.student.dto.StudentResponse;
import vn.edu.qlhs.student.entity.StudentEntity;
import vn.edu.qlhs.student.repository.ClassroomStudentRepository;
import vn.edu.qlhs.student.service.StudentMapper;
import vn.edu.qlhs.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final ClassroomAccessService classroomAccessService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<ClassroomResponse> list(Pageable pageable) {
        UserPrincipal principal = SecurityUtils.currentUser();
        Page<ClassroomEntity> page = principal.getRole() == UserRole.ADMIN
                ? classroomRepository.findByDeletedFalse(pageable)
                : classroomRepository.findByHomeroomTeacherIdAndDeletedFalse(principal.getId(), pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ClassroomResponse getById(Long id) {
        ClassroomEntity classroom = classroomAccessService.requireAccessibleClassroom(id, SecurityUtils.currentUser());
        return toResponse(classroom);
    }

    @Transactional
    public ClassroomResponse create(ClassroomRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        AcademicYearEntity year = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy năm học"));
        if (year.getEndDate().isBefore(year.getStartDate())) {
            throw new vn.edu.qlhs.common.exception.BusinessException("Năm học không hợp lệ");
        }
        Long teacherId = principal.getRole() == UserRole.ADMIN
                ? principal.getId()
                : principal.getId();
        ClassroomEntity entity = ClassroomEntity.builder()
                .name(request.getName())
                .gradeLevel(request.getGradeLevel())
                .academicYearId(request.getAcademicYearId())
                .homeroomTeacherId(teacherId)
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();
        entity = classroomRepository.save(entity);
        auditLogService.log(principal.getId(), "CREATE", "CLASSROOM", entity.getId(), entity.getName(), true);
        return toResponse(entity);
    }

    @Transactional
    public ClassroomResponse update(Long id, ClassroomRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        ClassroomEntity classroom = classroomAccessService.requireAccessibleClassroom(id, principal);
        academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy năm học"));
        classroom.setName(request.getName());
        classroom.setGradeLevel(request.getGradeLevel());
        classroom.setAcademicYearId(request.getAcademicYearId());
        classroom.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            classroom.setStatus(request.getStatus());
        }
        classroom = classroomRepository.save(classroom);
        auditLogService.log(principal.getId(), "UPDATE", "CLASSROOM", classroom.getId(), classroom.getName(), true);
        return toResponse(classroom);
    }

    @Transactional
    public void delete(Long id) {
        UserPrincipal principal = SecurityUtils.currentUser();
        ClassroomEntity classroom = classroomAccessService.requireAccessibleClassroom(id, principal);
        classroom.setDeleted(true);
        classroomRepository.save(classroom);
        auditLogService.log(principal.getId(), "DELETE", "CLASSROOM", classroom.getId(), classroom.getName(), true);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> listStudents(Long classroomId) {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        List<StudentEntity> students = classroomStudentRepository.findActiveStudentsByClassroom(classroomId);
        return students.stream().map(StudentMapper::toResponse).toList();
    }

    private ClassroomResponse toResponse(ClassroomEntity entity) {
        long count = classroomStudentRepository.findByClassroomIdAndStatusAndDeletedFalse(entity.getId(), "ACTIVE").size();
        String yearName = academicYearRepository.findById(entity.getAcademicYearId())
                .map(AcademicYearEntity::getName).orElse(null);
        String teacherName = userRepository.findByIdAndDeletedFalse(entity.getHomeroomTeacherId())
                .map(u -> u.getFullName()).orElse(null);
        return ClassroomResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .gradeLevel(entity.getGradeLevel())
                .academicYearId(entity.getAcademicYearId())
                .academicYearName(yearName)
                .homeroomTeacherId(entity.getHomeroomTeacherId())
                .homeroomTeacherName(teacherName)
                .description(entity.getDescription())
                .status(entity.getStatus())
                .studentCount(count)
                .build();
    }
}

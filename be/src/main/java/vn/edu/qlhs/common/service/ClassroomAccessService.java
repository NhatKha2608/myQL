package vn.edu.qlhs.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.qlhs.classroom.entity.ClassroomEntity;
import vn.edu.qlhs.classroom.repository.ClassroomRepository;
import vn.edu.qlhs.common.enums.UserRole;
import vn.edu.qlhs.common.exception.AccessDeniedBusinessException;
import vn.edu.qlhs.common.exception.ResourceNotFoundException;
import vn.edu.qlhs.security.UserPrincipal;

@Service
@RequiredArgsConstructor
public class ClassroomAccessService {

    private final ClassroomRepository classroomRepository;

    public ClassroomEntity requireAccessibleClassroom(Long classroomId, UserPrincipal principal) {
        ClassroomEntity classroom = classroomRepository.findByIdAndDeletedFalse(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
        assertAccess(classroom, principal);
        return classroom;
    }

    public void assertAccess(ClassroomEntity classroom, UserPrincipal principal) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }
        if (principal.getRole() == UserRole.TEACHER
                && classroom.getHomeroomTeacherId().equals(principal.getId())) {
            return;
        }
        throw new AccessDeniedBusinessException("Bạn không có quyền truy cập lớp học này");
    }

    public void assertStudentInClassroom(Long classroomId, Long studentId) {
        if (!classroomRepository.isActiveStudentInClassroom(classroomId, studentId)) {
            throw new vn.edu.qlhs.common.exception.BusinessException("Học sinh không thuộc lớp học này");
        }
    }
}

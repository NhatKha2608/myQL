package vn.edu.qlhs.student.service;

import vn.edu.qlhs.student.dto.StudentResponse;
import vn.edu.qlhs.student.entity.StudentEntity;

public final class StudentMapper {

    private StudentMapper() {
    }

    public static StudentResponse toResponse(StudentEntity entity) {
        return StudentResponse.builder()
                .id(entity.getId())
                .studentCode(entity.getStudentCode())
                .fullName(entity.getFullName())
                .dateOfBirth(entity.getDateOfBirth())
                .gender(entity.getGender())
                .avatarUrl(entity.getAvatarUrl())
                .address(entity.getAddress())
                .status(entity.getStatus())
                .build();
    }
}

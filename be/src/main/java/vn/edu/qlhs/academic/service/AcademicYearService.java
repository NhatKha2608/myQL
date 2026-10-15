package vn.edu.qlhs.academic.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qlhs.academic.dto.AcademicYearResponse;
import vn.edu.qlhs.academic.entity.AcademicYearEntity;
import vn.edu.qlhs.academic.repository.AcademicYearRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;

    @Transactional(readOnly = true)
    public List<AcademicYearResponse> listAll() {
        return academicYearRepository.findAllByOrderByStartDateDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private AcademicYearResponse toResponse(AcademicYearEntity entity) {
        return AcademicYearResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus())
                .build();
    }
}

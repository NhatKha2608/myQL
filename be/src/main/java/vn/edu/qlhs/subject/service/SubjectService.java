package vn.edu.qlhs.subject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qlhs.common.exception.BusinessException;
import vn.edu.qlhs.subject.dto.SubjectRequest;
import vn.edu.qlhs.subject.dto.SubjectResponse;
import vn.edu.qlhs.subject.entity.SubjectEntity;
import vn.edu.qlhs.subject.repository.SubjectRepository;

import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public List<SubjectResponse> listActive() {
        return subjectRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            throw new BusinessException("Vui lòng nhập tên môn học");
        }
        if (subjectRepository.findFirstByNameIgnoreCase(name).isPresent()) {
            throw new BusinessException("Môn học đã tồn tại");
        }
        return toResponse(findOrCreate(name));
    }

    @Transactional
    public SubjectEntity findOrCreate(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new BusinessException("Vui lòng nhập tên môn học");
        }
        return subjectRepository.findFirstByNameIgnoreCase(name)
                .orElseGet(() -> subjectRepository.save(SubjectEntity.builder()
                        .code(uniqueCode(name))
                        .name(name)
                        .active(true)
                        .build()));
    }

    private String uniqueCode(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        String base = normalized.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (base.isEmpty()) {
            base = "MON";
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }
        String candidate = base;
        int suffix = 2;
        while (subjectRepository.existsByCode(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    private SubjectResponse toResponse(SubjectEntity entity) {
        return SubjectResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.isActive())
                .build();
    }
}

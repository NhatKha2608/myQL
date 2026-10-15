package vn.edu.qlhs.student.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.qlhs.audit.service.AuditLogService;
import vn.edu.qlhs.classroom.entity.ClassroomEntity;
import vn.edu.qlhs.classroom.repository.ClassroomRepository;
import vn.edu.qlhs.common.exception.BusinessException;
import vn.edu.qlhs.common.exception.ResourceNotFoundException;
import vn.edu.qlhs.common.service.ClassroomAccessService;
import vn.edu.qlhs.security.SecurityUtils;
import vn.edu.qlhs.security.UserPrincipal;
import vn.edu.qlhs.student.dto.StudentRequest;
import vn.edu.qlhs.student.dto.StudentResponse;
import vn.edu.qlhs.student.entity.ClassroomStudentEntity;
import vn.edu.qlhs.student.entity.StudentEntity;
import vn.edu.qlhs.student.repository.ClassroomStudentRepository;
import vn.edu.qlhs.student.repository.StudentRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomAccessService classroomAccessService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<StudentResponse> search(String keyword, Long classroomId, String status, Pageable pageable) {
        if (classroomId != null) {
            classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
            Pageable ordered = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            return studentRepository.searchByClassroomOrder(
                            emptyToNull(keyword), classroomId, emptyToNull(status), ordered)
                    .map(s -> enrich(s, classroomId));
        }
        return studentRepository.search(emptyToNull(keyword), classroomId, emptyToNull(status), pageable)
                .map(s -> enrich(s, classroomId));
    }

    @Transactional(readOnly = true)
    public StudentResponse getById(Long id) {
        StudentEntity student = studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học sinh"));
        assertCanViewStudent(student.getId());
        return enrich(student, null);
    }

    @Transactional
    public StudentResponse create(StudentRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        String requestedCode = emptyToNull(request.getStudentCode());
        if (requestedCode != null && studentRepository.existsByStudentCodeAndDeletedFalse(requestedCode)) {
            throw new BusinessException("Mã học sinh đã tồn tại");
        }
        StudentEntity entity = mapNew(request);
        if (requestedCode == null) {
            entity = saveWithAutoCode(entity);
        } else {
            entity = studentRepository.save(entity);
        }
        if (request.getClassroomId() != null) {
            enroll(entity.getId(), request.getClassroomId(), principal);
        }
        auditLogService.log(principal.getId(), "CREATE", "STUDENT", entity.getId(), entity.getStudentCode(), true);
        return enrich(entity, request.getClassroomId());
    }

    @Transactional
    public StudentResponse update(Long id, StudentRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        StudentEntity entity = studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học sinh"));
        assertCanViewStudent(id);
        String requestedCode = emptyToNull(request.getStudentCode());
        if (requestedCode != null) {
            studentRepository.findByStudentCodeAndDeletedFalse(requestedCode)
                    .filter(s -> !s.getId().equals(id))
                    .ifPresent(s -> {
                        throw new BusinessException("Mã học sinh đã tồn tại");
                    });
            entity.setStudentCode(requestedCode);
        }
        entity.setFullName(request.getFullName());
        entity.setDateOfBirth(request.getDateOfBirth());
        entity.setGender(request.getGender());
        entity.setAvatarUrl(request.getAvatarUrl());
        entity.setAddress(request.getAddress());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        entity = studentRepository.save(entity);
        if (request.getClassroomId() != null) {
            enroll(entity.getId(), request.getClassroomId(), principal);
        }
        auditLogService.log(principal.getId(), "UPDATE", "STUDENT", entity.getId(), entity.getStudentCode(), true);
        return enrich(entity, request.getClassroomId());
    }

    @Transactional
    public void delete(Long id) {
        UserPrincipal principal = SecurityUtils.currentUser();
        StudentEntity entity = studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học sinh"));
        assertCanViewStudent(id);
        classroomStudentRepository.findByStudentIdAndDeletedFalse(id)
                .forEach(enrollment -> enrollment.setDeleted(true));
        entity.setDeleted(true);
        studentRepository.save(entity);
        auditLogService.log(principal.getId(), "DELETE", "STUDENT", entity.getId(), entity.getStudentCode(), true);
    }

    @Transactional
    public int importExcel(MultipartFile file, Long classroomId) throws IOException {
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("File vượt quá 5MB");
        }
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        UserPrincipal principal = SecurityUtils.currentUser();
        int imported = 0;
        int sortOrder = nextSortOrder(classroomId) - 1;
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            int nameColumn = resolveNameColumn(header);
            Integer birthDateColumn = findColumn(header, "ngaysinh", "ngaythangnamsinh", "dateofbirth", "dob");
            Integer birthYearColumn = findColumn(header, "namsinh");
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String name = cellString(row.getCell(nameColumn));
                if (name == null) {
                    continue;
                }
                LocalDate birthDate = birthDateColumn == null ? null : readBirthDate(row.getCell(birthDateColumn));
                if (birthDate == null && birthYearColumn != null) {
                    birthDate = readBirthDate(row.getCell(birthYearColumn));
                }
                StudentEntity entity = saveWithAutoCode(StudentEntity.builder()
                        .fullName(name)
                        .dateOfBirth(birthDate)
                        .status("ACTIVE")
                        .build());
                sortOrder++;
                enroll(entity.getId(), classroomId, principal, sortOrder);
                imported++;
            }
        }
        auditLogService.log(principal.getId(), "IMPORT", "STUDENT", classroomId, "Import " + imported + " học sinh", true);
        return imported;
    }

    @Transactional(readOnly = true)
    public byte[] importTemplateExcel() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("HocSinh");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("HoTen");
            header.createCell(1).setCellValue("NamSinh");
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("Nguyễn Văn A");
            sample.createCell(1).setCellValue(2018);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(Long classroomId) throws IOException {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        List<StudentEntity> students = classroomStudentRepository.findActiveStudentsByClassroom(classroomId);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("HocSinh");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("MaHS");
            header.createCell(1).setCellValue("HoTen");
            header.createCell(2).setCellValue("NgaySinh");
            header.createCell(3).setCellValue("GioiTinh");
            int r = 1;
            for (StudentEntity s : students) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(s.getStudentCode());
                row.createCell(1).setCellValue(s.getFullName());
                if (s.getDateOfBirth() != null) {
                    row.createCell(2).setCellValue(s.getDateOfBirth().toString());
                }
                if (s.getGender() != null) {
                    row.createCell(3).setCellValue(s.getGender());
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private int nextSortOrder(Long classroomId) {
        Integer max = classroomStudentRepository.findMaxSortOrder(classroomId);
        return (max == null ? 0 : max) + 1;
    }

    private void enroll(Long studentId, Long classroomId, UserPrincipal principal) {
        enroll(studentId, classroomId, principal, nextSortOrder(classroomId));
    }

    private void enroll(Long studentId, Long classroomId, UserPrincipal principal, int sortOrder) {
        classroomAccessService.requireAccessibleClassroom(classroomId, principal);
        classroomStudentRepository.findByClassroomIdAndStudentIdAndStatusAndDeletedFalse(classroomId, studentId, "ACTIVE")
                .ifPresent(cs -> {
                    throw new BusinessException("Học sinh đã được ghi danh vào lớp");
                });
        classroomStudentRepository.save(ClassroomStudentEntity.builder()
                .classroomId(classroomId)
                .studentId(studentId)
                .enrolledAt(LocalDate.now())
                .status("ACTIVE")
                .sortOrder(sortOrder)
                .build());
    }

    private void assertCanViewStudent(Long studentId) {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal.getRole() == vn.edu.qlhs.common.enums.UserRole.ADMIN) {
            return;
        }
        List<ClassroomEntity> classes = classroomRepository.findByHomeroomTeacherIdAndDeletedFalse(principal.getId());
        boolean allowed = classes.stream().anyMatch(c ->
                classroomRepository.isActiveStudentInClassroom(c.getId(), studentId));
        if (!allowed) {
            throw new vn.edu.qlhs.common.exception.AccessDeniedBusinessException("Không có quyền truy cập học sinh này");
        }
    }

    private StudentEntity mapNew(StudentRequest request) {
        return StudentEntity.builder()
                .studentCode(request.getStudentCode())
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .avatarUrl(request.getAvatarUrl())
                .address(request.getAddress())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();
    }

    private StudentResponse enrich(StudentEntity entity, Long classroomId) {
        StudentResponse response = StudentMapper.toResponse(entity);
        if (classroomId != null) {
            classroomRepository.findByIdAndDeletedFalse(classroomId).ifPresent(c -> {
                response.setClassroomId(c.getId());
                response.setClassroomName(c.getName());
            });
        } else {
            classroomStudentRepository.findFirstByStudentIdAndStatusAndDeletedFalseOrderByEnrolledAtDesc(entity.getId(), "ACTIVE")
                    .flatMap(cs -> classroomRepository.findByIdAndDeletedFalse(cs.getClassroomId()))
                    .ifPresent(c -> {
                        response.setClassroomId(c.getId());
                        response.setClassroomName(c.getName());
                    });
        }
        return response;
    }

    private StudentEntity saveWithAutoCode(StudentEntity entity) {
        entity.setStudentCode("TMP-" + UUID.randomUUID());
        entity = studentRepository.saveAndFlush(entity);
        entity.setStudentCode(nextNumericCode(entity.getId()));
        return studentRepository.saveAndFlush(entity);
    }

    private int resolveNameColumn(Row header) {
        Integer column = findColumn(header, "hoten", "hovaten", "ten");
        if (column != null) {
            return column;
        }
        String first = normalizeHeader(header == null ? null : cellString(header.getCell(0)));
        if (first.equals("mahs") || first.equals("mahocsinh") || first.equals("stt")) {
            return 1;
        }
        return 0;
    }

    private Integer findColumn(Row header, String... keys) {
        if (header == null) {
            return null;
        }
        Set<String> wanted = Set.of(keys);
        short last = header.getLastCellNum();
        for (int c = 0; c < last; c++) {
            if (wanted.contains(normalizeHeader(cellString(header.getCell(c))))) {
                return c;
            }
        }
        return null;
    }

    static LocalDate readBirthDate(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        CellType type = cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType()
                : cell.getCellType();
        if (type == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        return parseBirthDate(cellString(cell));
    }

    static LocalDate parseBirthDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        if (value.matches("\\d{4}")) {
            int year = Integer.parseInt(value);
            int currentYear = LocalDate.now().getYear();
            if (year >= 1980 && year <= currentYear) {
                return LocalDate.of(year, 1, 1);
            }
            return null;
        }
        for (DateTimeFormatter formatter : BIRTH_DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // thử định dạng tiếp theo
            }
        }
        return null;
    }

    private static final DateTimeFormatter[] BIRTH_DATE_FORMATS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE,
            strictDate("d/M/uuuu"),
            strictDate("d-M-uuuu"),
            strictDate("d.M.uuuu"),
    };

    private static DateTimeFormatter strictDate(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT);
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        String stripped = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return stripped.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private String nextNumericCode(Long id) {
        String candidate = String.valueOf(id);
        if (!studentRepository.existsByStudentCode(candidate)) {
            return candidate;
        }
        long next = id;
        do {
            next++;
            candidate = String.valueOf(next);
        } while (studentRepository.existsByStudentCode(candidate));
        return candidate;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String cellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType()
                : cell.getCellType();
        if (type == CellType.STRING) {
            String value = cell.getStringCellValue();
            return value != null && !value.isBlank() ? value.trim() : null;
        }
        if (type == CellType.NUMERIC) {
            double number = cell.getNumericCellValue();
            if (number == Math.rint(number)) {
                return String.valueOf((long) number);
            }
            return String.valueOf(number);
        }
        return null;
    }
}

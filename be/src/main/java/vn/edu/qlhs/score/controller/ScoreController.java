package vn.edu.qlhs.score.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import vn.edu.qlhs.common.dto.ApiResponse;
import vn.edu.qlhs.common.dto.PageResponse;
import vn.edu.qlhs.score.dto.*;
import vn.edu.qlhs.score.service.ScoreService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    @GetMapping("/api/v1/classrooms/{classroomId}/scores")
    public ApiResponse<List<StudentScoreSummaryResponse>> listScores(@PathVariable Long classroomId) {
        return ApiResponse.ok(scoreService.listClassroomScores(classroomId));
    }

    @GetMapping("/api/v1/classrooms/{classroomId}/score-transactions")
    public ApiResponse<PageResponse<ScoreTransactionResponse>> listTransactions(
            @PathVariable Long classroomId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(scoreService.listTransactions(classroomId, pageable)));
    }

    @GetMapping("/api/v1/classrooms/{classroomId}/score-rules")
    public ApiResponse<List<ScoreRuleResponse>> listRules(@PathVariable Long classroomId) {
        return ApiResponse.ok(scoreService.listRules(classroomId));
    }

    @PostMapping("/api/v1/classrooms/{classroomId}/score-rules")
    public ApiResponse<ScoreRuleResponse> createRule(
            @PathVariable Long classroomId,
            @Valid @RequestBody ScoreRuleRequest request) {
        return ApiResponse.ok(scoreService.createRule(classroomId, request));
    }

    @PutMapping("/api/v1/score-rules/{id}")
    public ApiResponse<ScoreRuleResponse> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody ScoreRuleRequest request) {
        return ApiResponse.ok(scoreService.updateRule(id, request));
    }

    @DeleteMapping("/api/v1/score-rules/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        scoreService.deactivateRule(id);
        return ApiResponse.okMessage("Vô hiệu hóa quy tắc điểm thành công");
    }

    @PostMapping("/api/v1/classrooms/{classroomId}/score-transactions")
    public ApiResponse<ScoreTransactionResponse> createTransaction(
            @PathVariable Long classroomId,
            @Valid @RequestBody ScoreTransactionRequest request) {
        return ApiResponse.ok(scoreService.createTransaction(classroomId, request));
    }

    @GetMapping("/api/v1/students/{studentId}/score-history")
    public ApiResponse<List<ScoreTransactionResponse>> scoreHistory(
            @PathVariable Long studentId,
            @RequestParam Long classroomId) {
        return ApiResponse.ok(scoreService.studentHistory(studentId, classroomId));
    }

    @PostMapping("/api/v1/score-transactions/{id}/reverse")
    public ApiResponse<ScoreTransactionResponse> reverse(@PathVariable Long id) {
        return ApiResponse.ok(scoreService.reverse(id));
    }

    @GetMapping("/api/v1/classrooms/{classroomId}/leaderboard")
    public ApiResponse<List<LeaderboardEntryResponse>> leaderboard(
            @PathVariable Long classroomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(scoreService.leaderboard(classroomId, from, to));
    }
}

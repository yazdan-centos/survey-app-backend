package org.mapnaom.surveyappbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.question.CreateQuestionRequest;
import org.mapnaom.surveyappbackend.dto.question.QuestionResponseDto;
import org.mapnaom.surveyappbackend.dto.question.QuestionSearchRequest;
import org.mapnaom.surveyappbackend.entity.Question;
import org.mapnaom.surveyappbackend.service.QuestionExcelService;
import org.mapnaom.surveyappbackend.service.QuestionService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionExcelService questionExcelService;

    @GetMapping("/search")
    public ResponseEntity<Page<QuestionResponseDto>> searchQuestions(
            @Valid @ModelAttribute QuestionSearchRequest filter,
            @PageableDefault(size = 20, sort = "displayOrder") Pageable pageable) {
        return ResponseEntity.ok(questionService.search(filter, pageable));
    }

    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<QuestionResponseDto>> searchQuestionsWithBody(
            @Valid @RequestBody QuestionSearchRequest filter,
            @PageableDefault(size = 20, sort = "displayOrder") Pageable pageable) {
        return ResponseEntity.ok(questionService.search(filter, pageable));
    }

    @PostMapping
    public ResponseEntity<Question> createQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        return ResponseEntity.ok(questionService.create(request));
    }

    @GetMapping("/survey/{surveyId}")
    public ResponseEntity<List<QuestionResponseDto>> getQuestionsBySurveyId(@PathVariable Long surveyId) {
        return ResponseEntity.ok(questionService.getSurveyQuestions(surveyId));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importQuestionsFromExcel(
            @RequestParam Long surveyId,
            @RequestParam MultipartFile file) {
        questionExcelService.importQuestions(surveyId, file);
        return ResponseEntity.ok("Questions imported successfully");
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportQuestionsToExcel(@RequestParam Long surveyId) {
        byte[] data = questionExcelService.exportQuestions(surveyId);

        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=questions.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(resource);
    }

    @GetMapping("/template")
    public ResponseEntity<ByteArrayResource> downloadQuestionImportTemplate() {
        byte[] data = questionExcelService.downloadWorksheetTemplate();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=questions-template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }
}

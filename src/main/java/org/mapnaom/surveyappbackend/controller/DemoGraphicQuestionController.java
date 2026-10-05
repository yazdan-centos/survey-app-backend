package org.mapnaom.surveyappbackend.controller;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.entity.DemoGraphicQuestion;
import org.mapnaom.surveyappbackend.service.DemoGraphicQuestionService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/demographic-questions")
@RequiredArgsConstructor
public class DemoGraphicQuestionController {
    private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final DemoGraphicQuestionService service;

    @GetMapping
    public ResponseEntity<List<DemoGraphicQuestion>> getAll(@RequestParam(required = false) String groupKey) {
        return ResponseEntity.ok(groupKey == null ? service.findAll() : service.findByGroupKey(groupKey));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemoGraphicQuestion> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<DemoGraphicQuestion> create(@RequestBody DemoGraphicQuestion question) {
        return ResponseEntity.ok(service.create(question));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DemoGraphicQuestion> update(@PathVariable Long id, @RequestBody DemoGraphicQuestion question) {
        return ResponseEntity.ok(service.update(id, question));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<DemoGraphicQuestion>> importFromExcelFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.importFromExcelFile(file));
    }

    @GetMapping("/template")
    public ResponseEntity<ByteArrayResource> downloadTemplateExcelFile() {
        byte[] data = service.downloadWorksheetTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=demographic-questions-template.xlsx")
                .contentType(EXCEL_MEDIA_TYPE)
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }
}

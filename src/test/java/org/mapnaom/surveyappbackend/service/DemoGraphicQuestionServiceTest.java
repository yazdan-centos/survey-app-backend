package org.mapnaom.surveyappbackend.service;

import jakarta.persistence.EntityManager;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.entity.DemoGraphicQuestion;
import org.mapnaom.surveyappbackend.repository.DemoGraphicQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.auto_quote_keyword=true",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
@Import(DemoGraphicQuestionService.class)
class DemoGraphicQuestionServiceTest {
    @Autowired DemoGraphicQuestionService service;
    @Autowired DemoGraphicQuestionRepository repository;
    @Autowired EntityManager entityManager;

    @Test
    void createsUpdatesReadsAndDeletesOrderedOptions() {
        DemoGraphicQuestion created = service.create(question("board", 0, "Your role?", "Director", "Other"));
        entityManager.clear();
        assertThat(service.findById(created.getId()).getOptions()).containsExactly("Director", "Other");

        service.update(created.getId(), question("board", 1, "Your position?", "Manager", "Expert"));
        entityManager.flush();
        entityManager.clear();
        assertThat(service.findByGroupKey("board")).singleElement().satisfies(q -> {
            assertThat(q.getId()).isEqualTo(created.getId());
            assertThat(q.getQuestion()).isEqualTo("Your position?");
            assertThat(q.getDisplayOrder()).isEqualTo(1);
            assertThat(q.getOptions()).containsExactly("Manager", "Expert");
        });
        assertThat(service.findAll()).hasSize(1);
        service.delete(created.getId());
        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void rejectsDuplicatePositionsAndMissingIds() {
        service.create(question("managers", 0, "First?", "Yes"));
        assertThatThrownBy(() -> service.create(question("managers", 0, "Second?", "No")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);
        assertThatThrownBy(() -> service.findById(10035L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void importsWorkbookWithOptionalTypeAndKeepsOptionOrder() throws Exception {
        var file = workbook(new String[]{"managers", "Your role?", "", "0", "Director", "Other"},
                new String[]{"stakeholders", "Your organization?", "select", "0", "Private", "Public"});
        assertThat(service.importFromExcelFile(file)).hasSize(2);
        entityManager.clear();
        assertThat(service.findByGroupKey("managers")).singleElement().satisfies(q -> {
            assertThat(q.getType()).isNull();
            assertThat(q.getOptions()).containsExactly("Director", "Other");
        });
        assertThat(service.findByGroupKey("stakeholders")).singleElement().satisfies(q -> {
            assertThat(q.getType()).isEqualTo("select");
            assertThat(q.getOptions()).containsExactly("Private", "Public");
        });
    }

    @Test
    void invalidLaterRowDoesNotImportEarlierRows() throws Exception {
        var file = workbook(new String[]{"board", "First?", "", "0", "Yes", "No"},
                new String[]{"board", "Second?", "", "0", "Yes", "No"});
        assertThatThrownBy(() -> service.importFromExcelFile(file))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);
        assertThat(repository.count()).isZero();
    }

    @Test
    void generatedTemplateCanBeFilledAndImported() throws Exception {
        byte[] template = service.downloadWorksheetTemplate();
        byte[] filled;
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(template));
             var output = new ByteArrayOutputStream()) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("groupKey");
            assertThat(sheet.getRow(0).getCell(16).getStringCellValue()).isEqualTo("option13");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("board");
            row.createCell(1).setCellValue("Your position?");
            row.createCell(3).setCellValue(0);
            row.createCell(4).setCellValue("Director");
            row.createCell(5).setCellValue("Other");
            workbook.write(output);
            filled = output.toByteArray();
        }
        var file = new MockMultipartFile("file", "demographics.xlsx", "application/octet-stream", filled);
        assertThat(service.importFromExcelFile(file)).singleElement()
                .satisfies(question -> assertThat(question.getOptions()).containsExactly("Director", "Other"));
    }

    private DemoGraphicQuestion question(String group, int order, String text, String... options) {
        DemoGraphicQuestion result = new DemoGraphicQuestion();
        result.setGroupKey(group);
        result.setDisplayOrder(order);
        result.setQuestion(text);
        result.setOptions(List.of(options));
        return result;
    }

    private MockMultipartFile workbook(String[]... rows) throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Demographics");
            var header = sheet.createRow(0);
            String[] names = {"groupKey", "question", "type", "displayOrder", "option1", "option2"};
            for (int i = 0; i < names.length; i++) header.createCell(i).setCellValue(names[i]);
            for (int i = 0; i < rows.length; i++) {
                var row = sheet.createRow(i + 1);
                for (int j = 0; j < rows[i].length; j++) row.createCell(j).setCellValue(rows[i][j]);
            }
            workbook.write(output);
            return new MockMultipartFile("file", "demographics.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }
}

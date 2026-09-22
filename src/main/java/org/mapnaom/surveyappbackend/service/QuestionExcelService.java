package org.mapnaom.surveyappbackend.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mapnaom.surveyappbackend.entity.Question;
import org.mapnaom.surveyappbackend.entity.QuestionLevel;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.entity.SurveyRole;
import org.mapnaom.surveyappbackend.repository.QuestionRepository;
import org.mapnaom.surveyappbackend.repository.SurveyRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestionExcelService {

    private static final List<String> TEMPLATE_HEADERS = List.of(
            "code", "text", "role", "level_title", "level_score", "level_order");

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;

    public byte[] downloadWorksheetTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Questions");
            sheet.createFreezePane(0, 1);
            Row header = sheet.createRow(0);

            for (int i = 0; i < TEMPLATE_HEADERS.size(); i++) {
                header.createCell(i).setCellValue(TEMPLATE_HEADERS.get(i));
                sheet.setColumnWidth(i, i == 1 ? 60 * 256 : 20 * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate question worksheet template", e);
        }
    }

    public void importQuestions(UUID surveyId, MultipartFile file) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new RuntimeException("Survey not found"));

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            Map<String, Question> questionMap = new LinkedHashMap<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String code = getString(row.getCell(0));
                String text = getString(row.getCell(1));
                String roleStr = getString(row.getCell(2));
                String levelTitle = getString(row.getCell(3));
                Integer levelScore = getInteger(row.getCell(4));
                Integer levelOrder = getInteger(row.getCell(5));

                String key = code + "_" + roleStr;

                Question question = questionMap.computeIfAbsent(key, k -> {
                    Question q = new Question();
                    q.setSurvey(survey);
                    q.setCode(code);
                    q.setText(text);
                    q.setRole(SurveyRole.valueOf(roleStr));
                    q.setLevels(new ArrayList<>());
                    return q;
                });

                QuestionLevel level = new QuestionLevel();
                level.setQuestion(question);
                level.setTitle(Double.parseDouble(levelTitle));
                level.setScore(levelScore);
                level.setLevelOrder(levelOrder);

                question.getLevels().add(level);
            }

            questionRepository.saveAll(questionMap.values());

        } catch (Exception e) {
            throw new RuntimeException("Failed to import Excel file", e);
        }
    }

    public byte[] exportQuestions(UUID surveyId) {
        List<Question> questions = questionRepository.findBySurveyId(surveyId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Questions");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("code");
            header.createCell(1).setCellValue("text");
            header.createCell(2).setCellValue("role");
            header.createCell(3).setCellValue("level_title");
            header.createCell(4).setCellValue("level_score");
            header.createCell(5).setCellValue("level_order");

            int rowNum = 1;
            for (Question question : questions) {
                if (question.getLevels() == null || question.getLevels().isEmpty()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(question.getCode());
                    row.createCell(1).setCellValue(question.getText());
                    row.createCell(2).setCellValue(question.getRole().name());
                    continue;
                }

                for (QuestionLevel level : question.getLevels()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(question.getCode());
                    row.createCell(1).setCellValue(question.getText());
                    row.createCell(2).setCellValue(question.getRole().name());
                    row.createCell(3).setCellValue(level.getTitle());
                    row.createCell(4).setCellValue(level.getScore());
                    row.createCell(5).setCellValue(level.getLevelOrder());
                }

            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export questions", e);
        }
    }

    private String getString(Cell cell) {
        return cell == null ? null : cell.toString().trim();
    }

    private Integer getInteger(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        String value = cell.toString().trim();
        return value.isEmpty() ? null : Integer.parseInt(value);
    }
}

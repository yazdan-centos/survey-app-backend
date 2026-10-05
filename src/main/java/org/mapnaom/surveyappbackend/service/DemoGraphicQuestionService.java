package org.mapnaom.surveyappbackend.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mapnaom.surveyappbackend.entity.DemoGraphicQuestion;
import org.mapnaom.surveyappbackend.repository.DemoGraphicQuestionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DemoGraphicQuestionService {
    private static final int TEMPLATE_OPTION_COLUMNS = 13;
    private final DemoGraphicQuestionRepository repository;

    @Transactional(readOnly = true)
    public List<DemoGraphicQuestion> findAll() {
        return repository.findAllByOrderByGroupKeyAscDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<DemoGraphicQuestion> findByGroupKey(String groupKey) {
        return repository.findByGroupKeyOrderByDisplayOrderAsc(groupKey);
    }

    @Transactional(readOnly = true)
    public DemoGraphicQuestion findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demographic question not found"));
    }

    @Transactional
    public DemoGraphicQuestion create(DemoGraphicQuestion input) {
        DemoGraphicQuestion entity = new DemoGraphicQuestion();
        copyFields(entity, input);
        return save(entity);
    }

    @Transactional
    public DemoGraphicQuestion update(Long id, DemoGraphicQuestion input) {
        DemoGraphicQuestion entity = findById(id);
        copyFields(entity, input);
        return save(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
        repository.flush();
    }

    public byte[] downloadWorksheetTemplate() {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Demographics");
            sheet.createFreezePane(0, 1);
            var header = sheet.createRow(0);
            String[] columns = {"groupKey", "question", "type", "displayOrder"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
                sheet.setColumnWidth(i, i == 1 ? 60 * 256 : 22 * 256);
            }
            for (int option = 1; option <= TEMPLATE_OPTION_COLUMNS; option++) {
                int column = columns.length + option - 1;
                header.createCell(column).setCellValue("option" + option);
                sheet.setColumnWidth(column, 40 * 256);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create demographic question template", exception);
        }
    }

    /**
     * Imports .xlsx rows with headers groupKey, question, type, displayOrder, option1, option2, etc.
     * One row represents one question; type may be blank, and at least one option is required.
     */
    @Transactional
    public List<DemoGraphicQuestion> importFromExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excel file is empty");
        }
        List<DemoGraphicQuestion> questions = new ArrayList<>();
        Set<String> positions = new HashSet<>();
        try (var input = file.getInputStream(); var workbook = new XSSFWorkbook(input)) {
            if (workbook.getNumberOfSheets() == 0) throw badRow(1, "worksheet is required");
            var sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Map<String, Integer> columns = headers(sheet.getRow(0), formatter);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmpty(row, formatter)) continue;
                int number = i + 1;
                DemoGraphicQuestion entity = new DemoGraphicQuestion();
                entity.setGroupKey(value(row.getCell(columns.get("groupKey")), formatter, number));
                entity.setQuestion(value(row.getCell(columns.get("question")), formatter, number));
                entity.setType(value(row.getCell(columns.get("type")), formatter, number));
                String order = value(row.getCell(columns.get("displayOrder")), formatter, number);
                try {
                    entity.setDisplayOrder(Integer.parseInt(order));
                } catch (NumberFormatException exception) {
                    throw badRow(number, "displayOrder must be a nonnegative integer");
                }
                for (int option = 1; columns.containsKey("option" + option); option++) {
                    String text = value(row.getCell(columns.get("option" + option)), formatter, number);
                    if (text != null) entity.getOptions().add(text);
                }
                validate(entity, number);
                String position = entity.getGroupKey() + "\u0000" + entity.getDisplayOrder();
                if (!positions.add(position) || repository.existsByGroupKeyAndDisplayOrder(
                        entity.getGroupKey(), entity.getDisplayOrder())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Row " + number + ": groupKey/displayOrder already exists");
                }
                questions.add(entity);
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read a valid .xlsx workbook", exception);
        }
        if (questions.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No questions found");
        try {
            return repository.saveAllAndFlush(questions);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Question position already exists", exception);
        }
    }

    private DemoGraphicQuestion save(DemoGraphicQuestion entity) {
        try {
            return repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Question position already exists", exception);
        }
    }

    private void copyFields(DemoGraphicQuestion target, DemoGraphicQuestion input) {
        if (input == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is required");
        validate(input, null);
        target.setGroupKey(input.getGroupKey().trim());
        target.setQuestion(input.getQuestion().trim());
        target.setType(input.getType() == null || input.getType().isBlank() ? null : input.getType().trim());
        target.setDisplayOrder(input.getDisplayOrder());
        List<String> options = input.getOptions().stream().map(String::trim).toList();
        target.getOptions().clear();
        target.getOptions().addAll(options);
    }

    private void validate(DemoGraphicQuestion entity, Integer row) {
        if (entity.getGroupKey() == null || entity.getGroupKey().isBlank() || entity.getGroupKey().trim().length() > 30)
            throw badRow(row, "groupKey is required and must have at most 30 characters");
        if (entity.getQuestion() == null || entity.getQuestion().isBlank())
            throw badRow(row, "question is required");
        if (entity.getType() != null && entity.getType().trim().length() > 20)
            throw badRow(row, "type must have at most 20 characters");
        if (entity.getDisplayOrder() < 0)
            throw badRow(row, "displayOrder must be a nonnegative integer");
        if (entity.getOptions() == null || entity.getOptions().isEmpty() ||
                entity.getOptions().stream().anyMatch(option -> option == null || option.isBlank()))
            throw badRow(row, "at least one nonblank option is required");
    }

    private Map<String, Integer> headers(Row header, DataFormatter formatter) {
        if (header == null) throw badRow(1, "header row is required");
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : header) {
            String name = value(cell, formatter, 1);
            if (name == null) continue;
            if (!List.of("groupKey", "question", "type", "displayOrder").contains(name) &&
                    !name.matches("option[1-9][0-9]*")) throw badRow(1, "unknown column: " + name);
            if (columns.putIfAbsent(name, cell.getColumnIndex()) != null)
                throw badRow(1, "duplicate column: " + name);
        }
        for (String required : List.of("groupKey", "question", "type", "displayOrder", "option1")) {
            if (!columns.containsKey(required)) throw badRow(1, "missing column: " + required);
        }
        for (int option = 1; option <= columns.size() - 4; option++) {
            if (!columns.containsKey("option" + option)) throw badRow(1, "option columns must be consecutive");
        }
        return columns;
    }

    private boolean isEmpty(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (value(cell, formatter, row.getRowNum() + 1) != null) return false;
        }
        return true;
    }

    private String value(Cell cell, DataFormatter formatter, int row) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.FORMULA || cell.getCellType() == CellType.ERROR)
            throw badRow(row, "formula and error cells are not supported");
        String text = formatter.formatCellValue(cell).trim();
        return text.isEmpty() ? null : text;
    }

    private ResponseStatusException badRow(Integer row, String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                (row == null ? "" : "Row " + row + ": ") + message);
    }
}

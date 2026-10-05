package org.mapnaom.surveyappbackend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.entity.DemoGraphicQuestion;
import org.mapnaom.surveyappbackend.service.DemoGraphicQuestionService;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DemoGraphicQuestionControllerTest {
    private static final String BASE = "/api/demographic-questions";
    private static final String BODY = """
            {"groupKey":"board","question":"Your role?","displayOrder":0,"options":["Director","Other"]}
            """;
    private DemoGraphicQuestionService service;
    private MockMvc mvc;
    private DemoGraphicQuestion question;

    @BeforeEach
    void setUp() {
        service = mock(DemoGraphicQuestionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new DemoGraphicQuestionController(service)).build();
        question = new DemoGraphicQuestion();
        question.setId(10003L);
        question.setGroupKey("board");
        question.setQuestion("Your role?");
        question.setDisplayOrder(0);
        question.setOptions(List.of("Director", "Other"));
    }

    @Test
    void exposesCrudAndGroupFiltering() throws Exception {
        when(service.findAll()).thenReturn(List.of(question));
        when(service.findByGroupKey("board")).thenReturn(List.of(question));
        when(service.findById(question.getId())).thenReturn(question);
        when(service.create(any())).thenReturn(question);
        when(service.update(eq(question.getId()), any())).thenReturn(question);

        mvc.perform(get(BASE)).andExpect(status().isOk()).andExpect(jsonPath("$[0].options[1]").value("Other"));
        mvc.perform(get(BASE).param("groupKey", "board")).andExpect(status().isOk());
        mvc.perform(get(BASE + "/{id}", question.getId())).andExpect(status().isOk());
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.question").value("Your role?"));
        mvc.perform(put(BASE + "/{id}", question.getId()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.groupKey").value("board"));
        mvc.perform(delete(BASE + "/{id}", question.getId())).andExpect(status().isNoContent());
        verify(service).delete(question.getId());
    }

    @Test
    void exposesImportAndTemplateDownload() throws Exception {
        var file = new MockMultipartFile("file", "questions.xlsx", "application/octet-stream", new byte[]{1});
        when(service.importFromExcelFile(file)).thenReturn(List.of(question));
        mvc.perform(multipart(BASE + "/import").file(file)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupKey").value("board"));

        byte[] template = {1, 2, 3};
        when(service.downloadWorksheetTemplate()).thenReturn(template);
        mvc.perform(get(BASE + "/template")).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=demographic-questions-template.xlsx"))
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(template));
    }
}

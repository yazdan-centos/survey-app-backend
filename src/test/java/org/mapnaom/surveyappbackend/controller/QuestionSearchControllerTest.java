package org.mapnaom.surveyappbackend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.dto.question.QuestionResponseDto;
import org.mapnaom.surveyappbackend.dto.question.QuestionSearchRequest;
import org.mapnaom.surveyappbackend.entity.SurveyRole;
import org.mapnaom.surveyappbackend.repository.CriterionRepository;
import org.mapnaom.surveyappbackend.repository.QuestionRepository;
import org.mapnaom.surveyappbackend.repository.SurveyRepository;
import org.mapnaom.surveyappbackend.service.QuestionExcelService;
import org.mapnaom.surveyappbackend.service.QuestionService;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QuestionSearchControllerTest {
    private QuestionService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(QuestionService.class);
        mvc = mvc(service);
    }

    @Test
    void bindsAllFiltersAndReturnsPaginatedDtos() throws Exception {
        UUID id = UUID.randomUUID();
        UUID surveyId = UUID.randomUUID();
        UUID criterionId = UUID.randomUUID();
        UUID dimensionId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        var dto = new QuestionResponseDto(id, surveyId, "Q1", "Quality", SurveyRole.BOARD,
                0, criterionId, "Strategy", null,
                List.of(new QuestionResponseDto.Level(levelId, 1, "Excellent")));
        when(service.search(any(), any())).thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(1, 5), 6));

        mvc.perform(get("/api/questions/search")
                        .param("q", "quality").param("id", id.toString())
                        .param("code", "Q1").param("text", "Quality").param("role", "BOARD")
                        .param("displayOrder", "0").param("surveyId", surveyId.toString())
                        .param("surveyTitle", "Annual").param("surveyVersion", "2026").param("surveyActive", "false")
                        .param("criterionId", criterionId.toString()).param("criterionName", "Strategy")
                        .param("dimensionId", dimensionId.toString()).param("dimensionKey", "leadership")
                        .param("dimensionLabel", "Leadership").param("dimensionDisplayOrder", "3")
                        .param("levelId", levelId.toString()).param("levelNumber", "1")
                        .param("description", "Excellent").param("levelTitle", "2.5").param("levelScore", "4.5")
                        .param("createdAtFrom", "2026-01-01T00:00:00Z").param("createdAtTo", "2026-12-31T00:00:00Z")
                        .param("updatedAtFrom", "2026-02-01T00:00:00Z").param("updatedAtTo", "2026-12-31T00:00:00Z")
                        .param("page", "1").param("size", "5").param("sort", "code,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].levels[0].description").value("Excellent"));

        var filterCaptor = ArgumentCaptor.forClass(QuestionSearchRequest.class);
        var pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).search(filterCaptor.capture(), pageCaptor.capture());
        QuestionSearchRequest expected = new QuestionSearchRequest();
        expected.setQ("quality"); expected.setId(id); expected.setCode("Q1"); expected.setText("Quality");
        expected.setRole(SurveyRole.BOARD); expected.setDisplayOrder(0); expected.setSurveyId(surveyId);
        expected.setSurveyTitle("Annual"); expected.setSurveyVersion("2026"); expected.setSurveyActive(false);
        expected.setCriterionId(criterionId); expected.setCriterionName("Strategy");
        expected.setDimensionId(dimensionId); expected.setDimensionKey("leadership");
        expected.setDimensionLabel("Leadership"); expected.setDimensionDisplayOrder(3);
        expected.setLevelId(levelId); expected.setLevelNumber(1); expected.setDescription("Excellent");
        expected.setLevelTitle(2.5); expected.setLevelScore(4.5);
        expected.setCreatedAtFrom(Instant.parse("2026-01-01T00:00:00Z"));
        expected.setCreatedAtTo(Instant.parse("2026-12-31T00:00:00Z"));
        expected.setUpdatedAtFrom(Instant.parse("2026-02-01T00:00:00Z"));
        expected.setUpdatedAtTo(Instant.parse("2026-12-31T00:00:00Z"));
        assertThat(filterCaptor.getValue()).isEqualTo(expected);
        assertThat(pageCaptor.getValue()).isEqualTo(PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "code")));
    }

    @Test
    void acceptsJsonSearchObjectWithPageableAndReturnsPageMetadata() throws Exception {
        UUID surveyId = UUID.randomUUID();
        UUID dimensionId = UUID.randomUUID();
        UUID criterionId = UUID.randomUUID();
        var dto = new QuestionResponseDto(UUID.randomUUID(), surveyId, "Q1", "Leadership", SurveyRole.BOARD,
                0, criterionId, "Strategy", null, List.of());
        when(service.search(any(), any())).thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(1, 5), 6));

        mvc.perform(post("/api/questions/search")
                        .param("page", "1").param("size", "5").param("sort", "code,desc", "text,asc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Leadership",
                                  "description": "Excellent",
                                  "role": "BOARD",
                                  "surveyId": "%s",
                                  "dimensionId": "%s",
                                  "criterionId": "%s",
                                  "surveyActive": false,
                                  "displayOrder": 0,
                                  "createdAtFrom": "2026-01-01T00:00:00Z"
                                }
                                """.formatted(surveyId, dimensionId, criterionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("Q1"))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));

        QuestionSearchRequest expected = new QuestionSearchRequest();
        expected.setText("Leadership");
        expected.setDescription("Excellent");
        expected.setRole(SurveyRole.BOARD);
        expected.setSurveyId(surveyId);
        expected.setDimensionId(dimensionId);
        expected.setCriterionId(criterionId);
        expected.setSurveyActive(false);
        expected.setDisplayOrder(0);
        expected.setCreatedAtFrom(Instant.parse("2026-01-01T00:00:00Z"));
        verify(service).search(eq(expected), eq(PageRequest.of(1, 5,
                Sort.by(Sort.Order.desc("code"), Sort.Order.asc("text")))));
    }

    @Test
    void emptyJsonSearchObjectUsesDefaultPagination() throws Exception {
        when(service.search(any(), any())).thenAnswer(invocation -> Page.empty(invocation.getArgument(1)));
        mvc.perform(post("/api/questions/search").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));
        verify(service).search(eq(new QuestionSearchRequest()), eq(PageRequest.of(0, 20, Sort.by("displayOrder"))));
    }

    @Test
    void rejectsMissingOrInvalidJsonSearchObjects() throws Exception {
        for (String body : List.of("", "null", "{", "{\"role\":\"INVALID\"}",
                "{\"surveyId\":\"invalid\"}", "{\"createdAtFrom\":\"invalid\"}")) {
            mvc.perform(post("/api/questions/search").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void appliesDefaultPaginationAndReturnsEmptyContent() throws Exception {
        when(service.search(any(), any())).thenAnswer(invocation -> Page.empty(invocation.getArgument(1)));
        mvc.perform(get("/api/questions/search"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.size").value(20));
        verify(service).search(eq(new QuestionSearchRequest()), eq(PageRequest.of(0, 20, Sort.by("displayOrder"))));
    }

    @Test
    void rejectsMalformedTypedFilters() throws Exception {
        for (String field : List.of("role", "id", "surveyId", "dimensionId", "criterionId", "levelId",
                "displayOrder", "levelNumber", "surveyActive", "createdAtFrom", "levelScore")) {
            mvc.perform(get("/api/questions/search").param(field, "invalid"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void rejectsUnsupportedSortAndReversedTimestampRanges() throws Exception {
        QuestionRepository repository = mock(QuestionRepository.class);
        MockMvc realServiceMvc = mvc(new QuestionService(repository, mock(SurveyRepository.class), mock(CriterionRepository.class)));
        realServiceMvc.perform(get("/api/questions/search").param("sort", "unknown,asc"))
                .andExpect(status().isBadRequest());
        for (String field : List.of("createdAt", "updatedAt")) {
            realServiceMvc.perform(get("/api/questions/search")
                            .param(field + "From", "2026-12-31T00:00:00Z")
                            .param(field + "To", "2026-01-01T00:00:00Z"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(repository);
    }

    private MockMvc mvc(QuestionService questionService) {
        return MockMvcBuilders.standaloneSetup(new QuestionController(questionService, mock(QuestionExcelService.class)))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
    }
}

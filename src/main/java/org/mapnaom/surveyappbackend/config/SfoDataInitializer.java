package org.mapnaom.surveyappbackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapnaom.surveyappbackend.entity.*;
import org.mapnaom.surveyappbackend.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Component
@Order(15)
@RequiredArgsConstructor
@Slf4j
public class SfoDataInitializer implements ApplicationRunner {
    private final SurveyRepository surveys;
    private final DimensionRepository dimensions;
    private final CriterionRepository criteria;
    private final QuestionRepository questions;
    private final DemoGraphicQuestionRepository demographics;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = IOException.class)
    public void run(ApplicationArguments args) throws IOException {
        Seed seed;
        try (var input = new ClassPathResource("static/sfo_questions.json").getInputStream()) {
            seed = objectMapper.readValue(input, Seed.class);
        }
        Survey survey = surveys.findByVersion("sfo-1.0.0").orElseGet(() -> {
            Survey created = new Survey();
            created.setTitle("پیمایش سازمان استراتژی‌محور (SFO)");
            created.setVersion("sfo-1.0.0");
            created.setActive(false);
            return surveys.save(created);
        });
        var existingCodes = new HashSet<String>();
        questions.findBySurveyId(survey.getId()).stream()
                .filter(question -> question.getRole() == SurveyRole.MANAGERS)
                .forEach(question -> existingCodes.add(question.getCode()));
        int displayOrder = 1;
        int added = 0;
        for (Principle principle : seed.principles()) {
            Dimension dimension = dimensions.findByKey("sfo-principle-" + principle.id()).orElseGet(() -> {
                Dimension created = new Dimension();
                created.setKey("sfo-principle-" + principle.id());
                created.setLabel(principle.title());
                created.setDisplayOrder(principle.id());
                return dimensions.save(created);
            });
            Criterion criterion = criteria.findByDimensionIdAndName(dimension.getId(), principle.subtitle())
                    .orElseGet(() -> {
                        Criterion created = new Criterion();
                        created.setDimension(dimension);
                        created.setName(principle.subtitle());
                        return criteria.save(created);
                    });
            for (Prompt prompt : principle.questions()) {
                if (existingCodes.add(prompt.code())) {
                    Question question = new Question();
                    question.setSurvey(survey);
                    question.setCriterion(criterion);
                    question.setRole(SurveyRole.MANAGERS);
                    question.setCode(prompt.code());
                    question.setText(prompt.text());
                    question.setDisplayOrder(displayOrder);
                    questions.save(question);
                    added++;
                }
                displayOrder++;
            }
        }
        for (int index = 0; index < seed.questions_array().size(); index++) {
            if (demographics.existsByGroupKeyAndDisplayOrder("sfo-managers", index)) continue;
            Demographic seedQuestion = seed.questions_array().get(index);
            DemoGraphicQuestion question = new DemoGraphicQuestion();
            question.setGroupKey("sfo-managers");
            question.setDisplayOrder(index);
            question.setQuestion(seedQuestion.prompt());
            question.setType("select");
            question.setOptions(new ArrayList<>(seedQuestion.options()));
            demographics.save(question);
        }
        log.info("Initialized SFO survey {}: added {} assessment questions", survey.getId(), added);
    }

    private record Seed(List<Demographic> questions_array, List<Principle> principles) {}
    private record Demographic(String id, String prompt, List<String> options) {}
    private record Principle(int id, String title, String subtitle, List<Prompt> questions) {}
    private record Prompt(String code, String text) {}
}

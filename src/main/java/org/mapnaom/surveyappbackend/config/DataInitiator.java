package org.mapnaom.surveyappbackend.config;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.entity.Dimension;
import org.mapnaom.surveyappbackend.entity.Question;
import org.mapnaom.surveyappbackend.entity.QuestionLevel;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.entity.SurveyRole;
import org.mapnaom.surveyappbackend.repository.DimensionRepository;
import org.mapnaom.surveyappbackend.repository.QuestionRepository;
import org.mapnaom.surveyappbackend.repository.SurveyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitiator implements ApplicationRunner {

    private static final List<DimensionSeed> DIMENSIONS = List.of(
            new DimensionSeed("customerFocus", "تمرکز بر مشتری"),
            new DimensionSeed("resourcesCapabilities", "منابع و قابلیت‌ها"),
            new DimensionSeed("strategicVision", "چشم‌انداز استراتژیک"),
            new DimensionSeed("valueCreation", "ارزش‌آفرینی"),
            new DimensionSeed("qualityFocus", "تمرکز بر کیفیت")
    );

    private final SurveyRepository surveyRepository;
    private final DimensionRepository dimensionRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;



    @Value("classpath:surveyQuestions.json")
    private Resource questionsResource;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (surveyRepository.existsByVersion("1.0.0")) {
            return;
        }

        Survey survey = new Survey();
        survey.setTitle("پیمایش کلاس جهانی");
        survey.setVersion("1.0.0");
        survey.setActive(true);
        survey = surveyRepository.save(survey);

        Map<String, Dimension> dimensionsByKey = loadDimensions();
        Map<String, List<QuestionSeed>> questionsByRole = objectMapper.readValue(
                questionsResource.getInputStream(),
                new TypeReference<>() {
                });

        for (Map.Entry<String, List<QuestionSeed>> roleEntry : questionsByRole.entrySet()) {
            SurveyRole role = SurveyRole.valueOf(roleEntry.getKey().toUpperCase());
            int displayOrder = 1;
            for (QuestionSeed seed : roleEntry.getValue()) {
                Question question = new Question();
                question.setSurvey(survey);
                question.setDimension(requireDimension(dimensionsByKey, seed.dimensionKey()));
                question.setRole(role);
                question.setCode(seed.code());
                question.setCriterion(seed.criterion());
                question.setText(seed.criterion());
                question.setDisplayOrder(displayOrder++);

                for (int index = 0; index < seed.levels().size(); index++) {
                    QuestionLevel level = new QuestionLevel();
                    level.setQuestion(question);
                    level.setLevelNumber(index + 1);
                    level.setTitle(index + 1);
                    level.setScore(index + 1);
                    level.setDescription(seed.levels().get(index));
                    question.getLevels().add(level);
                }

                questionRepository.save(question);
            }
        }
    }

    private Map<String, Dimension> loadDimensions() {
        Map<String, Dimension> dimensionsByKey = new LinkedHashMap<>();
        for (int index = 0; index < DIMENSIONS.size(); index++) {
            DimensionSeed seed = DIMENSIONS.get(index);
            int displayOrder = index + 1;
            Dimension dimension = dimensionRepository.findByKey(seed.key())
                    .map(existing -> updateDimension(existing, seed, displayOrder))
                    .orElseGet(() -> createDimension(seed, displayOrder));
            dimensionsByKey.put(seed.key(), dimensionRepository.save(dimension));
        }
        return dimensionsByKey;
    }

    private Dimension createDimension(DimensionSeed seed, int displayOrder) {
        Dimension dimension = new Dimension();
        dimension.setKey(seed.key());
        return updateDimension(dimension, seed, displayOrder);
    }

    private Dimension updateDimension(Dimension dimension, DimensionSeed seed, int displayOrder) {
        dimension.setLabel(seed.label());
        dimension.setDisplayOrder(displayOrder);
        return dimension;
    }

    private Dimension requireDimension(Map<String, Dimension> dimensionsByKey, String key) {
        Dimension dimension = dimensionsByKey.get(key);
        if (dimension == null) {
            throw new IllegalArgumentException("Unknown survey dimension: " + key);
        }
        return dimension;
    }

    private record DimensionSeed(String key, String label) {
    }

    private record QuestionSeed(String code, String dimensionKey, String dimensionLabel,
                                String criterion, List<String> levels) {
    }
}

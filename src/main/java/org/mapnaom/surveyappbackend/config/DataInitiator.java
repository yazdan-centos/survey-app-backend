package org.mapnaom.surveyappbackend.config;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.entity.Criterion;
import org.mapnaom.surveyappbackend.repository.CriterionRepository;
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
@org.springframework.core.annotation.Order(10)
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
    private final CriterionRepository criterionRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;



    @Value("classpath:surveyQuestions.json")
    private Resource questionsResource;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        Map<String, List<QuestionSeed>> questionsByRole;
        try (var input = questionsResource.getInputStream()) {
            questionsByRole = objectMapper.readValue(input, new TypeReference<>() {});
        }
        Map<String, Dimension> dimensionsByKey = loadDimensions();
        Map<String, Map<String, Criterion>> criteriaByDimension = loadCriteria(dimensionsByKey, questionsByRole);

        if (surveyRepository.existsByVersion("1.0.0")) {
            return;
        }

        Survey survey = new Survey();
        survey.setTitle("پیمایش کلاس جهانی");
        survey.setVersion("1.0.0");
        survey.setActive(true);
        survey = surveyRepository.save(survey);

        for (Map.Entry<String, List<QuestionSeed>> roleEntry : questionsByRole.entrySet()) {
            SurveyRole role = SurveyRole.valueOf(roleEntry.getKey().toUpperCase());
            int displayOrder = 1;
            for (QuestionSeed seed : roleEntry.getValue()) {
                Question question = new Question();
                question.setSurvey(survey);
                Criterion criterion = criteriaByDimension.get(seed.dimensionKey()).get(seed.criterion());
                question.setRole(role);
                question.setCode(seed.code());
                question.setCriterion(criterion);
                criterion.getQuestions().add(question);
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

    private Map<String, Map<String, Criterion>> loadCriteria(
            Map<String, Dimension> dimensionsByKey, Map<String, List<QuestionSeed>> questionsByRole) {
        Map<String, Map<String, Criterion>> criteriaByDimension = new LinkedHashMap<>();
        for (List<QuestionSeed> seeds : questionsByRole.values()) {
            for (QuestionSeed seed : seeds) {
                Dimension dimension = requireDimension(dimensionsByKey, seed.dimensionKey());
                criteriaByDimension.computeIfAbsent(seed.dimensionKey(), key -> new LinkedHashMap<>())
                        .computeIfAbsent(seed.criterion(), name -> criterionRepository
                                .findByDimensionIdAndName(dimension.getId(), name)
                                .orElseGet(() -> {
                                    Criterion criterion = new Criterion();
                                    criterion.setName(name);
                                    criterion.setDimension(dimension);
                                    Criterion saved = criterionRepository.save(criterion);
                                    dimension.getCriteria().add(saved);
                                    return saved;
                                }));
            }
        }
        return criteriaByDimension;
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

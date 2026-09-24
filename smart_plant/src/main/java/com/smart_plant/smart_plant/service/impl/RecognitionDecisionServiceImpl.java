package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.AiRecognitionRecord;
import com.smart_plant.smart_plant.service.RecognitionDecision;
import com.smart_plant.smart_plant.service.RecognitionDecisionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class RecognitionDecisionServiceImpl implements RecognitionDecisionService {

    private static final int SOURCE_TYPE_CAMERA = 2;
    private static final int RESULT_STATUS_SUCCESS = 1;
    private static final long TYPE_DISEASE = 2L;
    private static final long TYPE_PEST = 3L;

    @Override
    public RecognitionDecision decide(AiRecognitionRecord record) {
        if (record == null) {
            return normal(false);
        }

        boolean automatic = Integer.valueOf(SOURCE_TYPE_CAMERA).equals(record.getSourceType());
        boolean success = Integer.valueOf(RESULT_STATUS_SUCCESS).equals(record.getResultStatus());
        boolean actionableType = isDiseaseOrPest(record.getRecognitionType());
        boolean abnormal = !success || actionableType || hasActionableAbnormalSignal(record);

        if (!abnormal) {
            return normal(automatic);
        }

        Integer alertLevel = alertLevelFromSeverity(record.getSeverityLevel(), success);
        Integer taskPriority = priorityFromSeverity(record.getSeverityLevel());
        Integer taskType = actionableType ? 3 : 5;
        boolean createAlert = automatic;
        boolean createFarmTask = automatic && success && record.getPlotId() != null;
        return new RecognitionDecision(
                automatic,
                true,
                createAlert,
                createFarmTask,
                alertLevel,
                taskPriority,
                taskType,
                abnormalReason(record, success, actionableType));
    }

    private RecognitionDecision normal(boolean automatic) {
        return new RecognitionDecision(automatic, false, false, false, 1, 2, 5, "Recognition result is normal");
    }

    private boolean isDiseaseOrPest(Long recognitionType) {
        return Long.valueOf(TYPE_DISEASE).equals(recognitionType) || Long.valueOf(TYPE_PEST).equals(recognitionType);
    }

    private boolean hasActionableAbnormalSignal(AiRecognitionRecord record) {
        Integer severityLevel = record.getSeverityLevel();
        if (severityLevel != null && severityLevel >= 2) {
            return true;
        }
        String text = join(record.getResultName(), record.getResultSummary(), record.getResultDetail(), record.getSuggestion());
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        return lowerText.contains("abnormal")
                || lowerText.contains("disease")
                || lowerText.contains("pest")
                || lowerText.contains("infection")
                || lowerText.contains("wilt")
                || lowerText.contains("yellow")
                || lowerText.contains("rot")
                || lowerText.contains("mildew");
    }

    private Integer alertLevelFromSeverity(Integer severityLevel, boolean success) {
        if (!success) {
            return 2;
        }
        if (severityLevel == null) {
            return 1;
        }
        return switch (severityLevel) {
            case 3 -> 3;
            case 2 -> 2;
            default -> 1;
        };
    }

    private Integer priorityFromSeverity(Integer severityLevel) {
        if (severityLevel == null) {
            return 2;
        }
        return switch (severityLevel) {
            case 3 -> 4;
            case 2 -> 3;
            case 1 -> 2;
            default -> 2;
        };
    }

    private String abnormalReason(AiRecognitionRecord record, boolean success, boolean actionableType) {
        if (!success) {
            return "AI recognition failed";
        }
        if (actionableType) {
            return "Disease or pest recognition result";
        }
        if (record.getSeverityLevel() != null && record.getSeverityLevel() >= 2) {
            return "Recognition severity is " + record.getSeverityLevel();
        }
        return "Recognition text contains abnormal signal";
    }

    private String join(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                builder.append(value).append(' ');
            }
        }
        return builder.toString();
    }
}

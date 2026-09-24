package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.entity.AiRecognitionRecord;

public interface RecognitionDecisionService {

    RecognitionDecision decide(AiRecognitionRecord record);
}

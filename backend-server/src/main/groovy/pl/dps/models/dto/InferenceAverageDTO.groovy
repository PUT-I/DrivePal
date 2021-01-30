package pl.dps.models.dto

class InferenceAverageDTO {

    Set<String> modelNames

    Map<String, Float> averageInferenceTimes

    Map<String, Float> averageProcessingTimes

}

package pl.dps.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import pl.dps.models.DiagnosticData
import pl.dps.models.dto.AverageTimesDTO
import pl.dps.repositories.DiagnosticDataRepository

@Service
class DiagnosticDataService {

    @Autowired
    DiagnosticDataRepository diagnosticDataRepository

    DiagnosticData save(DiagnosticData inferenceStatistic) {
        return diagnosticDataRepository.save(inferenceStatistic)
    }

    Optional<DiagnosticData> findOne(Long id) {
        return diagnosticDataRepository.findById(id)
    }

    List<DiagnosticData> findAll() {
        return diagnosticDataRepository.findAll()
    }

    List<AverageTimesDTO> getTotalAverage(String soc) {
        List<DiagnosticData> diagnosticData = findAll()

        if (soc != null && soc.toUpperCase() != "ALL") {
            diagnosticData = diagnosticData.findAll { it.soc == soc }
        }

        Set<String> modelNames = diagnosticData
                .collect { it.modelName }
                .unique()

        Map<String, Float> averageInferenceTimes = new HashMap<>()
        Map<String, Float> averageProcessingTimes = new HashMap<>()
        for (String modelName in modelNames) {
            Float averageInferenceTime = diagnosticData
                    .findAll { it.modelName == modelName }
                    .average { it.inferenceTime } as Float

            Float averageProcessingTime = diagnosticData
                    .findAll { it.modelName == modelName }
                    .average { it.processingTime } as Float

            averageInferenceTimes.put(modelName, averageInferenceTime)
            averageProcessingTimes.put(modelName, averageProcessingTime)
        }

        List<AverageTimesDTO> result = new ArrayList<>(modelNames.size())
        for (String modelName in modelNames) {
            result.add(new AverageTimesDTO(modelName: modelName,
                    averageInferenceTime: averageInferenceTimes[modelName],
                    averageProcessingTime: averageProcessingTimes[modelName]))
        }

        return result
    }
}

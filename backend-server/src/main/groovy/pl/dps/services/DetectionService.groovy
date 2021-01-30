package pl.dps.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import pl.dps.models.Detection
import pl.dps.repositories.DetectionRepository

@Service
class DetectionService {

    @Autowired
    private DetectionRepository detectionRepository

    Detection save(Detection detection) {
        return detectionRepository.save(detection)
    }

    Iterable<Detection> saveAll(Iterable<Detection> detections) {
        return detectionRepository.saveAll(detections)
    }

    Optional<Detection> findOne(Long id) {
        return detectionRepository.findById(id)
    }

    List<Detection> findAll() {
        return detectionRepository.findAll()
    }

    List<Detection> findAllById(Iterable<Long> ids) {
        return detectionRepository.findAllById(ids)
    }

    void delete(Detection detection) {
        detectionRepository.delete(detection)
    }
}

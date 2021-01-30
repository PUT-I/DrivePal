package pl.dps.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import pl.dps.models.DetectionImage
import pl.dps.repositories.DetectionImageRepository

@Service
class DetectionImageService {

    @Autowired
    private DetectionImageRepository detectionImageRepository

    DetectionImage save(DetectionImage detectionImage) {
        return detectionImageRepository.save(detectionImage)
    }

    Optional<DetectionImage> findOne(Long id) {
        return detectionImageRepository.findById(id)
    }

    List<DetectionImage> findAll() {
        return detectionImageRepository.findAll()
    }

    void delete(DetectionImage detectionImage) {
        detectionImageRepository.delete(detectionImage)
    }
}

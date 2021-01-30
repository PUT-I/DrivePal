package pl.dps.models.dto

import pl.dps.models.DetectionImage

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import java.time.LocalDateTime

class DetectionImageUrlDTO {
    @NotNull
    Long id

    @NotNull
    LocalDateTime timestamp

    @NotBlank
    String modelName

    @NotBlank
    String entityUrl

    DetectionImageUrlDTO(DetectionImage detectionImage, String hostUrl = "") {
        id = detectionImage.id
        timestamp = detectionImage.timestamp
        modelName = detectionImage.modelName

        entityUrl = "${hostUrl}/api/detection/${id}"
    }
}


package pl.dps.models.dto


import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

class DetectionImageDTO {

    Long id

    @NotBlank
    String image

    @NotNull
    List<DetectionDTO> detections

    @NotNull
    String modelName
}


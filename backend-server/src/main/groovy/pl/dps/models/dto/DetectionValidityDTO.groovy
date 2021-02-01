package pl.dps.models.dto

import javax.validation.constraints.NotNull

class DetectionValidityDTO {
    String modelName

    @NotNull
    Double valid

    @NotNull
    Double invalid

    @NotNull
    Double unverified
}


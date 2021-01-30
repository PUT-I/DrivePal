package pl.dps.models.dto

import javax.validation.constraints.NotNull

class DetectionValidityDTO {
    @NotNull
    Double valid

    @NotNull
    Double invalid

    @NotNull
    Double unverified
}


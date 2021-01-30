package pl.dps.models.dto


import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

class DetectionDTO {

    Long id

    @NotBlank
    String className

    @NotNull
    Float confidence

    @NotNull
    Float locationLeft

    @NotNull
    Float locationTop

    @NotNull
    Float locationRight

    @NotNull
    Float locationBottom

    Boolean valid

}


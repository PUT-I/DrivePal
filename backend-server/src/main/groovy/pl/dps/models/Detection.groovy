package pl.dps.models

import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity()
class Detection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @ManyToOne()
    DetectionImage detectionImage
}

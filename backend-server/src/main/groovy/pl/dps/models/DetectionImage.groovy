package pl.dps.models

import org.hibernate.annotations.CreationTimestamp

import javax.persistence.*
import javax.validation.constraints.FutureOrPresent
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import java.time.LocalDateTime

@Entity()
class DetectionImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @NotNull
    byte[] image

    @FutureOrPresent
    @CreationTimestamp
    LocalDateTime timestamp

    @NotBlank
    String modelName

    @OneToMany(cascade = CascadeType.ALL)
    List<Detection> detections

    void setDetections(List<Detection> detections) {
        this.detections = detections
        for (Detection detection : detections) {
            detection.detectionImage = this
        }
    }
}

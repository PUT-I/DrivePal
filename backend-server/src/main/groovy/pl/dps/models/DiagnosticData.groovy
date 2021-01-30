package pl.dps.models

import org.hibernate.annotations.CreationTimestamp

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.validation.constraints.FutureOrPresent
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import java.time.LocalDateTime

@Entity
class DiagnosticData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id

    @FutureOrPresent
    @CreationTimestamp
    public LocalDateTime timeStamp

    @NotBlank
    public String soc

    public String deviceId

    @NotNull
    public Float processingTime

    @NotNull
    public Float inferenceTime

    @NotNull
    public String modelName

    @NotNull
    public String source

    @NotBlank
    public String version

    public Float cpuTemperature

}

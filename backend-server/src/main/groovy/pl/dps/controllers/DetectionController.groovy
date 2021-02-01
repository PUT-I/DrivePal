package pl.dps.controllers

import javassist.NotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.dps.models.Detection
import pl.dps.models.DetectionImage
import pl.dps.models.dto.DetectionDTO
import pl.dps.models.dto.DetectionImageDTO
import pl.dps.models.dto.DetectionImageUrlDTO
import pl.dps.models.dto.DetectionValidityDTO
import pl.dps.services.DetectionImageService
import pl.dps.services.DetectionService

import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@RestController
@RequestMapping("/api/detection")
class DetectionController extends AbstractController {

    @Autowired
    DetectionService detectionService

    @Autowired
    DetectionImageService detectionImageService

    @PostMapping()
    ResponseEntity saveDetection(@Valid @RequestBody DetectionImageDTO detectionImageDTO) {
        def detectionImage = convertFromDTO(detectionImageDTO)

        DetectionImage savedDetectionImage = detectionImageService.save(detectionImage)

        return new ResponseEntity(savedDetectionImage, HttpStatus.OK)
    }

    @GetMapping()
    ResponseEntity getAll(HttpServletRequest request) {
        List<DetectionImage> detectionImages = detectionImageService.findAll()

        List<DetectionImageUrlDTO> detectionImagesDto = detectionImages.collect { DetectionImage detection ->
            new DetectionImageUrlDTO(detection, "http://server.drivepal.pl:${request.getServerPort()}")
        }

        return new ResponseEntity(detectionImagesDto, HttpStatus.OK)
    }

    @GetMapping(value = "/{id}/image", produces = "image/png")
    ResponseEntity getDetectionImage(@PathVariable Long id) {
        Optional<DetectionImage> detectionImage = detectionImageService.findOne(id)
        if (detectionImage.isEmpty()) {
            throw new NotFoundException("Couldn't find resource")
        }

        def decodedImage = Base64.getDecoder().decode(detectionImage.get().image)
        return new ResponseEntity(decodedImage, HttpStatus.OK)
    }

    @DeleteMapping(value = "/{id}/image")
    ResponseEntity deleteDetectionImage(@PathVariable Long id) {
        Optional<DetectionImage> detectionImage = detectionImageService.findOne(id)
        if (detectionImage.isEmpty()) {
            throw new NotFoundException("Couldn't find resource")
        }

        detectionImageService.delete(detectionImage.get())

        return new ResponseEntity(HttpStatus.OK)
    }

    @GetMapping("/{id}")
    ResponseEntity getDetection(@PathVariable Long id) {
        Optional<DetectionImage> detectionImage = detectionImageService.findOne(id)
        if (detectionImage.isEmpty()) {
            throw new NotFoundException("Couldn't find resource")
        }

        def detectionImageDto = convertToDTO(detectionImage.get())
        return new ResponseEntity(detectionImageDto, HttpStatus.OK)
    }

    @PatchMapping()
    ResponseEntity validateDetections(@RequestBody ArrayList<DetectionDTO> detectionsDto) {
        List<Detection> detections = detectionService.findAllById(detectionsDto.collect { it.id })

        for (def detection in detections) {
            def detectionDto = detectionsDto.find { it.id == detection.id }
            detection.valid = detectionDto.valid
        }
        detectionService.saveAll(detections)
        return new ResponseEntity("Successfully patched", HttpStatus.NO_CONTENT)
    }

    @GetMapping("/valid")
    ResponseEntity getValidityPercentage() {
        List<Detection> detections = detectionService.findAll()

        Set<String> modelNames = detections
                .collect { it.detectionImage }
                .unique()
                .collect { it.modelName }
                .unique()

        List<DetectionValidityDTO> detectionValidityList = new ArrayList<>(modelNames.size())

        for (String modelName in modelNames) {
            Double detectionCount = detections.count { it.detectionImage.modelName == modelName }.toFloat()

            Double validPercentage = detections.count {
                it.detectionImage.modelName == modelName && it.valid
            } / detectionCount

            Double invalidPercentage = detections.count { //noinspection GroovyPointlessBoolean
                it.detectionImage.modelName == modelName && it.valid == false
            } / detectionCount

            Double unverifiedPercentage = detections.count {
                it.detectionImage.modelName == modelName && it.valid == null
            } / detectionCount

            DetectionValidityDTO detectionValidityDTO = new DetectionValidityDTO()
            detectionValidityDTO.modelName = modelName
            detectionValidityDTO.valid = validPercentage.round(3)
            detectionValidityDTO.invalid = invalidPercentage.round(3)
            detectionValidityDTO.unverified = unverifiedPercentage.round(3)

            detectionValidityList.add(detectionValidityDTO)
        }

        return new ResponseEntity(detectionValidityList, HttpStatus.OK)
    }

    private static DetectionImageDTO convertToDTO(DetectionImage detectionImage) {
        DetectionImageDTO detectionImageDTO = new DetectionImageDTO()
        detectionImageDTO.id = detectionImage.id
        detectionImageDTO.modelName = detectionImage.modelName
        detectionImageDTO.modelName = detectionImage.modelName

        def detections = new ArrayList<DetectionDTO>(detectionImage.detections.size())
        for (Detection detection in detectionImage.detections) {
            DetectionDTO detectionDTO = new DetectionDTO()
            detectionDTO.id = detection.id
            detectionDTO.className = detection.className
            detectionDTO.confidence = detection.confidence
            detectionDTO.locationLeft = detection.locationLeft
            detectionDTO.locationTop = detection.locationTop
            detectionDTO.locationRight = detection.locationRight
            detectionDTO.locationBottom = detection.locationBottom
            detectionDTO.valid = detection.valid

            detections.add(detectionDTO)
        }

        detectionImageDTO.detections = detections

        return detectionImageDTO
    }

    private static DetectionImage convertFromDTO(DetectionImageDTO detectionImageDTO) {
        DetectionImage detectionImage = new DetectionImage()
        detectionImage.image = detectionImageDTO.image.bytes
        detectionImage.modelName = detectionImageDTO.modelName

        def detections = new ArrayList<Detection>(detectionImageDTO.detections.size())
        for (DetectionDTO detectionDTO in detectionImageDTO.detections) {
            Detection detection = new Detection()
            detection.className = detectionDTO.className
            detection.confidence = detectionDTO.confidence
            detection.locationLeft = detectionDTO.locationLeft
            detection.locationTop = detectionDTO.locationTop
            detection.locationRight = detectionDTO.locationRight
            detection.locationBottom = detectionDTO.locationBottom

            detections.add(detection)
        }

        detectionImage.detections = detections

        return detectionImage
    }

}
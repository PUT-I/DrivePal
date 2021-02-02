package pl.dps.controllers

import javassist.NotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.dps.Utils
import pl.dps.models.DiagnosticData
import pl.dps.models.SocDictionary
import pl.dps.models.dto.AverageTimesDTO
import pl.dps.services.DiagnosticDataService
import pl.dps.services.SocDictionaryService

import javax.validation.Valid
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/diagnostic-data")
class DiagnosticDataController extends AbstractController {

    @Autowired
    DiagnosticDataService diagnosticDataService

    @Autowired
    SocDictionaryService socDictionaryService

    @PostMapping
    ResponseEntity saveDiagnosticData(@Valid @RequestBody DiagnosticData diagnosticData) {
        def savedData = diagnosticDataService.save(diagnosticData)
        return new ResponseEntity(savedData, HttpStatus.OK)
    }

    @GetMapping("/average")
    ResponseEntity getTotalAverage(@RequestParam(required = false) String soc,
                                   @RequestParam(required = false) Boolean useSocDictionary) {

        if (useSocDictionary && soc != null && soc.toUpperCase() != "ALL") {
            List<SocDictionary> socDictionary = socDictionaryService.findAll()
            SocDictionary dictionaryEntry = socDictionary.find { it.name == soc }
            if (dictionaryEntry != null) {
                soc = dictionaryEntry.code
            }
        }
        List<AverageTimesDTO> inferenceAverage = diagnosticDataService.getTotalAverage(soc)

        return new ResponseEntity(inferenceAverage, HttpStatus.OK)
    }

    @GetMapping("/{id}")
    ResponseEntity getOneDiagnosticData(@PathVariable Long id) {
        def diagnosticData = diagnosticDataService.findOne(id)
        if (diagnosticData.isEmpty()) {
            throw new NotFoundException("Couldn't find resource")
        }
        if (diagnosticData.isPresent()) {
            return new ResponseEntity(diagnosticData.get(), HttpStatus.OK)
        }
        throw new NotFoundException("Couldn't find resource")
    }

    @GetMapping("/models")
    ResponseEntity getModels() {
        List<DiagnosticData> diagnosticData = diagnosticDataService.findAll()

        Set<String> models = diagnosticData.collect { it.modelName }.unique()

        return new ResponseEntity(models, HttpStatus.OK)
    }

    @GetMapping("/soc")
    ResponseEntity getSoc(@RequestParam(required = false) Boolean useSocDictionary) {
        List<DiagnosticData> diagnosticData = diagnosticDataService.findAll()

        Set<String> socs = diagnosticData.collect { it.soc }.unique()

        if (useSocDictionary) {
            List<SocDictionary> socDictionary = socDictionaryService.findAll()

            socs = socs.collect { String soc ->
                def dictionaryEntry = socDictionary.find { it.code == soc }
                if (dictionaryEntry == null) {
                    return soc
                }

                return dictionaryEntry.name
            }
        }

        return new ResponseEntity(socs, HttpStatus.OK)
    }

    @GetMapping("/devices")
    ResponseEntity getDevices() {
        List<DiagnosticData> diagnosticData = diagnosticDataService.findAll()

        Set<String> devices = diagnosticData
                .collect { it.deviceId }.unique()
                .findAll { it != null }

        return new ResponseEntity(devices, HttpStatus.OK)
    }

    @GetMapping
    ResponseEntity getAllDiagnosticData(@RequestParam(required = false) String from,
                                        @RequestParam(required = false) String to,
                                        @RequestParam(required = false) String modelName,
                                        @RequestParam(required = false) String deviceId) {
        List<DiagnosticData> diagnosticData = diagnosticDataService.findAll()

        if (modelName != null && !modelName.isBlank()) {
            diagnosticData = diagnosticData.findAll { it.modelName == modelName }
        }

        if (deviceId != null && !deviceId.isBlank()) {
            diagnosticData = diagnosticData.findAll { it.deviceId == deviceId }
        }

        if (from != null && !from.isBlank()) {
            LocalDateTime dateFrom = Utils.parseDate(from)
            diagnosticData = diagnosticData.findAll { it.timeStamp >= dateFrom }
        }

        if (to != null && !to.isBlank()) {
            LocalDateTime dateTo = Utils.parseDate(to)
            diagnosticData = diagnosticData.findAll { it.timeStamp <= dateTo }
        }

        return new ResponseEntity(diagnosticData, HttpStatus.OK)
    }
}

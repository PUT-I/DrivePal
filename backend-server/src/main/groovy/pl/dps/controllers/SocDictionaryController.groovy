package pl.dps.controllers

import javassist.NotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.dps.models.SocDictionary
import pl.dps.services.SocDictionaryService

import javax.validation.Valid

@RestController
@RequestMapping("/api/soc-dictionary")
class SocDictionaryController extends AbstractController {

    @Autowired
    SocDictionaryService socDictionaryService

    @PostMapping
    ResponseEntity saveEntity(@Valid @RequestBody SocDictionary socDictionary) {
        def savedDictionaryEntry = socDictionaryService.save(socDictionary)
        return new ResponseEntity(savedDictionaryEntry, HttpStatus.OK)
    }

    @PatchMapping
    ResponseEntity updateEntity(@RequestBody SocDictionary requestDictionaryEntry) {
        def dictionaryEntryOptional = socDictionaryService.findByCode(requestDictionaryEntry.code)

        if (dictionaryEntryOptional == null) {
            throw new NotFoundException("Couldn't find resource")
        }

        def dictionaryEntry = dictionaryEntryOptional.get()
        dictionaryEntry.name = requestDictionaryEntry.name

        socDictionaryService.save(dictionaryEntry)

        return new ResponseEntity(dictionaryEntry, HttpStatus.OK)
    }

    @GetMapping("/{id}")
    ResponseEntity getEntity(@PathVariable Long id) {
        def dictionaryEntry = socDictionaryService.findOne(id)
        if (dictionaryEntry.isEmpty()) {
            throw new NotFoundException("Couldn't find resource")
        }
        if (dictionaryEntry.isPresent()) {
            return new ResponseEntity(dictionaryEntry, HttpStatus.OK)
        }
        throw new NotFoundException("Couldn't find resource")
    }

    @DeleteMapping("/{id}")
    ResponseEntity deleteEntity(@PathVariable Long id) {
        def dictionaryEntry = socDictionaryService.findOne(id)
        if (dictionaryEntry == null) {
            throw new NotFoundException("Couldn't find resource")
        }

        socDictionaryService.delete(dictionaryEntry.get())
        return new ResponseEntity(dictionaryEntry.get(), HttpStatus.OK)
    }

    @GetMapping
    ResponseEntity getAllEntities() {
        return new ResponseEntity(socDictionaryService.findAll(), HttpStatus.OK)
    }

}
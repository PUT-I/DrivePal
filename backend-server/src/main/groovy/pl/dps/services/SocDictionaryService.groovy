package pl.dps.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import pl.dps.models.SocDictionary
import pl.dps.repositories.SocDictionaryRepository

@Service
class SocDictionaryService {
    @Autowired
    SocDictionaryRepository socDictionaryRepository

    SocDictionary save(SocDictionary dictionaryEntry) {
        return socDictionaryRepository.save(dictionaryEntry)
    }

    Optional<SocDictionary> findByCode(String code) {

        return socDictionaryRepository.findByCode(code)
    }

    Optional<SocDictionary> findOne(Long id) {
        return socDictionaryRepository.findById(id)
    }

    List<SocDictionary> findAll() {
        return socDictionaryRepository.findAll()
    }

    void delete(SocDictionary dictionaryEntry) {
        socDictionaryRepository.delete(dictionaryEntry)
    }
}
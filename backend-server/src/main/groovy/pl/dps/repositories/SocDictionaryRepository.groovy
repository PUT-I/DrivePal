package pl.dps.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pl.dps.models.SocDictionary

@Repository
interface SocDictionaryRepository extends JpaRepository<SocDictionary, Long> {
    Optional<SocDictionary> findByCode(String code);
}
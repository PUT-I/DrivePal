package pl.dps.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pl.dps.models.Detection

@Repository
interface DetectionRepository extends JpaRepository<Detection, Long> {

}
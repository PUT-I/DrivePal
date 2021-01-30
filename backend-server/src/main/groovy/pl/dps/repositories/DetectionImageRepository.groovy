package pl.dps.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pl.dps.models.DetectionImage

@Repository
interface DetectionImageRepository extends JpaRepository<DetectionImage, Long> {

}
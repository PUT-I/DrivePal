package pl.dps.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pl.dps.models.DiagnosticData

@Repository
interface DiagnosticDataRepository extends JpaRepository<DiagnosticData, Long> {
}
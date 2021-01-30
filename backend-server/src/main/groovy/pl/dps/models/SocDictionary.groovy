package pl.dps.models

import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@NamedQuery(name = "SocDictionary.findByCode",
        query = "SELECT soc FROM SocDictionary soc WHERE LOWER(soc.code) = LOWER(?1)")
class SocDictionary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id

    @NotBlank
    @Column(unique = true)
    public String code

    @NotBlank
    public String name
}

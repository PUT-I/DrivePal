package pl.dps.models.enums

enum SignType {
    WARNING(0),
    INFORMATION(1),
    PROHIBITION(2),
    MANDATORY(3)

    private int id

    private SignType(int id){
        this.id = id
    }
}
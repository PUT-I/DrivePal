package pl.dps

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime

class Utils {
    static DateFormat isoDateFormat1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    static DateFormat isoDateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")

    static LocalDateTime parseDate(String dateString) {
        if (dateString.contains("Z")) {
            try {
                return isoDateFormat1.parse(dateString).toLocalDateTime()
            } catch(ParseException ignored){
                return isoDateFormat2.parse(dateString).toLocalDateTime()
            }
        } else {
            return LocalDateTime.parse(dateString)
        }
    }
}

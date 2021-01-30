package pl.dps

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties

@SpringBootApplication
@EnableConfigurationProperties
class DPSApplication {
    static void main(String[] args) {
        SpringApplication.run(DPSApplication, args)
    }
}
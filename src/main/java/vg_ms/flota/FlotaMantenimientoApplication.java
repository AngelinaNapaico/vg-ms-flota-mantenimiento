package vg_ms.flota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@SpringBootApplication
@EnableReactiveMongoRepositories
public class FlotaMantenimientoApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlotaMantenimientoApplication.class, args);
    }
}

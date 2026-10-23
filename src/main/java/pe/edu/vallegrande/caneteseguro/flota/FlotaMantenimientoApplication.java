package pe.edu.vallegrande.caneteseguro.flota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;

/**
 * Microservicio de Flota y Mantenimiento.
 */
@SpringBootApplication
@EnableReactiveMongoAuditing
public class FlotaMantenimientoApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlotaMantenimientoApplication.class, args);
    }
}


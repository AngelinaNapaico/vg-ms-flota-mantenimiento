package pe.edu.vallegrande.flota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * Microservicio de Flota y Mantenimiento - Estándar PR241.
 */
@SpringBootApplication
@EnableReactiveMongoRepositories(basePackages = "pe.edu.vallegrande.flota.infrastructure.repository")
public class FlotaMantenimientoApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlotaMantenimientoApplication.class, args);
    }
}

package co.edu.proyecto.orden;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de order-api. {@code @EnableScheduling} activa la tarea periódica del publicador de la
 * bandeja de salida (outbox → Kafka); no pertenece a la configuración de Kafka.
 */
@SpringBootApplication
@EnableScheduling
public class OrdenApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdenApiApplication.class, args);
    }
}

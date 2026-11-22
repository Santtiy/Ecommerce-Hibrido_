package co.edu.proyecto.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada de billing-worker: simula el cobro de cada orden. */
@SpringBootApplication
public class BillingWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingWorkerApplication.class, args);
    }
}

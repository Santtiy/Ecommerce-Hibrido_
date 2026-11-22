package co.edu.proyecto.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada de inventory-worker: simula la reserva de stock. */
@SpringBootApplication
public class InventoryWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryWorkerApplication.class, args);
    }
}

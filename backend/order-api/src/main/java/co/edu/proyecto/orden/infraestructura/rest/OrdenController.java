package co.edu.proyecto.orden.infraestructura.rest;

import co.edu.proyecto.orden.aplicacion.RegistrarOrdenService;
import co.edu.proyecto.orden.dominio.ItemOrden;
import co.edu.proyecto.orden.dominio.Orden;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** POST /order: valida la entrada, registra la orden y responde 202 Accepted. */
@RestController
public class OrdenController {

    private static final String MONEDA = "COP";

    private final RegistrarOrdenService registrarOrden;

    public OrdenController(RegistrarOrdenService registrarOrden) {
        this.registrarOrden = registrarOrden;
    }

    @PostMapping("/order")
    public ResponseEntity<RespuestaOrden> crear(@Valid @RequestBody CrearOrdenRequest solicitud) {
        List<ItemOrden> items = solicitud.items().stream().map(item -> item.aDominio(MONEDA)).toList();
        Orden orden = registrarOrden.registrar(solicitud.cliente(), items);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(RespuestaOrden.desde(orden));
    }
}

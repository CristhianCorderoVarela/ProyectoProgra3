package cr.ac.una.restunaclient.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import cr.ac.una.restunaclient.model.DetalleOrden;
import cr.ac.una.restunaclient.model.Orden;
import cr.ac.una.restunaclient.model.Producto;
import cr.ac.una.restunaclient.service.RestClient;
import cr.ac.una.restunaclient.util.AppContext;
import cr.ac.una.restunaclient.util.FlowController;
import cr.ac.una.restunaclient.util.Mensaje;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VentanaVentasController implements Initializable {

    // HEADER
    @FXML private Label lblTitle;
    @FXML private Label lblUsuario;
    @FXML private Label lblFechaHora;
    @FXML private Button btnVolver;

    // Datos orden
    @FXML private Label lblOrdenInfo;
    @FXML private Label lblMesaInfo;
    @FXML private Button btnSeleccionarOrden;

    // Cliente
    @FXML private TextField txtCliente;
    @FXML private Button btnBuscarCliente;

    // Tabla productos
    @FXML private TableView<DetalleOrden> tblProductos;
    @FXML private TableColumn<DetalleOrden, String> colProducto;
    @FXML private TableColumn<DetalleOrden, Integer> colCantidad;
    @FXML private TableColumn<DetalleOrden, String> colPrecio;
    @FXML private TableColumn<DetalleOrden, String> colSubtotal;
    @FXML private TableColumn<DetalleOrden, String> colAcciones; // en V1 no la usamos activamente

    @FXML private Button btnAgregarProducto;      // V1: deshabilitado (edición solo en Órdenes)
    @FXML private Button btnModificarCantidad;    // V1: deshabilitado
    @FXML private Button btnEliminarProducto;     // V1: deshabilitado

    // Totales
    @FXML private Label lblSubtotal;
    @FXML private CheckBox chkImpuestoVentas;
    @FXML private Label lblPorcentajeIV;
    @FXML private Label lblImpuestoVentas;
    @FXML private CheckBox chkImpuestoServicio;
    @FXML private Label lblPorcentajeIS;
    @FXML private Label lblImpuestoServicio;
    @FXML private TextField txtDescuento;
    @FXML private Label lblDescuento;
    @FXML private Label lblDescuentoMax;
    @FXML private Label lblTotal;

    // Pagos
    @FXML private TextField txtEfectivo;
    @FXML private TextField txtTarjeta;
    @FXML private Label lblVuelto;

    // Botones finales
    @FXML private Button btnProcesarPago;
    @FXML private Button btnImprimir;
    @FXML private Button btnEnviarEmail;
    @FXML private Button btnCancelar;

    // ====== Gson con adaptadores para java.time ======
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDate.class,
                    (JsonDeserializer<java.time.LocalDate>) (je, t, ctx) ->
                            java.time.LocalDate.parse(je.getAsString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (JsonDeserializer<java.time.LocalDateTime>) (je, t, ctx) -> {
                        String s = je.getAsString();
                        try {
                            return java.time.LocalDateTime.parse(
                                    s, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        } catch (Exception e) {
                            // fallback si viene solo la fecha
                            return java.time.LocalDate.parse(
                                    s, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
                        }
                    })
            .create();

    private final ObservableList<DetalleOrden> lineas = FXCollections.observableArrayList();
    private Orden ordenSeleccionada;

    // Config negocio (puedes llevarlo a AppContext si quieres)
    private static final BigDecimal IV_PORC = new BigDecimal("0.13");   // 13%
    private static final BigDecimal SERV_PORC = new BigDecimal("0.10"); // 10%
    private static final BigDecimal DESCUENTO_MAX = new BigDecimal("15"); // 15%

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Usuario + fecha/hora
        var user = AppContext.getInstance().getUsuarioLogueado();
        lblUsuario.setText(user != null ? "Usuario: " + user.getNombre() : "Usuario: —");
        lblFechaHora.setText(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now()));

        // Tabla
        colProducto.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getProducto() != null ? d.getValue().getProducto().getNombre() : "Producto"
                )
        );
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(d -> new SimpleStringProperty("₡" + fmt(d.getValue().getPrecioUnitario())));
        colSubtotal.setCellValueFactory(d -> new SimpleStringProperty("₡" + fmt(d.getValue().getSubtotal())));
        colAcciones.setCellValueFactory(d -> new SimpleStringProperty(""));

        tblProductos.setItems(lineas);

        // En V1 la edición de líneas se hace en “Órdenes”
        btnAgregarProducto.setDisable(true);
        btnModificarCantidad.setDisable(true);
        btnEliminarProducto.setDisable(true);

        // Defaults de caja
        chkImpuestoVentas.setSelected(true);
        chkImpuestoServicio.setSelected(true);
        txtDescuento.setText("0");
        txtEfectivo.setText("0.00");
        txtTarjeta.setText("0.00");

        // Cálculos reactivos
        txtDescuento.setOnKeyReleased(e -> onCalcularTotales(null));
        txtEfectivo.setOnKeyReleased(e -> onCalcularVuelto(null));
        txtTarjeta.setOnKeyReleased(e -> onCalcularVuelto(null));
        chkImpuestoVentas.setOnAction(e -> onCalcularTotales(null));
        chkImpuestoServicio.setOnAction(e -> onCalcularTotales(null));

        // Estado inicial
        limpiarPantalla();
    }

    private void limpiarPantalla() {
        lblOrdenInfo.setText("Orden: —");
        lblMesaInfo.setText("Mesa: —");
        txtCliente.clear();
        lineas.clear();

        lblSubtotal.setText("₡0.00");
        lblImpuestoVentas.setText("₡0.00");
        lblImpuestoServicio.setText("₡0.00");
        lblDescuento.setText("-₡0.00");
        lblTotal.setText("₡0.00");
        lblVuelto.setText("₡0.00");
        ordenSeleccionada = null;
    }

    // ---------- Util ----------
    private static String fmt(BigDecimal n) {
        if (n == null) return "0.00";
        return String.format("%,.2f", n);
    }
    private static BigDecimal nz(BigDecimal n) {
        return n == null ? BigDecimal.ZERO : n;
    }
    private static BigDecimal parseMonto(String txt) {
        try {
            String t = txt == null ? "0" : txt.trim().replace("₡","").replace(",","");
            return new BigDecimal(t);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    private static BigDecimal parsePct(String txt) {
        try {
            String t = txt == null ? "0" : txt.replace("%","").trim();
            return new BigDecimal(t);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // ---------- Acciones UI ----------
    @FXML
    private void onVolver(ActionEvent event) {
        FlowController.getInstance().goToView("MenuPrincipal", "RestUNA - Menú", 1000, 560);
    }

    @FXML
    private void onSeleccionarOrden(ActionEvent event) {
        try {
            String res = RestClient.get("/ordenes/activas");
            Map<String, Object> body = RestClient.parseResponse(res);
            if (!Boolean.TRUE.equals(body.get("success"))) {
                Mensaje.showWarning("Facturación", "No se pudieron obtener órdenes activas.");
                return;
            }
            String data = gson.toJson(body.get("data"));
            List<Orden> ordenes = gson.fromJson(data, new TypeToken<List<Orden>>(){}.getType());
            if (ordenes == null || ordenes.isEmpty()) {
                Mensaje.showInfo("Facturación", "No hay órdenes abiertas.");
                return;
            }

            // Mapeamos display -> Orden
            LinkedHashMap<String, Orden> map = new LinkedHashMap<>();
            for (Orden o : ordenes) {
                map.put(buildDisplay(o), o);
            }

            List<String> opciones = new ArrayList<>(map.keySet());
            ChoiceDialog<String> dlg = new ChoiceDialog<>(opciones.get(0), opciones);
            dlg.setTitle("Seleccionar Orden");
            dlg.setHeaderText("Elija una orden abierta");
            dlg.setContentText("Órdenes:");

            Optional<String> opt = dlg.showAndWait();
            if (opt.isEmpty()) return;

            this.ordenSeleccionada = map.get(opt.get());
            cargarDetallesDeOrden(this.ordenSeleccionada.getId());

            // Info cabecera
            lblOrdenInfo.setText("Orden #" + ordenSeleccionada.getId());
            String mesaTxt = (ordenSeleccionada.getMesa() != null)
                    ? (ordenSeleccionada.getMesa().getIdentificador() != null
                    && !ordenSeleccionada.getMesa().getIdentificador().isBlank()
                    ? ordenSeleccionada.getMesa().getIdentificador()
                    : String.valueOf(ordenSeleccionada.getMesa().getId()))
                    : "Barra";
            lblMesaInfo.setText("Mesa: " + mesaTxt);

            onCalcularTotales(null);

        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la lista de órdenes.");
        }
    }

    /** Crea el texto visible en el diálogo para una orden. */
    private String buildDisplay(Orden o) {
        String ubic = (o.getMesa() != null
                ? "Mesa " + ((o.getMesa().getIdentificador() != null && !o.getMesa().getIdentificador().isBlank())
                ? o.getMesa().getIdentificador()
                : (o.getMesa().getId() != null ? o.getMesa().getId() : "—"))
                : "Barra");
        String fecha = (o.getFechaHora() != null)
                ? o.getFechaHora().toString().replace('T',' ')
                : "";
        return "Orden #" + o.getId() + " · " + ubic + (fecha.isEmpty() ? "" : " · " + fecha);
    }

    private void cargarDetallesDeOrden(Long ordenId) {
        try {
            String res = RestClient.get("/ordenes/" + ordenId + "/detalles");
            Map<String, Object> body = RestClient.parseResponse(res);
            if (!Boolean.TRUE.equals(body.get("success"))) {
                Mensaje.showWarning("Facturación", "No se pudieron cargar los detalles.");
                return;
            }
            String data = gson.toJson(body.get("data"));
            List<DetalleOrden> detalles = gson.fromJson(data, new TypeToken<List<DetalleOrden>>(){}.getType());
            lineas.setAll(detalles != null ? detalles : Collections.emptyList());
            onCalcularTotales(null);
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error cargando detalles de la orden.");
        }
    }

    @FXML
    private void onBuscarCliente(ActionEvent event) {
        Mensaje.showInfo("Cliente", "Búsqueda de cliente (opcional). V1: ingresa el nombre manualmente.");
    }

    @FXML
    private void onAgregarProducto(ActionEvent event) {
        Mensaje.showInfo("Productos", "La edición de productos se realiza en la ventana Órdenes (V1).");
    }

    @FXML
    private void onModificarCantidad(ActionEvent event) {
        Mensaje.showInfo("Cantidad", "La edición de cantidades se realiza en Órdenes (V1).");
    }

    @FXML
    private void onEliminarProducto(ActionEvent event) {
        Mensaje.showInfo("Eliminar", "La eliminación de productos se realiza en Órdenes (V1).");
    }

    @FXML
    private void onCalcularTotales(ActionEvent event) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetalleOrden d : lineas) {
            subtotal = subtotal.add(nz(d.getSubtotal()));
        }

        // Impuestos
        BigDecimal iv = BigDecimal.ZERO;
        if (chkImpuestoVentas.isSelected()) {
            iv = subtotal.multiply(IV_PORC);
        }

        BigDecimal serv = BigDecimal.ZERO;
        if (chkImpuestoServicio.isSelected()) {
            serv = subtotal.multiply(SERV_PORC);
        }

        // Descuento (%)
        BigDecimal descPct = parsePct(txtDescuento.getText());
        if (descPct.compareTo(DESCUENTO_MAX) > 0) {
            descPct = DESCUENTO_MAX;
            txtDescuento.setText(DESCUENTO_MAX.toPlainString());
        } else if (descPct.compareTo(BigDecimal.ZERO) < 0) {
            descPct = BigDecimal.ZERO;
            txtDescuento.setText("0");
        }
        BigDecimal descuento = subtotal.add(iv).add(serv).multiply(descPct.movePointLeft(2));

        BigDecimal total = subtotal.add(iv).add(serv).subtract(descuento);

        lblSubtotal.setText("₡" + fmt(subtotal));
        lblImpuestoVentas.setText("₡" + fmt(iv));
        lblImpuestoServicio.setText("₡" + fmt(serv));
        lblDescuento.setText("-₡" + fmt(descuento));
        lblTotal.setText("₡" + fmt(total));

        onCalcularVuelto(null);
    }

    @FXML
    private void onCalcularVuelto(ActionEvent event) {
        BigDecimal total = parseMonto(lblTotal.getText());
        BigDecimal efectivo = parseMonto(txtEfectivo.getText());
        BigDecimal tarjeta = parseMonto(txtTarjeta.getText());
        BigDecimal pagado = efectivo.add(tarjeta);
        BigDecimal vuelto = pagado.subtract(total);
        if (vuelto.compareTo(BigDecimal.ZERO) < 0) vuelto = BigDecimal.ZERO;
        lblVuelto.setText("₡" + fmt(vuelto));
    }

    @FXML
    private void onProcesarPago(ActionEvent event) {
        if (ordenSeleccionada == null) {
            Mensaje.showWarning("Facturación", "Primero selecciona una orden.");
            return;
        }
        if (lineas.isEmpty()) {
            Mensaje.showWarning("Facturación", "La orden no tiene productos.");
            return;
        }

        BigDecimal total = parseMonto(lblTotal.getText());
        BigDecimal efectivo = parseMonto(txtEfectivo.getText());
        BigDecimal tarjeta = parseMonto(txtTarjeta.getText());
        BigDecimal pagado = efectivo.add(tarjeta);

        if (pagado.compareTo(total) < 0) {
            Mensaje.showWarning("Pago insuficiente", "El monto pagado es menor que el total.");
            return;
        }

        try {
            // Construir payload de factura
            Map<String, Object> payload = new HashMap<>();
            payload.put("ordenId", ordenSeleccionada.getId());
            payload.put("clienteNombre", Optional.ofNullable(txtCliente.getText()).orElse("").trim());

            Map<String, Object> resumen = new HashMap<>();
            resumen.put("subtotal", parseMonto(lblSubtotal.getText()));
            resumen.put("impuestoVentas", parseMonto(lblImpuestoVentas.getText()));
            resumen.put("impuestoServicio", parseMonto(lblImpuestoServicio.getText()));
            // descuento se muestra como "-₡x", lo convertimos a valor positivo
            BigDecimal desc = parseMonto(lblDescuento.getText().replace("-",""));
            resumen.put("descuento", desc);
            resumen.put("total", total);
            payload.put("resumen", resumen);

            Map<String, Object> pagos = new HashMap<>();
            pagos.put("efectivo", efectivo);
            pagos.put("tarjeta", tarjeta);
            payload.put("pagos", pagos);

            List<Map<String, Object>> items = new ArrayList<>();
            for (DetalleOrden d : lineas) {
                Map<String, Object> it = new HashMap<>();
                Long pid = d.getProductoId() != null ? d.getProductoId()
                        : (d.getProducto() != null ? d.getProducto().getId() : null);
                it.put("productoId", pid);
                it.put("nombre", d.getProducto() != null ? d.getProducto().getNombre() : "Producto");
                it.put("cantidad", d.getCantidad());
                it.put("precioUnitario", d.getPrecioUnitario());
                items.add(it);
            }
            payload.put("items", items);

            // 1) Crear factura
            String resFactura = RestClient.post("/facturas", payload);
            Map<String, Object> r1 = RestClient.parseResponse(resFactura);
            if (!Boolean.TRUE.equals(r1.get("success"))) {
                Mensaje.showError("Error", "No se pudo crear la factura:\n" + r1.get("message"));
                return;
            }

            // 2) Marcar orden como FACTURADA (opción A)
            boolean facturada = false;
            try {
                String resFact = RestClient.post("/ordenes/" + ordenSeleccionada.getId() + "/facturar", null);
                Map<String, Object> rA = RestClient.parseResponse(resFact);
                facturada = Boolean.TRUE.equals(rA.get("success"));
            } catch (Exception ignored) { }

            // 2b) Fallback opción B (PUT estado)
            if (!facturada) {
                Map<String, Object> upd = Map.of("estado", "FACTURADA");
                try {
                    String resPut = RestClient.put("/ordenes/" + ordenSeleccionada.getId(), upd);
                    Map<String, Object> rB = RestClient.parseResponse(resPut);
                    facturada = Boolean.TRUE.equals(rB.get("success"));
                } catch (Exception ignored) { }
            }

            Mensaje.showSuccess("Éxito", facturada
                    ? "Factura registrada y orden marcada como FACTURADA."
                    : "Factura registrada. (No se pudo actualizar el estado de la orden.)");

            limpiarPantalla();

        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Fallo procesando el pago:\n" + e.getMessage());
        }
    }

    @FXML
    private void onImprimir(ActionEvent event) {
        Mensaje.showInfo("Imprimir", "V1: genera e imprime el comprobante luego de procesar el pago.");
    }

    @FXML
    private void onEnviarEmail(ActionEvent event) {
        Mensaje.showInfo("Email", "V1: envío por correo pendiente (requiere servicio en backend).");
    }

    @FXML
    private void onCancelar(ActionEvent event) {
        limpiarPantalla();
    }
}
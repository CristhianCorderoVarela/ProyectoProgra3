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
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

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

    // Tabla productos (detalle de la ORDEN actual)
    @FXML private TableView<DetalleOrden> tblProductos;
    @FXML private TableColumn<DetalleOrden, String> colProducto;
    @FXML private TableColumn<DetalleOrden, Integer> colCantidad;
    @FXML private TableColumn<DetalleOrden, String> colPrecio;
    @FXML private TableColumn<DetalleOrden, String> colSubtotal;
    @FXML private TableColumn<DetalleOrden, String> colAcciones;

    @FXML private Button btnAgregarProducto;
    @FXML private Button btnModificarCantidad;
    @FXML private Button btnEliminarProducto;

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

    // Gson (con adaptadores para java.time.*)
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
                            return java.time.LocalDate.parse(
                                    s, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
                        }
                    })
            .create();

    // Detalle de la orden (líneas que se ven en tblProductos)
    private final ObservableList<DetalleOrden> lineas = FXCollections.observableArrayList();
    private Orden ordenSeleccionada;

    // ===== Cliente seleccionado actual =====
    private Long   clienteSeleccionadoId     = null;
    private String clienteSeleccionadoNombre = null;
    private String clienteSeleccionadoCorreo = null;

    // ===== Autocomplete clientes =====
    private final ContextMenu menuClientes = new ContextMenu();
    private List<Map<String,Object>> ultimosClientes = Collections.emptyList();
    private final PauseTransition clienteSearchDelay = new PauseTransition(Duration.millis(250));

    // ===== Catálogo de productos (todos los productos activos del backend) =====
    // Lo cargamos una sola vez y luego filtramos en memoria.
    private final ObservableList<Producto> catalogoProductos = FXCollections.observableArrayList();

    // Config negocio
    private static final BigDecimal IV_PORC       = new BigDecimal("0.13"); // 13%
    private static final BigDecimal SERV_PORC     = new BigDecimal("0.10"); // 10%
    private static final BigDecimal DESCUENTO_MAX = new BigDecimal("15");   // 15%

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        var user = AppContext.getInstance().getUsuarioLogueado();
        lblUsuario.setText(user != null ? "Usuario: " + user.getNombre() : "Usuario: —");
        lblFechaHora.setText(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now()));

        // Config tabla de productos (líneas de la orden en facturación)
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

        // Impuestos / pago defaults
        chkImpuestoVentas.setSelected(true);
        chkImpuestoServicio.setSelected(true);
        txtDescuento.setText("0");
        txtEfectivo.setText("0.00");
        txtTarjeta.setText("0.00");

        // Recalcular totales / vuelto en vivo
        txtDescuento.setOnKeyReleased(e -> onCalcularTotales(null));
        txtEfectivo.setOnKeyReleased(e -> onCalcularVuelto(null));
        txtTarjeta.setOnKeyReleased(e -> onCalcularVuelto(null));
        chkImpuestoVentas.setOnAction(e -> onCalcularTotales(null));
        chkImpuestoServicio.setOnAction(e -> onCalcularTotales(null));

        // ==== Autocomplete Cliente ====
        clienteSearchDelay.setOnFinished(e -> buscarYMostrarSugerencias(txtCliente.getText().trim()));
        txtCliente.focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) {
                menuClientes.hide();
            }
        });

        limpiarPantalla(); // también setea estados iniciales de botones
    }

    // =================================================================================
    // ESTADO / RESET
    // =================================================================================
    private void limpiarPantalla() {
        lblOrdenInfo.setText("Orden: —");
        lblMesaInfo.setText("Mesa: —");

        // limpiar cliente
        txtCliente.clear();
        clienteSeleccionadoId     = null;
        clienteSeleccionadoNombre = null;
        clienteSeleccionadoCorreo = null;
        menuClientes.hide();
        ultimosClientes = Collections.emptyList();

        // limpiar líneas y totales
        lineas.clear();
        lblSubtotal.setText("₡0.00");
        lblImpuestoVentas.setText("₡0.00");
        lblImpuestoServicio.setText("₡0.00");
        lblDescuento.setText("-₡0.00");
        lblTotal.setText("₡0.00");
        lblVuelto.setText("₡0.00");

        ordenSeleccionada = null;

        // botones de edición de productos
        updateBotonesEdicion();
    }

    /**
     * Habilita/deshabilita los botones relacionados con productos según tengamos una orden seleccionada.
     * - Agregar: SÍ se puede si ya hay ordenSeleccionada.
     * - Modificar/Eliminar: todavía NO implementados (quedan deshabilitados siempre).
     */
    private void updateBotonesEdicion() {
        boolean hayOrden = (ordenSeleccionada != null);

        btnAgregarProducto.setDisable(!hayOrden);
        btnModificarCantidad.setDisable(true);
        btnEliminarProducto.setDisable(true);
    }

    // =================================================================================
    // UTILIDADES NUMÉRICAS
    // =================================================================================
    private static String fmt(BigDecimal n) {
        if (n == null) return "0.00";
        return String.format("%,.2f", n);
    }

    private static BigDecimal nz(BigDecimal n) {
        return n == null ? BigDecimal.ZERO : n;
    }

    private static BigDecimal parseMonto(String txt) {
        try {
            String t = txt == null ? "0" : txt.trim().replace("₡", "").replace(",", "");
            return new BigDecimal(t);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal parsePct(String txt) {
        try {
            String t = txt == null ? "0" : txt.replace("%", "").trim();
            return new BigDecimal(t);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // =================================================================================
    // NAVEGACIÓN
    // =================================================================================
    @FXML
    private void onVolver(ActionEvent event) {
        FlowController.getInstance().goToView("MenuPrincipal", "RestUNA - Menú", 1000, 560);
    }

    // =================================================================================
    // ORDENES
    // =================================================================================
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

            lblOrdenInfo.setText("Orden #" + ordenSeleccionada.getId());
            String mesaTxt = (ordenSeleccionada.getMesa() != null)
                    ? (
                    ordenSeleccionada.getMesa().getIdentificador() != null
                            && !ordenSeleccionada.getMesa().getIdentificador().isBlank()
                            ? ordenSeleccionada.getMesa().getIdentificador()
                            : String.valueOf(ordenSeleccionada.getMesa().getId())
            )
                    : "Barra";
            lblMesaInfo.setText("Mesa: " + mesaTxt);

            onCalcularTotales(null);

            updateBotonesEdicion();

        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la lista de órdenes.");
        }
    }

    private String buildDisplay(Orden o) {
        String ubic = (o.getMesa() != null
                ? "Mesa " + (
                o.getMesa().getIdentificador() != null
                        && !o.getMesa().getIdentificador().isBlank()
                        ? o.getMesa().getIdentificador()
                        : (o.getMesa().getId() != null ? o.getMesa().getId() : "—")
        )
                : "Barra");

        String fecha = (o.getFechaHora() != null)
                ? o.getFechaHora().toString().replace('T', ' ')
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
            List<DetalleOrden> detalles = gson.fromJson(data, new TypeToken<List<DetalleOrden>>() {}.getType());

            lineas.setAll(detalles != null ? detalles : Collections.emptyList());
            onCalcularTotales(null);

        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error cargando detalles de la orden.");
        }
    }

    // =================================================================================
    // CLIENTE (autocomplete + botón Buscar)
    // =================================================================================
    @FXML
    private void onClienteKeyTyped(KeyEvent e) {
        // si el texto ya no coincide con el cliente confirmado, limpiamos selección
        if (clienteSeleccionadoNombre == null ||
                !txtCliente.getText().trim().equalsIgnoreCase(
                        clienteSeleccionadoNombre == null ? "" : clienteSeleccionadoNombre.trim())) {

            clienteSeleccionadoId     = null;
            clienteSeleccionadoNombre = null;
            clienteSeleccionadoCorreo = null;
        }

        clienteSearchDelay.stop();
        clienteSearchDelay.playFromStart();
    }

    private void buscarYMostrarSugerencias(String term) {
        try {
            if (term == null || term.isBlank()) {
                menuClientes.hide();
                ultimosClientes = Collections.emptyList();
                return;
            }

            String res = RestClient.get("/clientes/buscar?q=" + term);
            Map<String, Object> body = RestClient.parseResponse(res);

            if (!Boolean.TRUE.equals(body.get("success"))) {
                menuClientes.hide();
                ultimosClientes = Collections.emptyList();
                return;
            }

            String dataJson = gson.toJson(body.get("data"));
            List<Map<String, Object>> clientes = gson.fromJson(
                    dataJson,
                    new TypeToken<List<Map<String, Object>>>(){}.getType()
            );

            ultimosClientes = (clientes != null) ? clientes : Collections.emptyList();

            if (ultimosClientes.isEmpty()) {
                menuClientes.hide();
                return;
            }

            List<MenuItem> items = new ArrayList<>();
            for (Map<String,Object> c : ultimosClientes) {
                String display = buildClienteDisplay(c);
                MenuItem mi = new MenuItem(display);
                mi.setOnAction(evt -> {
                    seleccionarCliente(c);
                    menuClientes.hide();
                });
                items.add(mi);
            }

            menuClientes.getItems().setAll(items);

            Window w = txtCliente.getScene().getWindow();
            if (!menuClientes.isShowing()) {
                menuClientes.show(
                        txtCliente,
                        w.getX() + txtCliente.localToScene(0,0).getX() + txtCliente.getScene().getX(),
                        w.getY() + txtCliente.localToScene(0,0).getY() + txtCliente.getScene().getY() + txtCliente.getHeight()
                );
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            menuClientes.hide();
        }
    }

    private String buildClienteDisplay(Map<String,Object> c) {
        Object idObj   = c.get("id");
        Object nomObj  = c.get("nombre");
        Object corObj  = c.get("correo");
        Object telObj  = c.get("telefono");

        String idStr  = (idObj  != null ? idObj.toString()  : "—");
        String nomStr = (nomObj != null ? nomObj.toString() : "(Sin nombre)");
        String corStr = (corObj != null ? corObj.toString() : "");
        String telStr = (telObj != null ? telObj.toString() : "");

        StringBuilder sb = new StringBuilder();
        sb.append(nomStr).append(" [ID:").append(idStr).append("]");
        if (!corStr.isBlank()) sb.append(" <").append(corStr).append(">");
        if (!telStr.isBlank()) sb.append(" (").append(telStr).append(")");
        return sb.toString();
    }

    private void seleccionarCliente(Map<String,Object> clienteSel) {
        Object idObj   = clienteSel.get("id");
        Object nomObj  = clienteSel.get("nombre");
        Object corObj  = clienteSel.get("correo");

        clienteSeleccionadoId     = parseIdToLong(idObj);
        clienteSeleccionadoNombre = (nomObj != null ? nomObj.toString() : null);
        clienteSeleccionadoCorreo = (corObj != null ? corObj.toString() : null);

        txtCliente.setText(clienteSeleccionadoNombre != null ? clienteSeleccionadoNombre : "");
        Mensaje.showSuccess("Cliente", "Cliente asignado a la venta.");
    }

    @FXML
    private void onBuscarCliente(ActionEvent event) {
        try {
            String criterio = txtCliente.getText() != null ? txtCliente.getText().trim() : "";
            if (criterio.isEmpty()) {
                Mensaje.showInfo("Cliente", "Digite nombre / correo / teléfono en la caja de texto.");
                txtCliente.requestFocus();
                return;
            }

            String res = RestClient.get("/clientes/buscar?q=" + criterio);
            Map<String, Object> body = RestClient.parseResponse(res);
            if (!Boolean.TRUE.equals(body.get("success"))) {
                Mensaje.showWarning("Cliente", "No se pudo consultar clientes.");
                return;
            }

            String dataJson = gson.toJson(body.get("data"));
            List<Map<String, Object>> clientes = gson.fromJson(
                    dataJson,
                    new TypeToken<List<Map<String, Object>>>(){}.getType()
            );

            if (clientes == null || clientes.isEmpty()) {
                Mensaje.showInfo("Cliente", "No se encontraron clientes.");
                return;
            }

            if (clientes.size() == 1) {
                seleccionarCliente(clientes.get(0));
                return;
            }

            LinkedHashMap<String, Map<String,Object>> opcionesMap = new LinkedHashMap<>();
            for (Map<String,Object> c : clientes) {
                opcionesMap.put(buildClienteDisplay(c), c);
            }

            List<String> opciones = new ArrayList<>(opcionesMap.keySet());
            ChoiceDialog<String> dlg = new ChoiceDialog<>(opciones.get(0), opciones);
            dlg.setTitle("Seleccionar cliente");
            dlg.setHeaderText("Clientes encontrados");
            dlg.setContentText("Seleccione:");

            Optional<String> elegidoOpt = dlg.showAndWait();
            if (elegidoOpt.isEmpty()) return;

            Map<String,Object> clienteSel = opcionesMap.get(elegidoOpt.get());
            seleccionarCliente(clienteSel);

        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Cliente", "Error buscando/seleccionando cliente:\n" + e.getMessage());
        }
    }

    // =================================================================================
    // PRODUCTOS: AGREGAR EN FACTURACIÓN (selector con tabla y filtro en vivo)
    // =================================================================================
    @FXML
    private void onAgregarProducto(ActionEvent event) {
        // 1. Necesitamos una orden activa
        if (ordenSeleccionada == null) {
            Mensaje.showWarning("Productos", "Primero seleccione una orden.");
            return;
        }

        try {
            // 2. cargar catálogo una sola vez (lazy load)
            if (catalogoProductos.isEmpty()) {
                cargarCatalogoProductosDesdeBackend();
            }

            // 3. abrir diálogo de selección con filtro
            ProductoCantidadSelection sel = mostrarDialogoSeleccionProducto();
            if (sel == null || sel.producto == null) {
                return; // canceló o no seleccionó
            }

            Long productoId = sel.producto.getId();
            int cantidadNueva = sel.cantidad;

            if (cantidadNueva <= 0) {
                Mensaje.showWarning("Cantidad", "La cantidad debe ser mayor a 0.");
                return;
            }

            // 4. revisamos si ya existe ese producto en la orden actual
            DetalleOrden detalleExistente = buscarDetalleExistenteEnOrden(productoId);

            boolean ok;
            if (detalleExistente != null) {
                // ya existía -> sumamos cantidades
                int cantidadTotal = detalleExistente.getCantidad() + cantidadNueva;
                ok = actualizarDetalleEnBackend(
                        ordenSeleccionada.getId(),
                        detalleExistente.getId(),
                        cantidadTotal
                );
            } else {
                // no existía -> creamos nueva línea
                ok = agregarDetalleEnBackend(
                        ordenSeleccionada.getId(),
                        productoId,
                        cantidadNueva
                );
            }

            if (!ok) {
                Mensaje.showError("Productos", "No se pudo aplicar el producto a la orden.");
                return;
            }

            // 5. refrescar tabla + totales
            cargarDetallesDeOrden(ordenSeleccionada.getId());
            onCalcularTotales(null);

            Mensaje.showSuccess("Productos", "Producto aplicado correctamente.");

        } catch (Exception e1) {
            e1.printStackTrace();
            Mensaje.showError("Productos", "Error agregando producto:\n" + e1.getMessage());
        }
    }

    /**
     * Busca en 'lineas' si ya hay un DetalleOrden para ese productoId.
     * Devuelve el DetalleOrden si lo encuentra; si no, null.
     */
    private DetalleOrden buscarDetalleExistenteEnOrden(Long productoId) {
        if (productoId == null) return null;
        for (DetalleOrden det : lineas) {
            Long pid = det.getProductoId() != null
                    ? det.getProductoId()
                    : (det.getProducto() != null ? det.getProducto().getId() : null);
            if (pid != null && pid.equals(productoId)) {
                return det;
            }
        }
        return null;
    }

    /**
     * Llama al backend para decir:
     * "En la orden X, el detalle Y ahora tiene cantidad = nuevaCantidad".
     *
     * Asumimos endpoint tipo:
     *   PUT /ordenes/{ordenId}/detalles/{detalleId}
     * Body:
     *   { "cantidad": <nuevaCantidad> }
     */
    private boolean actualizarDetalleEnBackend(Long ordenId, Long detalleId, int nuevaCantidad) {
        try {
            Map<String,Object> payload = new HashMap<>();
            payload.put("cantidad", nuevaCantidad);

            String res = RestClient.put(
                    "/ordenes/" + ordenId + "/detalles/" + detalleId,
                    payload
            );

            Map<String,Object> body = RestClient.parseResponse(res);
            return Boolean.TRUE.equals(body.get("success"));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Llama al backend para obtener TODOS los productos activos
     * y los guarda en catalogoProductos.
     *
     * IMPORTANTE:
     * Ajustá el endpoint si tu backend expone otro (por ej. "/productos/activos").
     * Asumimos: GET /productos -> { success:true, data:[ {id, nombre, nombreCorto, precio, ...}, ...] }
     */
    private void cargarCatalogoProductosDesdeBackend() {
        try {
            String res = RestClient.get("/productos");
            Map<String,Object> body = RestClient.parseResponse(res);

            if (!Boolean.TRUE.equals(body.get("success"))) {
                Mensaje.showWarning("Productos", "No se pudieron obtener los productos.");
                return;
            }

            String dataJson = gson.toJson(body.get("data"));
            List<Producto> prods = gson.fromJson(
                    dataJson,
                    new TypeToken<List<Producto>>(){}.getType()
            );

            catalogoProductos.setAll(
                    prods != null ? prods : Collections.emptyList()
            );
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Productos", "Error cargando catálogo:\n" + e.getMessage());
        }
    }

    /**
     * Muestra un Stage modal con:
     * - TextField para filtrar
     * - TableView con todos los productos (filtrados en vivo)
     * - TextField cantidad
     * - Botón Agregar
     *
     * Retorna la selección (producto + cantidad) o null si canceló.
     */
    private ProductoCantidadSelection mostrarDialogoSeleccionProducto() {
        // Tabla de productos filtrables
        TableView<Producto> tbl = new TableView<>();
        TableColumn<Producto, String> colNom = new TableColumn<>("Producto");
        TableColumn<Producto, String> colPrec = new TableColumn<>("Precio");

        colNom.setCellValueFactory(p ->
                new SimpleStringProperty(
                        p.getValue().getNombre() != null ? p.getValue().getNombre() : "(sin nombre)"
                )
        );
        colPrec.setCellValueFactory(p ->
                new SimpleStringProperty(
                        "₡" + (p.getValue().getPrecio() != null ? fmt(p.getValue().getPrecio()) : "0.00")
                )
        );

        colNom.setPrefWidth(220);
        colPrec.setPrefWidth(80);
        tbl.getColumns().addAll(colNom, colPrec);

        // Lista filtrada que ve la tabla
        ObservableList<Producto> filtrada = FXCollections.observableArrayList();
        filtrada.setAll(catalogoProductos);
        tbl.setItems(filtrada);

        // Campo de búsqueda
        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar (nombre, corto, id...)");

        // Campo cantidad
        TextField txtCantidad = new TextField("1");
        txtCantidad.setPrefWidth(60);
        txtCantidad.setAlignment(Pos.CENTER_RIGHT);

        Label lblCant = new Label("Cant.:");
        lblCant.setLabelFor(txtCantidad);

        // Botones aceptar / cancelar
        Button btnOK = new Button("Agregar");
        Button btnCancel = new Button("Cancelar");

        HBox topBox = new HBox(10, new Label("Filtrar:"), txtBuscar);
        topBox.setAlignment(Pos.CENTER_LEFT);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bottomBox = new HBox(10, lblCant, txtCantidad, spacer, btnCancel, btnOK);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, topBox, tbl, bottomBox);
        root.setPrefSize(400, 400);
        VBox.setVgrow(tbl, Priority.ALWAYS);
        root.setStyle("-fx-background-color: white; -fx-padding: 15;");

        Stage stage = new Stage();
        stage.setTitle("Seleccionar Producto");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(btnAgregarProducto.getScene().getWindow());
        stage.setScene(new Scene(root));

        final ProductoCantidadSelection[] resultHolder = new ProductoCantidadSelection[1];
        resultHolder[0] = null;

        // Filtrado en vivo
        txtBuscar.textProperty().addListener((obs, oldV, newV) -> {
            String filtro = (newV != null) ? newV.trim().toLowerCase() : "";
            filtrada.clear();
            if (filtro.isBlank()) {
                filtrada.addAll(catalogoProductos);
            } else {
                for (Producto p : catalogoProductos) {
                    String nombre      = p.getNombre() != null ? p.getNombre().toLowerCase() : "";
                    String nombreCorto = p.getNombreCorto() != null ? p.getNombreCorto().toLowerCase() : "";
                    String idStr       = p.getId() != null ? p.getId().toString() : "";
                    String precioStr   = p.getPrecio() != null ? p.getPrecio().toPlainString() : "";

                    if (nombre.contains(filtro)
                            || nombreCorto.contains(filtro)
                            || idStr.contains(filtro)
                            || precioStr.contains(filtro)) {
                        filtrada.add(p);
                    }
                }
            }
        });

        // Doble click en tabla = intentar aceptar directo
        tbl.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                Producto seleccionado = tbl.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    Integer cantVal = parseCantidadSeguro(txtCantidad.getText());
                    if (cantVal != null && cantVal > 0) {
                        resultHolder[0] = new ProductoCantidadSelection(seleccionado, cantVal);
                        stage.close();
                    }
                }
            }
        });

        // Botón cancelar
        btnCancel.setOnAction(ev -> {
            resultHolder[0] = null;
            stage.close();
        });

        // Botón OK
        btnOK.setOnAction(ev -> {
            Producto seleccionado = tbl.getSelectionModel().getSelectedItem();
            if (seleccionado == null) {
                Mensaje.showWarning("Producto", "Seleccione un producto.");
                return;
            }
            Integer cantVal = parseCantidadSeguro(txtCantidad.getText());
            if (cantVal == null || cantVal <= 0) {
                Mensaje.showWarning("Cantidad", "Cantidad inválida.");
                return;
            }

            resultHolder[0] = new ProductoCantidadSelection(seleccionado, cantVal);
            stage.close();
        });

        stage.showAndWait();
        return resultHolder[0];
    }

    private Integer parseCantidadSeguro(String txt) {
        try {
            return Integer.parseInt(txt.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Crea o suma línea de detalle en backend:
     * POST /ordenes/{id}/detalles con
     * { "ordenId":..., "productoId":..., "cantidad":... }
     */
    private boolean agregarDetalleEnBackend(Long ordenId, Long productoId, int cantidad) {
        try {
            Map<String,Object> payload = new HashMap<>();
            payload.put("ordenId", ordenId);
            payload.put("productoId", productoId);
            payload.put("cantidad", cantidad);

            String res = RestClient.post("/ordenes/" + ordenId + "/detalles", payload);
            Map<String,Object> body = RestClient.parseResponse(res);

            return Boolean.TRUE.equals(body.get("success"));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void onModificarCantidad(ActionEvent event) {
        Mensaje.showInfo("Cantidad", "Edición de cantidad se hará aquí después 😇 (por ahora usar Órdenes).");
    }

    @FXML
    private void onEliminarProducto(ActionEvent event) {
        Mensaje.showInfo("Eliminar", "Eliminar producto se hará aquí después 😇 (por ahora usar Órdenes).");
    }

    // =================================================================================
    // TOTALES Y VUELTO
    // =================================================================================
    @FXML
    private void onCalcularTotales(ActionEvent event) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetalleOrden d : lineas) {
            subtotal = subtotal.add(nz(d.getSubtotal()));
        }

        BigDecimal iv = BigDecimal.ZERO;
        if (chkImpuestoVentas.isSelected()) {
            iv = subtotal.multiply(IV_PORC);
        }

        BigDecimal serv = BigDecimal.ZERO;
        if (chkImpuestoServicio.isSelected()) {
            serv = subtotal.multiply(SERV_PORC);
        }

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
        BigDecimal total    = parseMonto(lblTotal.getText());
        BigDecimal efectivo = parseMonto(txtEfectivo.getText());
        BigDecimal tarjeta  = parseMonto(txtTarjeta.getText());
        BigDecimal pagado   = efectivo.add(tarjeta);
        BigDecimal vuelto   = pagado.subtract(total);
        if (vuelto.compareTo(BigDecimal.ZERO) < 0) vuelto = BigDecimal.ZERO;
        lblVuelto.setText("₡" + fmt(vuelto));
    }

    // =================================================================================
    // PAGO / FACTURAR
    // =================================================================================
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

        BigDecimal total    = parseMonto(lblTotal.getText());
        BigDecimal efectivo = parseMonto(txtEfectivo.getText());
        BigDecimal tarjeta  = parseMonto(txtTarjeta.getText());
        BigDecimal pagado   = efectivo.add(tarjeta);

        if (pagado.compareTo(total) < 0) {
            Mensaje.showWarning("Pago insuficiente", "El monto pagado es menor que el total.");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("ordenId", ordenSeleccionada.getId());

            // Cliente
            if (clienteSeleccionadoId != null) {
                payload.put("clienteId", clienteSeleccionadoId);
            }
            payload.put("clienteNombre",
                    (clienteSeleccionadoNombre != null && !clienteSeleccionadoNombre.isBlank())
                            ? clienteSeleccionadoNombre
                            : Optional.ofNullable(txtCliente.getText()).orElse("").trim()
            );
            if (clienteSeleccionadoCorreo != null && !clienteSeleccionadoCorreo.isBlank()) {
                payload.put("clienteCorreo", clienteSeleccionadoCorreo);
            }

            Map<String, Object> resumen = new HashMap<>();
            resumen.put("subtotal", parseMonto(lblSubtotal.getText()));
            resumen.put("impuestoVentas", parseMonto(lblImpuestoVentas.getText()));
            resumen.put("impuestoServicio", parseMonto(lblImpuestoServicio.getText()));
            BigDecimal desc = parseMonto(lblDescuento.getText().replace("-", ""));
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

            // 1. crear factura
            String resFactura = RestClient.post("/facturas", payload);
            Map<String, Object> r1 = RestClient.parseResponse(resFactura);
            if (!Boolean.TRUE.equals(r1.get("success"))) {
                Mensaje.showError("Error", "No se pudo crear la factura:\n" + r1.get("message"));
                return;
            }

            // 2. marcar orden como FACTURADA
            boolean facturada = false;
            try {
                String resFact = RestClient.post("/ordenes/" + ordenSeleccionada.getId() + "/facturar", null);
                Map<String, Object> rA = RestClient.parseResponse(resFact);
                facturada = Boolean.TRUE.equals(rA.get("success"));
            } catch (Exception ignored) { }

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
        Mensaje.showInfo(
                "Email",
                (clienteSeleccionadoCorreo != null && !clienteSeleccionadoCorreo.isBlank())
                        ? "Enviar comprobante a: " + clienteSeleccionadoCorreo
                        : "V1: envío por correo pendiente (requiere servicio en backend)."
        );
    }

    @FXML
    private void onCancelar(ActionEvent event) {
        limpiarPantalla();
    }

    // =================================================================================
    // HELPERS
    // =================================================================================
    /**
     * Convierte ids que vienen como 1, "1", 1.0, "1.0" -> Long
     */
    private Long parseIdToLong(Object idObj) {
        if (idObj == null) return null;
        String s = idObj.toString();
        if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ex) {
            try {
                Double d = Double.valueOf(idObj.toString());
                return d.longValue();
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    /**
     * Estructura pequeña para devolver selección desde el popup de productos.
     */
    private static class ProductoCantidadSelection {
        final Producto producto;
        final int cantidad;

        private ProductoCantidadSelection(Producto producto, int cantidad) {
            this.producto = producto;
            this.cantidad = cantidad;
        }
    }
}
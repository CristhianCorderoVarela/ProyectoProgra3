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
import cr.ac.una.restunaclient.util.I18n;
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
import java.math.RoundingMode;
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
    @FXML private Label lblCliente;
    @FXML private TextField txtCliente;
    @FXML private Button btnBuscarCliente;

    // Tabla productos
    @FXML private Label lblProductosOrden;
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
    @FXML private Label lblResumenVenta;
    @FXML private Label lblSubtotalLabel;
    @FXML private Label lblSubtotal;
    @FXML private CheckBox chkImpuestoVentas;
    @FXML private Label lblPorcentajeIV;
    @FXML private Label lblImpuestoVentas;
    @FXML private CheckBox chkImpuestoServicio;
    @FXML private Label lblPorcentajeIS;
    @FXML private Label lblImpuestoServicio;
    @FXML private Label lblDescuentoLabel;
    @FXML private TextField txtDescuento;
    @FXML private Label lblDescuento;
    @FXML private Label lblDescuentoMax;
    @FXML private Label lblTotalLabel;
    @FXML private Label lblTotal;

    // Pagos
    @FXML private Label lblMetodosPago;
    @FXML private Label lblEfectivoLabel;
    @FXML private TextField txtEfectivo;
    @FXML private Label lblTarjetaLabel;
    @FXML private TextField txtTarjeta;
    @FXML private Label lblVueltoLabel;
    @FXML private Label lblVuelto;

    // Botones finales
    @FXML private Button btnProcesarPago;
    @FXML private Button btnImprimir;
    @FXML private Button btnEnviarEmail;
    @FXML private Button btnCancelar;

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

    private final ObservableList<DetalleOrden> lineas = FXCollections.observableArrayList();
    private Orden ordenSeleccionada;

    // Cliente seleccionado
    private Long   clienteSeleccionadoId     = null;
    private String clienteSeleccionadoNombre = null;
    private String clienteSeleccionadoCorreo = null;

    // Autocomplete clientes
    private final ContextMenu menuClientes = new ContextMenu();
    private List<Map<String,Object>> ultimosClientes = Collections.emptyList();
    private final PauseTransition clienteSearchDelay = new PauseTransition(Duration.millis(250));

    // Catálogo productos
    private final ObservableList<Producto> catalogoProductos = FXCollections.observableArrayList();

    // Config negocio
    private static final BigDecimal IV_PORC       = new BigDecimal("0.13"); // 13%
    private static final BigDecimal SERV_PORC     = new BigDecimal("0.10"); // 10%
    private static final BigDecimal DESCUENTO_MAX = new BigDecimal("15");   // 15%

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        var user = AppContext.getInstance().getUsuarioLogueado();
        lblUsuario.setText(user != null ? I18n.get("facturacion.usuario") + user.getNombre() : I18n.get("facturacion.usuario") + "—");
        lblFechaHora.setText(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now()));

        // Config tabla
        colProducto.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getProducto() != null ? d.getValue().getProducto().getNombre() : I18n.get("facturacion.producto")
                )
        );
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(d -> new SimpleStringProperty("₡" + fmt(d.getValue().getPrecioUnitario())));
        colSubtotal.setCellValueFactory(d -> new SimpleStringProperty("₡" + fmt(d.getValue().getSubtotal())));
        colAcciones.setCellValueFactory(d -> new SimpleStringProperty(""));

        tblProductos.setItems(lineas);

        tblProductos.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> updateBotonesEdicion()
        );

        // Defaults
        chkImpuestoVentas.setSelected(true);
        chkImpuestoServicio.setSelected(true);
        txtDescuento.setText("0");
        txtEfectivo.setText("0.00");
        txtTarjeta.setText("0.00");

        // Recalcular en vivo
        txtDescuento.setOnKeyReleased(e -> onCalcularTotales(null));
        txtEfectivo.setOnKeyReleased(e -> onCalcularVuelto(null));
        txtTarjeta.setOnKeyReleased(e -> onCalcularVuelto(null));
        chkImpuestoVentas.setOnAction(e -> onCalcularTotales(null));
        chkImpuestoServicio.setOnAction(e -> onCalcularTotales(null));

        // Autocomplete cliente
        clienteSearchDelay.setOnFinished(e -> buscarYMostrarSugerencias(txtCliente.getText().trim()));
        txtCliente.focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) {
                menuClientes.hide();
            }
        });

        limpiarPantalla();
        actualizarTextos();
    }

    // ========== ESTADO / RESET ==========
    private void limpiarPantalla() {
        lblOrdenInfo.setText(I18n.get("facturacion.ordenNoAsignada"));
        lblMesaInfo.setText(I18n.get("facturacion.mesaNoAsignada"));

        txtCliente.clear();
        clienteSeleccionadoId = null;
        clienteSeleccionadoNombre = null;
        clienteSeleccionadoCorreo = null;
        menuClientes.hide();
        ultimosClientes = Collections.emptyList();

        lineas.clear();
        lblSubtotal.setText("₡0.00");
        lblImpuestoVentas.setText("₡0.00");
        lblImpuestoServicio.setText("₡0.00");
        lblDescuento.setText("-₡0.00");
        lblTotal.setText("₡0.00");
        lblVuelto.setText("₡0.00");

        txtDescuento.setText("0");
        txtEfectivo.setText("0.00");
        txtTarjeta.setText("0.00");
        chkImpuestoVentas.setSelected(true);
        chkImpuestoServicio.setSelected(true);

        ordenSeleccionada = null;
        updateBotonesEdicion();
    }

    private void updateBotonesEdicion() {
        boolean hayOrden = (ordenSeleccionada != null);
        boolean haySeleccion = (tblProductos.getSelectionModel().getSelectedItem() != null);

        btnAgregarProducto.setDisable(!hayOrden);
        btnModificarCantidad.setDisable(!hayOrden || !haySeleccion);
        btnEliminarProducto.setDisable(!hayOrden || !haySeleccion);
    }

    private static String fmt(BigDecimal n) {
        if (n == null) {
            return "0.00";
        }
        return String.format(java.util.Locale.US, "%,.2f", n);
    }

    private static BigDecimal nz(BigDecimal n) {
        return n == null ? BigDecimal.ZERO : n;
    }

    private static BigDecimal parseMonto(String txt) {
        try {
            if (txt == null || txt.trim().isEmpty()) {
                return BigDecimal.ZERO;
            }

            String limpio = txt.trim()
                    .replace("₡", "")
                    .replace("€", "")
                    .replace("$", "")
                    .replace(" ", "")
                    .replace("\u00A0", "")
                    .trim();

            if (limpio.isEmpty()) {
                return BigDecimal.ZERO;
            }

            int ultimaComa = limpio.lastIndexOf(',');
            int ultimoPunto = limpio.lastIndexOf('.');

            if (ultimaComa > -1 && ultimoPunto == -1) {
                if (limpio.indexOf(',') != ultimaComa) {
                    limpio = limpio.substring(0, ultimaComa).replace(",", "") + "." + limpio.substring(ultimaComa + 1);
                } else {
                    limpio = limpio.replace(",", ".");
                }
            } else if (ultimoPunto > -1 && ultimaComa == -1) {
                if (limpio.indexOf('.') != ultimoPunto) {
                    limpio = limpio.substring(0, ultimoPunto).replace(".", "") + "." + limpio.substring(ultimoPunto + 1);
                }
            } else if (ultimaComa > -1 && ultimoPunto > -1) {
                if (ultimaComa > ultimoPunto) {
                    limpio = limpio.replace(".", "").replace(",", ".");
                } else {
                    limpio = limpio.replace(",", "");
                }
            }

            BigDecimal resultado = new BigDecimal(limpio);
            return resultado.setScale(2, RoundingMode.HALF_UP);

        } catch (NumberFormatException e) {
            System.err.println("❌ Error parseando monto: '" + txt + "' - " + e.getMessage());
            e.printStackTrace();
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal parsePct(String txt) {
        try {
            if (txt == null || txt.trim().isEmpty()) {
                return BigDecimal.ZERO;
            }

            String limpio = txt.replace("%", "").trim();

            if (limpio.isEmpty()) {
                return BigDecimal.ZERO;
            }

            BigDecimal resultado = new BigDecimal(limpio);
            return resultado.setScale(2, RoundingMode.HALF_UP);

        } catch (NumberFormatException e) {
            System.err.println("❌ Error parseando porcentaje: '" + txt + "' - " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // ========== NAVEGACIÓN ==========
    @FXML
    private void onVolver(ActionEvent event) {
        FlowController.getInstance().goToView("MenuPrincipal", "RestUNA - Menú", 1000, 560);
    }

    // ========== ORDENES ==========
    @FXML
    private void onSeleccionarOrden(ActionEvent event) {
        try {
            String res = RestClient.get("/ordenes/activas");
            Map<String, Object> body = RestClient.parseResponse(res);
            if (!Boolean.TRUE.equals(body.get("success"))) {
                Mensaje.showWarning(I18n.get("facturacion.titulo"), I18n.get("facturacion.errorCargarOrdenes"));
                return;
            }

            String data = gson.toJson(body.get("data"));
            List<Orden> ordenes = gson.fromJson(data, new TypeToken<List<Orden>>(){}.getType());
            if (ordenes == null || ordenes.isEmpty()) {
                Mensaje.showInfo(I18n.get("facturacion.titulo"), I18n.get("facturacion.noOrdenesAbiertas"));
                return;
            }

            LinkedHashMap<String, Orden> map = new LinkedHashMap<>();
            for (Orden o : ordenes) {
                map.put(buildDisplay(o), o);
            }

            List<String> opciones = new ArrayList<>(map.keySet());
            ChoiceDialog<String> dlg = new ChoiceDialog<>(opciones.get(0), opciones);
            dlg.setTitle(I18n.get("facturacion.seleccionarOrden"));
            dlg.setHeaderText(I18n.get("facturacion.eligaOrden"));
            dlg.setContentText(I18n.get("facturacion.ordenes"));

            Optional<String> opt = dlg.showAndWait();
            if (opt.isEmpty()) return;

            this.ordenSeleccionada = map.get(opt.get());
            cargarDetallesDeOrden(this.ordenSeleccionada.getId());

            lblOrdenInfo.setText(I18n.get("facturacion.ordenNum") + ordenSeleccionada.getId());
            String mesaTxt = (ordenSeleccionada.getMesa() != null)
                    ? (ordenSeleccionada.getMesa().getIdentificador() != null
                            && !ordenSeleccionada.getMesa().getIdentificador().isBlank()
                            ? ordenSeleccionada.getMesa().getIdentificador()
                            : String.valueOf(ordenSeleccionada.getMesa().getId()))
                    : I18n.get("facturacion.barra");
            lblMesaInfo.setText(I18n.get("facturacion.mesa") + mesaTxt);

            onCalcularTotales(null);
            updateBotonesEdicion();

        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError(I18n.get("app.error"), I18n.get("facturacion.errorCargarListaOrdenes"));
        }
    }

    private String buildDisplay(Orden o) {
        String ubic = (o.getMesa() != null
                ? I18n.get("facturacion.mesa") + (o.getMesa().getIdentificador() != null
                        && !o.getMesa().getIdentificador().isBlank()
                        ? o.getMesa().getIdentificador()
                        : (o.getMesa().getId() != null ? o.getMesa().getId() : "—"))
                : I18n.get("facturacion.barra"));

        String fecha = (o.getFechaHora() != null)
                ? o.getFechaHora().toString().replace('T', ' ')
                : "";

        return I18n.get("facturacion.ordenNum") + o.getId() + " · " + ubic + (fecha.isEmpty() ? "" : " · " + fecha);
    }

    private void cargarDetallesDeOrden(Long ordenId) {
        try {
            String res = RestClient.get("/ordenes/" + ordenId + "/detalles");
            Map<String, Object> body = RestClient.parseResponse(res);
            if (!Boolean.TRUE.equals(body.get("success"))) {
                Mensaje.showWarning(I18n.get("facturacion.titulo"), I18n.get("facturacion.errorCargarDetalles"));
                return;
            }

            String data = gson.toJson(body.get("data"));
            List<DetalleOrden> detalles = gson.fromJson(data, new TypeToken<List<DetalleOrden>>() {}.getType());

            lineas.setAll(detalles != null ? detalles : Collections.emptyList());
            onCalcularTotales(null);

        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError(I18n.get("app.error"), I18n.get("facturacion.errorCargarDetallesOrden"));
        }
    }

    // ========== CLIENTE ==========
    @FXML
    private void onClienteKeyTyped(KeyEvent e) {
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
        String nomStr = (nomObj != null ? nomObj.toString() : I18n.get("facturacion.sinNombre"));
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
        Mensaje.showSuccess(I18n.get("facturacion.cliente"), I18n.get("facturacion.clienteAsignado"));
    }

    @FXML
    private void onBuscarCliente(ActionEvent event) {
        try {
            String criterio = txtCliente.getText() != null ? txtCliente.getText().trim() : "";
            if (criterio.isEmpty()) {
                Mensaje.showInfo(I18n.get("facturacion.cliente"), I18n.get("facturacion.digiteCriterio"));
                txtCliente.requestFocus();
                return;
            }

            String res = RestClient.get("/clientes/buscar?q=" + criterio);
            Map<String, Object> body = RestClient.parseResponse(res);
            if (!Boolean.TRUE.equals(body.get("success"))) {
                Mensaje.showWarning(I18n.get("facturacion.cliente"), I18n.get("facturacion.errorConsultarClientes"));
                return;
            }

            String dataJson = gson.toJson(body.get("data"));
            List<Map<String, Object>> clientes = gson.fromJson(
                    dataJson,
                    new TypeToken<List<Map<String, Object>>>(){}.getType()
            );

            if (clientes == null || clientes.isEmpty()) {
                Mensaje.showInfo(I18n.get("facturacion.cliente"), I18n.get("facturacion.clientesNoEncontrados"));
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
            dlg.setTitle(I18n.get("facturacion.seleccionarCliente"));
            dlg.setHeaderText(I18n.get("facturacion.clientesEncontrados"));
            dlg.setContentText(I18n.get("facturacion.seleccione"));

            Optional<String> elegidoOpt = dlg.showAndWait();
            if (elegidoOpt.isEmpty()) return;

            Map<String,Object> clienteSel = opcionesMap.get(elegidoOpt.get());
            seleccionarCliente(clienteSel);

        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError(I18n.get("facturacion.cliente"), I18n.get("facturacion.errorBuscarCliente") + e.getMessage());
        }
    }

    // ========== PRODUCTOS ==========
    @FXML
    private void onAgregarProducto(ActionEvent event) {
        if (ordenSeleccionada == null) {
            Mensaje.showWarning(I18n.get("facturacion.productos"), I18n.get("facturacion.seleccioneOrdenPrimero"));
            return;
        }

        try {
            if (catalogoProductos.isEmpty()) {
                cargarCatalogoProductosDesdeBackend();
            }

            ProductoCantidadSelection sel = mostrarDialogoSeleccionProducto();
            if (sel == null || sel.producto == null) {
                return;
            }

            Long productoId = sel.producto.getId();
            int cantidadNueva = sel.cantidad;

            if (cantidadNueva <= 0) {
                Mensaje.showWarning(I18n.get("facturacion.cantidad"), I18n.get("facturacion.cantidadMayorCero"));
                return;
            }

            DetalleOrden detalleExistente = buscarDetalleExistenteEnOrden(productoId);

            boolean ok;
            if (detalleExistente != null) {
                int cantidadTotal = detalleExistente.getCantidad() + cantidadNueva;
                ok = actualizarDetalleEnBackend(
                        ordenSeleccionada.getId(),
                        detalleExistente.getId(),
                        cantidadTotal
                );
            } else {
                ok = agregarDetalleEnBackend(
                        ordenSeleccionada.getId(),
                        productoId,
                        cantidadNueva
                );
            }

            if (!ok) {
                Mensaje.showError(I18n.get("facturacion.productos"), I18n.get("facturacion.errorAplicarProducto"));
                return;
            }

            cargarDetallesDeOrden(ordenSeleccionada.getId());
            onCalcularTotales(null);

            Mensaje.showSuccess(I18n.get("facturacion.productos"), I18n.get("facturacion.productoAplicado"));

        } catch (Exception e1) {
            e1.printStackTrace();
            Mensaje.showError(I18n.get("facturacion.productos"), I18n.get("facturacion.errorAgregarProducto") + e1.getMessage());
        }
    }

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

    private void cargarCatalogoProductosDesdeBackend() {
        try {
            String res = RestClient.get("/productos");
            Map<String,Object> body = RestClient.parseResponse(res);

            if (!Boolean.TRUE.equals(body.get("success"))) {
                Mensaje.showWarning(I18n.get("facturacion.productos"), I18n.get("facturacion.errorObtenerProductos"));
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
            Mensaje.showError(I18n.get("facturacion.productos"), I18n.get("facturacion.errorCargarCatalogo") + e.getMessage());
        }
    }

    private ProductoCantidadSelection mostrarDialogoSeleccionProducto() {
        TableView<Producto> tbl = new TableView<>();
        TableColumn<Producto, String> colNom = new TableColumn<>(I18n.get("facturacion.producto"));
        TableColumn<Producto, String> colPrec = new TableColumn<>(I18n.get("facturacion.precio"));

        colNom.setCellValueFactory(p ->
                new SimpleStringProperty(
                        p.getValue().getNombre() != null ? p.getValue().getNombre() : I18n.get("facturacion.sinNombre")
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

        ObservableList<Producto> filtrada = FXCollections.observableArrayList();
        filtrada.setAll(catalogoProductos);
        tbl.setItems(filtrada);

        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText(I18n.get("facturacion.buscarProducto"));

        TextField txtCantidad = new TextField("1");
        txtCantidad.setPrefWidth(60);
        txtCantidad.setAlignment(Pos.CENTER_RIGHT);

        Label lblCant = new Label(I18n.get("facturacion.cantidadAbrev"));
        lblCant.setLabelFor(txtCantidad);

        Button btnOK = new Button(I18n.get("facturacion.agregar"));
        Button btnCancel = new Button(I18n.get("app.cancelar"));

        HBox topBox = new HBox(10, new Label(I18n.get("facturacion.filtrar")), txtBuscar);
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
        stage.setTitle(I18n.get("facturacion.seleccionarProducto"));
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(btnAgregarProducto.getScene().getWindow());
        stage.setScene(new Scene(root));

        final ProductoCantidadSelection[] resultHolder = new ProductoCantidadSelection[1];
        resultHolder[0] = null;

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

        btnCancel.setOnAction(ev -> {
            resultHolder[0] = null;
            stage.close();
        });

        btnOK.setOnAction(ev -> {
            Producto seleccionado = tbl.getSelectionModel().getSelectedItem();
            if (seleccionado == null) {
                Mensaje.showWarning(I18n.get("facturacion.producto"), I18n.get("facturacion.seleccioneProducto"));
                return;
            }
            Integer cantVal = parseCantidadSeguro(txtCantidad.getText());
            if (cantVal == null || cantVal <= 0) {
                Mensaje.showWarning(I18n.get("facturacion.cantidad"), I18n.get("facturacion.cantidadInvalida"));
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
        DetalleOrden seleccionado = tblProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Mensaje.showWarning(I18n.get("facturacion.modificar"), I18n.get("facturacion.seleccioneProductoTabla"));
            return;
        }

        TextInputDialog dlg = new TextInputDialog(String.valueOf(seleccionado.getCantidad()));
        dlg.setTitle(I18n.get("facturacion.modificarCantidad"));
        dlg.setHeaderText(I18n.get("facturacion.producto") + ": " + 
            (seleccionado.getProducto() != null ? seleccionado.getProducto().getNombre() : ""));
        dlg.setContentText(I18n.get("facturacion.nuevaCantidad"));

        Optional<String> resultado = dlg.showAndWait();
        if (resultado.isEmpty()) return;

        try {
            int nuevaCantidad = Integer.parseInt(resultado.get().trim());
            if (nuevaCantidad <= 0) {
                Mensaje.showWarning(I18n.get("facturacion.cantidad"), I18n.get("facturacion.cantidadMayorCero"));
                return;
            }

            boolean ok = actualizarDetalleEnBackend(
                ordenSeleccionada.getId(),
                seleccionado.getId(),
                nuevaCantidad
            );

            if (ok) {
                cargarDetallesDeOrden(ordenSeleccionada.getId());
                onCalcularTotales(null);
                Mensaje.showSuccess(I18n.get("facturacion.modificar"), I18n.get("facturacion.cantidadActualizada"));
            } else {
                Mensaje.showError(I18n.get("app.error"), I18n.get("facturacion.errorActualizarCantidad"));
            }

        } catch (NumberFormatException e) {
            Mensaje.showWarning(I18n.get("facturacion.cantidad"), I18n.get("facturacion.ingreseNumeroValido"));
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError(I18n.get("app.error"), I18n.get("facturacion.errorModificarCantidad") + e.getMessage());
        }
    }

    @FXML
    private void onEliminarProducto(ActionEvent event) {
        DetalleOrden seleccionado = tblProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Mensaje.showWarning(I18n.get("facturacion.eliminar"), I18n.get("facturacion.seleccioneProductoTabla"));
            return;
        }

        String nombreProd = seleccionado.getProducto() != null ? 
            seleccionado.getProducto().getNombre() : I18n.get("facturacion.esteProducto");

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle(I18n.get("facturacion.confirmarEliminacion"));
        confirmacion.setHeaderText(I18n.get("facturacion.eliminarProductoPregunta") + nombreProd + "?");
        confirmacion.setContentText(I18n.get("facturacion.accionNoReversible"));

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {
            return;
        }

        try {
            String res = RestClient.delete(
                "/ordenes/" + ordenSeleccionada.getId() + "/detalles/" + seleccionado.getId()
            );

            Map<String,Object> body = RestClient.parseResponse(res);
            if (Boolean.TRUE.equals(body.get("success"))) {
                cargarDetallesDeOrden(ordenSeleccionada.getId());
                onCalcularTotales(null);
                Mensaje.showSuccess(I18n.get("facturacion.eliminar"), I18n.get("facturacion.productoEliminado"));
            } else {
                Mensaje.showError(I18n.get("app.error"), I18n.get("facturacion.errorEliminarProducto"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError(I18n.get("app.error"), I18n.get("facturacion.errorEliminandoProducto") + e.getMessage());
        }
    }

    // ========== TOTALES Y VUELTO ==========
    @FXML
    private void onCalcularTotales(ActionEvent event) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetalleOrden d : lineas) {
            subtotal = subtotal.add(nz(d.getSubtotal()));
        }
        System.out.println("🔢 Subtotal calculado: " + subtotal);

        BigDecimal iv = BigDecimal.ZERO;
        if (chkImpuestoVentas.isSelected()) {
            iv = subtotal.multiply(IV_PORC).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal serv = BigDecimal.ZERO;
        if (chkImpuestoServicio.isSelected()) {
            serv = subtotal.multiply(SERV_PORC).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal descPct = parsePct(txtDescuento.getText());
        System.out.println("🔢 Descuento %: " + descPct);

        if (descPct.compareTo(DESCUENTO_MAX) > 0) {
            descPct = DESCUENTO_MAX;
            txtDescuento.setText(DESCUENTO_MAX.toPlainString());
        } else if (descPct.compareTo(BigDecimal.ZERO) < 0) {
            descPct = BigDecimal.ZERO;
            txtDescuento.setText("0");
        }

        BigDecimal baseDescuento = subtotal.add(iv).add(serv);
        BigDecimal descuentoMonto = baseDescuento
                .multiply(descPct)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal total = baseDescuento.subtract(descuentoMonto);

        System.out.println("🔢 Total calculado: " + total);

        lblSubtotal.setText("₡" + fmt(subtotal));
        lblImpuestoVentas.setText("₡" + fmt(iv));
        lblImpuestoServicio.setText("₡" + fmt(serv));
        lblDescuento.setText("-₡" + fmt(descuentoMonto));
        lblTotal.setText("₡" + fmt(total));

        onCalcularVuelto(null);
    }

    @FXML
    private void onCalcularVuelto(ActionEvent event) {
        String totalText = lblTotal.getText();
        String efectivoText = txtEfectivo.getText();
        String tarjetaText = txtTarjeta.getText();

        System.out.println("💰 Calculando vuelto:");
        System.out.println("   Total (texto): '" + totalText + "'");
        System.out.println("   Efectivo (texto): '" + efectivoText + "'");
        System.out.println("   Tarjeta (texto): '" + tarjetaText + "'");

        BigDecimal total = parseMonto(totalText);
        BigDecimal efectivo = parseMonto(efectivoText);
        BigDecimal tarjeta = parseMonto(tarjetaText);

        System.out.println("   Total (parseado): " + total);
        System.out.println("   Efectivo (parseado): " + efectivo);
        System.out.println("   Tarjeta (parseado): " + tarjeta);

        BigDecimal pagado = efectivo.add(tarjeta);

        System.out.println("   Pagado total: " + pagado);

        BigDecimal vuelto = pagado.subtract(total);

        System.out.println("   Vuelto calculado: " + vuelto);

        if (vuelto.compareTo(BigDecimal.ZERO) < 0) {
            vuelto = BigDecimal.ZERO;
        }

        String vueltoFormateado = "₡" + fmt(vuelto);
        System.out.println("   Vuelto (formateado): '" + vueltoFormateado + "'");

        lblVuelto.setText(vueltoFormateado);
    }

    // ========== PAGO / FACTURAR ==========
    @FXML
    private void onProcesarPago(ActionEvent event) {
        if (ordenSeleccionada == null) {
            Mensaje.showWarning(I18n.get("facturacion.titulo"), I18n.get("facturacion.seleccioneOrdenPrimero"));
            return;
        }
        if (lineas.isEmpty()) {
            Mensaje.showWarning(I18n.get("facturacion.titulo"), I18n.get("facturacion.ordenSinProductos"));
            return;
        }

        String totalText = lblTotal.getText();
        String efectivoText = txtEfectivo.getText();
        String tarjetaText = txtTarjeta.getText();

        System.out.println("\n💳 PROCESANDO PAGO:");
        System.out.println("   Total: '" + totalText + "'");
        System.out.println("   Efectivo: '" + efectivoText + "'");
        System.out.println("   Tarjeta: '" + tarjetaText + "'");

        BigDecimal total = parseMonto(totalText);
        BigDecimal efectivo = parseMonto(efectivoText);
        BigDecimal tarjeta = parseMonto(tarjetaText);
        BigDecimal pagado = efectivo.add(tarjeta);

        System.out.println("   Total (BD): " + total);
        System.out.println("   Efectivo (BD): " + efectivo);
        System.out.println("   Tarjeta (BD): " + tarjeta);
        System.out.println("   Pagado (BD): " + pagado);

        if (pagado.compareTo(total) < 0) {
            BigDecimal faltante = total.subtract(pagado);
            System.out.println("   ❌ PAGO INSUFICIENTE. Falta: " + faltante);
            Mensaje.showWarning(I18n.get("facturacion.pagoInsuficiente"),
                    String.format(I18n.get("facturacion.faltaDetalle"),
                            fmt(faltante), fmt(total), fmt(pagado)));
            return;
        }

        System.out.println("   ✅ Pago válido, procediendo...");

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("ordenId", ordenSeleccionada.getId());

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

            BigDecimal descuentoPorcentaje = parsePct(txtDescuento.getText());
            resumen.put("descuentoPorcentaje", descuentoPorcentaje);

            BigDecimal descuentoMonto = parseMonto(lblDescuento.getText());
            resumen.put("descuentoMonto", descuentoMonto);

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
                it.put("nombre", d.getProducto() != null ? d.getProducto().getNombre() : I18n.get("facturacion.producto"));
                it.put("cantidad", d.getCantidad());
                it.put("precioUnitario", d.getPrecioUnitario());
                items.add(it);
            }
            payload.put("items", items);

            System.out.println("📤 Enviando payload al servidor...");
            System.out.println(new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(payload));

            String resFactura = RestClient.post("/facturas", payload);
            Map<String, Object> r1 = RestClient.parseResponse(resFactura);
            if (!Boolean.TRUE.equals(r1.get("success"))) {
                String errorMsg = r1.get("message") != null ? r1.get("message").toString() : I18n.get("facturacion.errorDesconocido");
                Mensaje.showError(I18n.get("app.error"), I18n.get("facturacion.errorCrearFactura") + errorMsg);
                return;
            }

            boolean facturada = false;
            try {
                String resFact = RestClient.post("/ordenes/" + ordenSeleccionada.getId() + "/facturar", null);
                Map<String, Object> rA = RestClient.parseResponse(resFact);
                facturada = Boolean.TRUE.equals(rA.get("success"));
            } catch (Exception ignored) {
            }

            if (!facturada) {
                Map<String, Object> upd = Map.of("estado", "FACTURADA");
                try {
                    String resPut = RestClient.put("/ordenes/" + ordenSeleccionada.getId(), upd);
                    Map<String, Object> rB = RestClient.parseResponse(resPut);
                    facturada = Boolean.TRUE.equals(rB.get("success"));
                } catch (Exception ignored) {
                }
            }

            Mensaje.showSuccess(I18n.get("app.exito"), facturada
                    ? I18n.get("facturacion.facturaRegistradaExito")
                    : I18n.get("facturacion.facturaRegistradaParcial"));

            limpiarPantalla();

        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError(I18n.get("app.error"), I18n.get("facturacion.errorProcesarPago") + e.getMessage());
        }
    }

    @FXML
    private void onImprimir(ActionEvent event) {
        Mensaje.showInfo(I18n.get("facturacion.imprimir"), I18n.get("facturacion.imprimirInfo"));
    }

    @FXML
    private void onEnviarEmail(ActionEvent event) {
        Mensaje.showInfo(
                I18n.get("facturacion.email"),
                (clienteSeleccionadoCorreo != null && !clienteSeleccionadoCorreo.isBlank())
                        ? I18n.get("facturacion.enviarComprobanteA") + clienteSeleccionadoCorreo
                        : I18n.get("facturacion.emailPendiente")
        );
    }

    @FXML
    private void onCancelar(ActionEvent event) {
        limpiarPantalla();
        Mensaje.showInfo(I18n.get("facturacion.cancelado"), I18n.get("facturacion.ventaCancelada"));
    }

    // ========== HELPERS ==========
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
     * Actualiza todos los textos de la interfaz según el idioma actual
     */
    private void actualizarTextos() {
        boolean esEspanol = I18n.isSpanish();

        // Header
        lblTitle.setText(I18n.get("facturacion.titulo"));
        btnVolver.setText(I18n.get("facturacion.volver"));

        // Orden
        btnSeleccionarOrden.setText(I18n.get("facturacion.seleccionarOrden"));

        // Cliente
        if (lblCliente != null) {
            lblCliente.setText(I18n.get("facturacion.cliente") + ":");
        }
        txtCliente.setPromptText(I18n.get("facturacion.nombreClienteOpcional"));
        btnBuscarCliente.setText(I18n.get("app.buscar"));

        // Productos
        if (lblProductosOrden != null) {
            lblProductosOrden.setText(I18n.get("facturacion.productosOrden"));
        }
        btnAgregarProducto.setText(I18n.get("facturacion.agregarProducto"));
        colProducto.setText(I18n.get("facturacion.producto"));
        colCantidad.setText(I18n.get("facturacion.cantidadAbrev"));
        colPrecio.setText(I18n.get("facturacion.precioUnit"));
        colSubtotal.setText(I18n.get("facturacion.subtotal"));

        btnModificarCantidad.setText(I18n.get("facturacion.modificarCantidad"));
        btnEliminarProducto.setText(I18n.get("facturacion.eliminar"));

        // Resumen
        if (lblResumenVenta != null) {
            lblResumenVenta.setText(I18n.get("facturacion.resumenVenta"));
        }
        if (lblSubtotalLabel != null) {
            lblSubtotalLabel.setText(I18n.get("facturacion.subtotal") + ":");
        }
        chkImpuestoVentas.setText(I18n.get("facturacion.impuestoVentas"));
        chkImpuestoServicio.setText(I18n.get("facturacion.servicio"));
        if (lblDescuentoLabel != null) {
            lblDescuentoLabel.setText(I18n.get("facturacion.descuentoAbrev") + ":");
        }
        lblDescuentoMax.setText(I18n.get("facturacion.descuentoMaximo") + " 15%");
        if (lblTotalLabel != null) {
            lblTotalLabel.setText(I18n.get("facturacion.total").toUpperCase() + ":");
        }

        // Métodos de pago
        if (lblMetodosPago != null) {
            lblMetodosPago.setText(I18n.get("facturacion.metodosPago"));
        }
        if (lblEfectivoLabel != null) {
            lblEfectivoLabel.setText(I18n.get("facturacion.efectivo") + ":");
        }
        if (lblTarjetaLabel != null) {
            lblTarjetaLabel.setText(I18n.get("facturacion.tarjeta") + ":");
        }
        if (lblVueltoLabel != null) {
            lblVueltoLabel.setText(I18n.get("facturacion.vuelto") + ":");
        }

        // Botones finales
        btnProcesarPago.setText(I18n.get("facturacion.procesarPago"));
        btnImprimir.setText(I18n.get("facturacion.imprimir"));
        btnEnviarEmail.setText(I18n.get("facturacion.enviarEmail"));
        btnCancelar.setText(I18n.get("facturacion.cancelarVenta"));

        // Placeholder de la tabla
        tblProductos.setPlaceholder(new Label(I18n.get("facturacion.noProductos")));
    }

    private static class ProductoCantidadSelection {
        final Producto producto;
        final int cantidad;

        private ProductoCantidadSelection(Producto producto, int cantidad) {
            this.producto = producto;
            this.cantidad = cantidad;
        }
    }
}
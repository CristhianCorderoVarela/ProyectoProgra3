package cr.ac.una.restunaclient.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cr.ac.una.restunaclient.model.*;
import cr.ac.una.restunaclient.service.RestClient;
import cr.ac.una.restunaclient.util.AppContext;
import cr.ac.una.restunaclient.util.FlowController;
import cr.ac.una.restunaclient.util.I18n;
import cr.ac.una.restunaclient.util.Mensaje;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controlador para gestión de órdenes
 * Permite crear/editar órdenes, agregar productos y gestionar detalles
 */
public class OrdenesController implements Initializable {

    // ==================== HEADER ====================
    @FXML private Label lblTitle;
    @FXML private Label lblUsuario;
    @FXML private Button btnVolver;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelarOrden;

    // === CABECERA: tipo de orden / ubicación ===
    @FXML private ToggleButton tgMesa;
    @FXML private ToggleButton tgBarra;
    @FXML private ToggleGroup tgTipoOrden;
    @FXML private ComboBox<Salon> cmbSalonSelect;
    @FXML private ComboBox<Mesa> cmbMesaSelect;
    @FXML private Button btnElegirMesa;
    @FXML private Label lblEstadoOrden;

    // ==================== INFO ORDEN ====================
    @FXML private Label lblMesaInfo;
    @FXML private Label lblSalonInfo;
    @FXML private Label lblFechaHora;
    @FXML private TextArea txtObservaciones;

    // ==================== SELECTOR DE PRODUCTOS ====================
    @FXML private TextField txtBuscarProducto;
    @FXML private ComboBox<GrupoProducto> cmbGrupos;
    @FXML private FlowPane flowProductos;

    // ==================== TABLA DE DETALLES ====================
    @FXML private TableView<DetalleOrden> tblDetalles;
    @FXML private TableColumn<DetalleOrden, String> colProducto;
    @FXML private TableColumn<DetalleOrden, Integer> colCantidad;
    @FXML private TableColumn<DetalleOrden, String> colPrecio;
    @FXML private TableColumn<DetalleOrden, String> colSubtotal;

    // ==================== TOTALES ====================
    @FXML private Label lblTotal;

    // ==================== VARIABLES DE ESTADO ====================
    private Orden ordenActual;
    private Mesa mesaSeleccionada;
    private Salon salonSeleccionado;
    private ObservableList<DetalleOrden> detallesOrden;
    private List<GrupoProducto> listaGrupos;
    private List<Producto> listaProductos;
    private boolean modoEdicion = false;

    // Listas que alimentan los ComboBox
    private final ObservableList<Salon> listaSalonesDisponibles = FXCollections.observableArrayList();
    private final ObservableList<Mesa> listaMesasDisponibles = FXCollections.observableArrayList();

    // Tipo de orden actual (Mesa o Barra)
    private enum TipoOrden { MESA, BARRA }
    private TipoOrden tipoOrdenActual = TipoOrden.MESA;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Recuperar lo que dejó la vista anterior (VistaSalones, etc.)
        mesaSeleccionada = (Mesa) AppContext.getInstance().get("mesaSeleccionada");
        salonSeleccionado = (Salon) AppContext.getInstance().get("salonSeleccionado");

        // Si no viene mesa => asumimos barra
        tipoOrdenActual = (mesaSeleccionada == null) ? TipoOrden.BARRA : TipoOrden.MESA;

        configurarTabla();
        configurarCombosProductos();
        configurarCabeceraTipoOrden();

        // 1. Cargar salones desde el backend
        cargarSalones();

        // 2. Seleccionar salón / mesa inicial según lo que venía del contexto
        inicializarSeleccionMesa();

        // 3. Llenar panel izquierdo (productos)
        cargarGrupos();
        cargarProductos();

        // 4. Si la mesa está ocupada, cargar la orden existente, si no crear una nueva
        cargarOrdenExistente();

        // 5. Refrescar labels
        actualizarCabeceraVisual();
        actualizarInfoOrden();
        actualizarTextos();

        // Búsqueda dinámica de productos
        txtBuscarProducto.textProperty().addListener((obs, old, val) -> filtrarProductos(val));
    }

    // ==================== CONFIG TABLA DETALLES ====================

    private void configurarTabla() {
        colProducto.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getProducto() != null
                    ? data.getValue().getProducto().getNombre()
                    : ""
            )
        );

        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));

        colPrecio.setCellValueFactory(data ->
            new SimpleStringProperty(
                String.format("₡%.2f", data.getValue().getPrecioUnitario())
            )
        );

        colSubtotal.setCellValueFactory(data ->
            new SimpleStringProperty(
                String.format("₡%.2f", data.getValue().getSubtotal())
            )
        );

        detallesOrden = FXCollections.observableArrayList();
        tblDetalles.setItems(detallesOrden);

        // Doble clic para editar cantidad
        tblDetalles.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                DetalleOrden detalle = tblDetalles.getSelectionModel().getSelectedItem();
                if (detalle != null) {
                    editarCantidad(detalle);
                }
            }
        });

        // Menú contextual (clic derecho)
        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemEditar = new MenuItem(I18n.isSpanish() ? "✏️ Editar cantidad" : "✏️ Edit quantity");
        MenuItem itemEliminar = new MenuItem(I18n.isSpanish() ? "🗑️ Eliminar" : "🗑️ Delete");

        itemEditar.setOnAction(e -> {
            DetalleOrden d = tblDetalles.getSelectionModel().getSelectedItem();
            if (d != null) editarCantidad(d);
        });

        itemEliminar.setOnAction(e -> {
            DetalleOrden d = tblDetalles.getSelectionModel().getSelectedItem();
            if (d != null) eliminarDetalle(d);
        });

        contextMenu.getItems().addAll(itemEditar, itemEliminar);
        tblDetalles.setContextMenu(contextMenu);
    }

    // ==================== CONFIG COMBO DE GRUPOS ====================

    private void configurarCombosProductos() {
        cmbGrupos.setConverter(new javafx.util.StringConverter<GrupoProducto>() {
            @Override
            public String toString(GrupoProducto grupo) {
                return grupo != null ? grupo.getNombre() : "";
            }
            @Override
            public GrupoProducto fromString(String string) {
                return null;
            }
        });

        cmbGrupos.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, grupo) -> {
                if (grupo != null) {
                    filtrarProductosPorGrupo(grupo);
                } else {
                    mostrarProductos(listaProductos);
                }
            }
        );
    }

    // ==================== CONFIG CABECERA MESA / BARRA ====================

    /**
     * Configura los toggles Mesa/Barra y los ComboBox de salón y mesa.
     * Aquí seteamos converters, celdas y listeners.
     */
    private void configurarCabeceraTipoOrden() {
        // Estado inicial visual de los toggles
        tgMesa.setSelected(tipoOrdenActual == TipoOrden.MESA);
        tgBarra.setSelected(tipoOrdenActual == TipoOrden.BARRA);

        // Cambiar tipo de orden cuando se presionan
        tgMesa.setOnAction(e -> setTipoOrden(TipoOrden.MESA));
        tgBarra.setOnAction(e -> setTipoOrden(TipoOrden.BARRA));

        // ====== Salón ======
        cmbSalonSelect.setItems(listaSalonesDisponibles);

        cmbSalonSelect.setConverter(new javafx.util.StringConverter<Salon>() {
            @Override
            public String toString(Salon salon) {
                return salon != null ? salon.getNombre() : "";
            }
            @Override
            public Salon fromString(String string) {
                return null;
            }
        });

        cmbSalonSelect.setCellFactory(listView -> new ListCell<Salon>() {
            @Override
            protected void updateItem(Salon item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNombre());
            }
        });

        cmbSalonSelect.setButtonCell(new ListCell<Salon>() {
            @Override
            protected void updateItem(Salon item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNombre());
            }
        });

        // Cuando el usuario selecciona un salón
        cmbSalonSelect.getSelectionModel().selectedItemProperty().addListener((obs, old, nuevoSalon) -> {
            salonSeleccionado = nuevoSalon;
            cargarMesasDeSalon(nuevoSalon != null ? nuevoSalon.getId() : null); // <-- carga mesas reales del backend
            actualizarCabeceraVisual();
        });

        // ====== Mesa ======
        cmbMesaSelect.setItems(listaMesasDisponibles);

        cmbMesaSelect.setConverter(new javafx.util.StringConverter<Mesa>() {
            @Override
            public String toString(Mesa mesa) {
                return mesa != null ? mesa.getIdentificador() : "";
            }
            @Override
            public Mesa fromString(String string) {
                return null;
            }
        });

        cmbMesaSelect.setCellFactory(listView -> new ListCell<Mesa>() {
            @Override
            protected void updateItem(Mesa item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getIdentificador());
            }
        });

        cmbMesaSelect.setButtonCell(new ListCell<Mesa>() {
            @Override
            protected void updateItem(Mesa item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getIdentificador());
            }
        });

        // Cuando el usuario elige una mesa específica
        cmbMesaSelect.getSelectionModel().selectedItemProperty().addListener((obs, old, nuevaMesa) -> {
            mesaSeleccionada = nuevaMesa;
            actualizarCabeceraVisual();
        });

        // Aplicar habilitado inicial (Mesa vs Barra)
        aplicarHabilitadoMesa();
    }

    // ==================== CARGA DE SALONES Y MESAS ====================

    /**
     * Llama GET /salones.
     * Este endpoint devuelve salones SIN las mesas ni la imagen.
     * Vamos a mapearlos nosotros a objetos Salon del cliente.
     */
    private void cargarSalones() {
        try {
            String jsonResponse = RestClient.get("/salones");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (!Boolean.TRUE.equals(response.get("success"))) {
                listaSalonesDisponibles.clear();
                listaMesasDisponibles.clear();
                return;
            }

            Gson gson = new Gson();
            String dataJson = gson.toJson(response.get("data"));

            // dataJson es una lista de Map<String,Object> con campos básicos del salón
            List<Map<String,Object>> salonesRaw = gson.fromJson(
                    dataJson,
                    new TypeToken<List<Map<String,Object>>>(){}.getType()
            );

            List<Salon> salones = new ArrayList<>();
            for (Map<String,Object> raw : salonesRaw) {
                Salon s = new Salon();

                // id
                Object idObj = raw.get("id");
                if (idObj instanceof Number) {
                    s.setId(((Number) idObj).longValue());
                }

                s.setNombre(Objects.toString(raw.get("nombre"), ""));
                s.setTipo(Objects.toString(raw.get("tipo"), ""));
                s.setCobraServicio(Objects.toString(raw.get("cobraServicio"), "S"));
                s.setEstado(Objects.toString(raw.get("estado"), "A"));

                Object verObj = raw.get("version");
                if (verObj instanceof Number) {
                    s.setVersion(((Number) verObj).longValue());
                }

                // MUY IMPORTANTE: inicializamos lista de mesas vacía
                s.setMesas(new ArrayList<>());

                salones.add(s);
            }

            listaSalonesDisponibles.setAll(salones);

        } catch (Exception e) {
            e.printStackTrace();
            // si falla la llamada no matamos la ventana
        }
    }

    /**
     * Dado un salón seleccionado, trae sus mesas con GET /salones/{id}/mesas
     * y llena cmbMesaSelect usando sólo el identificador.
     */
    private void cargarMesasDeSalon(Long salonId) {
        listaMesasDisponibles.clear();
        if (salonId == null) {
            cmbMesaSelect.getSelectionModel().clearSelection();
            mesaSeleccionada = null;
            return;
        }

        try {
            String jsonResponse = RestClient.get("/salones/" + salonId + "/mesas");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (!Boolean.TRUE.equals(response.get("success"))) {
                cmbMesaSelect.getSelectionModel().clearSelection();
                mesaSeleccionada = null;
                return;
            }

            Gson gson = new Gson();
            String dataJson = gson.toJson(response.get("data"));

            // dataJson es lista de mesasDTO [{id, salonId, identificador, estado...}, ...]
            List<Map<String,Object>> mesasRaw = gson.fromJson(
                    dataJson,
                    new TypeToken<List<Map<String,Object>>>(){}.getType()
            );

            List<Mesa> mesas = new ArrayList<>();
            for (Map<String,Object> rawMesa : mesasRaw) {
                Mesa m = new Mesa();

                Object idObj = rawMesa.get("id");
                if (idObj instanceof Number) {
                    m.setId(((Number)idObj).longValue());
                }

                m.setIdentificador(Objects.toString(rawMesa.get("identificador"), ""));
                m.setEstado(Objects.toString(rawMesa.get("estado"), "LIBRE"));

                mesas.add(m);
            }

            listaMesasDisponibles.setAll(mesas);

            // si ya había mesaSeleccionada en memoria, tratar de volver a marcarla
            if (mesaSeleccionada != null) {
                for (Mesa m : mesas) {
                    if (m.getId().equals(mesaSeleccionada.getId())) {
                        cmbMesaSelect.getSelectionModel().select(m);
                        mesaSeleccionada = m;
                        break;
                    }
                }
            } else {
                cmbMesaSelect.getSelectionModel().clearSelection();
            }

        } catch (Exception e) {
            e.printStackTrace();
            listaMesasDisponibles.clear();
            cmbMesaSelect.getSelectionModel().clearSelection();
            mesaSeleccionada = null;
        }
    }

    /**
     * Luego de cargar salones desde el servidor, intentamos:
     * 1. Seleccionar el salón que venía en AppContext.
     * 2. Cargar las mesas de ese salón.
     * 3. Seleccionar la mesa que venía en AppContext (si aplica).
     */
    private void inicializarSeleccionMesa() {
        // Seleccionar el salón que venía del contexto
        if (salonSeleccionado != null && salonSeleccionado.getId() != null) {
            for (Salon s : listaSalonesDisponibles) {
                if (s.getId().equals(salonSeleccionado.getId())) {
                    cmbSalonSelect.getSelectionModel().select(s);
                    salonSeleccionado = s; // reusar la instancia cargada aquí
                    break;
                }
            }
        }

        // Cargar mesas reales del salón seleccionado
        if (salonSeleccionado != null) {
            cargarMesasDeSalon(salonSeleccionado.getId());
        } else {
            cargarMesasDeSalon(null);
        }

        // Intentar seleccionar la mesa previa si existe en la lista actual
        if (mesaSeleccionada != null && mesaSeleccionada.getId() != null) {
            for (Mesa m : listaMesasDisponibles) {
                if (m.getId().equals(mesaSeleccionada.getId())) {
                    cmbMesaSelect.getSelectionModel().select(m);
                    mesaSeleccionada = m;
                    break;
                }
            }
        }

        // Aplicar habilitado/inhabilitado según Mesa/Barra (tipoOrdenActual)
        aplicarHabilitadoMesa();
    }

    // ==================== CARGA GRUPOS / PRODUCTOS / ORDEN ====================

    private void cargarGrupos() {
        try {
            String jsonResponse = RestClient.get("/grupos");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                listaGrupos = gson.fromJson(dataJson, new TypeToken<List<GrupoProducto>>(){}.getType());

                cmbGrupos.getItems().clear();
                cmbGrupos.getItems().add(null); // Opción "Todos"
                cmbGrupos.getItems().addAll(listaGrupos);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cargar grupos:\n" + e.getMessage());
        }
    }

    /**
     * Usa /productos.
     * Incluye manejo defensivo si el backend responde HTML (ej. Payara error page).
     */
    private void cargarProductos() {
        try {
            String jsonResponse = RestClient.get("/productos");

            // Defensa si vino un HTML en vez de JSON
            if (jsonResponse == null || jsonResponse.trim().startsWith("<")) {
                listaProductos = new ArrayList<>();
                mostrarProductos(listaProductos);
                Mensaje.showError("Error", "No se pudieron cargar los productos.");
                return;
            }

            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                listaProductos = gson.fromJson(dataJson, new TypeToken<List<Producto>>(){}.getType());
                mostrarProductos(listaProductos);
            } else {
                listaProductos = new ArrayList<>();
                mostrarProductos(listaProductos);
                Mensaje.showWarning("Aviso", String.valueOf(response.get("message")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            listaProductos = new ArrayList<>();
            mostrarProductos(listaProductos);
            Mensaje.showError("Error", "Error al cargar productos:\n" + e.getMessage());
        }
    }

    /**
     * Si la mesa está ocupada buscamos la orden actual.
     * Si no, creamos una orden nueva.
     */
    private void cargarOrdenExistente() {
        // Caso barra o mesa libre => orden nueva
        if (mesaSeleccionada == null || !mesaSeleccionada.isOcupada()) {
            ordenActual = new Orden();
            ordenActual.setMesaId(mesaSeleccionada != null ? mesaSeleccionada.getId() : null);
            ordenActual.setUsuarioId(AppContext.getInstance().getUsuarioLogueado().getId());
            modoEdicion = false;
            lblEstadoOrden.setText(I18n.isSpanish() ? "Nueva" : "New");
            return;
        }

        // Caso mesa ocupada => buscar orden abierta en esa mesa
        try {
            String jsonResponse = RestClient.get("/ordenes/mesa/" + mesaSeleccionada.getId());
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                ordenActual = gson.fromJson(dataJson, Orden.class);

                cargarDetallesDeOrden();
                modoEdicion = true;
                lblEstadoOrden.setText(I18n.isSpanish() ? "En curso" : "In progress");
            } else {
                // No había orden previa, entonces nueva orden
                ordenActual = new Orden();
                ordenActual.setMesaId(mesaSeleccionada.getId());
                ordenActual.setUsuarioId(AppContext.getInstance().getUsuarioLogueado().getId());
                modoEdicion = false;
                lblEstadoOrden.setText(I18n.isSpanish() ? "Nueva" : "New");
            }
        } catch (Exception e) {
            // Si el GET /ordenes/mesa/{id} falló, seguimos como nueva orden
            ordenActual = new Orden();
            ordenActual.setMesaId(mesaSeleccionada.getId());
            ordenActual.setUsuarioId(AppContext.getInstance().getUsuarioLogueado().getId());
            modoEdicion = false;
            lblEstadoOrden.setText(I18n.isSpanish() ? "Nueva" : "New");
        }
    }

    private void cargarDetallesDeOrden() {
        if (ordenActual == null || ordenActual.getId() == null) return;

        try {
            String jsonResponse = RestClient.get("/ordenes/" + ordenActual.getId() + "/detalles");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));

                List<DetalleOrden> detalles = gson.fromJson(
                        dataJson,
                        new TypeToken<List<DetalleOrden>>(){}.getType()
                );

                detallesOrden.clear();
                detallesOrden.addAll(detalles);
                calcularTotal();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cargar detalles:\n" + e.getMessage());
        }
    }

    // ==================== PINTAR PRODUCTOS ====================

    private void mostrarProductos(List<Producto> productos) {
        flowProductos.getChildren().clear();

        if (productos == null || productos.isEmpty()) {
            Label lblVacio = new Label(
                I18n.isSpanish() ? "No hay productos disponibles" : "No products available"
            );
            lblVacio.setStyle("-fx-text-fill: #999; -fx-font-size: 14px;");
            flowProductos.getChildren().add(lblVacio);
            return;
        }

        for (Producto producto : productos) {
            flowProductos.getChildren().add(crearBotonProducto(producto));
        }
    }

    private VBox crearBotonProducto(Producto producto) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(120, 100);
        card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 10;" +
            "-fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 10;" +
            "-fx-padding: 10; -fx-cursor: hand;"
        );

        Label lblNombre = new Label(producto.getNombreDisplay());
        lblNombre.setFont(Font.font("System", FontWeight.BOLD, 12));
        lblNombre.setWrapText(true);
        lblNombre.setMaxWidth(100);
        lblNombre.setAlignment(Pos.CENTER);

        Label lblPrecio = new Label(producto.getPrecioFormateado());
        lblPrecio.setStyle("-fx-text-fill: #FF7A00; -fx-font-size: 13px; -fx-font-weight: bold;");

        card.getChildren().addAll(lblNombre, lblPrecio);

        // Hover
        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: #FFF8F0; -fx-background-radius: 10;" +
                "-fx-border-color: #FF7A00; -fx-border-width: 2; -fx-border-radius: 10;" +
                "-fx-padding: 10; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);"
            );
        });

        card.setOnMouseExited(e -> {
            card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10;" +
                "-fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 10;" +
                "-fx-padding: 10; -fx-cursor: hand;"
            );
        });

        // Click -> agrega producto
        card.setOnMouseClicked(e -> agregarProducto(producto));

        return card;
    }

    private void filtrarProductos(String busqueda) {
        if (listaProductos == null || listaProductos.isEmpty()) {
            mostrarProductos(Collections.emptyList());
            return;
        }

        if (busqueda == null || busqueda.trim().isEmpty()) {
            if (cmbGrupos.getValue() != null) {
                filtrarProductosPorGrupo(cmbGrupos.getValue());
            } else {
                mostrarProductos(listaProductos);
            }
            return;
        }

        String filtro = busqueda.toLowerCase();
        List<Producto> filtrados = new ArrayList<>();

        for (Producto p : listaProductos) {
            if ((p.getNombre() != null && p.getNombre().toLowerCase().contains(filtro)) ||
                (p.getNombreCorto() != null && p.getNombreCorto().toLowerCase().contains(filtro))) {
                filtrados.add(p);
            }
        }

        mostrarProductos(filtrados);
    }

    private void filtrarProductosPorGrupo(GrupoProducto grupo) {
        if (listaProductos == null || listaProductos.isEmpty() || grupo == null) {
            mostrarProductos(Collections.emptyList());
            return;
        }

        List<Producto> filtrados = new ArrayList<>();

        for (Producto p : listaProductos) {
            if (p.getGrupoId() != null && p.getGrupoId().equals(grupo.getId())) {
                filtrados.add(p);
            }
        }

        mostrarProductos(filtrados);
    }

    // ==================== DETALLES ORDEN ====================

    private void agregarProducto(Producto producto) {
        // Ver si el producto ya está en la orden
        for (DetalleOrden detalle : detallesOrden) {
            if (detalle.getProducto() != null &&
                detalle.getProducto().getId().equals(producto.getId())) {

                detalle.setCantidad(detalle.getCantidad() + 1);
                detalle.calcularSubtotal();
                tblDetalles.refresh();
                calcularTotal();
                return;
            }
        }

        // Nuevo detalle
        DetalleOrden nuevo = new DetalleOrden();
        nuevo.setProducto(producto);
        nuevo.setProductoId(producto.getId());
        nuevo.setCantidad(1);
        nuevo.setPrecioUnitario(producto.getPrecio());
        nuevo.calcularSubtotal();

        detallesOrden.add(nuevo);
        calcularTotal();
    }

    private void editarCantidad(DetalleOrden detalle) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(detalle.getCantidad()));
        dialog.setTitle(I18n.isSpanish() ? "Editar Cantidad" : "Edit Quantity");
        dialog.setHeaderText(detalle.getProducto().getNombre());
        dialog.setContentText(I18n.isSpanish() ? "Cantidad:" : "Quantity:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(valor -> {
            try {
                int nuevaCantidad = Integer.parseInt(valor);
                if (nuevaCantidad > 0) {
                    detalle.setCantidad(nuevaCantidad);
                    detalle.calcularSubtotal();
                    tblDetalles.refresh();
                    calcularTotal();
                } else {
                    Mensaje.showWarning("Error", I18n.isSpanish()
                        ? "La cantidad debe ser mayor a 0"
                        : "Quantity must be greater than 0");
                }
            } catch (NumberFormatException ex) {
                Mensaje.showWarning("Error", I18n.isSpanish()
                    ? "Cantidad inválida"
                    : "Invalid quantity");
            }
        });
    }

    private void eliminarDetalle(DetalleOrden detalle) {
        boolean confirmar = Mensaje.showConfirmation(
            I18n.isSpanish() ? "Confirmar" : "Confirm",
            I18n.isSpanish()
                ? "¿Eliminar " + detalle.getProducto().getNombre() + "?"
                : "Delete " + detalle.getProducto().getNombre() + "?"
        );

        if (confirmar) {
            detallesOrden.remove(detalle);
            calcularTotal();
        }
    }

    private void calcularTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleOrden d : detallesOrden) {
            total = total.add(d.getSubtotal());
        }
        lblTotal.setText(String.format("₡%.2f", total));
    }

    // ==================== EVENTOS DE BOTONES MAIN ====================

    @FXML
    private void onVolver(ActionEvent event) {
        if (!detallesOrden.isEmpty()) {
            boolean confirmar = Mensaje.showConfirmation(
                I18n.isSpanish() ? "Confirmar" : "Confirm",
                I18n.isSpanish()
                    ? "¿Salir sin guardar? Se perderán los cambios."
                    : "Exit without saving? Changes will be lost."
            );
            if (!confirmar) return;
        }

        FlowController.getInstance().goToView("MenuPrincipal", "RestUNA - Menú Salonero", 1000, 560);
    }

    @FXML
    private void onGuardar(ActionEvent event) {
        if (detallesOrden.isEmpty()) {
            Mensaje.showWarning("Aviso", I18n.isSpanish()
                ? "Debe agregar al menos un producto"
                : "Must add at least one product");
            return;
        }

        // Validación según tipo de orden
        if (tipoOrdenActual == TipoOrden.MESA) {
            if (salonSeleccionado == null || mesaSeleccionada == null) {
                Mensaje.showWarning("Aviso", I18n.isSpanish()
                    ? "Debe seleccionar salón y mesa."
                    : "You must select room and table.");
                return;
            }
            ordenActual.setMesaId(mesaSeleccionada.getId());
        } else {
            // Barra → sin mesa
            ordenActual.setMesaId(null);
        }

        try {
            ordenActual.setObservaciones(txtObservaciones.getText());
            ordenActual.setDetalles(new ArrayList<>(detallesOrden));
            ordenActual.setUsuarioId(AppContext.getInstance().getUsuarioLogueado().getId());

            String jsonResponse;
            if (modoEdicion && ordenActual.getId() != null) {
                jsonResponse = RestClient.put("/ordenes/" + ordenActual.getId(), ordenActual);
            } else {
                jsonResponse = RestClient.post("/ordenes", ordenActual);
            }

            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (Boolean.TRUE.equals(response.get("success"))) {
                Mensaje.showSuccess("Éxito", I18n.isSpanish()
                    ? "Orden guardada correctamente"
                    : "Order saved successfully");

                // Ocupar mesa si es nueva orden de tipo MESA
                if (!modoEdicion && tipoOrdenActual == TipoOrden.MESA && mesaSeleccionada != null) {
                    RestClient.post("/salones/mesas/" + mesaSeleccionada.getId() + "/ocupar", null);
                }

                FlowController.getInstance().goToView("VistaSalones", "RestUNA - Salones", 1400, 800);
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al guardar orden:\n" + e.getMessage());
        }
    }

    @FXML
    private void onCancelarOrden(ActionEvent event) {
        if (ordenActual == null || ordenActual.getId() == null) {
            onVolver(null);
            return;
        }

        boolean confirmar = Mensaje.showConfirmation(
            I18n.isSpanish() ? "Confirmar Cancelación" : "Confirm Cancellation",
            I18n.isSpanish()
                ? "¿Está seguro de cancelar esta orden?\nEsta acción no se puede deshacer."
                : "Are you sure you want to cancel this order?\nThis action cannot be undone."
        );

        if (!confirmar) return;

        try {
            String jsonResponse = RestClient.post("/ordenes/" + ordenActual.getId() + "/cancelar", null);
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (Boolean.TRUE.equals(response.get("success"))) {
                Mensaje.showSuccess("Éxito", I18n.isSpanish()
                    ? "Orden cancelada correctamente"
                    : "Order cancelled successfully");

                // Liberar mesa si era tipo MESA
                if (mesaSeleccionada != null && tipoOrdenActual == TipoOrden.MESA) {
                    RestClient.post("/salones/mesas/" + mesaSeleccionada.getId() + "/liberar", null);
                }

                FlowController.getInstance().goToView("VistaSalones", "RestUNA - Salones", 1400, 800);
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cancelar orden:\n" + e.getMessage());
        }
    }

    @FXML
    private void onElegirMesa(ActionEvent event) {
        // Lugar listo para abrir un modal visual de plano de mesas
        Mensaje.showInfo(
            I18n.isSpanish() ? "Seleccionar mesa" : "Select table",
            I18n.isSpanish()
                ? "Aquí podrías abrir el mapa de mesas para elegir visualmente."
                : "Here you could open the floor plan to pick a table visually."
        );
    }

    // ==================== ESTADO VISUAL CABECERA ====================

    /**
     * Cambia entre orden de MESA y orden de BARRA.
     * Actualiza toggles, habilita/deshabilita combos y refresca labels.
     */
    private void setTipoOrden(TipoOrden tipo) {
        tipoOrdenActual = tipo;

        tgMesa.setSelected(tipoOrdenActual == TipoOrden.MESA);
        tgBarra.setSelected(tipoOrdenActual == TipoOrden.BARRA);

        aplicarHabilitadoMesa();
        actualizarCabeceraVisual();
    }

    /**
     * Habilita o deshabilita los combos Salón y Mesa según tipo de orden.
     * Si pasamos a Barra, borra la selección.
     */
    private void aplicarHabilitadoMesa() {
        boolean esMesa = (tipoOrdenActual == TipoOrden.MESA);

        cmbSalonSelect.setDisable(!esMesa);
        cmbMesaSelect.setDisable(!esMesa);
        btnElegirMesa.setDisable(!esMesa);

        if (!esMesa) {
            // Al pasar a Barra limpiamos todo lo de ubicación
            salonSeleccionado = null;
            mesaSeleccionada = null;
            cmbSalonSelect.getSelectionModel().clearSelection();
            cmbMesaSelect.getSelectionModel().clearSelection();
        }
    }

    /**
     * Refresca las etiquetas informativas (Mesa:, Salón:) que están en la parte superior.
     */
    private void actualizarCabeceraVisual() {
        if (tipoOrdenActual == TipoOrden.MESA) {
            String salonTxt = (salonSeleccionado != null) ? salonSeleccionado.getNombre() : "—";
            String mesaTxt  = (mesaSeleccionada != null) ? mesaSeleccionada.getIdentificador() : "—";

            lblMesaInfo.setText((I18n.isSpanish() ? "Mesa: " : "Table: ") + mesaTxt);
            lblSalonInfo.setText((I18n.isSpanish() ? "Salón: " : "Room: ") + salonTxt);
        } else {
            lblMesaInfo.setText(I18n.isSpanish() ? "Barra" : "Bar");
            lblSalonInfo.setText(I18n.isSpanish() ? "Salón: —" : "Room: —");
        }
    }

    /**
     * Pinta la info del usuario, la hora de la orden y refresca cabecera mesa/salón.
     */
    private void actualizarInfoOrden() {
        Usuario usuario = AppContext.getInstance().getUsuarioLogueado();
        lblUsuario.setText((I18n.isSpanish() ? "Salonero: " : "Waiter: ") + usuario.getNombre());

        if (ordenActual != null && ordenActual.getFechaHora() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            lblFechaHora.setText(ordenActual.getFechaHora().format(formatter));
        } else {
            lblFechaHora.setText("--/--/---- --:--");
        }

        actualizarCabeceraVisual();
    }

    /**
     * Ajusta los textos visibles según el idioma y el estado de la orden (nueva / en curso).
     */
    private void actualizarTextos() {
        boolean esEspanol = I18n.isSpanish();

        lblTitle.setText(esEspanol ? "📝 Gestión de Orden" : "📝 Order Management");
        btnVolver.setText(esEspanol ? "← Volver" : "← Back");
        btnGuardar.setText(esEspanol ? "💾 Guardar Orden" : "💾 Save Order");
        btnCancelarOrden.setText(esEspanol ? "❌ Cancelar Orden" : "❌ Cancel Order");

        txtBuscarProducto.setPromptText(esEspanol ? "Buscar producto..." : "Search product...");
        cmbGrupos.setPromptText(esEspanol ? "Todos los grupos" : "All groups");

        colProducto.setText(esEspanol ? "Producto" : "Product");
        colCantidad.setText(esEspanol ? "Cant." : "Qty");
        colPrecio.setText(esEspanol ? "Precio" : "Price");
        colSubtotal.setText(esEspanol ? "Subtotal" : "Subtotal");

        txtObservaciones.setPromptText(esEspanol ? "Observaciones adicionales..." : "Additional notes...");

        tgMesa.setText(esEspanol ? "Mesa" : "Table");
        tgBarra.setText(esEspanol ? "Barra" : "Bar");

        cmbSalonSelect.setPromptText(esEspanol ? "Seleccione" : "Select");
        cmbMesaSelect.setPromptText(esEspanol ? "Seleccione" : "Select");
        btnElegirMesa.setText(esEspanol ? "🪑 Elegir Mesa" : "🪑 Pick Table");

        lblEstadoOrden.setText(
            modoEdicion
                ? (esEspanol ? "En curso" : "In progress")
                : (esEspanol ? "Nueva"    : "New")
        );
    }
}
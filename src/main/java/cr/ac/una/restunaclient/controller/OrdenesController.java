package cr.ac.una.restunaclient.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.util.StringConverter;

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
    @FXML private TextField txtCompradorId; // lo dejamos declarado porque existe en tu código original y puede que esté en el model, aunque ya no lo usamos

    // ==================== SELECTOR DE PRODUCTOS ====================
    @FXML private TextField txtBuscarProducto;
    @FXML private ComboBox<GrupoProducto> cmbGrupos;
    // ❌ Eliminado el ComboBox<Cliente> cmbCliente; ya no usamos clientes
    @FXML private FlowPane flowProductos;

    // ==================== TABLA DE DETALLES ====================
    @FXML private TableView<DetalleOrden> tblDetalles;
    @FXML private TableColumn<DetalleOrden, String> colProducto;
    @FXML private TableColumn<DetalleOrden, Integer> colCantidad;
    @FXML private TableColumn<DetalleOrden, String> colPrecio;
    @FXML private TableColumn<DetalleOrden, String> colSubtotal;

    // ==================== TOTALES ====================
    @FXML private Label lblTotal;

    // ==================== PANEL DERECHO ====================
    @FXML private ScrollPane scrollOrdenes;
    @FXML private VBox vboxOrdenes;

    // ==================== VARIABLES DE ESTADO ====================
    private Orden ordenActual;
    private Mesa mesaSeleccionada;
    private Salon salonSeleccionado;
    private ObservableList<DetalleOrden> detallesOrden;
    private List<GrupoProducto> listaGrupos;
    private List<Producto> listaProductos;
    private final ObservableList<Orden> listaOrdenes = FXCollections.observableArrayList();
    private boolean modoEdicion = false;

    // ComboBox backing lists
    private final ObservableList<Salon> listaSalonesDisponibles = FXCollections.observableArrayList();
    private final ObservableList<Mesa> listaMesasDisponibles   = FXCollections.observableArrayList();

    // Gson config
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>)
                    (je, ttype, ctx) -> LocalDate.parse(je.getAsString()))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>)
                    (je, ttype, ctx) -> {
                        String s = je.getAsString();
                        try {
                            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        } catch (Exception e) {
                            try {
                                return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
                            } catch (Exception e2) {
                                throw new RuntimeException("Fecha inválida: " + s, e2);
                            }
                        }
                    })
            .create();

    private enum TipoOrden { MESA, BARRA }
    private TipoOrden tipoOrdenActual = TipoOrden.MESA;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        mesaSeleccionada  = (Mesa)  AppContext.getInstance().get("mesaSeleccionada");
        salonSeleccionado = (Salon) AppContext.getInstance().get("salonSeleccionado");

        tipoOrdenActual = (mesaSeleccionada == null) ? TipoOrden.BARRA : TipoOrden.MESA;

        configurarTabla();
        configurarCombosProductos();
        configurarCabeceraTipoOrden();

        cargarSalones();
        inicializarSeleccionMesa();
        cargarGrupos();
        cargarProductos();
        cargarOrdenExistente();

        actualizarCabeceraVisual();
        actualizarInfoOrden();
        actualizarTextos();

        cargarListaOrdenes();

        txtBuscarProducto.textProperty().addListener((obs, old, val) -> filtrarProductos(val));
    }

    // ==================== TABLA DETALLES ====================

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

        tblDetalles.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                DetalleOrden detalle = tblDetalles.getSelectionModel().getSelectedItem();
                if (detalle != null) {
                    editarCantidad(detalle);
                }
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemEditar   = new MenuItem(I18n.isSpanish() ? "✏️ Editar cantidad" : "✏️ Edit quantity");
        MenuItem itemEliminar = new MenuItem(I18n.isSpanish() ? "🗑️ Eliminar"        : "🗑️ Delete");

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

    // ==================== GRUPOS / PRODUCTOS ====================

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

    // ==================== CABECERA MESA / BARRA ====================

    private void configurarCabeceraTipoOrden() {
        tgMesa.setSelected(tipoOrdenActual == TipoOrden.MESA);
        tgBarra.setSelected(tipoOrdenActual == TipoOrden.BARRA);

        tgMesa.setOnAction(e -> setTipoOrden(TipoOrden.MESA));
        tgBarra.setOnAction(e -> setTipoOrden(TipoOrden.BARRA));

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

        cmbSalonSelect.getSelectionModel().selectedItemProperty().addListener((obs, old, nuevoSalon) -> {
            salonSeleccionado = nuevoSalon;
            cargarMesasDeSalon(nuevoSalon != null ? nuevoSalon.getId() : null);
            actualizarCabeceraVisual();
        });

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

        cmbMesaSelect.getSelectionModel().selectedItemProperty().addListener((obs, old, nuevaMesa) -> {
            mesaSeleccionada = nuevaMesa;
            actualizarCabeceraVisual();
        });

        aplicarHabilitadoMesa();
    }

    // ==================== SALONES / MESAS ====================

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

            List<Map<String,Object>> salonesRaw = gson.fromJson(
                    dataJson,
                    new TypeToken<List<Map<String,Object>>>(){}.getType()
            );

            List<Salon> salones = new ArrayList<>();
            for (Map<String,Object> raw : salonesRaw) {
                Salon s = new Salon();
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
                s.setMesas(new ArrayList<>());
                salones.add(s);
            }

            listaSalonesDisponibles.setAll(salones);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    private void inicializarSeleccionMesa() {
        if (salonSeleccionado != null && salonSeleccionado.getId() != null) {
            for (Salon s : listaSalonesDisponibles) {
                if (s.getId().equals(salonSeleccionado.getId())) {
                    cmbSalonSelect.getSelectionModel().select(s);
                    salonSeleccionado = s;
                    break;
                }
            }
        }

        if (salonSeleccionado != null) {
            cargarMesasDeSalon(salonSeleccionado.getId());
        } else {
            cargarMesasDeSalon(null);
        }

        if (mesaSeleccionada != null && mesaSeleccionada.getId() != null) {
            for (Mesa m : listaMesasDisponibles) {
                if (m.getId().equals(mesaSeleccionada.getId())) {
                    cmbMesaSelect.getSelectionModel().select(m);
                    mesaSeleccionada = m;
                    break;
                }
            }
        }

        aplicarHabilitadoMesa();
    }

    // ==================== GRUPOS / PRODUCTOS / ORDEN ====================

    private void cargarGrupos() {
        try {
            String jsonResponse = RestClient.get("/grupos");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                listaGrupos = gson.fromJson(dataJson, new TypeToken<List<GrupoProducto>>(){}.getType());

                cmbGrupos.getItems().clear();
                cmbGrupos.getItems().add(null);
                cmbGrupos.getItems().addAll(listaGrupos);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cargar grupos:\n" + e.getMessage());
        }
    }

    private void cargarProductos() {
        try {
            String jsonResponse = RestClient.get("/productos");

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

    private void cargarOrdenExistente() {
        if (mesaSeleccionada == null || !mesaSeleccionada.isOcupada()) {
            ordenActual = new Orden();
            ordenActual.setMesaId(mesaSeleccionada != null ? mesaSeleccionada.getId() : null);

            Usuario logged = AppContext.getInstance().getUsuarioLogueado();
            if (logged != null && logged.getId() != null) {
                ordenActual.setUsuarioId(logged.getId());
            }

            modoEdicion = false;
            lblEstadoOrden.setText(I18n.isSpanish() ? "Nueva" : "New");
            return;
        }

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
                ordenActual = new Orden();
                ordenActual.setMesaId(mesaSeleccionada.getId());

                Usuario logged = AppContext.getInstance().getUsuarioLogueado();
                if (logged != null && logged.getId() != null) {
                    ordenActual.setUsuarioId(logged.getId());
                }

                modoEdicion = false;
                lblEstadoOrden.setText(I18n.isSpanish() ? "Nueva" : "New");
            }
        } catch (Exception e) {
            ordenActual = new Orden();
            ordenActual.setMesaId(mesaSeleccionada.getId());

            Usuario logged = AppContext.getInstance().getUsuarioLogueado();
            if (logged != null && logged.getId() != null) {
                ordenActual.setUsuarioId(logged.getId());
            }

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

    // ==================== PRODUCTOS ====================

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

    // ==================== BOTONES MAIN ====================

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

        if (tipoOrdenActual == TipoOrden.MESA) {
            if (salonSeleccionado == null || mesaSeleccionada == null) {
                Mensaje.showWarning("Aviso", I18n.isSpanish()
                        ? "Debe seleccionar salón y mesa."
                        : "You must select room and table.");
                return;
            }
            ordenActual.setMesaId(mesaSeleccionada.getId());
        } else {
            ordenActual.setMesaId(null);
        }

        try {
            Usuario logged = AppContext.getInstance().getUsuarioLogueado();
            if (logged == null || logged.getId() == null) {
                Mensaje.showError("Error", "No hay usuario logueado.");
                return;
            }

            ordenActual.setUsuarioId(logged.getId());
            ordenActual.setObservaciones(txtObservaciones.getText());
            ordenActual.setDetalles(new ArrayList<>(detallesOrden));

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

                if (!modoEdicion && tipoOrdenActual == TipoOrden.MESA && mesaSeleccionada != null) {
                    try { RestClient.post("/salones/mesas/" + mesaSeleccionada.getId() + "/ocupar", null); } catch (Exception ignore) {}
                    try { RestClient.post("/mesas/" + mesaSeleccionada.getId() + "/ocupar", null); }       catch (Exception ignore) {}
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

                if (mesaSeleccionada != null && tipoOrdenActual == TipoOrden.MESA) {
                    try { RestClient.post("/salones/mesas/" + mesaSeleccionada.getId() + "/liberar", null); } catch (Exception ignore) {}
                    try { RestClient.post("/mesas/" + mesaSeleccionada.getId() + "/liberar", null); }       catch (Exception ignore) {}
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
        Mensaje.showInfo(
                I18n.isSpanish() ? "Seleccionar mesa" : "Select table",
                I18n.isSpanish()
                        ? "Aquí podrías abrir el mapa de mesas para elegir visualmente."
                        : "Here you could open the floor plan to pick a table visually."
        );
    }

    // ==================== ESTADO VISUAL CABECERA ====================

    private void setTipoOrden(TipoOrden tipo) {
        tipoOrdenActual = tipo;

        tgMesa.setSelected(tipoOrdenActual == TipoOrden.MESA);
        tgBarra.setSelected(tipoOrdenActual == TipoOrden.BARRA);

        aplicarHabilitadoMesa();
        actualizarCabeceraVisual();
    }

    private void aplicarHabilitadoMesa() {
        boolean esMesa = (tipoOrdenActual == TipoOrden.MESA);

        cmbSalonSelect.setDisable(!esMesa);
        cmbMesaSelect.setDisable(!esMesa);
        btnElegirMesa.setDisable(!esMesa);
    }

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

    // ==================== PANEL DERECHO (ÓRDENES ACTIVAS) ====================

    private String formatearUbicacion(Orden o) {
        try {
            if (o.getMesa() != null) {
                Mesa m = o.getMesa();
                String mesaTxt = (m.getIdentificador() != null && !m.getIdentificador().isBlank())
                        ? m.getIdentificador()
                        : (m.getId() != null ? String.valueOf(m.getId()) : "—");
                String salonTxt = (m.getSalonId() != null)
                        ? String.valueOf(m.getSalonId())
                        : "—";
                return "Salón " + salonTxt + " · Mesa " + mesaTxt;
            }
        } catch (Exception ignore) {
        }

        if (o.getMesaId() != null) {
            return "Mesa " + o.getMesaId();
        }
        return "Barra";
    }

    private void cargarListaOrdenes() {
        try {
            String jsonResponse = RestClient.get("/ordenes/activas");

            if (jsonResponse == null || jsonResponse.trim().startsWith("<")) {
                vboxOrdenes.getChildren().setAll(new Label("No hay órdenes activas."));
                return;
            }

            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (!Boolean.TRUE.equals(response.get("success"))) {
                vboxOrdenes.getChildren().setAll(new Label("No hay órdenes activas."));
                return;
            }

            String dataJson = gson.toJson(response.get("data"));
            List<Orden> ordenes = gson.fromJson(dataJson, new TypeToken<List<Orden>>() {}.getType());
            if (ordenes == null) ordenes = Collections.emptyList();

            ordenes.sort((a, b) -> {
                LocalDateTime ta = a.getFechaHora();
                LocalDateTime tb = b.getFechaHora();
                if (ta != null && tb != null) {
                    return tb.compareTo(ta);
                }
                if (a.getId() != null && b.getId() != null) {
                    return Long.compare(b.getId(), a.getId());
                }
                return 0;
            });

            listaOrdenes.setAll(ordenes);
            mostrarOrdenesEnLista();
        } catch (Exception e) {
            e.printStackTrace();
            vboxOrdenes.getChildren().setAll(new Label("Error al cargar órdenes."));
        }
    }

    private void mostrarOrdenesEnLista() {
        vboxOrdenes.getChildren().clear();

        if (listaOrdenes.isEmpty()) {
            Label lblVacio = new Label("No hay órdenes activas");
            lblVacio.setStyle("-fx-text-fill: #999; -fx-font-size: 13px;");
            vboxOrdenes.getChildren().add(lblVacio);
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");

        for (Orden o : listaOrdenes) {
            VBox card = new VBox(4);
            card.setStyle("-fx-background-color: #FFF8F0; -fx-background-radius: 8; "
                    + "-fx-padding: 10; -fx-border-color: #FF7A00; "
                    + "-fx-border-radius: 8; -fx-cursor: hand;");
            card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #FFEBD2; -fx-background-radius: 8; "
                    + "-fx-padding: 10; -fx-border-color: #FF7A00; -fx-border-radius: 8; -fx-cursor: hand;"));
            card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #FFF8F0; -fx-background-radius: 8; "
                    + "-fx-padding: 10; -fx-border-color: #FF7A00; -fx-border-radius: 8; -fx-cursor: hand;"));

            Label lblUbicacion = new Label("🪑 " + formatearUbicacion(o));
            lblUbicacion.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");

            String atendidoPor = (o.getUsuario() != null && o.getUsuario().getNombre() != null)
                    ? o.getUsuario().getNombre()
                    : (o.getUsuarioId() != null ? "ID " + o.getUsuarioId() : "—");
            Label lblAtiende = new Label("👤 Atendido por: " + atendidoPor);

            String fechaCorta = (o.getFechaHora() != null) ? o.getFechaHora().format(fmt) : "";
            Label lblEstado = new Label("📅 " + (o.getEstado() != null ? o.getEstado() : "—")
                    + (fechaCorta.isBlank() ? "" : " · " + fechaCorta));
            lblEstado.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

            card.getChildren().addAll(lblUbicacion, lblAtiende, lblEstado);
            card.setOnMouseClicked(e -> abrirOrdenExistente(o));
            vboxOrdenes.getChildren().add(card);
        }
    }

    private void abrirOrdenExistente(Orden orden) {
        try {
            this.ordenActual = orden;
            this.modoEdicion = true;
            cargarDetallesDeOrden();
            lblEstadoOrden.setText("En curso");
            Mensaje.showInfo("Orden", "Orden #" + orden.getId() + " cargada para continuar.");
        } catch (Exception e) {
            Mensaje.showError("Error", "No se pudo cargar la orden seleccionada.");
        }
    }
}
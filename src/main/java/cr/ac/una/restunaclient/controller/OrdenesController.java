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

    // Nuevas listas para combos
    private ObservableList<Salon> listaSalonesDisponibles = FXCollections.observableArrayList();
    private ObservableList<Mesa> listaMesasDisponibles = FXCollections.observableArrayList();

    private enum TipoOrden { MESA, BARRA }
    private TipoOrden tipoOrdenActual = TipoOrden.MESA; // default mesa

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Recuperar selección previa desde otra vista
        mesaSeleccionada = (Mesa) AppContext.getInstance().get("mesaSeleccionada");
        salonSeleccionado = (Salon) AppContext.getInstance().get("salonSeleccionado");

        // Si no viene mesa => asumimos que es orden de barra
        tipoOrdenActual = (mesaSeleccionada == null) ? TipoOrden.BARRA : TipoOrden.MESA;

        configurarTabla();
        configurarCombosProductos();
        configurarCabeceraTipoOrden();

        // 1. Traer salones (y mesas) del backend y poblar combos
        cargarSalonesYMesas();

        // 2. Ajustar selección inicial en combos según lo que venía del AppContext
        inicializarSeleccionMesa();

        // 3. Traer datos para el panel izquierdo
        cargarGrupos();
        cargarProductos();

        // 4. Ver si esa mesa ya tenía una orden abierta
        cargarOrdenExistente();

        // Pintar labels, textos, etc.
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

        // Menú contextual
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

    // ==================== CONFIG COMBOS DE PRODUCTOS ====================

    private void configurarCombosProductos() {
        // Combo de grupos de productos
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
                    // "Todos los grupos"
                    mostrarProductos(listaProductos);
                }
            }
        );
    }

    // ==================== CONFIG CABECERA MESA / BARRA ====================

    private void configurarCabeceraTipoOrden() {
    // Toggle inicial según tipo de orden
    tgMesa.setSelected(tipoOrdenActual == TipoOrden.MESA);
    tgBarra.setSelected(tipoOrdenActual == TipoOrden.BARRA);

    // Listeners de los toggles
    tgMesa.setOnAction(e -> setTipoOrden(TipoOrden.MESA));
    tgBarra.setOnAction(e -> setTipoOrden(TipoOrden.BARRA));

    // ====== Combo de Salón ======
    cmbSalonSelect.setItems(listaSalonesDisponibles);

    // Cómo se muestra el salón seleccionado (parte cerrada del combo)
    cmbSalonSelect.setConverter(new javafx.util.StringConverter<Salon>() {
        @Override
        public String toString(Salon salon) {
            return salon != null ? salon.getNombre() : "";
        }
        @Override
        public Salon fromString(String string) { return null; }
    });

    // Cómo se muestran las opciones en el dropdown
    cmbSalonSelect.setCellFactory(listView -> new ListCell<Salon>() {
        @Override
        protected void updateItem(Salon item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.getNombre()); // ej: "Salón Principal"
            }
        }
    });
    // Botón visible cuando ya hay uno seleccionado
    cmbSalonSelect.setButtonCell(new ListCell<Salon>() {
        @Override
        protected void updateItem(Salon item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.getNombre());
            }
        }
    });

    // Cuando el usuario cambia de salón
    cmbSalonSelect.getSelectionModel().selectedItemProperty().addListener((obs, old, nuevoSalon) -> {
        salonSeleccionado = nuevoSalon;
        actualizarMesasSegunSalon(); // <- recarga las mesas del salón
        actualizarCabeceraVisual();
    });

    // ====== Combo de Mesa ======
    cmbMesaSelect.setItems(listaMesasDisponibles);

    // Cómo se muestra la mesa seleccionada (parte cerrada del combo)
    cmbMesaSelect.setConverter(new javafx.util.StringConverter<Mesa>() {
        @Override
        public String toString(Mesa mesa) {
            return mesa != null ? mesa.getIdentificador() : "";
        }
        @Override
        public Mesa fromString(String string) { return null; }
    });

    // Cómo se muestran las opciones en el dropdown
    cmbMesaSelect.setCellFactory(listView -> new ListCell<Mesa>() {
        @Override
        protected void updateItem(Mesa item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                // Acá decides qué quieres ver exactamente:
                // solo el identificador, o algo como "M5 (ocupada)".
                // Te lo dejo solo con el identificador como pediste.
                setText(item.getIdentificador());
            }
        }
    });
    // Botón visible cuando ya hay una mesa seleccionada
    cmbMesaSelect.setButtonCell(new ListCell<Mesa>() {
        @Override
        protected void updateItem(Mesa item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.getIdentificador());
            }
        }
    });

    // Cuando el usuario elige la mesa
    cmbMesaSelect.getSelectionModel().selectedItemProperty().addListener((obs, old, nuevaMesa) -> {
        mesaSeleccionada = nuevaMesa;
        actualizarCabeceraVisual();
    });

    // Aplica habilitado / deshabilitado según Mesa vs Barra
    aplicarHabilitadoMesa();
}

    // ==================== CARGA DE SALONES Y MESAS ====================

    /**
     * Trae todos los salones del backend y llena cmbSalonSelect.
     * También guardamos sus mesas para poder filtrarlas más tarde.
     *
     * Se asume que:
     * - GET /salones devuelve un JSON con { success, data:[ {id,nombre,mesas:[...]} ] }
     * - Cada mesa tiene id, identificador, ocupada, etc.
     */
    private void cargarSalonesYMesas() {
        try {
            String jsonResponse = RestClient.get("/salones");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (!Boolean.TRUE.equals(response.get("success"))) {
                // si vino error del backend, dejamos las listas vacías pero no rompemos la ventana
                listaSalonesDisponibles.clear();
                listaMesasDisponibles.clear();
                return;
            }

            Gson gson = new Gson();
            String dataJson = gson.toJson(response.get("data"));
            List<Salon> salones = gson.fromJson(dataJson, new TypeToken<List<Salon>>(){}.getType());

            // mostramos solo salones activos
            listaSalonesDisponibles.setAll(salones);

        } catch (Exception e) {
            e.printStackTrace();
            // si falla la llamada, sigue corriendo la ventana con lo que venía en AppContext
        }
    }

    /**
     * Llena cmbMesaSelect con las mesas del salón actualmente seleccionado.
     * Si ya había una mesaSeleccionada que pertenece a ese salón, la vuelve a seleccionar.
     * Si no, limpia selección.
     */
    private void actualizarMesasSegunSalon() {
        listaMesasDisponibles.clear();

        if (salonSeleccionado != null && salonSeleccionado.getMesas() != null) {
            // acá puedes decidir si quieres mostrar TODAS las mesas
            // o solo las no ocupadas. Te doy la versión TODAS:
            listaMesasDisponibles.addAll(salonSeleccionado.getMesas());
        }

        // si la mesa actual ya no pertenece al salón nuevo, la quitamos
        if (mesaSeleccionada != null) {
            boolean aunExiste = listaMesasDisponibles.stream()
                    .anyMatch(m -> m.getId().equals(mesaSeleccionada.getId()));
            if (aunExiste) {
                cmbMesaSelect.getSelectionModel().select(mesaSeleccionada);
            } else {
                mesaSeleccionada = null;
                cmbMesaSelect.getSelectionModel().clearSelection();
            }
        } else {
            cmbMesaSelect.getSelectionModel().clearSelection();
        }
    }

    /**
     * Después de cargar salones desde backend y antes de mostrar la ventana
     * ajustamos "qué está seleccionado" en los combos si veníamos de VistaSalones.
     */
    private void inicializarSeleccionMesa() {
        // 1. Seleccionar el salón que venía del contexto, si existe
        if (salonSeleccionado != null) {
            // buscar ese salón en listaSalonesDisponibles
            for (Salon s : listaSalonesDisponibles) {
                if (s.getId().equals(salonSeleccionado.getId())) {
                    cmbSalonSelect.getSelectionModel().select(s);
                    salonSeleccionado = s; // usa la misma instancia que está en la lista
                    break;
                }
            }
        }

        // Si no había salón en contexto pero hay al menos uno, no selecciono nada automáticamente,
        // para que el salonero elija manualmente.

        // 2. Ahora que ya hay salón elegido (o no),
        // llenamos mesas según ese salón
        actualizarMesasSegunSalon();

        // 3. Seleccionamos la mesa que venía en el contexto SI pertenece a ese salón.
        if (mesaSeleccionada != null) {
            for (Mesa m : listaMesasDisponibles) {
                if (m.getId().equals(mesaSeleccionada.getId())) {
                    cmbMesaSelect.getSelectionModel().select(m);
                    mesaSeleccionada = m; // usa instancia consistente
                    break;
                }
            }
        }

        // Y terminamos aplicando enable/disable según sea MESA o BARRA
        aplicarHabilitadoMesa();
    }

    // ==================== CARGA DE GRUPOS / PRODUCTOS / ORDEN ====================

    private void cargarGrupos() {
        try {
            String jsonResponse = RestClient.get("/grupos");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                listaGrupos = gson.fromJson(dataJson, new TypeToken<List<GrupoProducto>>(){}.getType());

                cmbGrupos.getItems().clear();
                cmbGrupos.getItems().add(null); // "Todos los grupos"
                cmbGrupos.getItems().addAll(listaGrupos);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cargar grupos:\n" + e.getMessage());
        }
    }

    /**
     * Usa /productos (tu endpoint backend). Manejo defensivo si Payara devuelve HTML.
     */
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
                // No hay orden previa; nueva orden
                ordenActual = new Orden();
                ordenActual.setMesaId(mesaSeleccionada.getId());
                ordenActual.setUsuarioId(AppContext.getInstance().getUsuarioLogueado().getId());
                modoEdicion = false;
                lblEstadoOrden.setText(I18n.isSpanish() ? "Nueva" : "New");
            }
        } catch (Exception e) {
            System.out.println("No hay orden existente para esta mesa");
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
                List<DetalleOrden> detalles = gson.fromJson(dataJson,
                        new TypeToken<List<DetalleOrden>>(){}.getType());

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

        // Hover style
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

        // Click -> agrega producto a la orden
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
        // si ya existe el producto en la tabla, solo sumar cantidad
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

        // si no estaba, crear una línea nueva
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

    // ==================== EVENTOS BOTONES MAIN ====================

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

        // volver al menú del salonero
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

                // Ocupar mesa si es nueva orden MESA
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
        // Aquí más adelante puedes abrir una vista visual de mesas (mapa)
        Mensaje.showInfo(
            I18n.isSpanish() ? "Seleccionar mesa" : "Select table",
            I18n.isSpanish()
                ? "Aquí podrías abrir el mapa de mesas para elegir visualmente."
                : "Here you could open the floor plan to pick a table visually."
        );
    }

    // ==================== ACTUALIZAR CABECERA/INFO ====================

    private void setTipoOrden(TipoOrden tipo) {
        tipoOrdenActual = tipo;

        tgMesa.setSelected(tipoOrdenActual == TipoOrden.MESA);
        tgBarra.setSelected(tipoOrdenActual == TipoOrden.BARRA);

        aplicarHabilitadoMesa();
        actualizarCabeceraVisual();
    }

    /**
     * Habilita o deshabilita los combos Salón/Mesa según si la orden es de mesa o de barra.
     */
    private void aplicarHabilitadoMesa() {
        boolean esMesa = (tipoOrdenActual == TipoOrden.MESA);

        cmbSalonSelect.setDisable(!esMesa);
        cmbMesaSelect.setDisable(!esMesa);
        btnElegirMesa.setDisable(!esMesa);

        if (!esMesa) {
            // al pasar a Barra limpiamos selección de mesa/salón
            salonSeleccionado = null;
            mesaSeleccionada = null;
            cmbSalonSelect.getSelectionModel().clearSelection();
            cmbMesaSelect.getSelectionModel().clearSelection();
        }
    }

    /**
     * Refresca las etiquetas arriba (Mesa:, Salón:, etc.).
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
}
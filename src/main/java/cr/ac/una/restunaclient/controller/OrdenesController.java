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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.scene.layout.HBox;

/**
 * ⭐ CONTROLADOR DE ÓRDENES MEJORADO
 */
public class OrdenesController implements Initializable {

    @FXML private Label lblTitle;
    @FXML private Label lblUsuario;
    @FXML private Button btnVolver;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelarOrden;
    @FXML private Label lblUbicacionTipo;
    @FXML private Label lblUbicacionDetalle;
    @FXML private Label lblFechaHora;
    @FXML private Label lblSelectorBarra;
    @FXML private ComboBox<Salon> cmbBarraSelect;
    @FXML private TextArea txtObservaciones;
    @FXML private TextField txtBuscarProducto;
    @FXML private ComboBox<GrupoProducto> cmbGrupos;
    @FXML private FlowPane flowProductos;
    @FXML private TableView<DetalleOrden> tblDetalles;
    @FXML private TableColumn<DetalleOrden, String> colProducto;
    @FXML private TableColumn<DetalleOrden, Integer> colCantidad;
    @FXML private TableColumn<DetalleOrden, String> colPrecio;
    @FXML private TableColumn<DetalleOrden, String> colSubtotal;
    @FXML private Label lblTotal;
    @FXML private ScrollPane scrollOrdenes;
    @FXML private VBox vboxOrdenes;
    @FXML private HBox hboxSelectorBarra;
    @FXML private Button btnFacturar;
    
    private Orden ordenActual;
    private Mesa mesaSeleccionada;
    private Salon salonSeleccionado;
    private Salon barraSeleccionada;
    private ObservableList<DetalleOrden> detallesOrden;
    private List<GrupoProducto> listaGrupos;
    private List<Producto> listaProductos;
    private final ObservableList<Orden> listaOrdenes = FXCollections.observableArrayList();
    private boolean modoEdicion = false;
    private String modoOrden = "SALON";
    private final ObservableList<Salon> listaBarrasDisponibles = FXCollections.observableArrayList();
    
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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Object modoObj = AppContext.getInstance().get("modoOrden");
        if ("BARRA".equals(modoObj)) {
            modoOrden = "BARRA";
            mesaSeleccionada = null;
            salonSeleccionado = null;
        } else {
            modoOrden = "SALON";
            mesaSeleccionada = (Mesa) AppContext.getInstance().get("mesaSeleccionada");
            salonSeleccionado = (Salon) AppContext.getInstance().get("salonSeleccionado");
        }
        
        configurarTabla();
        configurarCombosProductos();
        configurarSelectorBarra();
        
        if ("BARRA".equals(modoOrden)) {
            cargarBarras();
        }
        
        cargarGrupos();
        cargarProductos();
        cargarOrdenExistente();
        configurarBotonFacturar();
        actualizarHeaderInformativo();
        actualizarTextos();
        cargarListaOrdenes();
        
        txtBuscarProducto.textProperty().addListener((obs, old, val) -> filtrarProductos(val));
    }

    private void configurarTabla() {
        colProducto.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getProducto() != null ? 
                data.getValue().getProducto().getNombre() : "")
        );
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(data -> 
            new SimpleStringProperty(String.format("₡%.2f", data.getValue().getPrecioUnitario()))
        );
        colSubtotal.setCellValueFactory(data -> 
            new SimpleStringProperty(String.format("₡%.2f", data.getValue().getSubtotal()))
        );
        
        detallesOrden = FXCollections.observableArrayList();
        tblDetalles.setItems(detallesOrden);
        
        tblDetalles.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                DetalleOrden detalle = tblDetalles.getSelectionModel().getSelectedItem();
                if (detalle != null) editarCantidad(detalle);
            }
        });
        
        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemEditar = new MenuItem(I18n.isSpanish() ? "✏ Editar cantidad" : "✏ Edit quantity");
        MenuItem itemEliminar = new MenuItem(I18n.isSpanish() ? "🗑 Eliminar" : "🗑 Delete");
        
        itemEditar.setOnAction(e -> {
            DetalleOrden detalle = tblDetalles.getSelectionModel().getSelectedItem();
            if (detalle != null) editarCantidad(detalle);
        });
        itemEliminar.setOnAction(e -> {
            DetalleOrden detalle = tblDetalles.getSelectionModel().getSelectedItem();
            if (detalle != null) eliminarDetalle(detalle);
        });
        
        contextMenu.getItems().addAll(itemEditar, itemEliminar);
        tblDetalles.setContextMenu(contextMenu);
    }

    private void configurarCombosProductos() {
    System.out.println("🔧 Configurando combo de grupos...");
    
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
    
    // ⭐ LISTENER: Detectar cambio de grupo
    cmbGrupos.getSelectionModel().selectedItemProperty().addListener(
        (obs, oldGrupo, newGrupo) -> {
            System.out.println("🔄 Cambio de grupo detectado:");
            System.out.println("   Anterior: " + (oldGrupo != null ? oldGrupo.getNombre() : "null"));
            System.out.println("   Nuevo: " + (newGrupo != null ? newGrupo.getNombre() : "TODOS"));
            
            if (newGrupo != null) {
                filtrarProductosPorGrupo(newGrupo);
            } else {
                System.out.println("📋 Grupo null = mostrar todos");
                mostrarProductos(listaProductos);
            }
        }
    );
    
    System.out.println("✅ Combo de grupos configurado");
}
    
    
    
    private void configurarSelectorBarra() {
        System.out.println("🔧 configurarSelectorBarra() - Modo: " + modoOrden);

        if (hboxSelectorBarra == null) {
            System.err.println("❌ hboxSelectorBarra es NULL - Revisar fx:id en FXML");
            return;
        }

        if (cmbBarraSelect == null) {
            System.err.println("❌ cmbBarraSelect es NULL - Revisar fx:id en FXML");
            return;
        }

        if ("BARRA".equals(modoOrden)) {
            System.out.println("✅ Mostrando selector de barra (modo BARRA)");

            // Mostrar el contenedor completo
            hboxSelectorBarra.setVisible(true);
            hboxSelectorBarra.setManaged(true);
            cmbBarraSelect.setItems(listaBarrasDisponibles);
            cmbBarraSelect.setConverter(new javafx.util.StringConverter<Salon>() {
                @Override
                public String toString(Salon salon) {
                    return salon != null ? salon.getNombre() : "";
                }

                @Override
                public Salon fromString(String string) {
                    return null;
                }
            });

            // Listener para actualizar header cuando cambia la barra
            cmbBarraSelect.getSelectionModel().selectedItemProperty().addListener(
                    (obs, old, nuevaBarra) -> {
                        barraSeleccionada = nuevaBarra;
                        System.out.println("📍 Barra seleccionada: "
                                + (nuevaBarra != null ? nuevaBarra.getNombre() : "null"));
                        actualizarHeaderInformativo();

                        // Feedback visual
                        if (nuevaBarra != null) {
                            cmbBarraSelect.setStyle(
                                    "-fx-border-color: #28a745; -fx-border-width: 2; "
                                    + "-fx-background-color: #d4edda; -fx-border-radius: 5;"
                            );
                        } else {
                            cmbBarraSelect.setStyle("");
                        }
                    }
            );

            System.out.println("✅ Selector configurado. Items: " + listaBarrasDisponibles.size());

        } else {
            System.out.println("⚪ Ocultando selector de barra (modo SALON/MESA)");

            hboxSelectorBarra.setVisible(false);
            hboxSelectorBarra.setManaged(false); // ⭐ CRÍTICO: managed=false para que no ocupe espacio

            // Limpiar selección
            barraSeleccionada = null;
            if (cmbBarraSelect.getSelectionModel() != null) {
                cmbBarraSelect.getSelectionModel().clearSelection();
            }
        }
    }


    private void cargarBarras() {
        try {
            System.out.println("🔄 Cargando barras disponibles...");

            String jsonResponse = RestClient.get("/salones");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (!Boolean.TRUE.equals(response.get("success"))) {
                System.err.println("❌ Error al cargar barras: " + response.get("message"));
                listaBarrasDisponibles.clear();
                return;
            }

            Gson gson = new Gson();
            String dataJson = gson.toJson(response.get("data"));
            List<Salon> salones = gson.fromJson(dataJson, new TypeToken<List<Salon>>() {
            }.getType());

            List<Salon> barras = new ArrayList<>();
            for (Salon salon : salones) {
                if ("BARRA".equals(salon.getTipo()) && "A".equals(salon.getEstado())) {
                    barras.add(salon);
                }
            }

            listaBarrasDisponibles.setAll(barras);

            System.out.println("✅ Barras disponibles: " + barras.size());
            for (Salon b : barras) {
                System.out.println("   📍 " + b.getNombre() + " (ID: " + b.getId() + ")");
            }

            if (barras.size() == 1) {
                cmbBarraSelect.getSelectionModel().selectFirst();
                System.out.println("✅ Auto-seleccionada barra: " + barras.get(0).getNombre());
            } else if (!barras.isEmpty()) {
                cmbBarraSelect.getSelectionModel().selectFirst();
                System.out.println("ℹ Múltiples barras disponibles, seleccione una");
            } else {
                System.err.println("⚠ No hay barras disponibles");
                Mensaje.showWarning(
                        I18n.isSpanish() ? "Sin Barras" : "No Bars",
                        I18n.isSpanish()
                        ? "No hay barras activas en el sistema."
                        : "No active bars in the system."
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error al cargar barras: " + e.getMessage());
            Mensaje.showError("Error", "Error al cargar barras:\n" + e.getMessage());
        }
    }
    
    
    
    
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
        System.out.println("📦 Cargando productos desde el backend...");
        
        String jsonResponse = RestClient.get("/productos");
        
        if (jsonResponse == null || jsonResponse.trim().startsWith("<")) {
            System.err.println("❌ Respuesta inválida del servidor (HTML o null)");
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
            
            System.out.println("✅ Productos cargados: " + listaProductos.size());
            
            // ⭐ DEBUG: Mostrar cada producto con su grupoId
            if (!listaProductos.isEmpty()) {
                System.out.println("📋 Detalle de productos:");
                for (Producto p : listaProductos) {
                    System.out.println("   📌 " + p.getNombre() + 
                        " (ID: " + p.getId() + 
                        ", GrupoID: " + p.getGrupoId() + 
                        ", Precio: " + p.getPrecioFormateado() + ")");
                }
            }
            
            // Mostrar todos los productos inicialmente
            mostrarProductos(listaProductos);
            
        } else {
            System.err.println("❌ Error en respuesta del servidor: " + response.get("message"));
            listaProductos = new ArrayList<>();
            mostrarProductos(listaProductos);
            Mensaje.showWarning("Aviso", String.valueOf(response.get("message")));
        }
        
    } catch (Exception e) {
        e.printStackTrace();
        System.err.println("❌ Excepción al cargar productos: " + e.getMessage());
        listaProductos = new ArrayList<>();
        mostrarProductos(listaProductos);
        Mensaje.showError("Error", "Error al cargar productos:\n" + e.getMessage());
    }
}

    private void cargarOrdenExistente() {
        if ("BARRA".equals(modoOrden) || mesaSeleccionada == null || !mesaSeleccionada.isOcupada()) {
            ordenActual = new Orden();
            
            if ("BARRA".equals(modoOrden)) {
                ordenActual.setMesaId(null);
            } else {
                ordenActual.setMesaId(mesaSeleccionada != null ? mesaSeleccionada.getId() : null);
            }
            
            Usuario logged = AppContext.getInstance().getUsuarioLogueado();
            if (logged != null && logged.getId() != null) {
                ordenActual.setUsuarioId(logged.getId());
            }
            
            modoEdicion = false;
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
            } else {
                ordenActual = new Orden();
                ordenActual.setMesaId(mesaSeleccionada.getId());
                
                Usuario logged = AppContext.getInstance().getUsuarioLogueado();
                if (logged != null && logged.getId() != null) {
                    ordenActual.setUsuarioId(logged.getId());
                }
                
                modoEdicion = false;
            }
        } catch (Exception e) {
            ordenActual = new Orden();
            ordenActual.setMesaId(mesaSeleccionada.getId());
            
            Usuario logged = AppContext.getInstance().getUsuarioLogueado();
            if (logged != null && logged.getId() != null) {
                ordenActual.setUsuarioId(logged.getId());
            }
            
            modoEdicion = false;
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

   private void mostrarProductos(List<Producto> productos) {
    System.out.println("🎨 mostrarProductos() llamado con " + 
        (productos != null ? productos.size() : 0) + " productos");
    
    flowProductos.getChildren().clear();
    
    if (productos == null || productos.isEmpty()) {
        Label lblVacio = new Label(
            I18n.isSpanish() ? "No hay productos disponibles" : "No products available"
        );
        lblVacio.setStyle("-fx-text-fill: #999; -fx-font-size: 14px;");
        flowProductos.getChildren().add(lblVacio);
        System.out.println("⚪ FlowPane vacío mostrado");
        return;
    }
    
    System.out.println("🎨 Renderizando " + productos.size() + " productos en la UI");
    
    for (Producto producto : productos) {
        VBox card = crearBotonProducto(producto);
        flowProductos.getChildren().add(card);
    }
    
    System.out.println("✅ FlowPane actualizado con " + productos.size() + " tarjetas");
}

    private VBox crearBotonProducto(Producto producto) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(120, 110);
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
        lblNombre.setStyle("-fx-text-fill: #333;");
        
        Label lblPrecio = new Label(producto.getPrecioFormateado());
        lblPrecio.setStyle("-fx-text-fill: #FF7A00; -fx-font-size: 14px; -fx-font-weight: bold;");
        
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
    System.out.println("🔍 Buscando: '" + busqueda + "'");
    
    if (listaProductos == null || listaProductos.isEmpty()) {
        System.err.println("⚠ listaProductos vacía");
        mostrarProductos(Collections.emptyList());
        return;
    }
    
    // Si no hay búsqueda, aplicar filtro de grupo si existe
    if (busqueda == null || busqueda.trim().isEmpty()) {
        GrupoProducto grupoSeleccionado = cmbGrupos.getValue();
        if (grupoSeleccionado != null) {
            System.out.println("🔄 Sin búsqueda, aplicando filtro de grupo");
            filtrarProductosPorGrupo(grupoSeleccionado);
        } else {
            System.out.println("📋 Sin búsqueda ni filtro, mostrando todos");
            mostrarProductos(listaProductos);
        }
        return;
    }
    
    String filtro = busqueda.toLowerCase().trim();
    List<Producto> filtrados = new ArrayList<>();
    
    // Buscar en nombre y nombreCorto
    for (Producto p : listaProductos) {
        boolean coincide = false;
        
        if (p.getNombre() != null && p.getNombre().toLowerCase().contains(filtro)) {
            coincide = true;
        }
        
        if (!coincide && p.getNombreCorto() != null && 
            p.getNombreCorto().toLowerCase().contains(filtro)) {
            coincide = true;
        }
        
        // ⭐ También verificar que pertenezca al grupo seleccionado (si hay uno)
        GrupoProducto grupoSeleccionado = cmbGrupos.getValue();
        if (coincide && grupoSeleccionado != null) {
            if (p.getGrupoId() == null || !p.getGrupoId().equals(grupoSeleccionado.getId())) {
                coincide = false; // No pertenece al grupo seleccionado
            }
        }
        
        if (coincide) {
            filtrados.add(p);
        }
    }
    
    System.out.println("📊 Resultados de búsqueda: " + filtrados.size());
    mostrarProductos(filtrados);
}


    private void filtrarProductosPorGrupo(GrupoProducto grupo) {
        System.out.println("🔍 Filtrando por grupo: " + (grupo != null ? grupo.getNombre() : "TODOS"));

        if (listaProductos == null || listaProductos.isEmpty()) {
            System.err.println("⚠ listaProductos está vacía o null");
            mostrarProductos(Collections.emptyList());
            return;
        }

        // Si no hay grupo seleccionado, mostrar todos los productos
        if (grupo == null) {
            System.out.println("📋 Mostrando todos los productos (" + listaProductos.size() + ")");
            mostrarProductos(listaProductos);
            return;
        }

        // Filtrar productos que pertenecen al grupo seleccionado
        List<Producto> filtrados = new ArrayList<>();

        System.out.println("🔎 Buscando productos del grupo ID: " + grupo.getId());

        for (Producto p : listaProductos) {
            // ⭐ VALIDACIÓN: Verificar si el producto pertenece al grupo
            boolean perteneceAlGrupo = false;

            if (p.getGrupoId() != null && p.getGrupoId().equals(grupo.getId())) {
                perteneceAlGrupo = true;
                System.out.println("   ✅ " + p.getNombre() + " (grupoId=" + p.getGrupoId() + ") → pertenece");
            } else {
                System.out.println("   ⚪ " + p.getNombre() + " (grupoId=" + p.getGrupoId() + ") → NO pertenece");
            }

            if (perteneceAlGrupo) {
                filtrados.add(p);
            }
        }

        System.out.println("📊 Productos filtrados: " + filtrados.size() + " de " + listaProductos.size());

        // ⭐ CRÍTICO: Mostrar los productos filtrados
        mostrarProductos(filtrados);

        // Feedback si no hay productos en el grupo
        if (filtrados.isEmpty()) {
            System.err.println("⚠ No hay productos activos en el grupo: " + grupo.getNombre());
        }
    }

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

 
    @FXML
    private void onVolver(ActionEvent event) {
        if (!detallesOrden.isEmpty()) {
            boolean confirmar = Mensaje.showConfirmation(
                    I18n.isSpanish() ? "¿Salir sin guardar?" : "Exit without saving?",
                    I18n.isSpanish()
                    ? "Hay productos en la orden actual que no se han guardado.\n\n"
                    + "¿Está seguro de salir? Los cambios se perderán."
                    : "There are products in the current order that have not been saved.\n\n"
                    + "Are you sure you want to exit? Changes will be lost."
            );

            if (!confirmar) {
                return; 
            }
        }

        AppContext.getInstance().remove("modoOrden");
        AppContext.getInstance().remove("mesaSeleccionada");
        AppContext.getInstance().remove("salonSeleccionado");

        if ("BARRA".equals(modoOrden)) {
            System.out.println("⬅ Regresando a Menú Principal (modo barra)");
            FlowController.getInstance().goToView(
                    "MenuPrincipal",
                    "RestUNA - Menú Principal",
                    1000,
                    560
            );
        } else {
            System.out.println("⬅ Regresando a Vista de Salones");
            FlowController.getInstance().goToView(
                    "VistaSalones",
                    "RestUNA - Salones",
                    1400,
                    800
            );
        }
    }

    @FXML
    private void onGuardar(ActionEvent event) {
        if (detallesOrden.isEmpty()) {
            Mensaje.showWarning(
                    I18n.isSpanish() ? "Aviso" : "Warning",
                    I18n.isSpanish()
                    ? "Debe agregar al menos un producto a la orden"
                    : "Must add at least one product to the order"
            );
            return;
        }

        if ("BARRA".equals(modoOrden)) {
            if (barraSeleccionada == null) {
                Mensaje.showWarning(
                        I18n.isSpanish() ? "Aviso" : "Warning",
                        I18n.isSpanish()
                        ? "Debe seleccionar una barra antes de guardar"
                        : "You must select a bar before saving"
                );
                cmbBarraSelect.setStyle(
                        "-fx-border-color: #dc3545; -fx-border-width: 2; "
                        + "-fx-background-color: #f8d7da; -fx-border-radius: 5;"
                );
                cmbBarraSelect.requestFocus();
                return;
            }
            ordenActual.setMesaId(null); 
        } 
        else {
            if (mesaSeleccionada == null) {
                Mensaje.showWarning(
                        I18n.isSpanish() ? "Aviso" : "Warning",
                        I18n.isSpanish()
                        ? "Debe seleccionar una mesa"
                        : "You must select a table"
                );
                return;
            }
            ordenActual.setMesaId(mesaSeleccionada.getId());
        }

        try {
            
            Usuario logged = AppContext.getInstance().getUsuarioLogueado();
            if (logged == null || logged.getId() == null) {
                Mensaje.showError("Error", "No hay usuario logueado.");
                return;
            }

            ordenActual.setUsuarioId(logged.getId());
            ordenActual.setObservaciones(txtObservaciones.getText());

            
            List<DetalleOrden> detallesParaEnviar = new ArrayList<>();
            for (DetalleOrden detalle : detallesOrden) {
                DetalleOrden detalleDTO = new DetalleOrden();

                if (detalle.getProducto() != null && detalle.getProducto().getId() != null) {
                    detalleDTO.setProductoId(detalle.getProducto().getId());
                } else if (detalle.getProductoId() != null) {
                    detalleDTO.setProductoId(detalle.getProductoId());
                } else {
                    System.err.println("⚠ Detalle sin productoId: " + detalle);
                    continue;
                }

                detalleDTO.setCantidad(detalle.getCantidad());
                detalleDTO.setPrecioUnitario(detalle.getPrecioUnitario());
                detalleDTO.calcularSubtotal();

                detallesParaEnviar.add(detalleDTO);
            }

            if (detallesParaEnviar.isEmpty()) {
                Mensaje.showError("Error", "No se pudieron preparar los detalles de la orden");
                return;
            }

            ordenActual.setDetalles(detallesParaEnviar);

            
            System.out.println("📤 Guardando orden:");
            System.out.println("   - Modo: " + modoOrden);
            System.out.println("   - Mesa ID: " + ordenActual.getMesaId());
            System.out.println("   - Barra: " + (barraSeleccionada != null ? barraSeleccionada.getNombre() : "N/A"));
            System.out.println("   - Usuario ID: " + ordenActual.getUsuarioId());
            System.out.println("   - Detalles: " + detallesParaEnviar.size());

            
            String jsonResponse;
            if (modoEdicion && ordenActual.getId() != null) {
                System.out.println("   - Actualizando orden existente ID: " + ordenActual.getId());
                jsonResponse = RestClient.put("/ordenes/" + ordenActual.getId(), ordenActual);
            } else {
                System.out.println("   - Creando nueva orden");
                jsonResponse = RestClient.post("/ordenes", ordenActual);
            }

            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (Boolean.TRUE.equals(response.get("success"))) {
                System.out.println("✅ Orden guardada exitosamente");

                
                if (!modoEdicion && "SALON".equals(modoOrden) && mesaSeleccionada != null) {
                    try {
                        RestClient.post("/mesas/" + mesaSeleccionada.getId() + "/ocupar", null);
                        System.out.println("✅ Mesa marcada como ocupada");
                    } catch (Exception e) {
                        System.err.println("⚠ No se pudo marcar mesa como ocupada: " + e.getMessage());
                    }
                }

                
                Mensaje.showSuccess(
                        I18n.isSpanish() ? "¡Éxito!" : "Success!",
                        I18n.isSpanish()
                        ? "✅ Orden guardada correctamente\n\n Si desea, puede crear otra orden"
                        : "✅ Order saved successfully\n\nWould you like to create another order?"
                );

                limpiarFormularioParaNuevaOrden();
                actualizarEstadoBotonFacturar();

                cargarListaOrdenes();

                System.out.println("✅ Formulario limpio y listo para nueva orden");

            } else {
                System.err.println("❌ Error del servidor: " + response.get("message"));
                Mensaje.showError("Error", response.get("message").toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Excepción al guardar orden: " + e.getMessage());
            Mensaje.showError("Error", "Error al guardar orden:\n" + e.getMessage());
        }
    }

    
    private void limpiarFormularioParaNuevaOrden() {
        // Limpiar detalles
        detallesOrden.clear();

        // Limpiar observaciones
        txtObservaciones.clear();

        // Resetear orden actual
        ordenActual = new Orden();

        // Mantener el contexto de ubicación según modo
        if ("BARRA".equals(modoOrden)) {
            ordenActual.setMesaId(null);
            // NO limpiar barraSeleccionada para facilitar múltiples órdenes en la misma barra
        } else {
            if (mesaSeleccionada != null) {
                ordenActual.setMesaId(mesaSeleccionada.getId());
            }
        }

        // Asignar usuario actual
        Usuario logged = AppContext.getInstance().getUsuarioLogueado();
        if (logged != null && logged.getId() != null) {
            ordenActual.setUsuarioId(logged.getId());
        }

        // Modo creación (no edición)
        modoEdicion = false;

        // Recalcular total
        calcularTotal();

        // Actualizar header
        actualizarHeaderInformativo();
    }

    @FXML
    private void onCancelarOrden(ActionEvent event) {
        if (ordenActual == null || ordenActual.getId() == null) {
            // Si no hay orden guardada, solo limpiar
            detallesOrden.clear();
            txtObservaciones.clear();
            calcularTotal();
            Mensaje.showInfo("Aviso", "Orden limpiada");
            return;
        }

        boolean confirmar = Mensaje.showConfirmation(
                I18n.isSpanish() ? "Confirmar Cancelación" : "Confirm Cancellation",
                I18n.isSpanish()
                ? "¿Está seguro de cancelar esta orden?\nEsta acción no se puede deshacer."
                : "Are you sure you want to cancel this order?\nThis action cannot be undone."
        );

        if (!confirmar) {
            return;
        }

        try {
            String jsonResponse = RestClient.post("/ordenes/" + ordenActual.getId() + "/cancelar", null);
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (Boolean.TRUE.equals(response.get("success"))) {
                Mensaje.showSuccess("Éxito", I18n.isSpanish()
                        ? "Orden cancelada correctamente"
                        : "Order cancelled successfully");

                if (mesaSeleccionada != null && "SALON".equals(modoOrden)) {
                    try {
                        RestClient.post("/salones/mesas/" + mesaSeleccionada.getId() + "/liberar", null);
                    } catch (Exception ignore) {
                    }
                    try {
                        RestClient.post("/mesas/" + mesaSeleccionada.getId() + "/liberar", null);
                    } catch (Exception ignore) {
                    }
                }

                // ⭐ CAMBIO: NO salir, solo limpiar
                detallesOrden.clear();
                txtObservaciones.clear();
                ordenActual = new Orden();
                modoEdicion = false;
                calcularTotal();

            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cancelar orden:\n" + e.getMessage());
        }
    }

    private void actualizarHeaderInformativo() {
        Usuario usuario = AppContext.getInstance().getUsuarioLogueado();
        if (usuario != null) {
            lblUsuario.setText(
                    (I18n.isSpanish() ? "Usuario: " : "User: ") + usuario.getNombre()
            );
        }

        if ("BARRA".equals(modoOrden)) {
            lblUbicacionTipo.setText(I18n.isSpanish() ? "🍹 Orden de Barra" : "🍹 Bar Order");
            lblUbicacionTipo.setStyle(
                    "-fx-font-weight: bold; -fx-text-fill: #FF7A00; -fx-font-size: 16px;"
            );

            if (barraSeleccionada != null) {
                lblUbicacionDetalle.setText(
                        (I18n.isSpanish() ? "Barra: " : "Bar: ") + barraSeleccionada.getNombre()
                );
                lblUbicacionDetalle.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
            } else {
                lblUbicacionDetalle.setText(
                        I18n.isSpanish()
                        ? "⚠ Debe seleccionar una barra"
                        : "⚠ You must select a bar"
                );
                lblUbicacionDetalle.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 14px;");
            }
        } // ✅ MODO SALÓN: Mostrar información de mesa y salón
        else {
            if (mesaSeleccionada != null) {
                lblUbicacionTipo.setText(
                        (I18n.isSpanish() ? "🪑 Mesa: " : "🪑 Table: ")
                        + mesaSeleccionada.getIdentificador()
                );
                lblUbicacionTipo.setStyle(
                        "-fx-font-weight: bold; -fx-text-fill: #FF7A00; -fx-font-size: 16px;"
                );

                if (salonSeleccionado != null) {
                    lblUbicacionDetalle.setText(
                            (I18n.isSpanish() ? "Salón: " : "Room: ")
                            + salonSeleccionado.getNombre()
                    );
                    lblUbicacionDetalle.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
                } else {
                    lblUbicacionDetalle.setText(I18n.isSpanish() ? "Salón: —" : "Room: —");
                    lblUbicacionDetalle.setStyle("-fx-text-fill: #999; -fx-font-size: 14px;");
                }
            } else {
                lblUbicacionTipo.setText(
                        I18n.isSpanish() ? "🍽 Nueva Orden" : "🍽 New Order"
                );
                lblUbicacionTipo.setStyle(
                        "-fx-font-weight: bold; -fx-text-fill: #FF7A00; -fx-font-size: 16px;"
                );

                lblUbicacionDetalle.setText(
                        I18n.isSpanish() ? "Sin mesa asignada" : "No table assigned"
                );
                lblUbicacionDetalle.setStyle("-fx-text-fill: #999; -fx-font-size: 14px;");
            }
        }

        // ✅ Actualizar fecha/hora (siempre visible)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        if (ordenActual != null && ordenActual.getFechaHora() != null) {
            lblFechaHora.setText(ordenActual.getFechaHora().format(formatter));
        } else {
            lblFechaHora.setText(LocalDateTime.now().format(formatter));
        }
    }

    private void actualizarTextos() {
        boolean esEspanol = I18n.isSpanish();
        
        if ("BARRA".equals(modoOrden)) {
            lblTitle.setText(esEspanol ? "📝 Orden de Barra" : "📝 Bar Order");
        } else {
            lblTitle.setText(esEspanol ? "📝 Gestión de Orden" : "📝 Order Management");
        }
        
        btnVolver.setText(esEspanol ? "← Volver" : "← Back");
        btnGuardar.setText(esEspanol ? "💾 Guardar Orden" : "💾 Save Order");
        btnCancelarOrden.setText(esEspanol ? "❌ Cancelar Orden" : "❌ Cancel Order");
        if (btnFacturar != null) {
            btnFacturar.setText(I18n.isSpanish() ? "💳 Facturar" : "💳 Bill");
}
        
        txtBuscarProducto.setPromptText(esEspanol ? "🔍 Buscar producto..." : "🔍 Search product...");
        cmbGrupos.setPromptText(esEspanol ? "Todos los grupos" : "All groups");
        
        colProducto.setText(esEspanol ? "Producto" : "Product");
        colCantidad.setText(esEspanol ? "Cant." : "Qty");
        colPrecio.setText(esEspanol ? "Precio" : "Price");
        colSubtotal.setText(esEspanol ? "Subtotal" : "Subtotal");
        
        txtObservaciones.setPromptText(esEspanol ? "Observaciones adicionales..." : "Additional notes...");
        
        if (lblSelectorBarra != null && lblSelectorBarra.isVisible()) {
            lblSelectorBarra.setText(esEspanol ? "Seleccione Barra:" : "Select Bar:");
            cmbBarraSelect.setPromptText(esEspanol ? "Seleccione una barra" : "Select a bar");
        }
    }

    private String formatearUbicacion(Orden o) {
        try {
            if (o.getMesa() != null) {
                Mesa m = o.getMesa();
                String mesaTxt = (m.getIdentificador() != null && !m.getIdentificador().isBlank())
                    ? m.getIdentificador()
                    : (m.getId() != null ? String.valueOf(m.getId()) : "—");
                
                String salonTxt = "Salón";
                Long salonIdMesa = null;
                try {
                    salonIdMesa = m.getSalonId();
                } catch (Exception ignore) {}
                
                if (salonIdMesa != null) {
                    salonTxt = "Salón " + salonIdMesa;
                }
                
                return salonTxt + " · Mesa " + mesaTxt;
            }
        } catch (Exception ignore) {}
        
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
            Mensaje.showInfo("Orden", "Orden #" + orden.getId() + " cargada para continuar.");
        } catch (Exception e) {
            Mensaje.showError("Error", "No se pudo cargar la orden seleccionada.");
        }
    }
    
    
/**
 * ⭐ NUEVO: Configura la visibilidad y estado del botón Facturar
 */
private void configurarBotonFacturar() {
    if (btnFacturar == null) {
        System.err.println("⚠ btnFacturar es null - revisar fx:id en FXML");
        return;
    }
    
    // Solo visible para cajeros y administradores
    boolean puedeFacturar = AppContext.getInstance().isCajero() || 
                            AppContext.getInstance().isAdministrador();
    
    btnFacturar.setVisible(puedeFacturar);
    btnFacturar.setManaged(puedeFacturar);
    
    // Deshabilitar si no hay productos
    actualizarEstadoBotonFacturar();
    
    // Listener para actualizar estado cuando cambian los detalles
    detallesOrden.addListener((javafx.collections.ListChangeListener.Change<? extends DetalleOrden> c) -> {
        actualizarEstadoBotonFacturar();
    });
}

/**
 * ⭐ NUEVO: Actualiza el estado del botón Facturar según la orden
 */
private void actualizarEstadoBotonFacturar() {
    if (btnFacturar == null) return;
    
    // Habilitar solo si:
    // 1. Hay una orden guardada (con ID)
    // 2. La orden tiene productos
    // 3. La orden está en estado ABIERTA
    boolean puedeFacturar = ordenActual != null && 
                            ordenActual.getId() != null &&
                            !detallesOrden.isEmpty() &&
                            ordenActual.isAbierta();
    
    btnFacturar.setDisable(!puedeFacturar);
    
    // Feedback visual
    if (puedeFacturar) {
        btnFacturar.setStyle(
            "-fx-background-color: #28a745; -fx-text-fill: white; " +
            "-fx-font-size: 14px; -fx-font-weight: bold; " +
            "-fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 8;"
        );
        btnFacturar.setTooltip(new Tooltip(
            I18n.isSpanish() ? 
            "Proceder a facturación con esta orden" :
            "Proceed to billing with this order"
        ));
    } else {
        btnFacturar.setStyle(
            "-fx-background-color: #6c757d; -fx-text-fill: white; " +
            "-fx-font-size: 14px; -fx-font-weight: bold; " +
            "-fx-padding: 10 20; -fx-background-radius: 8; -fx-opacity: 0.6;"
        );
        btnFacturar.setTooltip(new Tooltip(
            I18n.isSpanish() ? 
            "Debe guardar la orden primero" :
            "Must save the order first"
        ));
    }
}

/**
 * ⭐ NUEVO: Navega a facturación con la orden actual
 */
@FXML
private void onFacturar(ActionEvent event) {
    System.out.println("💳 Facturando orden desde ventana de órdenes");
    
    // Validaciones
    if (ordenActual == null || ordenActual.getId() == null) {
        Mensaje.showWarning(
            I18n.isSpanish() ? "Aviso" : "Warning",
            I18n.isSpanish() ? 
                "Debe guardar la orden antes de facturar" :
                "Must save the order before billing"
        );
        return;
    }
    
    if (detallesOrden.isEmpty()) {
        Mensaje.showWarning(
            I18n.isSpanish() ? "Aviso" : "Warning",
            I18n.isSpanish() ? 
                "La orden no tiene productos para facturar" :
                "The order has no products to bill"
        );
        return;
    }
    
    if (!ordenActual.isAbierta()) {
        Mensaje.showWarning(
            I18n.isSpanish() ? "Aviso" : "Warning",
            I18n.isSpanish() ? 
                "Esta orden ya fue facturada o cancelada" :
                "This order was already billed or cancelled"
        );
        return;
    }
    
    // Confirmar navegación si hay cambios sin guardar
    if (hayCambiosSinGuardar()) {
        boolean confirmar = Mensaje.showConfirmation(
            I18n.isSpanish() ? "Cambios sin guardar" : "Unsaved changes",
            I18n.isSpanish() ? 
                "Hay cambios sin guardar en la orden.\n\n" +
                "¿Desea guardarlos antes de facturar?" :
                "There are unsaved changes in the order.\n\n" +
                "Do you want to save them before billing?"
        );
        
        if (confirmar) {
            // Intentar guardar
            try {
                guardarOrdenActual();
            } catch (Exception e) {
                Mensaje.showError("Error", 
                    I18n.isSpanish() ? 
                    "No se pudo guardar la orden" :
                    "Could not save the order"
                );
                return;
            }
        }
    }
    
    try {
        // ⭐ Configurar contexto para VentanaVentas
        AppContext.getInstance().set("ordenParaFacturar", ordenActual);
        AppContext.getInstance().set("modoFacturacion", "ORDEN"); // ⭐ Indica origen
        
        // También pasar mesa/salón si están disponibles
        if (mesaSeleccionada != null) {
            AppContext.getInstance().set("mesaParaFacturar", mesaSeleccionada);
        }
        if (salonSeleccionado != null) {
            AppContext.getInstance().set("salonParaFacturar", salonSeleccionado);
        }
        
        System.out.println("✅ Contexto configurado para facturación");
        System.out.println("   - Orden ID: " + ordenActual.getId());
        System.out.println("   - Productos: " + detallesOrden.size());
        System.out.println("   - Mesa: " + (mesaSeleccionada != null ? mesaSeleccionada.getIdentificador() : "N/A"));
        
        // Navegar a ventana de facturación
        FlowController.getInstance().goToView(
            "VentanaVentas",
            I18n.isSpanish() ? 
                "Facturación - Orden #" + ordenActual.getId() :
                "Billing - Order #" + ordenActual.getId(),
            1200, 700
        );
        
    } catch (Exception e) {
        e.printStackTrace();
        Mensaje.showError("Error", 
            I18n.isSpanish() ? 
            "Error al navegar a facturación:\n" + e.getMessage() :
            "Error navigating to billing:\n" + e.getMessage()
        );
    }
}

/**
 * ⭐ NUEVO: Verifica si hay cambios sin guardar en la orden
 */
private boolean hayCambiosSinGuardar() {
    // Si no hay orden guardada, siempre hay cambios
    if (ordenActual == null || ordenActual.getId() == null) {
        return !detallesOrden.isEmpty();
    }
    
    // Comparar cantidad de detalles (método simple)
    // En un sistema más robusto, se compararía producto por producto
    try {
        String jsonResponse = RestClient.get("/ordenes/" + ordenActual.getId() + "/detalles");
        Map<String, Object> response = RestClient.parseResponse(jsonResponse);
        
        if (Boolean.TRUE.equals(response.get("success"))) {
            Gson gson = new Gson();
            String dataJson = gson.toJson(response.get("data"));
            List<DetalleOrden> detallesServidor = gson.fromJson(
                dataJson, 
                new TypeToken<List<DetalleOrden>>(){}.getType()
            );
            
            // Comparar tamaños como indicador simple
            return detallesOrden.size() != detallesServidor.size();
        }
    } catch (Exception e) {
        System.err.println("Error verificando cambios: " + e.getMessage());
    }
    
    return false; // Si falla, asumir que no hay cambios
}

/**
 * ⭐ NUEVO: Guarda la orden actual (versión simplificada del método onGuardar)
 */
private void guardarOrdenActual() throws Exception {
    if (detallesOrden.isEmpty()) {
        throw new Exception("No hay productos en la orden");
    }
    
    if ("BARRA".equals(modoOrden)) {
        if (barraSeleccionada == null) {
            throw new Exception("Debe seleccionar una barra");
        }
        ordenActual.setMesaId(null);
    } else {
        if (mesaSeleccionada == null) {
            throw new Exception("Debe seleccionar una mesa");
        }
        ordenActual.setMesaId(mesaSeleccionada.getId());
    }
    
    Usuario logged = AppContext.getInstance().getUsuarioLogueado();
    if (logged == null || logged.getId() == null) {
        throw new Exception("No hay usuario logueado");
    }
    
    ordenActual.setUsuarioId(logged.getId());
    ordenActual.setObservaciones(txtObservaciones.getText());
    
    List<DetalleOrden> detallesParaEnviar = new ArrayList<>();
    for (DetalleOrden detalle : detallesOrden) {
        DetalleOrden detalleDTO = new DetalleOrden();
        
        if (detalle.getProducto() != null && detalle.getProducto().getId() != null) {
            detalleDTO.setProductoId(detalle.getProducto().getId());
        } else if (detalle.getProductoId() != null) {
            detalleDTO.setProductoId(detalle.getProductoId());
        } else {
            continue;
        }
        
        detalleDTO.setCantidad(detalle.getCantidad());
        detalleDTO.setPrecioUnitario(detalle.getPrecioUnitario());
        detalleDTO.calcularSubtotal();
        
        detallesParaEnviar.add(detalleDTO);
    }
    
    if (detallesParaEnviar.isEmpty()) {
        throw new Exception("No se pudieron preparar los detalles");
    }
    
    ordenActual.setDetalles(detallesParaEnviar);
    
    String jsonResponse;
    if (modoEdicion && ordenActual.getId() != null) {
        jsonResponse = RestClient.put("/ordenes/" + ordenActual.getId(), ordenActual);
    } else {
        jsonResponse = RestClient.post("/ordenes", ordenActual);
    }
    
    Map<String, Object> response = RestClient.parseResponse(jsonResponse);
    
    if (!Boolean.TRUE.equals(response.get("success"))) {
        throw new Exception(response.get("message").toString());
    }
    
    // Actualizar ID si es nueva
    if (ordenActual.getId() == null) {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonDeserializer<LocalDateTime>)
                (je, t, ctx) -> LocalDateTime.parse(je.getAsString()))
            .create();
        String dataJson = gson.toJson(response.get("data"));
        Orden ordenGuardada = gson.fromJson(dataJson, Orden.class);
        ordenActual.setId(ordenGuardada.getId());
    }
    
    System.out.println("✅ Orden guardada antes de facturar: ID " + ordenActual.getId());
}

    
}
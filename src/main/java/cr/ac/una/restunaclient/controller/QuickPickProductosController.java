package cr.ac.una.restunaclient.controller;

import cr.ac.una.restunaclient.model.Producto;
import cr.ac.una.restunaclient.service.RestClient;
import cr.ac.una.restunaclient.util.I18n;
import cr.ac.una.restunaclient.util.Mensaje;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Punto 15:
 * - Muestra GRUPOS ordenados por cantidad de ventas (desc).
 * - Al tocar un grupo, muestra sus PRODUCTOS ordenados por ventas (desc).
 * - Interfaz táctil: botones grandes en TilePane.
 * - Sin “Top 10/20/50”.
 */
public class QuickPickProductosController implements Initializable {

    @FXML private Label lblTitulo;
    @FXML private TextField txtBuscar;
    @FXML private ListView<Map<String,Object>> listGrupos; // cada item: {id, nombre, ventas, productos?}
    @FXML private Label lblGrupoActual;
    @FXML private TilePane tileProductos;
    @FXML private Spinner<Integer> spCantidad;
    @FXML private Button btnAgregar, btnCancelar;

    private final Gson gson = new Gson();

    // selección
    private Producto productoSeleccionado;
    private int cantidadSeleccionada = 1;

    // catálogo cargado del grupo actual (para filtrar por texto)
    private List<Producto> productosDelGrupo = new ArrayList<>();

    // cache productos por grupo (si /grupos/ventas ya los trae)
    private final Map<Long, List<Producto>> productosPorGrupo = new HashMap<>();

    @Override
public void initialize(URL url, ResourceBundle rb) {
    lblTitulo.setText(I18n.isSpanish()
            ? "Menú rápido de productos (ordenado por ventas)"
            : "Quick Product Menu (sorted by sales)");

    // Táctil: spinner desde 1
    spCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));

    // ===== ListView táctil =====
    listGrupos.setFixedCellSize(60); // alto mayor para tacto
    listGrupos.setStyle("-fx-font-size: 16px;"); // fuente grande
    listGrupos.setCellFactory(lv -> new ListCell<>() {
        @Override protected void updateItem(Map<String,Object> item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(String.valueOf(item.getOrDefault("nombre", "—")));
            }
        }
    });

    // ===== Tile táctil =====
    tileProductos.setHgap(16);
    tileProductos.setVgap(16);
    tileProductos.setPadding(new Insets(12));
    tileProductos.setPrefTileWidth(220);
    tileProductos.setPrefTileHeight(140);

    cargarGruposOrdenados();
    configurarEventos();
}

    private void configurarEventos() {
    // ➊ Cambia por esto: recarga al cambiar selección
    listGrupos.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
        cargarProductosDesdeItemSeleccionado(newV);
    });

    // ➋ (Extra robustez) También recarga al hacer click en el item
    listGrupos.setOnMouseClicked(e -> {
        var sel = listGrupos.getSelectionModel().getSelectedItem();
        if (sel != null) cargarProductosDesdeItemSeleccionado(sel);
    });

    txtBuscar.textProperty().addListener((obs, o, n) -> renderProductosFiltrados(n));
}

    
    
    
    @SuppressWarnings("unchecked")
private void cargarProductosDesdeItemSeleccionado(Map<String,Object> item) {
    if (item == null) return;

    lblGrupoActual.setText(String.valueOf(item.getOrDefault("nombre", "—")));
    Long grupoId = parseLong(item.get("id"));

    // 1) Intentar con los productos embebidos del endpoint /grupos/ventas
    List<Producto> productos = extraerProductos(item.get("productos"));

    // 2) Fallback: si vienen vacíos, pedirlos al WS por grupo (endpoint común)
    if (productos.isEmpty() && grupoId != null) {
        try {
            // Usa el que tengas implementado: prueba primero /productos?grupoId=
            String res = RestClient.get("/productos?grupoId=" + grupoId);
            Map<String,Object> body = RestClient.parseResponse(res);
            if (Boolean.TRUE.equals(body.get("success"))) {
                String dataJson = gson.toJson(body.get("data"));
                productos = gson.fromJson(dataJson, new com.google.gson.reflect.TypeToken<List<Producto>>(){}.getType());
            }
        } catch (Exception ignored) {}
    }

    // Orden por ventas desc (si no hay totalVentas, queda 0)
    productos = safe(productos).stream()
            .sorted((p1,p2) -> Long.compare(getVentas(p2), getVentas(p1)))
            .collect(Collectors.toList());

    productosDelGrupo = productos;
    renderProductosFiltrados(txtBuscar.getText());

    if (productosDelGrupo.isEmpty()) {
        Mensaje.showWarning("Productos", "Este grupo no tiene productos activos.");
    }
}

@SuppressWarnings("unchecked")
private List<Producto> extraerProductos(Object prodsObj) {
    if (prodsObj == null) return new ArrayList<>();
    try {
        String json = gson.toJson(prodsObj); // convierte LinkedTreeMap -> JSON
        java.lang.reflect.Type t = new com.google.gson.reflect.TypeToken<List<Producto>>(){}.getType();
        return gson.fromJson(json, t);
    } catch (Exception e) {
        e.printStackTrace();
        return new ArrayList<>();
    }
}
    
    
    


    private void instalarCellFactoryGrupos() {
        listGrupos.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String nombre = String.valueOf(item.getOrDefault("nombre", "—"));
                    long ventas = parseLong(item.get("ventas"));
                    setText(ventas > 0 ? (nombre + "  —  " + I18n.get("facturacion.ventas") + ": " + ventas)
                                       : nombre);
                }
            }
        });
    }

    /* === REST helpers (ajusta endpoints según tu WS) ====================== */

    private void cargarGruposOrdenados() {
    try {
        String res = RestClient.get("/grupos/ventas");
        Map<String,Object> body = RestClient.parseResponse(res);

        List<Map<String,Object>> grupos = new ArrayList<>();
        if (Boolean.TRUE.equals(body.get("success"))) {
            String dataJson = gson.toJson(body.get("data"));
            Type t = new TypeToken<List<Map<String,Object>>>(){}.getType();
            grupos = gson.fromJson(dataJson, t);
        }

        // ⚠️ Orden por totalVentasGrupo (así lo devuelve el WS)
        grupos = safe(grupos).stream()
                .sorted((a,b) -> Long.compare(
                        parseLong(b.get("totalVentasGrupo")),
                        parseLong(a.get("totalVentasGrupo"))))
                .collect(Collectors.toList());

        listGrupos.getItems().setAll(grupos);
        if (!grupos.isEmpty()) {
            listGrupos.getSelectionModel().select(0); // dispara el listener y pinta productos
        }
    } catch (Exception e) {
        e.printStackTrace();
        Mensaje.showError("Grupos", "No fue posible cargar los grupos.");
    }
}

    private void cargarProductosDesdeGrupoSeleccion(Map<String,Object> grupoItem, Long grupoId) {
        productosDelGrupo.clear();
        tileProductos.getChildren().clear();
        productoSeleccionado = null;
        if (grupoId == null) return;

        // 1) Si ya tenemos productos del backend en cache, úsalo
        List<Producto> cache = productosPorGrupo.get(grupoId);
        if (cache != null && !cache.isEmpty()) {
            productosDelGrupo = cache;
            renderProductosFiltrados(txtBuscar.getText());
            return;
        }

        // 2) Fallback: tu endpoint existente /productos/ventas?grupoId=#
        cargarProductosDelGrupoOrdenados(grupoId);
    }

    private void cargarProductosDelGrupoOrdenados(Long grupoId) {
        productosDelGrupo.clear();
        tileProductos.getChildren().clear();
        productoSeleccionado = null;

        if (grupoId == null) return;

        try {
            String res = RestClient.get("/productos/ventas?grupoId=" + grupoId);
            Map<String,Object> body = RestClient.parseResponse(res);

            List<Producto> productos = new ArrayList<>();
            if (Boolean.TRUE.equals(body.get("success"))) {
                String dataJson = gson.toJson(body.get("data"));
                productos = gson.fromJson(dataJson, new TypeToken<List<Producto>>(){}.getType());
            }

            // Orden local por getTotalVentas() desc
            productos = safe(productos).stream()
                    .sorted((p1,p2) -> Long.compare(getVentas(p2), getVentas(p1)))
                    .collect(Collectors.toList());

            productosDelGrupo = productos;
            renderProductosFiltrados(txtBuscar.getText());
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Productos", "No fue posible cargar los productos del grupo.");
        }
    }

    /* === UI render ======================================================== */

    private void renderProductosFiltrados(String filtro) {
        tileProductos.getChildren().clear();

        String f = (filtro == null ? "" : filtro.trim().toLowerCase());
        List<Producto> lista = productosDelGrupo.stream()
                .filter(p -> f.isBlank() || matches(p, f))
                .collect(Collectors.toList());

        for (Producto p : lista) {
            tileProductos.getChildren().add(crearBotonProducto(p));
        }
        
        if (tileProductos.getChildren().isEmpty()) {
    Label vacio = new Label(I18n.isSpanish() ? "Sin productos" : "No products");
    vacio.setStyle("-fx-text-fill: #888; -fx-font-size: 14px;");
    tileProductos.getChildren().add(vacio);
}
    }

    private Region crearBotonProducto(Producto p) {
    String nombre = (p.getNombre() != null ? p.getNombre() : I18n.get("facturacion.producto"));
    String precio = (p.getPrecio() != null ? p.getPrecio().toPlainString() : "0");

    VBox card = new VBox(8);
    card.setAlignment(Pos.CENTER);
    card.setPadding(new Insets(14));
    card.setPrefSize(220, 140); // tamaño táctil
    card.setStyle("""
        -fx-background-color: white;
        -fx-border-color: #E0E0E0;
        -fx-background-radius: 12;
        -fx-border-radius: 12;
        -fx-cursor: hand;
    """);

    Label emoji = new Label("🍽");
    emoji.setStyle("-fx-font-size: 22px;");

    Label lblNombre = new Label(nombre);
    lblNombre.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #333;");
    lblNombre.setWrapText(true);
    lblNombre.setMaxWidth(200);

    Label lblPrecio = new Label("₡" + precio);
    lblPrecio.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");

    Label lblVentas = new Label("Ventas: " + getVentas(p));
    lblVentas.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

    card.getChildren().addAll(emoji, lblNombre, lblPrecio, lblVentas);

    card.setOnMouseClicked(ev -> selectProductoCard(card, p));
    return card;
}

    private void selectProductoCard(VBox card, Producto p) {
        tileProductos.getChildren().forEach(n ->
                n.setStyle(n.getStyle().replace("-fx-border-color: #3B82F6;", "-fx-border-color: #E0E0E0;")));
        card.setStyle(card.getStyle().replace("-fx-border-color: #E0E0E0;", "-fx-border-color: #3B82F6;"));
        this.productoSeleccionado = p;
    }

    /* === Acciones ========================================================= */

    @FXML
    private void onAgregar() {
        if (productoSeleccionado == null) {
            Mensaje.showWarning("Selección", "Seleccione un producto.");
            return;
        }
        cantidadSeleccionada = spCantidad.getValue() == null ? 1 : spCantidad.getValue();
        closeStage(true);
    }

    @FXML
    private void onCancelar() {
        productoSeleccionado = null;
        cantidadSeleccionada = 0;
        closeStage(false);
    }

    private void closeStage(boolean ok) {
        Stage st = (Stage) btnAgregar.getScene().getWindow();
        st.setUserData(ok ? List.of(productoSeleccionado, cantidadSeleccionada) : null);
        st.close();
    }

    /* === Helpers ========================================================== */

    private boolean matches(Producto p, String filtro) {
        String nombre = p.getNombre() != null ? p.getNombre().toLowerCase() : "";
        String corto  = p.getNombreCorto() != null ? p.getNombreCorto().toLowerCase() : "";
        String id     = p.getId() != null ? p.getId().toString() : "";
        return nombre.contains(filtro) || corto.contains(filtro) || id.contains(filtro);
    }

    private long getVentas(Producto p) {
        try {
            var m = p.getClass().getMethod("getTotalVentas");
            Object v = m.invoke(p);
            if (v instanceof Number) return ((Number) v).longValue();
            if (v != null) return Long.parseLong(v.toString());
        } catch (Exception ignored) {}
        return 0L;
    }

    private long parseLong(Object o) {
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0L; }
    }

    private <T> List<T> safe(List<T> in) { return in == null ? new ArrayList<>() : in; }

    @SuppressWarnings("unchecked")
    private List<Map<String,Object>> extractListOfMaps(Object value) {
        try {
            String json = gson.toJson(value);
            Type t = new TypeToken<List<Map<String,Object>>>(){}.getType();
            return gson.fromJson(json, t);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Mapea un objeto {id,nombre,precio,totalVentas} del backend a tu modelo Producto
     * (sin crear DTOs nuevos).
     */
    private Producto mapearProductoVMaProducto(Map<String,Object> vm) {
        Producto p = new Producto();
        try {
            // id
            Long id = parseLong(vm.get("id"));
            p.setId(id);

            // nombre
            String nombre = String.valueOf(vm.getOrDefault("nombre", ""));
            p.setNombre(nombre);
            p.setNombreCorto(nombre); // por si el UI filtra por nombre corto

            // precio
            BigDecimal precio = BigDecimal.ZERO;
            Object pr = vm.get("precio");
            if (pr != null) {
                try { precio = new BigDecimal(String.valueOf(pr)); } catch (Exception ignored) {}
            }
            p.setPrecio(precio);

            // ventas -> set por reflexión si tu modelo tiene setTotalVentas; si no, lo ignora
            Object tv = vm.get("totalVentas");
            if (tv != null) {
                try {
                    var m = p.getClass().getMethod("setTotalVentas", Long.class);
                    m.invoke(p, Long.valueOf(String.valueOf(tv)));
                } catch (Exception ignored) {}
            }

            // estado por si el UI lo consulta
            p.setEstado("A");
        } catch (Exception ignored) {}
        return p;
    }

    /** Devuelve el resultado (producto, cantidad) si la ventana ya cerró con éxito. */
    @SuppressWarnings("unchecked")
    public static Optional<Resultado> getResultFromStage(Stage stage) {
        Object ud = stage.getUserData();
        if (ud instanceof List<?> list && list.size() == 2 && list.get(0) instanceof Producto && list.get(1) instanceof Integer) {
            return Optional.of(new Resultado((Producto) list.get(0), (Integer) list.get(1)));
        }
        return Optional.empty();
    }

    public record Resultado(Producto producto, int cantidad) {}
}
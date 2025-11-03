package cr.ac.una.restunaclient.controller;

import cr.ac.una.restunaclient.model.CierreCaja;
import cr.ac.una.restunaclient.util.FlowController;
import cr.ac.una.restunaclient.util.AppContext;
import cr.ac.una.restunaclient.service.RestClient;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
    // imports nuevos
import java.util.concurrent.ConcurrentHashMap;

import java.net.URL;
import java.io.File;
import java.time.LocalDate;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import java.math.BigDecimal;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.List;

public class CierresCajaController implements Initializable {

    // ============ FXML ============
    @FXML private Label lblTitle;
    @FXML private Button btnVolver;

    @FXML private Label lblEstadoCaja;
    @FXML private Label lblCajero;
    @FXML private Label lblFechaApertura;
    @FXML private Label lblEfectivoSistema;
    @FXML private Label lblTarjetaSistema;
    @FXML private Label lblTotalSistema;

    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;
    @FXML private ComboBox<String> cmbFiltroCajero; // lo dejaremos solo con “Todos” (real)
    @FXML private Button btnBuscar;

    @FXML private TableView<CierreCaja> tblCierres;
    @FXML private TableColumn<CierreCaja, String> colFechaApertura;
    @FXML private TableColumn<CierreCaja, String> colFechaCierre;
    @FXML private TableColumn<CierreCaja, String> colCajero; // mostraremos usuarioId (real)
    @FXML private TableColumn<CierreCaja, String> colEfectivoSistema;
    @FXML private TableColumn<CierreCaja, String> colTarjetaSistema;
    @FXML private TableColumn<CierreCaja, String> colEfectivoDeclarado;
    @FXML private TableColumn<CierreCaja, String> colTarjetaDeclarado;
    @FXML private TableColumn<CierreCaja, String> colDiferenciaTotal;
    @FXML private TableColumn<CierreCaja, String> colEstado;

    @FXML private Button btnVerDetalle;
    @FXML private Button btnGenerarReporte;
    @FXML private Button btnRefrescar;

    @FXML private Label lblFormTitle;
    @FXML private Label lblInfoCajero;
    @FXML private Label lblInfoApertura;
    @FXML private Label lblInfoFacturas; // lo ocultamos si no hay conteo real
    @FXML private Label lblSistemaEfectivo;
    @FXML private Label lblSistemaTarjeta;
    @FXML private Label lblSistemaTotal;
    @FXML private Label lblEfectivoDeclarado;
    @FXML private Label lblTarjetaDeclarado;
    @FXML private TextField txtEfectivoDeclarado;
    @FXML private TextField txtTarjetaDeclarado;
    @FXML private VBox vboxDiferencias;
    @FXML private Label lblDiferenciaEfectivo;
    @FXML private Label lblDiferenciaTarjeta;
    @FXML private Label lblDiferenciaTotal;
    @FXML private Button btnCalcular;
    @FXML private Button btnCerrarCaja;
    @FXML private Button btnAbrirCaja;

    // ============ Estado ============
    private final ObservableList<CierreCaja> listaCierres = FXCollections.observableArrayList();
    private CierreCaja cierreActual;
    private final DecimalFormat formatoMoneda = new DecimalFormat("#,##0.00");
    private final DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");
    private final Gson gson = RestClient.getGson();

    private Long getUsuarioId() {
        // Caso típico en tu app: AppContext guarda el usuario logueado
        Object u = AppContext.getInstance().get("UsuarioId");
        if (u instanceof Long) return (Long) u;
        if (u instanceof Integer) return ((Integer) u).longValue();
        // Si tu AppContext guarda el DTO completo, ajusto aquí a como lo tengas:
        // p.ej. UsuarioDto dto = (UsuarioDto) AppContext.getInstance().get("Usuario");
        // return dto.getId();
        return null;
    }

    // añade este campo
private final cr.ac.una.restunaclient.service.ReportesService reportesServiceCli =
        new cr.ac.una.restunaclient.service.ReportesService();

@FXML
    private void onGenerarReporte() {
        CierreCaja sel = tblCierres.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("Selección requerida", "Seleccione un cierre.", Alert.AlertType.WARNING);
            return;
        }
        try {
            File pdf = reportesServiceCli.cierreByIdPdf(sel.getId());
            RestClient.openFile(pdf);
        } catch (Exception ex) {
            ex.printStackTrace();
            mostrarAlerta("Error", "No se pudo generar el reporte del cierre.\n" + ex.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTabla();
        configurarValidaciones();
        configurarUiInicial();
        cargarDatosIniciales();   // solo llena combo con "Todos"
        refrescarTodo();          // carga real (caja actual + historial)
    }

    private void configurarTabla() {
        colFechaApertura.setCellValueFactory(cd ->
            new SimpleStringProperty(safeDate(cd.getValue().getFechaApertura()))
        );
        colFechaCierre.setCellValueFactory(cd ->
            new SimpleStringProperty(safeDate(cd.getValue().getFechaCierre()))
        );
        // Mostramos usuarioId (dato real). Si luego quieres el nombre, expongo endpoint de usuarios.
        // En configurarTabla() – reemplaza el setCellValueFactory de colCajero:
colCajero.setCellValueFactory(cd ->
    new SimpleStringProperty(getNombreUsuario(cd.getValue().getUsuarioId()))
);



        colEfectivoSistema.setCellValueFactory(cd ->
            new SimpleStringProperty("₡" + safeMoney(cd.getValue().getEfectivoSistema()))
        );
        colTarjetaSistema.setCellValueFactory(cd ->
            new SimpleStringProperty("₡" + safeMoney(cd.getValue().getTarjetaSistema()))
        );
        colEfectivoDeclarado.setCellValueFactory(cd ->
            new SimpleStringProperty("₡" + safeMoney(cd.getValue().getEfectivoDeclarado()))
        );
        colTarjetaDeclarado.setCellValueFactory(cd ->
            new SimpleStringProperty("₡" + safeMoney(cd.getValue().getTarjetaDeclarado()))
        );

        colDiferenciaTotal.setCellValueFactory(cd -> {
            BigDecimal difEf = nz(cd.getValue().getDiferenciaEfectivo());
            BigDecimal difTj = nz(cd.getValue().getDiferenciaTarjeta());
            BigDecimal total = difEf.add(difTj);
            String s = "₡" + formatoMoneda.format(total.abs());
            return new SimpleStringProperty(total.signum() > 0 ? "+" + s : total.signum() < 0 ? "-" + s : s);
        });

        // colores (dinámicos, no “adornos”)
        colDiferenciaTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if (item.startsWith("+")) setStyle("-fx-text-fill:#007bff;-fx-font-weight:bold;");
                else if (item.startsWith("-")) setStyle("-fx-text-fill:#dc3545;-fx-font-weight:bold;");
                else setStyle("-fx-text-fill:#28a745;-fx-font-weight:bold;");
            }
        });

        colEstado.setCellValueFactory(cd -> new SimpleStringProperty(nzStr(cd.getValue().getEstado())));
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.equals("ABIERTO")
                        ? "-fx-text-fill:#28a745;-fx-font-weight:bold;"
                        : "-fx-text-fill:#6c757d;-fx-font-weight:bold;");
            }
        });

        tblCierres.setItems(listaCierres);
    }
    
    


private final Map<Long, String> cacheNombres = new ConcurrentHashMap<>();

private String getNombreUsuario(Long id) {
    if (id == null) return "—";
    return cacheNombres.computeIfAbsent(id, k -> {
        try {
            String json = RestClient.get("/usuarios/" + k);   // GET { data: {...} }
            Map<String,Object> resp = gson.fromJson(json, new TypeToken<Map<String,Object>>(){}.getType());
            if (Boolean.TRUE.equals(resp.get("success")) && resp.get("data") != null) {
                Map<?,?> data = (Map<?,?>) resp.get("data");
                Object nom = data.get("nombre");
                return nom == null ? "ID " + k : nom.toString();
            }
        } catch (Exception ignore) {}
        return "ID " + k; // fallback inocuo
    });
}

    private void configurarValidaciones() {
        txtEfectivoDeclarado.textProperty().addListener((obs, ov, nv) -> {
            if (!nv.matches("\\d*(\\.\\d{0,2})?")) txtEfectivoDeclarado.setText(ov);
        });
        txtTarjetaDeclarado.textProperty().addListener((obs, ov, nv) -> {
            if (!nv.matches("\\d*(\\.\\d{0,2})?")) txtTarjetaDeclarado.setText(ov);
        });
    }
    
    
    // helper para mostrar/ocultar
private void setAbrirCajaVisible(boolean show) {
    btnAbrirCaja.setVisible(show);
    btnAbrirCaja.setManaged(show);
}

    private void configurarUiInicial() {
        // Todo vacío/neutral (sin adornos)
        lblEstadoCaja.setText("Estado: —");
        lblCajero.setText("Usuario: —");
        lblFechaApertura.setText("Apertura: —");
        lblEfectivoSistema.setText("Efectivo: —");
        lblTarjetaSistema.setText("Tarjeta: —");
        lblTotalSistema.setText("Total: —");
        setAbrirCajaVisible(false);

        lblInfoCajero.setText("Usuario: —");
        lblInfoApertura.setText("Apertura: —");
        lblInfoFacturas.setVisible(false);
        lblInfoFacturas.setManaged(false);

        vboxDiferencias.setVisible(false);
        vboxDiferencias.setManaged(false);
        btnCerrarCaja.setDisable(true);
        btnCalcular.setDisable(true);
        txtEfectivoDeclarado.setDisable(true);
        txtTarjetaDeclarado.setDisable(true);
    }

    private void cargarDatosIniciales() {
        // Solo “Todos”, porque no tenemos endpoint de cajeros en lo que me pasaste
        cmbFiltroCajero.setItems(FXCollections.observableArrayList("Todos"));
        cmbFiltroCajero.getSelectionModel().selectFirst();
    }

    private void refrescarTodo() {
        cargarCierreActualConTotales();
        cargarHistorialUsuario();
    }

    /** Carga caja abierta con totales reales */
    private void cargarCierreActualConTotales() {
        
        
        cierreActual = null;
actualizarEstadoActual(null);
actualizarFormularioCierre(null);
setAbrirCajaVisible(true);  // <- mostrar botón
        Long usuarioId = getUsuarioId();
        if (usuarioId == null) {
            mostrarAlerta("Sesión", "No se encontró el usuario en sesión.", Alert.AlertType.ERROR);
            return;
        }
        try {
            String url = "/cierres/usuario/" + usuarioId + "/abierto/totales";
            String json = RestClient.get(url);
            Map<String, Object> resp = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
            Object data = resp.get("data");

            if (Boolean.TRUE.equals(resp.get("success")) && data != null) {
                CierreCaja cc = gson.fromJson(gson.toJson(data), CierreCaja.class);
                cierreActual = cc;
                actualizarEstadoActual(cc);
                actualizarFormularioCierre(cc);
            } else {
                cierreActual = null;
                actualizarEstadoActual(null);
                actualizarFormularioCierre(null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            mostrarAlerta("Error", "No se pudo cargar la caja actual.\n" + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /** Historial solo del usuario (real) */
    private void cargarHistorialUsuario() {
        Long usuarioId = getUsuarioId();
        if (usuarioId == null) return;
        try {
            String json = RestClient.get("/cierres/usuario/" + usuarioId);
            Map<String, Object> resp = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
            if (Boolean.TRUE.equals(resp.get("success"))) {
                List<CierreCaja> lista = gson.fromJson(
                        gson.toJson(resp.get("data")),
                        new TypeToken<List<CierreCaja>>(){}.getType());
                listaCierres.setAll(lista);
            } else {
                listaCierres.clear();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            listaCierres.clear();
            mostrarAlerta("Error", "No se pudo cargar el historial.\n" + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    
    
    @FXML
private void onAbrirCaja() {
    Long usuarioId = getUsuarioId();
    if (usuarioId == null) {
        mostrarAlerta("Sesión", "No se encontró el usuario en sesión.", Alert.AlertType.ERROR);
        return;
    }
    try {
        Map<String,Object> body = Map.of("usuarioId", usuarioId);
        String json = RestClient.post("/cierres/abrir", body);
        Map<String,Object> resp = gson.fromJson(json, new TypeToken<Map<String,Object>>(){}.getType());
        if (Boolean.TRUE.equals(resp.get("success"))) {
            mostrarAlerta("OK", "Caja abierta exitosamente.", Alert.AlertType.INFORMATION);
            refrescarTodo();
        } else {
            mostrarAlerta("Error", String.valueOf(resp.get("message")), Alert.AlertType.ERROR);
        }
    } catch (Exception ex) {
        ex.printStackTrace();
        mostrarAlerta("Error", "No se pudo abrir la caja.\n" + ex.getMessage(), Alert.AlertType.ERROR);
    }
}

    private void actualizarEstadoActual(CierreCaja cc) {
        if (cc != null && "ABIERTO".equals(cc.getEstado())) {
            setAbrirCajaVisible(false);
            lblEstadoCaja.setText("Estado: ABIERTO");
            // En actualizarEstadoActual():
lblCajero.setText("Usuario: " + getNombreUsuario(cc.getUsuarioId()));


            lblFechaApertura.setText("Apertura: " + safeDate(cc.getFechaApertura()));
            lblEfectivoSistema.setText("Efectivo: ₡" + safeMoney(cc.getEfectivoSistema()));
            lblTarjetaSistema.setText("Tarjeta: ₡" + safeMoney(cc.getTarjetaSistema()));
            BigDecimal total = nz(cc.getEfectivoSistema()).add(nz(cc.getTarjetaSistema()));
            lblTotalSistema.setText("Total: ₡" + formatoMoneda.format(total));
        } else {
            lblEstadoCaja.setText("Estado: CERRADO/—");
            lblCajero.setText("Usuario: —");
            lblFechaApertura.setText("Apertura: —");
            lblEfectivoSistema.setText("Efectivo: —");
            lblTarjetaSistema.setText("Tarjeta: —");
            lblTotalSistema.setText("Total: —");
        }
    }

    private void actualizarFormularioCierre(CierreCaja cc) {
        boolean abierto = cc != null && "ABIERTO".equals(cc.getEstado());
        // En actualizarFormularioCierre():
lblInfoCajero.setText("Usuario: " + (abierto ? getNombreUsuario(cc.getUsuarioId()) : "—"));
        lblInfoApertura.setText("Apertura: " + (abierto ? safeDate(cc.getFechaApertura()) : "—"));

        // si quieres mostrar conteo real de facturas, podemos expandir el WS; por ahora oculto si no viene
        lblInfoFacturas.setVisible(false);
        lblInfoFacturas.setManaged(false);

        lblSistemaEfectivo.setText("₡" + (abierto ? safeMoney(cc.getEfectivoSistema()) : "0.00"));
        lblSistemaTarjeta.setText("₡" + (abierto ? safeMoney(cc.getTarjetaSistema()) : "0.00"));
        BigDecimal total = abierto
                ? nz(cc.getEfectivoSistema()).add(nz(cc.getTarjetaSistema()))
                : BigDecimal.ZERO;
        lblSistemaTotal.setText("₡" + formatoMoneda.format(total));

        txtEfectivoDeclarado.setDisable(!abierto);
        txtTarjetaDeclarado.setDisable(!abierto);
        btnCalcular.setDisable(!abierto);
        btnCerrarCaja.setDisable(true); // se habilita después de Calcular

        vboxDiferencias.setVisible(false);
        vboxDiferencias.setManaged(false);
    }

    // ========= Eventos =========

    @FXML
    private void onVolver(ActionEvent e) {
        FlowController.getInstance().goHomeWithFade();
    }

    @FXML
private void onBuscar() {
    Long usuarioId = getUsuarioId();
    if (usuarioId == null) return;

    try {
        String url = "/cierres/usuario/" + usuarioId; // fallback

        // Si vienen fechas, construimos rango (ISO-8601)
        var ini = dpFechaInicio.getValue();
        var fin = dpFechaFin.getValue();

        if (ini != null || fin != null) {
            StringBuilder sb = new StringBuilder("/cierres/usuario/")
                    .append(usuarioId).append("/rango?");
            if (ini != null) {
                // 00:00:00 del día inicio
                sb.append("inicio=").append(ini.atStartOfDay().toString()).append("&");
            }
            if (fin != null) {
                // fin del día -> +1 día a las 00:00 y usamos '< fin'
                sb.append("fin=").append(fin.plusDays(1).atStartOfDay().toString()).append("&");
            }
            url = sb.toString();
            if (url.endsWith("&") || url.endsWith("?")) url = url.substring(0, url.length()-1);
        }

        String json = RestClient.get(url);
        Map<String,Object> resp = gson.fromJson(json, new TypeToken<Map<String,Object>>(){}.getType());
        if (Boolean.TRUE.equals(resp.get("success"))) {
            List<CierreCaja> lista = gson.fromJson(
                    gson.toJson(resp.get("data")),
                    new TypeToken<List<CierreCaja>>(){}.getType());
            listaCierres.setAll(lista);
        } else {
            listaCierres.clear();
            mostrarAlerta("Sin resultados", String.valueOf(resp.get("message")), Alert.AlertType.INFORMATION);
        }
    } catch (Exception ex) {
        ex.printStackTrace();
        listaCierres.clear();
        mostrarAlerta("Error", "No se pudo aplicar el filtro.\n" + ex.getMessage(), Alert.AlertType.ERROR);
    }
}

    @FXML
    private void onVerDetalle() {
        CierreCaja sel = tblCierres.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("Selección requerida", "Seleccione un cierre.", Alert.AlertType.WARNING);
            return;
        }
        mostrarAlerta("Detalle", "Vista de detalle en construcción.", Alert.AlertType.INFORMATION);
    }

    

    @FXML
    private void onRefrescar() {
        refrescarTodo();
        mostrarAlerta("OK", "Datos actualizados.", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void onCalcular() {
        if (!validarCampos() || cierreActual == null) return;

        BigDecimal efDecl = new BigDecimal(txtEfectivoDeclarado.getText());
        BigDecimal tjDecl = new BigDecimal(txtTarjetaDeclarado.getText());

        BigDecimal difEf = efDecl.subtract(nz(cierreActual.getEfectivoSistema()));
        BigDecimal difTj = tjDecl.subtract(nz(cierreActual.getTarjetaSistema()));
        BigDecimal difTt = difEf.add(difTj);

        lblDiferenciaEfectivo.setText(signMoney(difEf));
        lblDiferenciaTarjeta.setText(signMoney(difTj));
        lblDiferenciaTotal.setText(signMoney(difTt));

        aplicarColor(lblDiferenciaEfectivo, difEf);
        aplicarColor(lblDiferenciaTarjeta, difTj);
        aplicarColor(lblDiferenciaTotal, difTt);

        vboxDiferencias.setVisible(true);
        vboxDiferencias.setManaged(true);
        btnCerrarCaja.setDisable(false);
    }

    @FXML
    private void onCerrarCaja() {
        if (!validarCampos() || cierreActual == null) return;

        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setTitle("Confirmar Cierre");
        c.setHeaderText("¿Cerrar la caja?");
        c.setContentText("Se registrará el cierre con los montos declarados.");
        if (c.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            Map<String, Object> body = Map.of(
                    "efectivoDeclarado", new java.math.BigDecimal(txtEfectivoDeclarado.getText()),
                    "tarjetaDeclarado", new java.math.BigDecimal(txtTarjetaDeclarado.getText())
            );
            String json = cr.ac.una.restunaclient.service.RestClient.post(
                    "/cierres/" + cierreActual.getId() + "/cerrar", body);
            Map<String, Object> resp = new com.google.gson.Gson().fromJson(
                    json, new TypeToken<Map<String, Object>>() {}.getType());

            if (Boolean.TRUE.equals(resp.get("success"))) {
                mostrarAlerta("Éxito", "Caja cerrada correctamente.", Alert.AlertType.INFORMATION);
                Long cierreId = cierreActual.getId();
                try {
                   File pdf = reportesServiceCli.cierreByIdPdf(cierreId);
                    RestClient.openFile(pdf);
                } catch (Exception e) {
                    mostrarAlerta("Reporte",
                            "El cierre se registró, pero no se pudo abrir el PDF.\n" + e.getMessage(),
                            Alert.AlertType.WARNING);
                }
                limpiarFormulario();
                refrescarTodo();
            } else {
                mostrarAlerta("Error", String.valueOf(resp.get("message")), Alert.AlertType.ERROR);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            mostrarAlerta("Error", "No se pudo cerrar la caja.\n" + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ========= Auxiliares =========

    private boolean validarCampos() {
        if (txtEfectivoDeclarado.getText().isBlank()) {
            mostrarAlerta("Campo requerido", "Ingrese Efectivo declarado.", Alert.AlertType.WARNING);
            txtEfectivoDeclarado.requestFocus();
            return false;
        }
        if (txtTarjetaDeclarado.getText().isBlank()) {
            mostrarAlerta("Campo requerido", "Ingrese Tarjeta declarada.", Alert.AlertType.WARNING);
            txtTarjetaDeclarado.requestFocus();
            return false;
        }
        return true;
    }

    private void limpiarFormulario() {
        txtEfectivoDeclarado.clear();
        txtTarjetaDeclarado.clear();
        vboxDiferencias.setVisible(false);
        vboxDiferencias.setManaged(false);
        btnCerrarCaja.setDisable(true);
    }

    private void mostrarAlerta(String t, String m, Alert.AlertType tipo) {
        Alert a = new Alert(tipo);
        a.setTitle(t);
        a.setHeaderText(null);
        a.setContentText(m);
        a.showAndWait();
    }

    private String safeDate(LocalDateTime dt) {
        return dt == null ? "—" : dt.format(formatoFecha);
    }
    private String safeMoney(BigDecimal bd) {
        return formatoMoneda.format(nz(bd));
    }
    private String signMoney(BigDecimal bd) {
        BigDecimal v = nz(bd);
        String s = "₡" + formatoMoneda.format(v.abs());
        return v.signum() > 0 ? "+" + s : v.signum() < 0 ? "-" + s : s;
    }
    private BigDecimal nz(BigDecimal bd) { return bd == null ? BigDecimal.ZERO : bd; }
    private String nzStr(String s){ return s == null || s.isBlank() ? "—" : s; }

    private void aplicarColor(Label l, BigDecimal v) {
        if (v.signum() > 0) l.setStyle("-fx-text-fill:#007bff;-fx-font-weight:bold;");
        else if (v.signum() < 0) l.setStyle("-fx-text-fill:#dc3545;-fx-font-weight:bold;");
        else l.setStyle("-fx-text-fill:#28a745;-fx-font-weight:bold;");
    }
}
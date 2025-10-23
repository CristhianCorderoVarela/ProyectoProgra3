
-- ============================================================
-- CREACIÓN DE SECUENCIAS
-- ============================================================

CREATE SEQUENCE seq_usuario START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_salon START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_mesa START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_grupo_producto START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_producto START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_orden START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_detalle_orden START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_cliente START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_factura START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_detalle_factura START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_cierre_caja START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_parametros START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ============================================================
-- TABLA: USUARIO
-- Almacena los usuarios del sistema con sus roles
-- ============================================================

CREATE TABLE usuario (
    id NUMBER PRIMARY KEY,
    nombre VARCHAR2(100) NOT NULL,
    usuario VARCHAR2(50) NOT NULL UNIQUE,
    clave VARCHAR2(255) NOT NULL,
    rol VARCHAR2(20) NOT NULL CHECK (rol IN ('ADMINISTRATIVO', 'CAJERO', 'SALONERO')),
    estado CHAR(1) DEFAULT 'A' CHECK (estado IN ('A', 'I')),
    fecha_creacion DATE DEFAULT SYSDATE,
    version NUMBER DEFAULT 1
);

-- Índices
CREATE INDEX idx_usuario_usuario ON usuario(usuario);
CREATE INDEX idx_usuario_rol ON usuario(rol);

COMMENT ON TABLE usuario IS 'Usuarios del sistema con roles de seguridad';
COMMENT ON COLUMN usuario.rol IS 'ADMINISTRATIVO: acceso total, CAJERO: facturación, SALONERO: solo órdenes';
COMMENT ON COLUMN usuario.estado IS 'A=Activo, I=Inactivo';

-- ============================================================
-- TABLA: SALON
-- Salones o secciones del restaurante (incluye barra)
-- ============================================================

CREATE TABLE salon (
    id NUMBER PRIMARY KEY,
    nombre VARCHAR2(100) NOT NULL,
    tipo VARCHAR2(20) NOT NULL CHECK (tipo IN ('SALON', 'BARRA')),
    imagen_mesa BLOB,
    tipo_imagen VARCHAR2(50),
    cobra_servicio CHAR(1) DEFAULT 'S' CHECK (cobra_servicio IN ('S', 'N')),
    estado CHAR(1) DEFAULT 'A' CHECK (estado IN ('A', 'I')),
    version NUMBER DEFAULT 1
);

CREATE INDEX idx_salon_tipo ON salon(tipo);

COMMENT ON TABLE salon IS 'Salones y secciones del restaurante';
COMMENT ON COLUMN salon.tipo IS 'SALON: requiere diseño de mesas, BARRA: venta directa';
COMMENT ON COLUMN salon.cobra_servicio IS 'S=Sí cobra impuesto de servicio, N=No cobra';

-- ============================================================
-- TABLA: MESA
-- Mesas dentro de los salones con su posición gráfica
-- ============================================================

CREATE TABLE mesa (
    id NUMBER PRIMARY KEY,
    salon_id NUMBER NOT NULL,
    identificador VARCHAR2(20) NOT NULL,
    posicion_x NUMBER DEFAULT 0,
    posicion_y NUMBER DEFAULT 0,
    estado VARCHAR2(20) DEFAULT 'LIBRE' CHECK (estado IN ('LIBRE', 'OCUPADA')),
    version NUMBER DEFAULT 1,
    CONSTRAINT fk_mesa_salon FOREIGN KEY (salon_id) REFERENCES salon(id) ON DELETE CASCADE,
    CONSTRAINT uk_mesa_salon UNIQUE (salon_id, identificador)
);

CREATE INDEX idx_mesa_salon ON mesa(salon_id);
CREATE INDEX idx_mesa_estado ON mesa(estado);

COMMENT ON TABLE mesa IS 'Mesas de los salones con posicionamiento para diseño drag & drop';
COMMENT ON COLUMN mesa.estado IS 'LIBRE: disponible, OCUPADA: con orden activa';

-- ============================================================
-- TABLA: GRUPO_PRODUCTO
-- Categorías de productos para organización y menú rápido
-- ============================================================

CREATE TABLE grupo_producto (
    id NUMBER PRIMARY KEY,
    nombre VARCHAR2(100) NOT NULL,
    menu_rapido CHAR(1) DEFAULT 'N' CHECK (menu_rapido IN ('S', 'N')),
    total_ventas NUMBER DEFAULT 0,
    estado CHAR(1) DEFAULT 'A' CHECK (estado IN ('A', 'I')),
    version NUMBER DEFAULT 1
);

CREATE INDEX idx_grupo_menu_rapido ON grupo_producto(menu_rapido, total_ventas DESC);

COMMENT ON TABLE grupo_producto IS 'Grupos de productos: bebidas calientes, platos fuertes, etc.';
COMMENT ON COLUMN grupo_producto.total_ventas IS 'Contador para ordenar en menú por popularidad';

-- ============================================================
-- TABLA: PRODUCTO
-- Productos del menú del restaurante
-- ============================================================

CREATE TABLE producto (
    id NUMBER PRIMARY KEY,
    grupo_id NUMBER NOT NULL,
    nombre VARCHAR2(150) NOT NULL,
    nombre_corto VARCHAR2(50) NOT NULL,
    precio NUMBER(10,2) NOT NULL CHECK (precio >= 0),
    menu_rapido CHAR(1) DEFAULT 'N' CHECK (menu_rapido IN ('S', 'N')),
    total_ventas NUMBER DEFAULT 0,
    estado CHAR(1) DEFAULT 'A' CHECK (estado IN ('A', 'I')),
    version NUMBER DEFAULT 1,
    CONSTRAINT fk_producto_grupo FOREIGN KEY (grupo_id) REFERENCES grupo_producto(id)
);

CREATE INDEX idx_producto_grupo ON producto(grupo_id);
CREATE INDEX idx_producto_menu_rapido ON producto(menu_rapido, total_ventas DESC);

COMMENT ON TABLE producto IS 'Productos del menú del restaurante';
COMMENT ON COLUMN producto.nombre_corto IS 'Nombre breve para pantalla de selección';
COMMENT ON COLUMN producto.total_ventas IS 'Contador para ordenar en menú por popularidad';

-- ============================================================
-- TABLA: ORDEN
-- Órdenes/pedidos de los clientes
-- ============================================================

CREATE TABLE orden (
    id NUMBER PRIMARY KEY,
    mesa_id NUMBER,
    usuario_id NUMBER NOT NULL,
    fecha_hora DATE DEFAULT SYSDATE,
    estado VARCHAR2(20) DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA', 'FACTURADA', 'CANCELADA')),
    observaciones VARCHAR2(500),
    version NUMBER DEFAULT 1,
    CONSTRAINT fk_orden_mesa FOREIGN KEY (mesa_id) REFERENCES mesa(id),
    CONSTRAINT fk_orden_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE INDEX idx_orden_mesa ON orden(mesa_id);
CREATE INDEX idx_orden_usuario ON orden(usuario_id);
CREATE INDEX idx_orden_estado ON orden(estado);
CREATE INDEX idx_orden_fecha ON orden(fecha_hora);

COMMENT ON TABLE orden IS 'Órdenes de clientes ingresadas por saloneros';
COMMENT ON COLUMN orden.mesa_id IS 'NULL si es orden de barra o venta directa';

-- ============================================================
-- TABLA: DETALLE_ORDEN
-- Detalle de productos en cada orden
-- ============================================================

CREATE TABLE detalle_orden (
    id NUMBER PRIMARY KEY,
    orden_id NUMBER NOT NULL,
    producto_id NUMBER NOT NULL,
    cantidad NUMBER NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMBER(10,2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal NUMBER(10,2) NOT NULL CHECK (subtotal >= 0),
    version NUMBER DEFAULT 1,
    CONSTRAINT fk_detorden_orden FOREIGN KEY (orden_id) REFERENCES orden(id) ON DELETE CASCADE,
    CONSTRAINT fk_detorden_producto FOREIGN KEY (producto_id) REFERENCES producto(id)
);

CREATE INDEX idx_detorden_orden ON detalle_orden(orden_id);
CREATE INDEX idx_detorden_producto ON detalle_orden(producto_id);

COMMENT ON TABLE detalle_orden IS 'Productos incluidos en cada orden';

-- ============================================================
-- TABLA: CLIENTE
-- Información de clientes (opcional en facturación)
-- ============================================================

CREATE TABLE cliente (
    id NUMBER PRIMARY KEY,
    nombre VARCHAR2(150) NOT NULL,
    correo VARCHAR2(100),
    telefono VARCHAR2(20),
    estado CHAR(1) DEFAULT 'A' CHECK (estado IN ('A', 'I')),
    fecha_creacion DATE DEFAULT SYSDATE,
    version NUMBER DEFAULT 1
);

CREATE INDEX idx_cliente_correo ON cliente(correo);

COMMENT ON TABLE cliente IS 'Clientes del restaurante';

-- ============================================================
-- TABLA: CIERRE_CAJA
-- Control de cierres de caja por cajero
-- ============================================================

CREATE TABLE cierre_caja (
    id NUMBER PRIMARY KEY,
    usuario_id NUMBER NOT NULL,
    fecha_apertura DATE DEFAULT SYSDATE,
    fecha_cierre DATE,
    efectivo_declarado NUMBER(10,2),
    tarjeta_declarado NUMBER(10,2),
    efectivo_sistema NUMBER(10,2) DEFAULT 0,
    tarjeta_sistema NUMBER(10,2) DEFAULT 0,
    diferencia_efectivo NUMBER(10,2) DEFAULT 0,
    diferencia_tarjeta NUMBER(10,2) DEFAULT 0,
    estado VARCHAR2(20) DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO', 'CERRADO')),
    version NUMBER DEFAULT 1,
    CONSTRAINT fk_cierrecaja_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE INDEX idx_cierrecaja_usuario ON cierre_caja(usuario_id);
CREATE INDEX idx_cierrecaja_estado ON cierre_caja(estado);
CREATE INDEX idx_cierrecaja_fecha ON cierre_caja(fecha_apertura);

COMMENT ON TABLE cierre_caja IS 'Control de apertura y cierre de caja por cajero';

-- ============================================================
-- TABLA: FACTURA
-- Facturas generadas del sistema
-- ============================================================

CREATE TABLE factura (
    id NUMBER PRIMARY KEY,
    orden_id NUMBER,
    cliente_id NUMBER,
    usuario_id NUMBER NOT NULL,
    cierre_caja_id NUMBER,
    fecha_hora DATE DEFAULT SYSDATE,
    subtotal NUMBER(10,2) NOT NULL CHECK (subtotal >= 0),
    impuesto_venta NUMBER(10,2) DEFAULT 0 CHECK (impuesto_venta >= 0),
    impuesto_servicio NUMBER(10,2) DEFAULT 0 CHECK (impuesto_servicio >= 0),
    descuento NUMBER(10,2) DEFAULT 0 CHECK (descuento >= 0),
    total NUMBER(10,2) NOT NULL CHECK (total >= 0),
    monto_efectivo NUMBER(10,2) DEFAULT 0 CHECK (monto_efectivo >= 0),
    monto_tarjeta NUMBER(10,2) DEFAULT 0 CHECK (monto_tarjeta >= 0),
    vuelto NUMBER(10,2) DEFAULT 0 CHECK (vuelto >= 0),
    estado CHAR(1) DEFAULT 'A' CHECK (estado IN ('A', 'C')),
    version NUMBER DEFAULT 1,
    CONSTRAINT fk_factura_orden FOREIGN KEY (orden_id) REFERENCES orden(id),
    CONSTRAINT fk_factura_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    CONSTRAINT fk_factura_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_factura_cierre FOREIGN KEY (cierre_caja_id) REFERENCES cierre_caja(id)
);

CREATE INDEX idx_factura_orden ON factura(orden_id);
CREATE INDEX idx_factura_cliente ON factura(cliente_id);
CREATE INDEX idx_factura_usuario ON factura(usuario_id);
CREATE INDEX idx_factura_cierre ON factura(cierre_caja_id);
CREATE INDEX idx_factura_fecha ON factura(fecha_hora);

COMMENT ON TABLE factura IS 'Facturas emitidas por el sistema';
COMMENT ON COLUMN factura.orden_id IS 'NULL si es facturación rápida directa';

-- ============================================================
-- TABLA: DETALLE_FACTURA
-- Detalle de productos facturados
-- ============================================================

CREATE TABLE detalle_factura (
    id NUMBER PRIMARY KEY,
    factura_id NUMBER NOT NULL,
    producto_id NUMBER NOT NULL,
    cantidad NUMBER NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMBER(10,2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal NUMBER(10,2) NOT NULL CHECK (subtotal >= 0),
    version NUMBER DEFAULT 1,
    CONSTRAINT fk_detfactura_factura FOREIGN KEY (factura_id) REFERENCES factura(id) ON DELETE CASCADE,
    CONSTRAINT fk_detfactura_producto FOREIGN KEY (producto_id) REFERENCES producto(id)
);

CREATE INDEX idx_detfactura_factura ON detalle_factura(factura_id);
CREATE INDEX idx_detfactura_producto ON detalle_factura(producto_id);

COMMENT ON TABLE detalle_factura IS 'Productos facturados en cada factura';

-- ============================================================
-- TABLA: PARAMETROS
-- Parámetros generales del sistema
-- ============================================================

CREATE TABLE parametros (
    id NUMBER PRIMARY KEY,
    idioma VARCHAR2(10) DEFAULT 'es' CHECK (idioma IN ('es', 'en')),
    porc_impuesto_venta NUMBER(5,2) DEFAULT 13.00 CHECK (porc_impuesto_venta >= 0 AND porc_impuesto_venta <= 100),
    porc_impuesto_servicio NUMBER(5,2) DEFAULT 10.00 CHECK (porc_impuesto_servicio >= 0 AND porc_impuesto_servicio <= 100),
    porc_descuento_maximo NUMBER(5,2) DEFAULT 10.00 CHECK (porc_descuento_maximo >= 0 AND porc_descuento_maximo <= 100),
    nombre_restaurante VARCHAR2(150) NOT NULL,
    telefono1 VARCHAR2(20),
    telefono2 VARCHAR2(20),
    direccion VARCHAR2(250),
    correo_sistema VARCHAR2(100),
    clave_correo_sistema VARCHAR2(255),
    version NUMBER DEFAULT 1
);

COMMENT ON TABLE parametros IS 'Configuración general del sistema';

-- ============================================================
-- TRIGGERS PARA AUTO-INCREMENTO
-- ============================================================

CREATE OR REPLACE TRIGGER trg_usuario_id
BEFORE INSERT ON usuario
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        SELECT seq_usuario.NEXTVAL INTO :NEW.id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_salon_id
BEFORE INSERT ON salon
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        SELECT seq_salon.NEXTVAL INTO :NEW.id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_mesa_id
BEFORE INSERT ON mesa
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        SELECT seq_mesa.NEXTVAL INTO :NEW.id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_grupo_producto_id
BEFORE INSERT ON grupo_producto
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        SELECT seq_grupo_producto.NEXTVAL INTO :NEW.id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_producto_id
BEFORE INSERT ON producto
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        SELECT seq_producto.NEXTVAL INTO :NEW.id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_orden_id
BEFORE INSERT ON orden
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        SELECT seq_orden.NEXTVAL INTO :NEW.id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_detalle_orden_id
BEFORE INSERT ON detalle_orden
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        SELECT seq_detalle_orden.NEXTVAL INTO :NEW.id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_cliente_id
BEFORE INSERT ON cliente
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        SELECT seq_cliente.NEXTVAL INTO :NEW.id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_factura_id
BEFORE INSERT ON factura
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        SELECT seq_factura.NEXTVAL INTO :NEW.id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_detalle_factura_id
BEFORE INSERT ON detalle_factura
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        SELECT seq_detalle_factura.NEXTVAL INTO :NEW.id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_cierre_caja_id
BEFORE INSERT ON cierre_caja
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        SELECT seq_cierre_caja.NEXTVAL INTO :NEW.id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_parametros_id
BEFORE INSERT ON parametros
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        SELECT seq_parametros.NEXTVAL INTO :NEW.id FROM dual;
    END IF;
END;
/


-- Usuario administrador por defecto (contraseña: admin123)
INSERT INTO usuario (nombre, usuario, clave, rol, estado) 
VALUES ('Administrador', 'admin', 'admin123', 'ADMINISTRATIVO', 'A');


COMMIT;

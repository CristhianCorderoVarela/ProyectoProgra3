-- Verificar si ya existen parámetros
SELECT COUNT(*) FROM parametros;

-- Si el resultado es 0, entonces ejecuta esto:
INSERT INTO parametros (
    idioma,
    porc_impuesto_venta,
    porc_impuesto_servicio,
    porc_descuento_maximo,
    nombre_restaurante,
    telefono1,
    telefono2,
    direccion,
    correo_sistema
) VALUES (
    'es',
    13.00,
    10.00,
    10.00,
    'RestUNA',
    '2771-5555',
    '8888-9999',
    'Universidad Nacional, Sede Regional Brunca',
    'restuna@una.cr'
);

COMMIT;


SELECT * FROM parametros;


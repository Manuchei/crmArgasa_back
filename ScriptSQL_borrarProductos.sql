SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

-- 1. cliente_producto
DELETE FROM cliente_producto
WHERE producto_id IN (
    SELECT id_tmp.id
    FROM (
        SELECT id
        FROM productos
        WHERE codigo IN ('P6', 'P7', 'P10')
    ) AS id_tmp
);

-- 2. productos_movimientos
DELETE FROM productos_movimientos
WHERE producto_id IN (
    SELECT id_tmp.id
    FROM (
        SELECT id
        FROM productos
        WHERE codigo IN ('P6', 'P7', 'P10')
    ) AS id_tmp
);

-- 3. ruta_productos
DELETE FROM ruta_productos
WHERE producto_id IN (
    SELECT id_tmp.id
    FROM (
        SELECT id
        FROM productos
        WHERE codigo IN ('P6', 'P7', 'P10')
    ) AS id_tmp
);

-- 4. ruta_lineas
DELETE FROM ruta_lineas
WHERE producto_id IN (
    SELECT id_tmp.id
    FROM (
        SELECT id
        FROM productos
        WHERE codigo IN ('P6', 'P7', 'P10')
    ) AS id_tmp
);

-- 5. hijos dentro de productos
DELETE FROM productos
WHERE producto_id IN (
    SELECT id_tmp.id
    FROM (
        SELECT id
        FROM productos
        WHERE codigo IN ('P6', 'P7', 'P10')
    ) AS id_tmp
);

-- 6. productos finales
DELETE FROM productos
WHERE codigo IN ('P6', 'P7', 'P10');

COMMIT;

SET SQL_SAFE_UPDATES = 1;
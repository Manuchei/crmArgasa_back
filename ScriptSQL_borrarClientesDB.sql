SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM lineas_albaran_cliente WHERE albaran_id IN (SELECT id FROM albaranes_clientes WHERE cliente_id = 48);
DELETE FROM albaranes_clientes WHERE cliente_id = 48;

DELETE FROM lineas_factura_v2 WHERE factura_id IN (SELECT id FROM facturas_clientes WHERE cliente_id = 48);
DELETE FROM facturas_clientes WHERE cliente_id = 48;

DELETE FROM trabajos WHERE cliente_id = 48;

DELETE FROM clientes WHERE id = 48;

SET FOREIGN_KEY_CHECKS = 1;
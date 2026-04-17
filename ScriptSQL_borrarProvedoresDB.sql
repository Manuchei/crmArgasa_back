USE crmargasa_db;

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM facturas_proveedores WHERE proveedor_id = 9;
DELETE FROM albaranes_proveedores WHERE proveedor_id = 9;
DELETE FROM productos WHERE proveedor_id = 9;
DELETE FROM proveedores WHERE id = 9;

SET FOREIGN_KEY_CHECKS = 1;
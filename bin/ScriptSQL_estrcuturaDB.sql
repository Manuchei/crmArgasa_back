-- =========================================
-- 💼 BASE DE DATOS CRM EMPRESA (ARGASA / LUGA)
-- =========================================
CREATE DATABASE IF NOT EXISTS crmargasa_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE crmargasa_dbclientesclientes;

-- =========================================
-- 🧍 USUARIOS
-- =========================================
DROP TABLE IF EXISTS usuarios;
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER',
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================
-- 👥 CLIENTES
-- =========================================
DROP TABLE IF EXISTS clientes;
CREATE TABLE clientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100),
    empresa ENUM('Argasa', 'Luga') NOT NULL,
    telefono VARCHAR(20),
    direccion VARCHAR(255),
    debe DECIMAL(10,2) DEFAULT 0,
    ha_pagado BOOLEAN DEFAULT FALSE,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================
-- 🧰 PROVEEDORES
-- =========================================
DROP TABLE IF EXISTS proveedores;
CREATE TABLE proveedores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    oficio VARCHAR(100),
    trabaja_en_argasa BOOLEAN DEFAULT FALSE,
    trabaja_en_luga BOOLEAN DEFAULT FALSE,
    telefono VARCHAR(20),
    email VARCHAR(150),
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================
-- 🧾 FACTURAS PROVEEDOR
-- =========================================
DROP TABLE IF EXISTS facturas_proveedor;
CREATE TABLE facturas_proveedor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proveedor_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    concepto VARCHAR(255),
    importe DECIMAL(10,2) NOT NULL,
    pagada BOOLEAN DEFAULT FALSE,
    empresa ENUM('Argasa', 'Luga') NOT NULL,
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================
-- 💸 FACTURAS CLIENTE
-- =========================================
DROP TABLE IF EXISTS facturas_cliente;
CREATE TABLE facturas_cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    concepto VARCHAR(255),
    importe DECIMAL(10,2) NOT NULL,
    pagada BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================
-- ☎️ LLAMADAS (Calendario de llamadas)
-- =========================================
DROP TABLE IF EXISTS llamadas;
CREATE TABLE llamadas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT,
    proveedor_id BIGINT,
    fecha DATETIME NOT NULL,
    motivo VARCHAR(255),
    realizada BOOLEAN DEFAULT FALSE,
    observaciones TEXT,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE SET NULL,
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================
-- 🚚 RUTAS DE TRANSPORTISTAS
-- =========================================
DROP TABLE IF EXISTS rutas;
CREATE TABLE rutas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    fecha DATE NOT NULL,
    estado ENUM('ABIERTA','CERRADA') DEFAULT 'ABIERTA',
    observaciones TEXT,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS rutas_clientes;
CREATE TABLE rutas_clientes (
    ruta_id BIGINT,
    cliente_id BIGINT,
    pendiente BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (ruta_id, cliente_id),
    FOREIGN KEY (ruta_id) REFERENCES rutas(id) ON DELETE CASCADE,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================
-- ✅ DATOS DE PRUEBA
-- =========================================
INSERT INTO usuarios (nombre, email, password, rol) VALUES
('Administrador', 'admin@empresa.com', '$2a$10$abcdefgh1234567890abcdefghi12345678', 'ADMIN');

INSERT INTO proveedores (nombre, oficio, trabaja_en_argasa, trabaja_en_luga, telefono, email)
VALUES
('Juan Pérez', 'Electricista', TRUE, FALSE, '600123123', 'juan@correo.com'),
('Marta López', 'Fontanera', TRUE, TRUE, '611234567', 'marta@correo.com');

INSERT INTO clientes (nombre, apellido, empresa, telefono, direccion, debe, ha_pagado)
VALUES
('Carlos', 'Garrido', 'Argasa', '612345678', 'Calle Mayor 12', 200.00, FALSE),
('Lucía', 'Martín', 'Luga', '698765432', 'Av. Galicia 21', 0, TRUE);

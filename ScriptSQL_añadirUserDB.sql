USE crmargasa_db;

INSERT INTO usuarios (nombre, email, password, rol, creado_en)
VALUES (
  'admin',
  'admin@empresa.com',
  '$2a$10$XJzj4e7ZBcj6eGCO3spJde2w9t7zEB6T9Cznp/E6yZhkIMBiAybxi', -- Contraseña: admin123
  'ADMIN',
  NOW()
);

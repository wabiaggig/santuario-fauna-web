
USE db_santuario;
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE tbl_usuario u
JOIN tbl_rol r ON r.id_rol = u.id_rol
SET u.correo = 'veterinario@santuario.test',
    u.clave_hash = '$2a$10$WQ1On/SADFq58PCXKx5jiOraVwRbQY3VJjovGbUMn.ZZPTvIS2Ici',
    u.activo = 1
WHERE u.correo IN ('elena.vargas@santuario.test', 'veterinario@santuario.test')
  AND u.nombres = 'Elena Patricia'
  AND u.apellidos = 'Vargas Mendoza'
  AND r.nombre = 'Veterinario';

UPDATE tbl_usuario u
JOIN tbl_rol r ON r.id_rol = u.id_rol
SET u.correo = 'cuidador@santuario.test',
    u.clave_hash = '$2a$10$wogrpzHjBztNqsQwtMyXR.jo7lJPRyeiyLsOvvBiu62tfWJMjV/8W',
    u.activo = 1
WHERE u.correo IN ('luis.huaman@santuario.test', 'cuidador@santuario.test')
  AND u.nombres = 'Luis Alberto'
  AND u.apellidos = 'Huamán Salazar'
  AND r.nombre = 'Cuidador';

UPDATE tbl_usuario u
JOIN tbl_rol r ON r.id_rol = u.id_rol
SET u.correo = 'padrino@santuario.test',
    u.clave_hash = '$2a$10$JXhgBIEczyriubPdJsybVuozmRSMtCeGjS5iAcud/C7g09D1Vt.I6',
    u.activo = 1
WHERE u.correo IN ('ana.torres@correo.test', 'padrino@santuario.test')
  AND u.nombres = 'Ana Lucía'
  AND u.apellidos = 'Torres Rojas'
  AND r.nombre = 'Padrino';

COMMIT;

SELECT u.id_usuario, u.nombres, u.apellidos, u.correo, r.nombre AS rol,
       u.activo, p.id_padrino
FROM tbl_usuario u
JOIN tbl_rol r ON r.id_rol = u.id_rol
LEFT JOIN tbl_padrino p ON p.id_usuario = u.id_usuario
WHERE u.correo IN ('veterinario@santuario.test', 'cuidador@santuario.test', 'padrino@santuario.test')
ORDER BY u.id_usuario;

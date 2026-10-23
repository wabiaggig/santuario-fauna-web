-- ============================================================================
-- santuario_completo.sql : crea db_santuario desde cero, lista para la aplicacion.
-- Une en orden los scripts originales del proyecto:
--   1. database_santuario.sql
--   2. Alter_tbl_apadrinamiento.sql
--   3. Alter_tbl_donacion.sql
--   4. Agregar_periodo_aporte.sql
--   5. actualizar_usuarios_login.sql
-- ATENCION: borra la base db_santuario si ya existe.
-- ============================================================================

DROP DATABASE IF EXISTS db_santuario;

-- ----------------------------------------------------------------------------
-- database_santuario.sql
-- ----------------------------------------------------------------------------
CREATE DATABASE db_santuario
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE db_santuario;
SET NAMES utf8mb4;

CREATE TABLE tbl_rol (
  id_rol INT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(30) NOT NULL,
  descripcion VARCHAR(150) NOT NULL,
  PRIMARY KEY (id_rol),
  UNIQUE KEY uq_rol_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE tbl_usuario (
  id_usuario INT NOT NULL AUTO_INCREMENT,
  id_rol INT NOT NULL,
  nombres VARCHAR(80) NOT NULL,
  apellidos VARCHAR(80) NOT NULL,
  correo VARCHAR(120) NOT NULL,
  clave_hash VARCHAR(255) NULL,
  activo BOOLEAN NOT NULL DEFAULT FALSE,
  fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_usuario),
  UNIQUE KEY uq_usuario_correo (correo),
  CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES tbl_rol (id_rol),
  CONSTRAINT ck_usuario_activo CHECK (activo IN (0,1)),
  CONSTRAINT ck_usuario_clave CHECK
    (activo = 0 OR (clave_hash IS NOT NULL AND CHAR_LENGTH(TRIM(clave_hash)) > 0))
) ENGINE=InnoDB;

CREATE TABLE tbl_especie (
  id_especie INT NOT NULL AUTO_INCREMENT,
  nombre_comun VARCHAR(100) NOT NULL,
  nombre_cientifico VARCHAR(120) NOT NULL,
  descripcion VARCHAR(255) NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id_especie),
  UNIQUE KEY uq_especie_cientifico (nombre_cientifico),
  CONSTRAINT ck_especie_activo CHECK (activo IN (0,1))
) ENGINE=InnoDB;

CREATE TABLE tbl_zona (
  id_zona INT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(80) NOT NULL,
  tipo CHAR(1) NOT NULL,
  capacidad INT NOT NULL,
  descripcion VARCHAR(255) NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id_zona),
  UNIQUE KEY uq_zona_nombre (nombre),
  CONSTRAINT ck_zona_tipo CHECK (tipo IN ('C','R','H')),
  CONSTRAINT ck_zona_capacidad CHECK (capacidad > 0),
  CONSTRAINT ck_zona_activo CHECK (activo IN (0,1))
) ENGINE=InnoDB;

CREATE TABLE tbl_animal (
  id_animal INT NOT NULL AUTO_INCREMENT,
  codigo VARCHAR(20) NOT NULL,
  nombre VARCHAR(80) NOT NULL,
  id_especie INT NOT NULL,
  sexo CHAR(1) NOT NULL DEFAULT 'D',
  fecha_ingreso DATE NOT NULL,
  procedencia VARCHAR(150) NOT NULL,
  motivo_ingreso VARCHAR(255) NOT NULL,
  estado CHAR(1) NOT NULL DEFAULT 'A',
  estado_salud CHAR(1) NOT NULL DEFAULT 'E',
  fecha_inicio_cuarentena DATE NULL,
  fecha_fin_cuarentena DATE NULL,
  fecha_salida DATE NULL,
  foto_url VARCHAR(255) NULL,
  observaciones TEXT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id_animal),
  UNIQUE KEY uq_animal_codigo (codigo),
  KEY ix_animal_especie_estado (id_especie, estado),
  KEY ix_animal_cuarentena (estado, fecha_fin_cuarentena, fecha_inicio_cuarentena),
  KEY ix_animal_ingreso (fecha_ingreso),
  CONSTRAINT fk_animal_especie FOREIGN KEY (id_especie) REFERENCES tbl_especie (id_especie),
  CONSTRAINT ck_animal_sexo CHECK (sexo IN ('M','H','D')),
  CONSTRAINT ck_animal_estado CHECK (estado IN ('A','L','F')),
  CONSTRAINT ck_animal_salud CHECK (estado_salud IN ('E','T','S')),
  CONSTRAINT ck_animal_activo CHECK (activo IN (0,1)),
  CONSTRAINT ck_animal_inicio_cuarentena CHECK
    (fecha_inicio_cuarentena IS NULL OR fecha_inicio_cuarentena >= fecha_ingreso),
  CONSTRAINT ck_animal_fin_cuarentena CHECK
    (fecha_fin_cuarentena IS NULL OR
      (fecha_inicio_cuarentena IS NOT NULL AND fecha_fin_cuarentena >= fecha_inicio_cuarentena)),
  CONSTRAINT ck_animal_salida CHECK
    ((estado = 'A' AND fecha_salida IS NULL) OR
      (estado IN ('L','F') AND fecha_salida IS NOT NULL AND fecha_salida >= fecha_ingreso)),
  CONSTRAINT ck_animal_cuarentena_salida CHECK
    (fecha_salida IS NULL OR fecha_inicio_cuarentena IS NULL OR
      (fecha_fin_cuarentena IS NOT NULL AND fecha_fin_cuarentena <= fecha_salida))
) ENGINE=InnoDB;

CREATE TABLE tbl_alojamiento (
  id_alojamiento INT NOT NULL AUTO_INCREMENT,
  id_animal INT NOT NULL,
  id_zona INT NOT NULL,
  id_responsable INT NOT NULL,
  fecha_entrada DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_salida DATETIME NULL,
  observaciones VARCHAR(255) NULL,
  id_animal_vigente INT GENERATED ALWAYS AS
    (CASE WHEN fecha_salida IS NULL THEN id_animal ELSE NULL END) STORED,
  PRIMARY KEY (id_alojamiento),
  UNIQUE KEY uq_alojamiento_animal_vigente (id_animal_vigente),
  KEY ix_alojamiento_animal (id_animal, fecha_entrada),
  KEY ix_alojamiento_zona (id_zona, fecha_salida),
  CONSTRAINT fk_alojamiento_animal FOREIGN KEY (id_animal) REFERENCES tbl_animal (id_animal),
  CONSTRAINT fk_alojamiento_zona FOREIGN KEY (id_zona) REFERENCES tbl_zona (id_zona),
  CONSTRAINT fk_alojamiento_responsable FOREIGN KEY (id_responsable) REFERENCES tbl_usuario (id_usuario),
  CONSTRAINT ck_alojamiento_fechas CHECK (fecha_salida IS NULL OR fecha_salida >= fecha_entrada)
) ENGINE=InnoDB;

CREATE TABLE tbl_medicamento (
  id_medicamento INT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL,
  presentacion VARCHAR(100) NOT NULL,
  unidad_medida VARCHAR(20) NOT NULL,
  stock_actual DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  stock_minimo DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  costo_unitario DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id_medicamento),
  UNIQUE KEY uq_medicamento_presentacion (nombre, presentacion),
  CONSTRAINT ck_medicamento_stock CHECK (stock_actual >= 0 AND stock_minimo >= 0),
  CONSTRAINT ck_medicamento_costo CHECK (costo_unitario >= 0),
  CONSTRAINT ck_medicamento_activo CHECK (activo IN (0,1))
) ENGINE=InnoDB;

CREATE TABLE tbl_atencion_medica (
  id_atencion INT NOT NULL AUTO_INCREMENT,
  id_animal INT NOT NULL,
  id_veterinario INT NOT NULL,
  fecha_atencion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  diagnostico TEXT NOT NULL,
  tratamiento TEXT NOT NULL,
  estado_salud_anterior CHAR(1) NOT NULL,
  estado_salud_resultante CHAR(1) NOT NULL,
  observaciones TEXT NULL,
  PRIMARY KEY (id_atencion),
  KEY ix_atencion_animal_fecha (id_animal, fecha_atencion),
  KEY ix_atencion_fecha (fecha_atencion),
  CONSTRAINT fk_atencion_animal FOREIGN KEY (id_animal) REFERENCES tbl_animal (id_animal),
  CONSTRAINT fk_atencion_veterinario FOREIGN KEY (id_veterinario) REFERENCES tbl_usuario (id_usuario),
  CONSTRAINT ck_atencion_salud_anterior CHECK (estado_salud_anterior IN ('E','T','S')),
  CONSTRAINT ck_atencion_salud_resultante CHECK (estado_salud_resultante IN ('E','T','S'))
) ENGINE=InnoDB;

CREATE TABLE tbl_detalle_atencion (
  id_detalle INT NOT NULL AUTO_INCREMENT,
  id_atencion INT NOT NULL,
  id_medicamento INT NOT NULL,
  cantidad DECIMAL(12,2) NOT NULL,
  costo_unitario DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (id_detalle),
  UNIQUE KEY uq_detalle_atencion_medicamento (id_atencion, id_medicamento),
  CONSTRAINT fk_detalle_atencion FOREIGN KEY (id_atencion) REFERENCES tbl_atencion_medica (id_atencion),
  CONSTRAINT fk_detalle_medicamento FOREIGN KEY (id_medicamento) REFERENCES tbl_medicamento (id_medicamento),
  CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0),
  CONSTRAINT ck_detalle_costo CHECK (costo_unitario >= 0)
) ENGINE=InnoDB;

CREATE TABLE tbl_liberacion (
  id_liberacion INT NOT NULL AUTO_INCREMENT,
  id_animal INT NOT NULL,
  id_responsable INT NOT NULL,
  fecha_liberacion DATE NOT NULL,
  lugar VARCHAR(150) NOT NULL,
  observaciones TEXT NULL,
  PRIMARY KEY (id_liberacion),
  UNIQUE KEY uq_liberacion_animal (id_animal),
  KEY ix_liberacion_fecha (fecha_liberacion),
  CONSTRAINT fk_liberacion_animal FOREIGN KEY (id_animal) REFERENCES tbl_animal (id_animal),
  CONSTRAINT fk_liberacion_responsable FOREIGN KEY (id_responsable) REFERENCES tbl_usuario (id_usuario)
) ENGINE=InnoDB;

CREATE TABLE tbl_padrino (
  id_padrino INT NOT NULL AUTO_INCREMENT,
  id_usuario INT NULL,
  nombres VARCHAR(80) NOT NULL,
  apellidos VARCHAR(80) NOT NULL,
  tipo_documento VARCHAR(10) NULL,
  numero_documento VARCHAR(20) NULL,
  correo_contacto VARCHAR(120) NOT NULL,
  telefono VARCHAR(20) NULL,
  fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id_padrino),
  UNIQUE KEY uq_padrino_usuario (id_usuario),
  UNIQUE KEY uq_padrino_documento (tipo_documento, numero_documento),
  CONSTRAINT fk_padrino_usuario FOREIGN KEY (id_usuario) REFERENCES tbl_usuario (id_usuario),
  CONSTRAINT ck_padrino_activo CHECK (activo IN (0,1)),
  CONSTRAINT ck_padrino_documento CHECK
    ((tipo_documento IS NULL AND numero_documento IS NULL) OR
      (tipo_documento IS NOT NULL AND numero_documento IS NOT NULL))
) ENGINE=InnoDB;

CREATE TABLE tbl_apadrinamiento (
  id_apadrinamiento INT NOT NULL AUTO_INCREMENT,
  id_padrino INT NOT NULL,
  id_animal INT NOT NULL,
  fecha_inicio DATE NOT NULL,
  fecha_fin DATE NULL,
  aporte_mensual_acordado DECIMAL(10,2) NULL,
  observaciones VARCHAR(255) NULL,
  id_animal_vigente INT GENERATED ALWAYS AS
    (CASE WHEN fecha_fin IS NULL THEN id_animal ELSE NULL END) STORED,
  PRIMARY KEY (id_apadrinamiento),
  UNIQUE KEY uq_apadrinamiento_vigente (id_padrino, id_animal_vigente),
  UNIQUE KEY uq_apadrinamiento_padrino (id_apadrinamiento, id_padrino),
  KEY ix_apadrinamiento_animal (id_animal),
  CONSTRAINT fk_apadrinamiento_padrino FOREIGN KEY (id_padrino) REFERENCES tbl_padrino (id_padrino),
  CONSTRAINT fk_apadrinamiento_animal FOREIGN KEY (id_animal) REFERENCES tbl_animal (id_animal),
  CONSTRAINT ck_apadrinamiento_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
  CONSTRAINT ck_apadrinamiento_aporte CHECK (aporte_mensual_acordado IS NULL OR aporte_mensual_acordado > 0)
) ENGINE=InnoDB;

CREATE TABLE tbl_donacion (
  id_donacion INT NOT NULL AUTO_INCREMENT,
  id_padrino INT NOT NULL,
  id_apadrinamiento INT NULL,
  fecha_donacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  monto DECIMAL(10,2) NOT NULL,
  medio_pago VARCHAR(20) NOT NULL,
  referencia VARCHAR(100) NOT NULL,
  observaciones VARCHAR(255) NULL,
  PRIMARY KEY (id_donacion),
  UNIQUE KEY uq_donacion_referencia (medio_pago, referencia),
  KEY ix_donacion_fecha (fecha_donacion),
  CONSTRAINT fk_donacion_padrino FOREIGN KEY (id_padrino) REFERENCES tbl_padrino (id_padrino),
  CONSTRAINT fk_donacion_apadrinamiento FOREIGN KEY (id_apadrinamiento, id_padrino)
    REFERENCES tbl_apadrinamiento (id_apadrinamiento, id_padrino),
  CONSTRAINT ck_donacion_monto CHECK (monto > 0),
  CONSTRAINT ck_donacion_medio CHECK (medio_pago IN ('EFECTIVO','TRANSFERENCIA','YAPE','PLIN'))
) ENGINE=InnoDB;

START TRANSACTION;

INSERT INTO tbl_rol
 (id_rol, nombre, descripcion) VALUES
 (1, 'Veterinario', 'Gestiona el historial clínico, las cuarentenas y la autorización de liberación.'),
 (2, 'Cuidador', 'Registra animales, administra alojamientos y consulta la disponibilidad de las zonas.'),
 (3, 'Padrino', 'Consulta la evolución de los animales que apadrina y sus aportes registrados.');

INSERT INTO tbl_usuario
 (id_usuario, id_rol, nombres, apellidos, correo, fecha_registro) VALUES
 (1, 1, 'Elena Patricia', 'Vargas Mendoza', 'elena.vargas@santuario.test', '2026-04-06 08:30:00'),
 (2, 2, 'Luis Alberto', 'Huamán Salazar', 'luis.huaman@santuario.test', '2026-04-06 09:15:00'),
 (3, 3, 'Ana Lucía', 'Torres Rojas', 'ana.torres@correo.test', '2026-10-05 16:20:00');

INSERT INTO tbl_especie
 (id_especie, nombre_comun, nombre_cientifico, descripcion) VALUES
 (1, 'Guacamayo azul y amarillo', 'Ara ararauna', 'Ave registrada en el área de rehabilitación; se evalúa la recuperación del plumaje y la capacidad de vuelo.'),
 (2, 'Mono capuchino', 'Sapajus apella', 'Primate registrado en el área de rehabilitación; se realiza seguimiento del comportamiento y la adaptación al grupo.'),
 (3, 'Tortuga motelo', 'Chelonoidis denticulatus', 'Reptil terrestre registrado en el refugio; se controla su condición corporal y la integridad del caparazón.');

INSERT INTO tbl_zona
 (id_zona, nombre, tipo, capacidad, descripcion) VALUES
 (1, 'Unidad de cuarentena Los Cedros', 'C', 3, 'Tres recintos individuales con acceso restringido para observación de animales recién ingresados.'),
 (2, 'Aviario de rehabilitación Río Verde', 'R', 3, 'Recinto con perchas y espacio de vuelo destinado al seguimiento de aves en rehabilitación.'),
 (3, 'Hábitat de reptiles Las Palmeras', 'H', 2, 'Recinto con áreas de sombra, vegetación y refugios para alojamiento de tortugas terrestres.'),
 (4, 'Recinto de primates El Bosque', 'R', 2, 'Zona con estructuras de desplazamiento y área de observación para rehabilitación de primates.');

INSERT INTO tbl_animal
 (id_animal, codigo, nombre, id_especie, sexo, fecha_ingreso, procedencia, motivo_ingreso, estado, estado_salud, fecha_inicio_cuarentena, fecha_fin_cuarentena, fecha_salida, observaciones) VALUES
 (1, 'SAN-001', 'Azul', 1, 'D', '2026-10-03', 'Traslado desde una vivienda en Puerto Maldonado, Madre de Dios', 'Lesión superficial en el ala izquierda y pérdida parcial de plumaje.', 'A', 'T', '2026-10-03', NULL, NULL, 'Permanece en observación individual. Se registra la evolución de la lesión y la aceptación del alimento.'),
 (2, 'SAN-002', 'Kiro', 2, 'M', '2026-10-08', 'Entrega voluntaria en Tambopata, Madre de Dios', 'Animal mantenido en cautiverio doméstico; requiere evaluación de ingreso y adaptación.', 'A', 'E', '2026-10-08', NULL, NULL, 'Evaluación inicial pendiente de cierre. Alojado en un recinto individual de cuarentena.'),
 (3, 'SAN-003', 'Luna', 1, 'H', '2026-08-14', 'Traslado desde Laberinto, Madre de Dios', 'Limitación de vuelo y desgaste del plumaje al momento del ingreso.', 'A', 'S', '2026-08-14', '2026-09-03', NULL, 'Concluyó la cuarentena y el seguimiento clínico. Continúa en el aviario para evaluación de vuelo.'),
 (4, 'SAN-004', 'Moti', 3, 'D', '2026-08-24', 'Hallazgo próximo a una vía en Las Piedras, Madre de Dios', 'Debilidad general y baja condición corporal al momento del rescate.', 'A', 'T', '2026-08-24', '2026-09-13', NULL, 'Permanece en el hábitat de reptiles con seguimiento de condición corporal y movilidad.'),
 (5, 'SAN-005', 'Sol', 1, 'M', '2026-05-16', 'Entrega voluntaria en Puerto Maldonado, Madre de Dios', 'Recuperación de plumaje y evaluación de aptitud para retorno al entorno natural.', 'L', 'S', '2026-05-16', '2026-06-05', '2026-09-23', 'Completó el proceso de rehabilitación. Se cerró el alojamiento al registrar su liberación.'),
 (6, 'SAN-006', 'Nube', 2, 'D', '2026-06-15', 'Traslado de rescate desde Inambari, Madre de Dios', 'Condición general comprometida al ingreso y necesidad de seguimiento clínico.', 'F', 'T', '2026-06-15', '2026-07-05', '2026-07-15', 'Fallecimiento registrado durante el periodo de rehabilitación. Expediente conservado para consulta histórica.');

INSERT INTO tbl_alojamiento
 (id_alojamiento, id_animal, id_zona, id_responsable, fecha_entrada, fecha_salida, observaciones) VALUES
 (1, 1, 1, 2, '2026-10-03', NULL, 'Ingreso a cuarentena por lesión en el ala.'),
 (2, 2, 1, 2, '2026-10-08', NULL, 'Aislamiento inicial y evaluación de adaptación.'),
 (3, 3, 1, 2, '2026-08-14', '2026-09-03', 'Cuarentena de ingreso completada.'),
 (4, 3, 2, 2, '2026-09-03', NULL, 'Traslado al aviario después del cierre de cuarentena.'),
 (5, 4, 1, 2, '2026-08-24', '2026-09-13', 'Observación de ingreso y control de condición corporal.'),
 (6, 4, 3, 2, '2026-09-13', NULL, 'Traslado al hábitat de reptiles para seguimiento.'),
 (7, 5, 1, 2, '2026-05-16', '2026-06-05', 'Cuarentena de ingreso y valoración clínica.'),
 (8, 5, 2, 2, '2026-06-05', '2026-09-23', 'Rehabilitación de vuelo; alojamiento cerrado por liberación.'),
 (9, 6, 1, 2, '2026-06-15', '2026-07-05', 'Cuarentena de ingreso y seguimiento clínico.'),
 (10, 6, 4, 2, '2026-07-05', '2026-07-15', 'Traslado a rehabilitación; alojamiento cerrado por fallecimiento.');

INSERT INTO tbl_medicamento
 (id_medicamento, nombre, presentacion, unidad_medida, stock_actual, stock_minimo, costo_unitario) VALUES
 (1, 'Suplemento multivitamínico veterinario', 'Frasco de 100 comprimidos', 'comprimido', 96.00, 10.00, 2.50),
 (2, 'Clorhexidina acuosa', 'Frasco de 500 mL', 'mL', 490.00, 50.00, 0.20),
 (3, 'Suplemento mineral para reptiles', 'Envase de 250 g', 'g', 200.00, 20.00, 0.50);

INSERT INTO tbl_atencion_medica
 (id_atencion, id_animal, id_veterinario, fecha_atencion, diagnostico, tratamiento, estado_salud_anterior, estado_salud_resultante, observaciones) VALUES
 (1, 1, 1, '2026-10-05 09:30:00', 'Lesión superficial en el ala izquierda; movilidad reducida durante la observación.', 'Atención de la lesión, soporte nutricional y seguimiento de la recuperación funcional.', 'E', 'T', 'Permanece en cuarentena. Se registró aceptación del alimento y menor limitación de movimiento.'),
 (2, 3, 1, '2026-09-08 10:45:00', 'Control de recuperación: mejora de la condición corporal y ausencia de lesión visible en el ala.', 'Cierre del seguimiento clínico y continuidad de la evaluación de vuelo en el aviario.', 'T', 'S', 'Sin consumo de medicamentos en esta visita. Continúa el proceso de rehabilitación.'),
 (3, 4, 1, '2026-09-28 15:10:00', 'Baja condición corporal con recuperación progresiva respecto a la evaluación de ingreso.', 'Seguimiento de condición corporal, alimentación y movilidad durante la recuperación.', 'E', 'T', 'Se programó un nuevo control de condición corporal. Continúa alojada en el hábitat de reptiles.');

INSERT INTO tbl_detalle_atencion
 (id_detalle, id_atencion, id_medicamento, cantidad, costo_unitario) VALUES
 (1, 1, 1, 2.00, 2.50),
 (2, 1, 2, 10.00, 0.20),
 (3, 3, 1, 2.00, 2.50);

INSERT INTO tbl_liberacion
 (id_liberacion, id_animal, id_responsable, fecha_liberacion, lugar, observaciones) VALUES
 (1, 5, 1, '2026-09-23', 'Área de reinserción Río Verde, Tambopata, Madre de Dios', 'Liberación registrada tras finalizar el proceso de rehabilitación. Acta interna LIB-2026-001; cierre de alojamiento en la misma fecha.');

INSERT INTO tbl_padrino
 (id_padrino, id_usuario, nombres, apellidos, tipo_documento, numero_documento, correo_contacto, telefono, fecha_registro) VALUES
 (1, 3, 'Ana Lucía', 'Torres Rojas', 'DNI', '74853126', 'ana.torres@correo.test', '986542731', '2026-10-05 16:20:00'),
 (2, NULL, 'Carlos Enrique', 'Paredes Soto', 'DNI', '70648295', 'carlos.paredes@correo.test', '975318642', '2026-09-10 11:40:00');

INSERT INTO tbl_apadrinamiento
 (id_apadrinamiento, id_padrino, id_animal, fecha_inicio, aporte_mensual_acordado, observaciones) VALUES
 (1, 1, 1, '2026-10-06', 50.00, 'Apoyo mensual destinado a la alimentación y recuperación de Azul. Se entrega una actualización de su evolución al padrino.'),
 (2, 2, 3, '2026-09-18', 80.00, 'Apoyo mensual para alimentación y mantenimiento del aviario durante la rehabilitación de Luna.');

INSERT INTO tbl_donacion
 (id_donacion, id_padrino, id_apadrinamiento, fecha_donacion, monto, medio_pago, referencia, observaciones) VALUES
 (1, 1, 1, '2026-10-07 14:25:00', 50.00, 'TRANSFERENCIA', 'TRF-20261007-0184', 'Aporte de octubre correspondiente al apadrinamiento de Azul.'),
 (2, 2, 2, '2026-09-19 18:12:00', 80.00, 'YAPE', 'YAP-20260919-0427', 'Primer aporte mensual para el apadrinamiento de Luna.'),
 (3, 2, NULL, '2026-10-11 10:05:00', 30.00, 'EFECTIVO', 'REC-2026-0031', 'Donación general para compra de insumos de limpieza del refugio.');

COMMIT;

SHOW TABLES;

SET @id_especie_filtro = NULL;
SET @id_zona_filtro = NULL;
SELECT a.codigo, a.nombre, e.nombre_comun AS especie,
       z.nombre AS zona, a.fecha_inicio_cuarentena,
       DATEDIFF(CURDATE(), a.fecha_inicio_cuarentena) AS dias_cuarentena,
       a.estado_salud
FROM tbl_animal a
JOIN tbl_especie e ON e.id_especie = a.id_especie
LEFT JOIN tbl_alojamiento al ON al.id_animal = a.id_animal AND al.fecha_salida IS NULL
LEFT JOIN tbl_zona z ON z.id_zona = al.id_zona
WHERE a.estado = 'A'
  AND a.fecha_inicio_cuarentena IS NOT NULL
  AND a.fecha_inicio_cuarentena <= CURDATE()
  AND a.fecha_fin_cuarentena IS NULL
  AND (@id_especie_filtro IS NULL OR a.id_especie = @id_especie_filtro)
  AND (@id_zona_filtro IS NULL OR z.id_zona = @id_zona_filtro)
ORDER BY a.fecha_inicio_cuarentena, a.id_animal;

SET @tipo_zona_filtro = NULL;
SET @solo_con_cupos = 0;
SELECT z.id_zona, z.nombre, z.tipo, z.capacidad,
       COUNT(al.id_alojamiento) AS ocupados,
       z.capacidad - COUNT(al.id_alojamiento) AS disponibles
FROM tbl_zona z
LEFT JOIN tbl_alojamiento al ON al.id_zona = z.id_zona AND al.fecha_salida IS NULL
WHERE z.activo = 1 AND (@tipo_zona_filtro IS NULL OR z.tipo = @tipo_zona_filtro)
GROUP BY z.id_zona, z.nombre, z.tipo, z.capacidad
HAVING @solo_con_cupos = 0 OR disponibles > 0
ORDER BY z.id_zona;

SET @desde = DATE_SUB(CURDATE(), INTERVAL 1 YEAR);
SET @hasta = CURDATE();
SELECT YEAR(m.fecha) AS anio, QUARTER(m.fecha) AS trimestre,
       SUM(m.ingresos) AS animales_ingresados,
       SUM(m.liberaciones) AS animales_liberados,
       ROUND(100.0 * SUM(m.liberaciones) / NULLIF(SUM(m.ingresos),0),2)
         AS razon_liberaciones_sobre_ingresos_pct
FROM (
  SELECT fecha_ingreso AS fecha, 1 AS ingresos, 0 AS liberaciones FROM tbl_animal
  UNION ALL
  SELECT fecha_liberacion, 0, 1 FROM tbl_liberacion
) m
WHERE m.fecha BETWEEN @desde AND @hasta
GROUP BY YEAR(m.fecha), QUARTER(m.fecha)
ORDER BY anio, trimestre;

SELECT YEAR(a.fecha_atencion) AS anio, MONTH(a.fecha_atencion) AS mes,
       m.id_medicamento, m.nombre, m.unidad_medida,
       SUM(d.cantidad) AS cantidad_consumida,
       ROUND(SUM(d.cantidad * d.costo_unitario),2) AS costo_consumido_soles
FROM tbl_detalle_atencion d
JOIN tbl_atencion_medica a ON a.id_atencion = d.id_atencion
JOIN tbl_medicamento m ON m.id_medicamento = d.id_medicamento
WHERE a.fecha_atencion >= @desde AND a.fecha_atencion < DATE_ADD(@hasta, INTERVAL 1 DAY)
GROUP BY YEAR(a.fecha_atencion), MONTH(a.fecha_atencion), m.id_medicamento, m.nombre, m.unidad_medida
ORDER BY anio, mes, m.nombre;

-- ----------------------------------------------------------------------------
-- Alter_tbl_apadrinamiento.sql
-- ----------------------------------------------------------------------------
USE db_santuario; 
 
ALTER TABLE tbl_apadrinamiento
  MODIFY COLUMN fecha_inicio DATE NULL,
  ADD COLUMN estado_solicitud VARCHAR(12) NOT NULL DEFAULT 'APROBADA' AFTER observaciones,
  ADD COLUMN fecha_solicitud DATETIME NULL AFTER estado_solicitud,
  ADD COLUMN id_usuario_revisor INT NULL AFTER fecha_solicitud,
  ADD COLUMN fecha_revision DATETIME NULL AFTER id_usuario_revisor,
  ADD COLUMN motivo_rechazo VARCHAR(255) NULL AFTER fecha_revision,
  ADD KEY ix_apadrinamiento_estado_solicitud (estado_solicitud, fecha_solicitud),
  ADD KEY ix_apadrinamiento_usuario_revisor (id_usuario_revisor),
  ADD CONSTRAINT fk_apadrinamiento_usuario_revisor
    FOREIGN KEY (id_usuario_revisor) REFERENCES tbl_usuario(id_usuario),
  ADD CONSTRAINT ck_apadrinamiento_estado_solicitud CHECK
    (estado_solicitud IN ('PENDIENTE','APROBADA','RECHAZADA')),
  ADD CONSTRAINT ck_apadrinamiento_inicio_segun_estado CHECK
    ((estado_solicitud = 'APROBADA' AND fecha_inicio IS NOT NULL)
      OR (estado_solicitud IN ('PENDIENTE','RECHAZADA')
          AND fecha_inicio IS NULL AND fecha_fin IS NULL)),
  ADD CONSTRAINT ck_apadrinamiento_motivo_rechazo CHECK
    (estado_solicitud <> 'RECHAZADA' OR
      (motivo_rechazo IS NOT NULL AND CHAR_LENGTH(TRIM(motivo_rechazo)) > 0));
 
ALTER TABLE tbl_apadrinamiento
  DROP INDEX uq_apadrinamiento_vigente,
  MODIFY COLUMN id_animal_vigente INT GENERATED ALWAYS AS
    (CASE WHEN fecha_fin IS NULL
             AND estado_solicitud IN ('PENDIENTE','APROBADA')
          THEN id_animal ELSE NULL END) STORED,
  ADD UNIQUE KEY uq_apadrinamiento_vigente (id_padrino, id_animal_vigente);

-- ----------------------------------------------------------------------------
-- Alter_tbl_donacion.sql
-- ----------------------------------------------------------------------------
USE db_santuario; 

ALTER TABLE tbl_donacion
  ADD COLUMN estado_verificacion VARCHAR(15) NOT NULL DEFAULT 'CONFIRMADA' AFTER observaciones,
  ADD COLUMN fecha_solicitud DATETIME NULL AFTER estado_verificacion,
  ADD COLUMN comprobante_url VARCHAR(500) NULL AFTER fecha_solicitud,
  ADD COLUMN id_usuario_revisor INT NULL AFTER comprobante_url,
  ADD COLUMN fecha_revision DATETIME NULL AFTER id_usuario_revisor,
  ADD COLUMN motivo_observacion VARCHAR(255) NULL AFTER fecha_revision,
  ADD KEY ix_donacion_estado_fecha (estado_verificacion, fecha_solicitud),
  ADD KEY ix_donacion_revisor (id_usuario_revisor),
  ADD CONSTRAINT fk_donacion_revisor FOREIGN KEY (id_usuario_revisor)
    REFERENCES tbl_usuario (id_usuario),
  ADD CONSTRAINT ck_donacion_estado_verificacion CHECK
    (estado_verificacion IN ('PENDIENTE', 'OBSERVADA', 'CONFIRMADA', 'RECHAZADA')),
  ADD CONSTRAINT ck_donacion_motivo_observacion CHECK
    (estado_verificacion NOT IN ('OBSERVADA','RECHAZADA') OR
     (motivo_observacion IS NOT NULL AND CHAR_LENGTH(TRIM(motivo_observacion)) > 0));

-- ----------------------------------------------------------------------------
-- Agregar_periodo_aporte.sql
-- ----------------------------------------------------------------------------
USE db_santuario;
 
SELECT COUNT(*) AS existe_periodo
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'db_santuario'
  AND TABLE_NAME = 'tbl_donacion'
  AND COLUMN_NAME = 'periodo_aporte';
 
ALTER TABLE tbl_donacion
  ADD COLUMN periodo_aporte DATE NULL AFTER motivo_observacion,
  ADD INDEX ix_donacion_apadrinamiento_periodo (id_apadrinamiento, periodo_aporte); 
  
SELECT id_donacion, id_apadrinamiento, estado_verificacion, periodo_aporte
FROM tbl_donacion ORDER BY id_donacion DESC LIMIT 15;

-- ----------------------------------------------------------------------------
-- actualizar_usuarios_login.sql
-- ----------------------------------------------------------------------------
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


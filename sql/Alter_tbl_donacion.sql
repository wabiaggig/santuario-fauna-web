 
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
 

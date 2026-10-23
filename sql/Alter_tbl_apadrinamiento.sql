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

 

 

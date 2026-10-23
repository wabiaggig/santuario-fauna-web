 
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

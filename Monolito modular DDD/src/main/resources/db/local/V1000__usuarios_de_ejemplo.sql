-- Solo perfil local: perfiles de ejemplo para probar la autorización por rol y atributo.
INSERT INTO identidad.usuario (id, rol, centro_id, especialidad) VALUES ('pac-1', 'PACIENTE', NULL, NULL);
INSERT INTO identidad.usuario (id, rol, centro_id, especialidad) VALUES ('med-1', 'MEDICO_RURAL', 'centro-a', NULL);
INSERT INTO identidad.usuario (id, rol, centro_id, especialidad) VALUES ('esp-1', 'ESPECIALISTA', NULL, 'Cardiología');
INSERT INTO identidad.paciente_relacionado (usuario_id, paciente_id) VALUES ('med-1', 'pac-1');
INSERT INTO identidad.paciente_relacionado (usuario_id, paciente_id) VALUES ('esp-1', 'pac-1');

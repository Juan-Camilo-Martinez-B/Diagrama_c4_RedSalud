-- Solo perfil local: perfiles de ejemplo para probar la autorización por rol y atributo.
INSERT INTO identity.user_account (id, role, center_id, specialty) VALUES ('pac-1', 'PATIENT', NULL, NULL);
INSERT INTO identity.user_account (id, role, center_id, specialty) VALUES ('med-1', 'RURAL_DOCTOR', 'centro-a', NULL);
INSERT INTO identity.user_account (id, role, center_id, specialty) VALUES ('esp-1', 'SPECIALIST', NULL, 'Cardiología');
INSERT INTO identity.related_patient (user_id, patient_id) VALUES ('med-1', 'pac-1');
INSERT INTO identity.related_patient (user_id, patient_id) VALUES ('esp-1', 'pac-1');

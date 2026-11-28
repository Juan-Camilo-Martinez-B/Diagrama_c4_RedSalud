-- Rol administrativo: existe para negar el acceso clínico y dejar la denegación en auditoría.
INSERT INTO identity.user_account (id, role, center_id, specialty) VALUES ('admin-1', 'ADMINISTRATOR', NULL, NULL);

-- Insert new capability
INSERT INTO capabilities (name, description)
VALUES ('UPLOAD_OWN_AMKA_FILE', 'The ability of a teacher to upload his own amka file');

-- Assign UPLOAD_OWN_AMKA_FILE to TEACHER
INSERT INTO roles_capabilities (role_id, capability_id)
SELECT r.id, c.id
FROM roles r
         CROSS JOIN capabilities c
WHERE r.name = 'TEACHER'
  AND c.name = 'UPLOAD_OWN_AMKA_FILE';
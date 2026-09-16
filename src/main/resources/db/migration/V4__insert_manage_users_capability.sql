-- Insert new capability
INSERT INTO capabilities (name, description)
VALUES ('MANAGE_USERS', 'Create and manage other users');

-- Assign MANAGE_USERS to ADMIN
INSERT INTO roles_capabilities (role_id, capability_id)
SELECT r.id, c.id
FROM roles r
CROSS JOIN capabilities c
WHERE r.name = 'ADMIN'
  AND c.name = 'MANAGE_USERS';
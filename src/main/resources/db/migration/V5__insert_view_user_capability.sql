-- Insert VIEW_USER capability
INSERT INTO capabilities (name, description)
VALUES ('VIEW_USER', 'View any user account details');

-- Assign VIEW_USER to ADMIN
INSERT INTO roles_capabilities (role_id, capability_id)
SELECT r.id, c.id
FROM roles r
CROSS JOIN capabilities c
WHERE r.name = 'ADMIN'
  AND c.name = 'VIEW_USER';
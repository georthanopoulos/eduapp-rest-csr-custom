-- Insert new capability
INSERT INTO capabilities (name, description)
VALUES ('VIEW_TEACHER_REPORT', 'View the teacher status report, which includes personal data such as AMKA and AFM');

-- Assign VIEW_TEACHER_REPORT to ADMIN
INSERT INTO roles_capabilities (role_id, capability_id)
SELECT r.id, c.id
FROM roles r
         CROSS JOIN capabilities c
WHERE r.name = 'ADMIN'
  AND c.name = 'VIEW_TEACHER_REPORT';
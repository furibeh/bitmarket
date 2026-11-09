-- Session store of the Next.js BFF (RN-40, ADR-0011). Better Auth creates the tables in this
-- schema; the backend never reads or writes them.

-- Roles are global to the cluster, so the role may already exist from another database.
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'bitmarket_bff') THEN
    CREATE ROLE bitmarket_bff;
  END IF;
END
$$;

ALTER ROLE bitmarket_bff LOGIN PASSWORD '${bff_password}';
ALTER ROLE bitmarket_bff SET search_path = bff;

-- Plain GRANT instead of CREATE SCHEMA ... AUTHORIZATION: the latter requires SET ROLE, which the
-- non-superuser RDS master user does not have on roles it creates.
CREATE SCHEMA bff;
GRANT USAGE, CREATE ON SCHEMA bff TO bitmarket_bff;

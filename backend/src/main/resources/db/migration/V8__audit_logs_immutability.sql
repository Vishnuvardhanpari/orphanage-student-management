-- Milestone 12 QA — enforce append-only audit_logs at the database layer.
-- Application code must never UPDATE or DELETE audit rows; this trigger is
-- defense-in-depth for direct SQL access. TRUNCATE is intentionally not blocked
-- (used only by privileged maintenance / test reset paths, not the app).

CREATE OR REPLACE FUNCTION prevent_audit_logs_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only: % is not allowed', TG_OP;
END;
$$;

CREATE TRIGGER trg_audit_logs_immutable
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_logs_mutation();

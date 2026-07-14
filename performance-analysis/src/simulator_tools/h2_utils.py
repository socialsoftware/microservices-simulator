import psycopg2
import logging
import os
import time
import re

ALLOCATION_SIZE = 50


class H2DBManager:
    """Class responsible for communicating with the H2 database for faster agent training."""

    _conn = None
    _cached_tables = []

    @classmethod
    def _get_connection(cls):
        if cls._conn is None or cls._conn.closed:
            try:
                db_config = {
                    "dbname": "mem:msdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                    "user": "sa",
                    "password": "sa",
                    "host": os.environ.get("DB_HOST", "127.0.0.1"),
                    "port": int(os.environ.get("H2_PORT", 1521)),
                    "sslmode": "disable"
                }
                cls._conn = psycopg2.connect(**db_config)
                cls._conn.autocommit = True
            except Exception as e:
                logging.error(f"Failed to connect to H2 database: {e}")
                raise e
        return cls._conn

    @classmethod
    def _parse_sql(cls, lines: list[str]) -> str:
        """Parses an sql file to match H2 synstax.
        Returns the entire query, as well as a list with invalid sequences to be executed later."""

        clean_lines = []
        sequence_fixes = []

        for line in lines:
            # Ignore basic administrative settings
            if line.startswith("SET ") or line.startswith("SELECT pg_catalog.set_config"):
                continue

            if "pg_catalog.setval" in line:
                seq_match = re.search(r"setval\('([^']+)'", line)
                if seq_match:
                    seq_name = seq_match.group(1).replace("public.", "")
                    parts = line.split(",", 1)
                    if len(parts) > 1:
                        val_expr = parts[1].rsplit(",", 1)[0].strip()
                        sequence_fixes.append({
                            "seq": seq_name,
                            "val_expr": val_expr
                        })
                continue

            clean_lines.append(line)

        return "".join(clean_lines), sequence_fixes

    @classmethod
    def _advance_sequences(cls, sequence: list[str], cur):
        """Manually adcanve sequence IDs to sync the database.
        It advances an extra 'ALLOCATION_SIZE' which represents the amount of IDs the simulator reserves at a time."""

        for fix in sequence:
            seq = fix["seq"]
            val_expr = fix["val_expr"]
            try:
                if val_expr.isdigit():
                    # If it's a hardcoded number (like 501)
                    next_val = int(val_expr) + ALLOCATION_SIZE
                    cur.execute(
                        f'ALTER SEQUENCE public."{seq}" RESTART WITH {next_val};')
                else:
                    # If it's a subquery (like SELECT COALESCE(MAX(id)...)
                    query = val_expr[1:-1] if val_expr.startswith("(") else val_expr
                    cur.execute(query)
                    val = cur.fetchone()[0]
                    next_val = val + ALLOCATION_SIZE
                    cur.execute(
                        f'ALTER SEQUENCE public."{seq}" RESTART WITH {next_val};')
            except Exception as seq_e:
                logging.warning(
                    f"Could not sync sequence {seq}: {seq_e}")

    @classmethod
    def _populate_db(cls, max_retries: int = 5, retry_delay: float = 0.5):
        """Populates database with baseline_data snapshot.
        It carefully parses the snapshot's SQL to match H2 syntax."""

        snapshot_path = os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "initial_config", "baseline_data.sql")

        if not os.path.exists(snapshot_path):
            logging.warning(
                f"Baseline snapshot not found at {snapshot_path}. Database will be empty.")
            return

        try:
            with open(snapshot_path, 'r', encoding='utf-8-sig') as file:
                lines = file.readlines()

            populate_query, sequence_fixes = cls._parse_sql(lines)
        except Exception as e:
            logging.error(f"Failed to read snapshot file: {e}")
            return

        for _ in range(1, max_retries + 1):
            try:
                conn = cls._get_connection()
                with conn.cursor() as cur:
                    cur.execute(populate_query)
                    cls._advance_sequences(sequence_fixes, cur)
                return
            except psycopg2.OperationalError as e:
                logging.warning(
                    f"Database connection lost during populate: {e}. Reconnecting...")
                cls._conn = None
                time.sleep(retry_delay)
            except Exception as e:
                logging.error(f"Failed to populate database critically: {e}")
                break

    @classmethod
    def setup_db_state(cls):
        """Populates the database and creates a backup for faster resets. 
        Also caches tables in memory for faster operations."""

        conn = cls._get_connection()

        # Drop any old backups if they exist
        with conn.cursor() as cur:
            cur.execute("SET REFERENTIAL_INTEGRITY FALSE;")
            # Force MVStore to immediately garbage collect old chunks instead of waiting 45s
            cur.execute("SET RETENTION_TIME 0;")
            cur.execute("DROP SCHEMA IF EXISTS backup CASCADE;")
            cur.execute(
                "SELECT table_name FROM information_schema.tables "
                "WHERE table_schema = 'public' AND table_type = 'BASE TABLE';"
            )
            existing_tables = [row[0] for row in cur.fetchall()]
            for table in existing_tables:
                cur.execute(f'TRUNCATE TABLE public."{table}";')
            cur.execute("SET REFERENTIAL_INTEGRITY TRUE;")

        cls._populate_db()
        conn = cls._get_connection()  # fallback in case connection has any issue

        # Create state backup
        with conn.cursor() as cur:
            cur.execute("CREATE SCHEMA backup;")
            cur.execute(
                "SELECT table_name FROM information_schema.tables "
                "WHERE table_schema = 'public' AND table_type = 'BASE TABLE';"
            )
            # Cache ALL public schema Tables
            cls._cached_tables = [row[0] for row in cur.fetchall()]
            for table in cls._cached_tables:
                cur.execute(
                    f'CREATE TABLE backup."{table}" AS SELECT * FROM public."{table}";')

        logging.info("Database Setup Finished!")

    @classmethod
    def reset_db_state(cls):
        """Trucantes the entire public scheme and copies the state from the backup."""

        if not cls._cached_tables:
            logging.warning(
                "Database state not initialized. Running setup first...")
            cls.setup_db_state()
            return

        conn = cls._get_connection()
        with conn.cursor() as cur:
            cur.execute("SET REFERENTIAL_INTEGRITY FALSE;")

            for table in cls._cached_tables:
                cur.execute(f'TRUNCATE TABLE public."{table}";')
                cur.execute(
                    f'INSERT INTO public."{table}" SELECT * FROM backup."{table}";')

            cur.execute("SET REFERENTIAL_INTEGRITY TRUE;")

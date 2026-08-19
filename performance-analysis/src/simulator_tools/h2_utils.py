import requests
import logging
import os
import time
import re

ALLOCATION_SIZE = 50


class H2DBManager:
    """Class responsible for communicating with the Java API for faster agent training."""

    _cached_tables = []
    _base_url = None

    @classmethod
    def _get_base_url(cls):
        if cls._base_url is None:
            # Infer the gateway port based on the worker H2_PORT assignment
            h2_port = int(os.environ.get("H2_PORT", 1522))
            worker_id = h2_port - 1521
            gateway_port = int(os.environ.get(
                "GATEWAY_PORT", 8080 + worker_id))

            host = os.environ.get("DB_HOST", "127.0.0.1")
            cls._base_url = f"http://{host}:{gateway_port}/simulator/db"
        return cls._base_url

    @classmethod
    def _execute(cls, query):
        url = f"{cls._get_base_url()}/execute"
        resp = requests.post(url, data=query.encode(
            'utf-8'), headers={'Content-Type': 'text/plain'}, timeout=10)
        resp.raise_for_status()

    @classmethod
    def _query_tables(cls):
        url = f"{cls._get_base_url()}/tables"
        resp = requests.get(url, timeout=10)
        resp.raise_for_status()
        return resp.json()

    @classmethod
    def _query_single(cls, query):
        url = f"{cls._get_base_url()}/query-single"
        resp = requests.post(url, data=query.encode(
            'utf-8'), headers={'Content-Type': 'text/plain'}, timeout=10)
        resp.raise_for_status()
        return resp.json()

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
    def _advance_sequences(cls, sequence: list[str]):
        """Manually adcanve sequence IDs to sync the database.
        It advances an extra 'ALLOCATION_SIZE' which represents the amount of IDs the simulator reserves at a time."""

        for fix in sequence:
            seq = fix["seq"]
            val_expr = fix["val_expr"]
            try:
                if val_expr.isdigit():
                    # If it's a hardcoded number (like 501)
                    next_val = int(val_expr) + ALLOCATION_SIZE
                    cls._execute(
                        f'ALTER SEQUENCE public."{seq}" RESTART WITH {next_val};')
                else:
                    # If it's a subquery (like SELECT COALESCE(MAX(id)...)
                    query = val_expr[1:-1] if val_expr.startswith("(") else val_expr
                    val = cls._query_single(query)
                    next_val = val + ALLOCATION_SIZE
                    cls._execute(
                        f'ALTER SEQUENCE public."{seq}" RESTART WITH {next_val};')
            except Exception as seq_e:
                logging.warning(f"Could not sync sequence {seq}: {seq_e}")

    @classmethod
    def _populate_db(cls, max_retries: int = 5, retry_delay: float = 0.5):
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
                cls._execute(populate_query)
                cls._advance_sequences(sequence_fixes)
                return
            except requests.RequestException as e:
                logging.warning(
                    f"Connection lost during populate: {e}. Reconnecting...")
                time.sleep(retry_delay)
            except Exception as e:
                logging.error(f"Failed to populate database critically: {e}")
                break

    @classmethod
    def setup_db_state(cls):
        """Populates the database and creates a backup for faster resets. 
        Also caches tables in memory for faster operations."""

        # Drop any old backups if they exist
        try:
            cls._execute("SET REFERENTIAL_INTEGRITY FALSE;")
            # Force MVStore to immediately garbage collect old chunks instead of waiting 45s
            cls._execute("SET RETENTION_TIME 0;")
            cls._execute("DROP SCHEMA IF EXISTS backup CASCADE;")

            existing_tables = cls._query_tables()
            for table in existing_tables:
                cls._execute(f'TRUNCATE TABLE public."{table}";')

            cls._execute("SET REFERENTIAL_INTEGRITY TRUE;")
        except Exception as e:
            logging.warning(
                f"Initial setup cleanup failed (normal if DB is empty): {e}")

        cls._populate_db()

        # Create state backup
        cls._execute("CREATE SCHEMA backup;")

        # Cache ALL public schema Tables
        cls._cached_tables = cls._query_tables()
        for table in cls._cached_tables:
            cls._execute(
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

        cls._execute("SET REFERENTIAL_INTEGRITY FALSE;")
        for table in cls._cached_tables:
            cls._execute(f'TRUNCATE TABLE public."{table}";')
            cls._execute(
                f'INSERT INTO public."{table}" SELECT * FROM backup."{table}";')
        cls._execute("SET REFERENTIAL_INTEGRITY TRUE;")

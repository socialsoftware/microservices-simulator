import psycopg2
import psycopg2.errors
import logging
import os
import time

DB_CONFIG = {
    "dbname": "msdb",
    "user": "postgres",
    "password": "postgres",
    "host": "localhost",
    "port": 5432
}


class DBManager:
    """Class responsible for communicating with the postgres database."""

    _conn = None

    @classmethod
    def _get_connection(cls):
        """Returns the persistent connection, automatically reconnecting if it's dead or closed."""

        if cls._conn is None or cls._conn.closed:
            try:
                cls._conn = psycopg2.connect(**DB_CONFIG)
                cls._conn.autocommit = True
            except Exception as e:
                logging.error(f"Failed to connect to database: {e}")
                raise e
        return cls._conn

    @classmethod
    def reset_database(cls, max_retries=10, retry_delay=0.5):
        """
        Truncates all tables in the public schema and restarts identity.
        If blocked by simulator, it gently cancels the specific queries causing the blockage.
        """

        truncate_sql = """
        DO $$ DECLARE
            r RECORD;
        BEGIN
            FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
                EXECUTE 'TRUNCATE TABLE public.' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE';
            END LOOP;
        END $$;
        """

        cancel_locks_sql = """
        SELECT pg_cancel_backend(pid) 
        FROM pg_stat_activity 
        WHERE datname = current_database() 
        AND pid <> pg_backend_pid()
        AND state = 'active';
        """

        for attempt in range(1, max_retries + 1):
            try:
                conn = cls._get_connection()
                with conn.cursor() as cur:
                    # Set up a time limit to wait for locks
                    cur.execute("SET lock_timeout = '200ms';")
                    cur.execute(truncate_sql)

                logging.info(
                    f"Database reset successfully!")
                return

            except (psycopg2.errors.LockNotAvailable, psycopg2.errors.DeadlockDetected):
                logging.warning(
                    f"Database locked by Java simulator (Attempt {attempt}/{max_retries})...")
                try:
                    # Rollback the failed TRUNCATE transaction so we can execute new commands
                    conn.rollback()
                    with conn.cursor() as cur:
                        cur.execute(cancel_locks_sql)
                except Exception as cancel_e:
                    logging.error(f"Failed to cancel locks: {cancel_e}")

                time.sleep(retry_delay)
            except psycopg2.OperationalError as e:
                logging.warning(
                    f"Database connection lost: {e}. Reconnecting...")
                cls._conn = None
                time.sleep(retry_delay)
            except Exception as e:
                logging.error(f"Failed to reset database critically: {e}")
                break

        logging.error("Failed to reset database after maximum retries.")

    @classmethod
    def populate_database(cls, max_retries=5, retry_delay=0.5):
        """
        Populates the database with the baseline SQL snapshot.
        """

        snapshot_path = os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "initial_config", "baseline_data.sql")

        if not os.path.exists(snapshot_path):
            logging.warning(
                f"Baseline snapshot not found at {snapshot_path}. Database will be empty.")
            return

        try:
            with open(snapshot_path, 'r', encoding='utf-8-sig') as file:
                seed_sql = file.read()
        except Exception as e:
            logging.error(f"Failed to read snapshot file: {e}")
            return

        for _ in range(1, max_retries + 1):
            try:
                conn = cls._get_connection()
                with conn.cursor() as cur:
                    cur.execute(seed_sql)

                logging.info(
                    f"Database populated successfully with baseline snapshot!")
                return

            except psycopg2.OperationalError as e:
                logging.warning(
                    f"Database connection lost during seed: {e}. Reconnecting...")
                cls._conn = None
                time.sleep(retry_delay)
            except Exception as e:
                logging.error(f"Failed to populate database critically: {e}")
                break

        logging.error("Failed to populate database after maximum retries.")

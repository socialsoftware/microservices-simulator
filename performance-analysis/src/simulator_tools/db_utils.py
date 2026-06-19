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
    def reset_database(cls, max_retries: int = 10, retry_delay: float = 0.5):
        """
        Truncates all tables in the public schema and restarts identity.
        If blocked by simulator, it gently cancels the specific queries causing the blockage.
        """

        # Truncate is physically faster than Delete because it just dumps the file on disk instead of scanning rows.
        # Restart Identity resets auto-incrementing primary keys back to 1.
        # Essentially it resets the public tables without droping them.
        truncate_sql = """
        DO $$ DECLARE
            r RECORD;
        BEGIN
            FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
                EXECUTE 'TRUNCATE TABLE public.' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE';
            END LOOP;
        END $$;
        """

        # Asks Postgres to forcefully disconnect any Java connection currently holding a lock or idling.
        cancel_locks_sql = """
        SELECT pg_terminate_backend(pid) 
        FROM pg_stat_activity 
        WHERE datname = current_database() 
        AND pid <> pg_backend_pid();
        """

        for attempt in range(1, max_retries + 1):
            try:
                conn = cls._get_connection()
                with conn.cursor() as cur:
                    # Set up a time limit to wait for locks
                    cur.execute("SET lock_timeout = '200ms';")
                    cur.execute(truncate_sql)
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

        raise Exception("Failed to reset database after maximum retries!")

    @classmethod
    def populate_database(cls, max_retries: int = 5, retry_delay: float = 0.5):
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
                populate_sql = file.read()
        except Exception as e:
            logging.error(f"Failed to read snapshot file: {e}")
            return

        for _ in range(1, max_retries + 1):
            try:
                conn = cls._get_connection()
                with conn.cursor() as cur:
                    cur.execute(populate_sql)
                return

            except psycopg2.OperationalError as e:
                logging.warning(
                    f"Database connection lost during populate: {e}. Reconnecting...")
                cls._conn = None
                time.sleep(retry_delay)
            except Exception as e:
                logging.error(f"Failed to populate database critically: {e}")
                break

        raise Exception("Failed to populate database after maximum retries!")

    @classmethod
    def convert_tables_to_unlogged(cls):
        """
        Converts all tables in the public schema to UNLOGGED.
        This completely bypasses disk Write-Ahead Logging (WAL) for massive speedups.
        """

        # Normally postgres writes every change to disk twice, once to a Write-Ahead Log (WAL) and then to the actual table file.
        # By setting tables to UNLOGGED, postgres turns off the WAL. All database changes are kept primarily in RAM and synced to disk lazily.
        # This speeds up DB operations quite a lot at the cost of data corrupting when the computer loses power, which in our case
        # does not matter because this is a simulation.

        # Implements a "Topological Sort" retry loop
        unlogged_sql = """
        DO $$ 
        DECLARE
            r RECORD;
            converted_any BOOLEAN;
        BEGIN
            LOOP
                converted_any := FALSE;
                FOR r IN (
                    SELECT tablename FROM pg_tables 
                    WHERE schemaname = 'public' 
                      AND tablename NOT IN (
                          SELECT c.relname FROM pg_class c 
                          JOIN pg_namespace n ON n.oid = c.relnamespace 
                          WHERE n.nspname = 'public' AND c.relpersistence = 'u'
                      )
                ) LOOP
                    BEGIN
                        EXECUTE 'ALTER TABLE public.' || quote_ident(r.tablename) || ' SET UNLOGGED';
                        converted_any := TRUE;
                    EXCEPTION WHEN OTHERS THEN
                        -- Ignore foreign key dependency errors.
                        -- The table will be converted in a subsequent loop iteration 
                        -- once its dependent tables are converted.
                    END;
                END LOOP;
                
                -- Exit loop if no tables were converted in this pass (all done, or stuck)
                EXIT WHEN NOT converted_any;
            END LOOP;
        END $$;
        """

        try:
            conn = cls._get_connection()
            with conn.cursor() as cur:
                cur.execute(unlogged_sql)
            logging.info(
                "Successfully converted all tables to UNLOGGED.")
        except Exception as e:
            logging.error(f"Failed to convert tables to UNLOGGED: {e}")

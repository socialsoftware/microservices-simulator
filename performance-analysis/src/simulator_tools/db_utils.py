import psycopg2
import logging

DB_CONFIG = {
    "dbname": "msdb",
    "user": "postgres",
    "password": "postgres",
    "host": "localhost",
    "port": 5432
}


class DBManager:
    """Class responsible for communicating with the postgres database."""

    @staticmethod
    def reset_database():
        """
        Truncates all tables in the public schema.
        Restarts identity (auto-increment counters) and cascades to ignore foreign keys.
        """

        sql = """
        DO $$ DECLARE
            r RECORD;
        BEGIN
            FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
                EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE';
            END LOOP;
        END $$;
        """

        try:
            conn = psycopg2.connect(**DB_CONFIG)
            conn.autocommit = True  # We don't need a transaction for this script

            with conn.cursor() as cur:
                cur.execute(sql)

            conn.close()
            logging.info("Database reset successfully7.")
        except Exception as e:
            logging.error(f"Failed to reset database: {e}")

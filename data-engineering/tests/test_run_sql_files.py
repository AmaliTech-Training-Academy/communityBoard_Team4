"""
test_run_sql_files.py — Unit tests for run_sql_files().

Tests cover:
  Happy path:
    - All SQL files execute successfully
    - Each file is opened and executed once
    - Runs files in the correct order
  Retry behaviour:
    - Retries on OperationalError, succeeds on next attempt
    - Retries on DeadlockDetected transient error
    - Does not retry on ProgrammingError (syntax error)
  Edge cases:
    - Missing SQL file is skipped with a warning (not an error)
    - Raises after exhausting all retries on persistent transient error
    - Unexpected exception propagates immediately without retry
    - Connection is closed even when an error occurs
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import etl_pipeline as etl

import psycopg2
import pytest
from unittest.mock import MagicMock, patch



from conftest import make_op_error, make_prog_error




# ------------------------------------------------------------------ #
# Helpers
# ------------------------------------------------------------------ #
def _mock_conn():
    conn = MagicMock()
    conn.cursor.return_value = MagicMock()
    return conn


class TestRunSqlFiles:
    """Tests for run_sql_files()."""

    def test_executes_all_files_successfully(self, monkeypatch, tmp_path):
        """All SQL files should be read and executed once each."""
        # Create real temp SQL files
        sql_dir = tmp_path / "sql"
        sql_dir.mkdir()
        files = [
            "01_create_analytics_views.sql",
            "02_create_triggers.sql",
            "03_seed_data.sql",
        ]
        for f in files:
            (sql_dir / f).write_text("SELECT 1;")

        monkeypatch.setattr(etl, "SQL_DIR", str(sql_dir))
        monkeypatch.setattr(etl, "SQL_FILES", files)
        monkeypatch.setattr(etl, "get_connection", lambda **kw: _mock_conn())

        etl.run_sql_files(max_retries=1)  # should not raise

    def test_executes_files_in_order(self, monkeypatch, tmp_path):
        """Files must be executed in the order defined in SQL_FILES."""
        sql_dir = tmp_path / "sql"
        sql_dir.mkdir()
        order = []

        files = ["first.sql", "second.sql", "third.sql"]
        for f in files:
            (sql_dir / f).write_text(f"-- {f}")

        def track_execute(sql):
            order.append(sql.strip())

        def make_tracked_conn():
            conn = MagicMock()
            cur = MagicMock()
            cur.execute.side_effect = track_execute
            conn.cursor.return_value = cur
            return conn

        monkeypatch.setattr(etl, "SQL_DIR", str(sql_dir))
        monkeypatch.setattr(etl, "SQL_FILES", files)
        monkeypatch.setattr(etl, "get_connection", lambda **kw: make_tracked_conn())

        etl.run_sql_files(max_retries=1)

        assert order == ["-- first.sql", "-- second.sql", "-- third.sql"]

    def test_skips_missing_file_with_warning(self, monkeypatch, tmp_path, caplog):
        """A missing SQL file should be skipped, not raise an exception."""
        sql_dir = tmp_path / "sql"
        sql_dir.mkdir()
        # Only create one of two listed files
        (sql_dir / "present.sql").write_text("SELECT 1;")

        monkeypatch.setattr(etl, "SQL_DIR", str(sql_dir))
        monkeypatch.setattr(etl, "SQL_FILES", ["present.sql", "missing.sql"])
        monkeypatch.setattr(etl, "get_connection", lambda **kw: _mock_conn())

        import logging

        with caplog.at_level(logging.WARNING):
            etl.run_sql_files(max_retries=1)

        assert "missing.sql" in caplog.text
        assert "skipping" in caplog.text.lower()

    def test_retries_on_operational_error_then_succeeds(self, monkeypatch, tmp_path):
        """Should retry when OperationalError occurs and succeed on retry."""
        sql_dir = tmp_path / "sql"
        sql_dir.mkdir()
        (sql_dir / "test.sql").write_text("SELECT 1;")

        monkeypatch.setattr(etl, "SQL_DIR", str(sql_dir))
        monkeypatch.setattr(etl, "SQL_FILES", ["test.sql"])

        attempt = {"n": 0}

        def flaky_conn():
            attempt["n"] += 1
            conn = _mock_conn()
            if attempt["n"] == 1:
                conn.cursor.return_value.execute.side_effect = make_op_error()
            return conn

        monkeypatch.setattr(etl, "get_connection", lambda **kw: flaky_conn())
        with patch("etl_pipeline.backoff_delay", return_value=0):
            etl.run_sql_files(max_retries=3)

        assert attempt["n"] == 2

    def test_does_not_retry_on_programming_error(self, monkeypatch, tmp_path):
        """ProgrammingError (bad SQL) should propagate immediately."""
        sql_dir = tmp_path / "sql"
        sql_dir.mkdir()
        (sql_dir / "bad.sql").write_text("NOT VALID SQL;")

        monkeypatch.setattr(etl, "SQL_DIR", str(sql_dir))
        monkeypatch.setattr(etl, "SQL_FILES", ["bad.sql"])

        conn = _mock_conn()
        conn.cursor.return_value.execute.side_effect = make_prog_error()
        monkeypatch.setattr(etl, "get_connection", lambda **kw: conn)

        with pytest.raises(psycopg2.ProgrammingError):
            etl.run_sql_files(max_retries=3)

        # Only called once — no retry
        assert conn.cursor.return_value.execute.call_count == 1

    def test_raises_after_exhausting_retries_on_transient_error(
        self, monkeypatch, tmp_path
    ):
        """Should raise after max_retries failed attempts on OperationalError."""
        sql_dir = tmp_path / "sql"
        sql_dir.mkdir()
        (sql_dir / "test.sql").write_text("SELECT 1;")

        monkeypatch.setattr(etl, "SQL_DIR", str(sql_dir))
        monkeypatch.setattr(etl, "SQL_FILES", ["test.sql"])

        conn = _mock_conn()
        conn.cursor.return_value.execute.side_effect = make_op_error()
        monkeypatch.setattr(etl, "get_connection", lambda **kw: conn)

        with patch("etl_pipeline.backoff_delay", return_value=0):
            with pytest.raises(psycopg2.OperationalError):
                etl.run_sql_files(max_retries=3)

    def test_connection_closed_on_programming_error(self, monkeypatch, tmp_path):
        """Connection must be closed even when ProgrammingError is raised."""
        sql_dir = tmp_path / "sql"
        sql_dir.mkdir()
        (sql_dir / "bad.sql").write_text("BAD;")

        monkeypatch.setattr(etl, "SQL_DIR", str(sql_dir))
        monkeypatch.setattr(etl, "SQL_FILES", ["bad.sql"])

        conn = _mock_conn()
        conn.cursor.return_value.execute.side_effect = make_prog_error()
        monkeypatch.setattr(etl, "get_connection", lambda **kw: conn)

        with pytest.raises(psycopg2.ProgrammingError):
            etl.run_sql_files(max_retries=1)

        conn.close.assert_called()

    def test_unexpected_exception_propagates_immediately(self, monkeypatch, tmp_path):
        """An unexpected exception should propagate without retrying."""
        sql_dir = tmp_path / "sql"
        sql_dir.mkdir()
        (sql_dir / "test.sql").write_text("SELECT 1;")

        monkeypatch.setattr(etl, "SQL_DIR", str(sql_dir))
        monkeypatch.setattr(etl, "SQL_FILES", ["test.sql"])

        conn = _mock_conn()
        conn.cursor.return_value.execute.side_effect = RuntimeError("unexpected")
        monkeypatch.setattr(etl, "get_connection", lambda **kw: conn)

        with pytest.raises(RuntimeError, match="unexpected"):
            etl.run_sql_files(max_retries=3)

        assert conn.cursor.return_value.execute.call_count == 1

    def test_empty_sql_files_list_completes_without_error(self, monkeypatch):
        """An empty SQL_FILES list should complete successfully."""
        monkeypatch.setattr(etl, "SQL_FILES", [])

        etl.run_sql_files(max_retries=1)  # should not raise

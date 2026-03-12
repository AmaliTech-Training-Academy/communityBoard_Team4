"""
test_wait_for_postgres.py — Unit tests for wait_for_postgres().

Tests cover:
  Happy path:
    - Connects on first attempt and returns without error
    - Connects after initial failures (retries work)
    - Closes the probe connection after success
  Retry behaviour:
    - Calls backoff_delay between each failed attempt
    - Retries exactly max_retries times before raising
  Edge cases:
    - Raises RuntimeError when all retries exhausted
    - Error message contains attempt count and last error
    - max_retries=1 raises immediately on first failure
"""

import psycopg2
import pytest
from unittest.mock import MagicMock, call, patch

import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
import etl_pipeline as etl
from conftest import make_op_error


class TestWaitForPostgres:
    """Tests for wait_for_postgres()."""

    def test_succeeds_on_first_attempt(self, mock_connect):
        """Should return immediately when postgres is available."""
        fake_conn = MagicMock()
        mock_connect.return_value = fake_conn

        etl.wait_for_postgres(max_retries=3)

        mock_connect.assert_called_once()
        fake_conn.close.assert_called_once()

    def test_succeeds_after_two_failures(self, mock_connect):
        """Should return successfully after two failures then a success."""
        fake_conn = MagicMock()
        mock_connect.side_effect = [
            make_op_error(),
            make_op_error(),
            fake_conn,
        ]

        etl.wait_for_postgres(max_retries=3)

        assert mock_connect.call_count == 3
        fake_conn.close.assert_called_once()

    def test_raises_after_all_retries_exhausted(self, mock_connect):
        """Should raise RuntimeError when postgres never becomes reachable."""
        mock_connect.side_effect = make_op_error("connection refused")

        with pytest.raises(RuntimeError, match="did not become reachable"):
            etl.wait_for_postgres(max_retries=3)

    def test_attempt_count_in_error_message(self, mock_connect):
        """RuntimeError message should include the max_retries count."""
        mock_connect.side_effect = make_op_error()

        with pytest.raises(RuntimeError, match="3"):
            etl.wait_for_postgres(max_retries=3)

    def test_last_error_included_in_message(self, mock_connect):
        """RuntimeError should surface the last OperationalError."""
        mock_connect.side_effect = make_op_error("host unreachable")

        with pytest.raises(RuntimeError, match="host unreachable"):
            etl.wait_for_postgres(max_retries=2)

    def test_exact_retry_count(self, mock_connect):
        """connect() should be called exactly max_retries times on failure."""
        mock_connect.side_effect = make_op_error()

        with pytest.raises(RuntimeError):
            etl.wait_for_postgres(max_retries=5)

        assert mock_connect.call_count == 5

    def test_max_retries_one_raises_immediately(self, mock_connect):
        """max_retries=1 should raise after a single failed attempt."""
        mock_connect.side_effect = make_op_error()

        with pytest.raises(RuntimeError):
            etl.wait_for_postgres(max_retries=1)

        assert mock_connect.call_count == 1

    def test_backoff_called_on_each_failure(self, mock_connect):
        """backoff_delay should be called once per failed attempt."""
        mock_connect.side_effect = make_op_error()

        with patch("etl_pipeline.backoff_delay", return_value=0) as mock_backoff:
            with pytest.raises(RuntimeError):
                etl.wait_for_postgres(max_retries=4)

        assert mock_backoff.call_count == 4
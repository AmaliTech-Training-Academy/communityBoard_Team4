"""
test_get_connection.py — Unit tests for get_connection().

Tests cover:
  Happy path:
    - Connects successfully on first attempt
    - Returns connection with autocommit=False by default
    - Sets ISOLATION_LEVEL_AUTOCOMMIT when autocommit=True
  Retry behaviour:
    - Retries on OperationalError and succeeds on second attempt
    - Retries exact number of times before raising
  Edge cases:
    - Exhausts all retries, raises RuntimeError with message
    - Does not retry on non-OperationalError exceptions
"""

import psycopg2
import pytest
from unittest.mock import MagicMock, call, patch

import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
import etl_pipeline as etl
from conftest import make_op_error


class TestGetConnection:
    """Tests for get_connection()."""

    def test_returns_connection_on_first_attempt(self, mock_connect):
        """Should return the connection immediately when connect succeeds."""
        fake_conn = MagicMock()
        mock_connect.return_value = fake_conn

        result = etl.get_connection()

        assert result is fake_conn
        mock_connect.assert_called_once()

    def test_autocommit_false_by_default(self, mock_connect):
        """Should not set isolation level when autocommit=False (default)."""
        fake_conn = MagicMock()
        mock_connect.return_value = fake_conn

        etl.get_connection(autocommit=False)

        fake_conn.set_isolation_level.assert_not_called()

    def test_autocommit_true_sets_isolation_level(self, mock_connect):
        """Should set ISOLATION_LEVEL_AUTOCOMMIT when autocommit=True."""
        from psycopg2.extensions import ISOLATION_LEVEL_AUTOCOMMIT

        fake_conn = MagicMock()
        mock_connect.return_value = fake_conn

        etl.get_connection(autocommit=True)

        fake_conn.set_isolation_level.assert_called_once_with(
            ISOLATION_LEVEL_AUTOCOMMIT
        )

    def test_retries_on_operational_error_then_succeeds(self, mock_connect):
        """Should retry after OperationalError and return conn on success."""
        fake_conn = MagicMock()
        mock_connect.side_effect = [make_op_error(), fake_conn]

        result = etl.get_connection(retries=2)

        assert result is fake_conn
        assert mock_connect.call_count == 2

    def test_raises_runtime_error_after_all_retries_exhausted(self, mock_connect):
        """Should raise RuntimeError when all retry attempts fail."""
        mock_connect.side_effect = make_op_error("DB down")

        with pytest.raises(RuntimeError, match="Could not establish database"):
            etl.get_connection(retries=3)

        assert mock_connect.call_count == 3

    def test_retry_count_matches_retries_param(self, mock_connect):
        """Should attempt exactly `retries` times before giving up."""
        mock_connect.side_effect = make_op_error()

        with pytest.raises(RuntimeError):
            etl.get_connection(retries=5)

        assert mock_connect.call_count == 5

    def test_does_not_retry_on_unexpected_exception(self, mock_connect):
        """Non-OperationalError exceptions should propagate immediately."""
        mock_connect.side_effect = ValueError("unexpected")

        with pytest.raises(ValueError):
            etl.get_connection(retries=3)

        # Should only be called once — no retry on unexpected errors
        assert mock_connect.call_count == 1
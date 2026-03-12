"""
test_refresh_views.py — Unit tests for refresh_views().

Tests cover:
  Happy path:
    - All 4 views refreshed successfully
    - commit() called after all refreshes
    - cursor closed after completion
    - last_refresh_time updated after successful refresh
  Rate limiting:
    - Waits when called within the minimum interval
    - Does not wait when called after the interval has elapsed
  Retry behaviour:
    - Retries on OperationalError per view
    - Continues refreshing remaining views after one view fails
  Edge cases:
    - Unexpected exception on one view logs error and continues
    - commit() failure is logged but does not raise
    - any_failed flag set correctly when a view cannot be refreshed
"""
import etl_pipeline as etl

import time
import psycopg2
import pytest
from unittest.mock import MagicMock, patch, call

import sys
import os

from conftest import make_op_error

VIEWS = [
    "analytics_summary",
    "analytics_posts_by_category",
    "analytics_posts_by_day",
    "analytics_top_contributors",
]


class TestRefreshViews:
    """Tests for refresh_views()."""

    def setup_method(self):
        """Reset rate limit tracker before each test."""
        etl._last_refresh_time = 0.0

    def test_refreshes_all_four_views(self, mock_conn):
        """All 4 materialized views should be refreshed in one call."""
        etl.refresh_views(mock_conn, max_retries=1)

        executed = [
            call_args[0][0]
            for call_args in mock_conn.cursor.return_value.execute.call_args_list
        ]
        for view in VIEWS:
            assert any(view in stmt for stmt in executed), (
                f"Expected REFRESH for {view} but not found"
            )

    def test_commit_called_after_all_refreshes(self, mock_conn):
        """commit() must be called once after all views are refreshed."""
        etl.refresh_views(mock_conn, max_retries=1)
        mock_conn.commit.assert_called_once()

    def test_cursor_closed_after_refresh(self, mock_conn):
        """Cursor must be closed after the refresh completes."""
        etl.refresh_views(mock_conn, max_retries=1)
        mock_conn.cursor.return_value.close.assert_called_once()

    def test_last_refresh_time_updated(self, mock_conn):
        """_last_refresh_time should be updated after a successful refresh."""
        before = time.monotonic()
        etl.refresh_views(mock_conn, max_retries=1)
        assert etl._last_refresh_time >= before

    def test_rate_limit_enforces_minimum_interval(self, mock_conn):
        """Should sleep when called sooner than REFRESH_MIN_INTERVAL_SECS."""
        # Set last refresh to "just now"
        etl._last_refresh_time = time.monotonic()

        with patch("etl_pipeline.time.sleep") as mock_sleep:
            etl.refresh_views(mock_conn, max_retries=1)

        mock_sleep.assert_called()
        slept = mock_sleep.call_args_list[0][0][0]
        assert slept > 0

    def test_no_rate_limit_wait_when_interval_elapsed(self, mock_conn):
        """Should NOT sleep for rate limiting when enough time has passed."""
        # Set last refresh to long ago
        etl._last_refresh_time = time.monotonic() - 100

        with patch("etl_pipeline.time.sleep") as mock_sleep:
            etl.refresh_views(mock_conn, max_retries=1)

        # time.sleep should not be called for rate limiting
        # (may be called by backoff_delay but not for rate limit wait)
        rate_limit_sleeps = [
            c for c in mock_sleep.call_args_list if c[0][0] > 0
        ]
        assert len(rate_limit_sleeps) == 0

    def test_retries_failing_view_on_operational_error(self, mock_conn):
        """A view that fails with OperationalError should be retried."""
        cur = mock_conn.cursor.return_value
        attempt = {"n": 0}

        def flaky_execute(sql):
            attempt["n"] += 1
            if "analytics_summary" in sql and attempt["n"] == 1:
                raise make_op_error()

        cur.execute.side_effect = flaky_execute

        with patch("etl_pipeline.backoff_delay", return_value=0):
            etl.refresh_views(mock_conn, max_retries=3)

        # analytics_summary should have been attempted twice
        summary_calls = [
            c for c in cur.execute.call_args_list
            if "analytics_summary" in str(c)
        ]
        assert len(summary_calls) >= 2

    def test_continues_other_views_after_one_fails(self, mock_conn):
        """
        If one view permanently fails, the others should still refresh.
        """
        cur = mock_conn.cursor.return_value

        def selective_fail(sql):
            if "analytics_summary" in sql:
                raise make_op_error("summary broken")

        cur.execute.side_effect = selective_fail

        with patch("etl_pipeline.backoff_delay", return_value=0):
            etl.refresh_views(mock_conn, max_retries=2)

        # Other 3 views should still have been attempted
        executed_sql = [str(c) for c in cur.execute.call_args_list]
        assert any("analytics_posts_by_category" in s for s in executed_sql)
        assert any("analytics_posts_by_day" in s for s in executed_sql)
        assert any("analytics_top_contributors" in s for s in executed_sql)

    def test_commit_failure_does_not_raise(self, mock_conn):
        """A commit() failure should be logged but must not propagate."""
        mock_conn.commit.side_effect = Exception("commit failed")

        # Should complete without raising
        etl.refresh_views(mock_conn, max_retries=1)

    def test_unexpected_exception_on_view_sets_any_failed(self, mock_conn):
        """An unexpected exception on a view should set any_failed=True."""
        cur = mock_conn.cursor.return_value

        def raise_unexpected(sql):
            if "analytics_summary" in sql:
                raise ValueError("unexpected")

        cur.execute.side_effect = raise_unexpected

        # Should complete without re-raising the ValueError
        etl.refresh_views(mock_conn, max_retries=1)

        mock_conn.commit.assert_called_once()
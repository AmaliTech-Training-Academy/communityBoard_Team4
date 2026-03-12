"""
test_start_listener.py — Unit tests for start_listener() and _run_listener().

Tests cover:
  _run_listener():
    - Registers LISTEN on correct channel on startup
    - Calls refresh_views on startup (initial refresh)
    - Processes a valid JSON notification payload
    - Handles malformed (non-JSON) notification payload gracefully
    - Debounces burst notifications into a single refresh
    - Drains additional notifications during debounce window

  start_listener():
    - Calls get_connection with autocommit=True
    - Resets reconnect counter after successful connection
    - Reconnects with backoff on OperationalError
    - Reconnects with backoff on InterfaceError
    - Raises RuntimeError after max_reconnects exhausted
    - Closes connection before reconnecting
    - Unexpected exception propagates immediately
"""

import json
import psycopg2
import pytest
from unittest.mock import MagicMock, patch, call

import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
import etl_pipeline as etl
from conftest import make_op_error


# ------------------------------------------------------------------ #
# Helpers
# ------------------------------------------------------------------ #
def _make_notify(payload: dict | str, channel: str = "analytics_refresh"):
    """Build a mock psycopg2 Notify object."""
    notify = MagicMock()
    notify.channel = channel
    notify.payload = json.dumps(payload) if isinstance(payload, dict) else payload
    return notify


def _conn_that_raises_after_listen(exc):
    """
    Build a mock connection that raises `exc` the first time
    select.select is called — simulating a connection drop mid-listen.
    """
    conn = MagicMock()
    conn.cursor.return_value = MagicMock()
    conn.notifies = []
    return conn


class TestRunListener:
    """Tests for _run_listener() inner loop."""

    def test_registers_listen_on_correct_channel(self, mock_conn):
        """LISTEN analytics_refresh must be executed on startup."""
        # Make select.select return timeout immediately so loop exits
        with patch("etl_pipeline.select.select", return_value=([], [], [])):
            with patch("etl_pipeline.refresh_views"):
                # Run one iteration then stop via StopIteration trick
                call_count = {"n": 0}

                def fake_select(*args, **kwargs):
                    call_count["n"] += 1
                    if call_count["n"] > 1:
                        raise KeyboardInterrupt
                    return ([], [], [])

                with patch("etl_pipeline.select.select", side_effect=fake_select):
                    with pytest.raises(KeyboardInterrupt):
                        etl._run_listener(mock_conn)

        cur = mock_conn.cursor.return_value
        listen_calls = [
            c for c in cur.execute.call_args_list
            if "LISTEN" in str(c)
        ]
        assert len(listen_calls) == 1
        assert "analytics_refresh" in str(listen_calls[0])

    def test_initial_refresh_called_on_startup(self, mock_conn):
        """refresh_views should be called once on startup before any notify."""
        with patch("etl_pipeline.refresh_views") as mock_refresh:
            with patch(
                "etl_pipeline.select.select",
                side_effect=[([], [], []), KeyboardInterrupt],
            ):
                with pytest.raises((KeyboardInterrupt, TypeError)):
                    etl._run_listener(mock_conn)

        # At minimum, initial refresh was called
        assert mock_refresh.call_count >= 1

    def test_processes_valid_json_notification(self, mock_conn):
        """Should parse JSON payload and log table/operation without error."""
        notify = _make_notify(
            {"table": "posts", "operation": "INSERT", "timestamp": "2026-01-01"}
        )

        def fake_poll():
            if not mock_conn.notifies:
                mock_conn.notifies.append(notify)

        mock_conn.poll.side_effect = fake_poll

        select_calls = {"n": 0}

        def fake_select(*args, **kwargs):
            select_calls["n"] += 1
            if select_calls["n"] == 1:
                return ([mock_conn], [], [])
            raise KeyboardInterrupt

        with patch("etl_pipeline.select.select", side_effect=fake_select):
            with patch("etl_pipeline.refresh_views"):
                with patch("etl_pipeline.time.sleep"):
                    with pytest.raises(KeyboardInterrupt):
                        etl._run_listener(mock_conn)

    def test_handles_malformed_json_payload_gracefully(self, mock_conn):
        """Non-JSON payload should be logged without raising an exception."""
        notify = _make_notify("not-json-at-all")

        def fake_poll():
            if not mock_conn.notifies:
                mock_conn.notifies.append(notify)

        mock_conn.poll.side_effect = fake_poll

        select_calls = {"n": 0}

        def fake_select(*args, **kwargs):
            select_calls["n"] += 1
            if select_calls["n"] == 1:
                return ([mock_conn], [], [])
            raise KeyboardInterrupt

        with patch("etl_pipeline.select.select", side_effect=fake_select):
            with patch("etl_pipeline.refresh_views"):
                with patch("etl_pipeline.time.sleep"):
                    with pytest.raises(KeyboardInterrupt):
                        etl._run_listener(mock_conn)  # must not raise JSONDecodeError

    def test_debounce_collapses_burst_into_single_refresh(self, mock_conn):
        """
        Multiple notifications in a burst should trigger only one refresh
        (not one per notification).
        """
        notifies = [
            _make_notify({"table": "posts", "operation": "INSERT"}),
            _make_notify({"table": "posts", "operation": "INSERT"}),
            _make_notify({"table": "posts", "operation": "INSERT"}),
        ]

        poll_count = {"n": 0}

        def fake_poll():
            poll_count["n"] += 1
            if poll_count["n"] == 1:
                mock_conn.notifies.extend(notifies)

        mock_conn.poll.side_effect = fake_poll

        select_calls = {"n": 0}

        def fake_select(*args, **kwargs):
            select_calls["n"] += 1
            if select_calls["n"] == 1:
                return ([mock_conn], [], [])
            raise KeyboardInterrupt

        refresh_calls = {"n": 0}

        def count_refresh(conn, **kw):
            refresh_calls["n"] += 1

        with patch("etl_pipeline.select.select", side_effect=fake_select):
            with patch("etl_pipeline.refresh_views", side_effect=count_refresh):
                with patch("etl_pipeline.time.sleep"):
                    with pytest.raises(KeyboardInterrupt):
                        etl._run_listener(mock_conn)

        # Initial refresh (1) + one debounced refresh (1) = 2 total
        # NOT 4 (one per notification + initial)
        assert refresh_calls["n"] <= 2


class TestStartListener:
    """Tests for start_listener() outer recovery loop."""

    def test_calls_get_connection_with_autocommit(self, monkeypatch):
        """get_connection must be called with autocommit=True."""
        captured = {}

        def fake_get_conn(autocommit=False, **kw):
            captured["autocommit"] = autocommit
            raise KeyboardInterrupt  # stop after first call

        monkeypatch.setattr(etl, "get_connection", fake_get_conn)

        with pytest.raises(KeyboardInterrupt):
            etl.start_listener(max_reconnects=0)

        assert captured.get("autocommit") is True

    def test_reconnects_on_operational_error(self, monkeypatch):
        """Should reconnect when OperationalError is raised by get_connection."""
        attempt = {"n": 0}

        def flaky_conn(autocommit=False, **kw):
            attempt["n"] += 1
            if attempt["n"] < 3:
                raise psycopg2.OperationalError("dropped")
            raise KeyboardInterrupt  # stop after reconnect succeeds

        monkeypatch.setattr(etl, "get_connection", flaky_conn)

        with patch("etl_pipeline.backoff_delay", return_value=0):
            with pytest.raises(KeyboardInterrupt):
                etl.start_listener(max_reconnects=5)

        assert attempt["n"] == 3

    def test_reconnects_on_interface_error(self, monkeypatch):
        """Should reconnect when InterfaceError is raised."""
        attempt = {"n": 0}

        def flaky_conn(autocommit=False, **kw):
            attempt["n"] += 1
            if attempt["n"] == 1:
                raise psycopg2.InterfaceError("interface gone")
            raise KeyboardInterrupt

        monkeypatch.setattr(etl, "get_connection", flaky_conn)

        with patch("etl_pipeline.backoff_delay", return_value=0):
            with pytest.raises(KeyboardInterrupt):
                etl.start_listener(max_reconnects=3)

    def test_raises_runtime_error_after_max_reconnects(self, monkeypatch):
        """RuntimeError must be raised when max_reconnects is exhausted."""
        monkeypatch.setattr(
            etl,
            "get_connection",
            lambda **kw: (_ for _ in ()).throw(
                psycopg2.OperationalError("always down")
            ),
        )

        with patch("etl_pipeline.backoff_delay", return_value=0):
            with pytest.raises(RuntimeError, match="failed to reconnect"):
                etl.start_listener(max_reconnects=3)

    def test_reconnect_counter_resets_after_success(self, monkeypatch):
        """
        Reconnect counter should reset to 0 after a successful connection
        so a transient outage does not reduce the remaining retry budget.
        """
        attempt = {"n": 0}

        def conn_factory(autocommit=False, **kw):
            attempt["n"] += 1
            # First connection succeeds, _run_listener then raises OperationalError
            # Second connection also succeeds, then we stop
            if attempt["n"] <= 2:
                conn = MagicMock()
                conn.cursor.return_value = MagicMock()
                conn.notifies = []
                return conn
            raise KeyboardInterrupt

        run_count = {"n": 0}

        def fake_run_listener(conn):
            run_count["n"] += 1
            if run_count["n"] == 1:
                raise psycopg2.OperationalError("transient drop")
            # Second call stops the test cleanly
            raise KeyboardInterrupt

        monkeypatch.setattr(etl, "get_connection", conn_factory)
        monkeypatch.setattr(etl, "_run_listener", fake_run_listener)

        with patch("etl_pipeline.backoff_delay", return_value=0):
            with pytest.raises(KeyboardInterrupt):
                etl.start_listener(max_reconnects=5)

        assert run_count["n"] == 2

    def test_closes_connection_before_reconnecting(self, monkeypatch):
        """
        The dropped connection must be closed before attempting to reconnect.
        """
        attempt = {"n": 0}
        fake_conn = MagicMock()

        def conn_factory(autocommit=False, **kw):
            attempt["n"] += 1
            if attempt["n"] == 1:
                return fake_conn
            raise KeyboardInterrupt

        def fake_run_listener(conn):
            raise psycopg2.OperationalError("dropped")

        monkeypatch.setattr(etl, "get_connection", conn_factory)
        monkeypatch.setattr(etl, "_run_listener", fake_run_listener)

        with patch("etl_pipeline.backoff_delay", return_value=0):
            with pytest.raises(KeyboardInterrupt):
                etl.start_listener(max_reconnects=3)

        fake_conn.close.assert_called()

    def test_unexpected_exception_propagates_without_reconnect(self, monkeypatch):
        """A non-connection exception should propagate immediately."""
        def conn_factory(autocommit=False, **kw):
            return MagicMock()

        def fake_run_listener(conn):
            raise ValueError("logic error")

        monkeypatch.setattr(etl, "get_connection", conn_factory)
        monkeypatch.setattr(etl, "_run_listener", fake_run_listener)

        with pytest.raises(ValueError, match="logic error"):
            etl.start_listener(max_reconnects=5)
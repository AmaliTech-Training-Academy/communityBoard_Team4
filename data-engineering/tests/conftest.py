"""
conftest.py — Shared pytest fixtures for CommunityBoard ETL tests.

All tests are unit tests using mocks — no real database required.
Fixtures here are available to all test files automatically.
"""

import time
from unittest.mock import MagicMock

import psycopg2
import pytest


# ------------------------------------------------------------------ #
# Fixture: suppress real sleep so tests run instantly
# ------------------------------------------------------------------ #
@pytest.fixture(autouse=True)
def no_sleep(monkeypatch):
    """
    Replace time.sleep with a no-op for all tests automatically.
    Without this, backoff delays would make the test suite take minutes.
    """
    monkeypatch.setattr(time, "sleep", lambda _: None)


# ------------------------------------------------------------------ #
# Fixture: mock psycopg2.connect
# ------------------------------------------------------------------ #
@pytest.fixture
def mock_connect(monkeypatch):
    """
    Return a MagicMock replacing psycopg2.connect.
    Tests configure .side_effect or .return_value as needed.
    """
    mock = MagicMock()
    monkeypatch.setattr(psycopg2, "connect", mock)
    return mock


# ------------------------------------------------------------------ #
# Fixture: a ready mock DB connection with cursor
# ------------------------------------------------------------------ #
@pytest.fixture
def mock_conn():
    """
    A pre-configured mock psycopg2 connection with a mock cursor.
    Use when a function receives a connection as an argument.
    """
    conn = MagicMock()
    cur = MagicMock()
    conn.cursor.return_value = cur
    conn.notifies = []
    return conn


# ------------------------------------------------------------------ #
# Fixture: remove jitter from backoff_delay
# ------------------------------------------------------------------ #
@pytest.fixture
def no_jitter(monkeypatch):
    """
    Make backoff jitter always 0 so sequences are deterministic.
    """
    import random

    monkeypatch.setattr(random, "uniform", lambda a, b: 0.0)


# ------------------------------------------------------------------ #
# Helpers
# ------------------------------------------------------------------ #
def make_op_error(msg: str = "connection refused") -> psycopg2.OperationalError:
    return psycopg2.OperationalError(msg)


def make_prog_error(msg: str = "syntax error") -> psycopg2.ProgrammingError:
    return psycopg2.ProgrammingError(msg)

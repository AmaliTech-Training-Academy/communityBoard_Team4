"""
test_backoff.py — Unit tests for the backoff_delay helper.

Tests cover:
  - Happy path: correct exponential formula
  - Happy path: jitter is added within expected bounds
  - Happy path: delay is capped at max_delay
  - Happy path: custom base and max values
  - Edge case: attempt 0 produces the base delay
  - Edge case: very large attempt number still capped at max
  - Edge case: zero jitter_factor produces exact exponential value
"""

import etl_pipeline as etl

from unittest.mock import patch

import pytest

import sys

import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))


class TestBackoffDelay:
    """Tests for backoff_delay()."""

    def test_attempt_zero_returns_base(self, no_jitter):
        """Attempt 0 with no jitter should return exactly the base delay."""
        result = etl.backoff_delay(attempt=0, base=2.0, max_delay=60.0, jitter_factor=0)
        assert result == pytest.approx(2.0)

    def test_attempt_one_doubles_base(self, no_jitter):
        """Attempt 1 should return base * 2^1 = 4s (no jitter)."""
        result = etl.backoff_delay(attempt=1, base=2.0, max_delay=60.0, jitter_factor=0)
        assert result == pytest.approx(4.0)

    def test_attempt_two_quadruples_base(self, no_jitter):
        """Attempt 2 should return base * 2^2 = 8s (no jitter)."""
        result = etl.backoff_delay(attempt=2, base=2.0, max_delay=60.0, jitter_factor=0)
        assert result == pytest.approx(8.0)

    def test_delay_capped_at_max(self, no_jitter):
        """Large attempt numbers must never exceed max_delay."""
        result = etl.backoff_delay(
            attempt=20, base=2.0, max_delay=60.0, jitter_factor=0
        )
        assert result == pytest.approx(60.0)

    def test_custom_base_and_max(self, no_jitter):
        """Custom base and max values should be respected."""
        result = etl.backoff_delay(attempt=1, base=1.0, max_delay=10.0, jitter_factor=0)
        assert result == pytest.approx(2.0)

    def test_jitter_within_bounds(self):
        """
        With default jitter_factor=0.5, total delay must be between
        the pure exponential value and 1.5x that value.
        """
        base_delay = 2.0 * (2**0)  # attempt=0 → 2s
        for _ in range(50):
            result = etl.backoff_delay(
                attempt=0, base=2.0, max_delay=60.0, jitter_factor=0.5
            )
            assert base_delay <= result <= base_delay * 1.5 + 0.001

    def test_sleep_is_called_with_total_delay(self, no_jitter):
        """backoff_delay must call time.sleep with the computed delay."""
        with patch("etl_pipeline.time.sleep") as mock_sleep:
            etl.backoff_delay(attempt=0, base=2.0, max_delay=60.0, jitter_factor=0)
            mock_sleep.assert_called_once_with(pytest.approx(2.0))

    def test_return_value_matches_sleep_argument(self, no_jitter):
        """The returned float must equal what was passed to time.sleep."""
        slept = []
        with patch("etl_pipeline.time.sleep", side_effect=lambda s: slept.append(s)):
            result = etl.backoff_delay(
                attempt=1, base=2.0, max_delay=60.0, jitter_factor=0
            )
        assert result == pytest.approx(slept[0])

    def test_attempt_zero_jitter_factor_zero(self):
        """Zero jitter_factor must produce exactly the base delay (no randomness)."""
        result = etl.backoff_delay(attempt=0, base=5.0, max_delay=60.0, jitter_factor=0)
        assert result == pytest.approx(5.0)

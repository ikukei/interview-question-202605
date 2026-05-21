"""
Python Feature Flag Demo

Usage:
    python main.py [base_url] [subject_key] [region] [subject] [release_key] [poll_interval]

Defaults:
    base_url      = http://localhost:8080
    subject_key   = python-demo-user
    region        = Asia
    subject       = vip
    release_key   = <today yyyyMMdd>
    poll_interval = 3  (seconds)
"""
from __future__ import annotations

import sys
import time
from datetime import date
from pathlib import Path

# Allow running from the python-demo directory without installing the SDK.
sys.path.insert(0, str(Path(__file__).parent.parent / "python-sdk"))

from feature_flag_sdk import FeatureClient, FeatureClientError, FeatureContext


def _arg(index: int, default: str) -> str:
    val = sys.argv[index] if len(sys.argv) > index else ""
    return val.strip() or default


def main() -> None:
    base_url = _arg(1, "http://localhost:8080")
    subject_key = _arg(2, "python-demo-user")
    region = _arg(3, "Asia")
    subject = _arg(4, "vip")
    release_key = _arg(5, date.today().strftime("%Y%m%d"))
    poll_interval = int(_arg(6, "3"))

    client = FeatureClient(
        base_url=base_url,
        app_key="python-demo",
        environment="local",
    )

    context = FeatureContext(
        subject_key=subject_key,
        region=region,
        subject=subject,
        release_key=release_key,
        attributes={
            "region": region,
            "subject": subject,
            "release": release_key,
            "platform": "python-demo",
        },
    )

    known_flags: set[str] = set()

    print("=== Python Feature Flag Demo ===")
    print(f"App: python-demo  region: {region}  subject: {subject}  release: {release_key}")
    print(f"Polling every {poll_interval}s — press Ctrl+C to exit.\n")

    while True:
        try:
            current_keys = client.list_flag_keys()

            for flag_key in current_keys:
                if flag_key not in known_flags:
                    ts = _now()
                    print(f"\n[{ts}] NEW FLAG DETECTED: {flag_key}")
                    try:
                        ev = client.evaluate(flag_key, context)
                        _print_evaluation(ev)
                    except FeatureClientError as exc:
                        print(f"  Evaluation failed: {exc}")
                    finally:
                        known_flags.add(flag_key)

            if current_keys:
                ts = _now()
                print(f"\n[{ts}] All feature flags ({len(current_keys)}):")
                try:
                    evaluations = client.evaluate_all(context)
                    for ev in evaluations:
                        status = "on" if ev.enabled else "off"
                        print(f"  {ev.flag_key}: {status}  ({ev.reason_code})")
                except FeatureClientError as exc:
                    print(f"  Batch evaluation failed: {exc}")
            else:
                print(f"[{_now()}] No feature flags found")

        except FeatureClientError as exc:
            print(f"[{_now()}] Error: {exc}")

        try:
            time.sleep(poll_interval)
        except KeyboardInterrupt:
            print("\nExiting.")
            break


def _print_evaluation(ev) -> None:
    print(f"  Flag Key         : {ev.flag_key}")
    print(f"  Enabled          : {ev.enabled}")
    print(f"  Reason           : {ev.reason_code}")
    print(f"  Snapshot Version : {ev.snapshot_version}")
    print(f"  Release          : {ev.release_key or 'none'}")


def _now() -> str:
    from datetime import datetime
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


if __name__ == "__main__":
    main()

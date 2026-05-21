from __future__ import annotations

import json
import urllib.error
import urllib.parse
import urllib.request
from typing import Optional

from .models import FeatureContext, FeatureEvaluation


class FeatureClientError(Exception):
    pass


class FeatureClient:
    def __init__(self, base_url: str, app_key: str, environment: str) -> None:
        self._base_url = base_url.rstrip("/")
        self._app_key = app_key
        self._environment = environment

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def list_flag_keys(self) -> list[str]:
        params = urllib.parse.urlencode({"appKey": self._app_key, "environment": self._environment})
        url = f"{self._base_url}/api/v1/flags?{params}"
        data = self._get(url)
        return [item["flagKey"] for item in data]

    def evaluate(self, flag_key: str, context: FeatureContext) -> FeatureEvaluation:
        url = f"{self._base_url}/api/v1/evaluations/flags/{urllib.parse.quote(flag_key, safe='')}"
        body = {
            "appKey": self._app_key,
            "environment": self._environment,
            "context": context.to_dict(),
        }
        data = self._post(url, body)
        return FeatureEvaluation.from_dict(data)

    def evaluate_all(self, context: FeatureContext) -> list[FeatureEvaluation]:
        flag_keys = self.list_flag_keys()
        if not flag_keys:
            return []
        url = f"{self._base_url}/api/v1/evaluations:batch"
        body = {
            "appKey": self._app_key,
            "environment": self._environment,
            "flagKeys": flag_keys,
            "context": context.to_dict(),
        }
        data = self._post(url, body)
        return [FeatureEvaluation.from_dict(item) for item in data]

    def bool_variation(
        self,
        flag_key: str,
        context: FeatureContext,
        default_value: bool = False,
    ) -> bool:
        try:
            return self.evaluate(flag_key, context).enabled
        except Exception:
            return default_value

    # ------------------------------------------------------------------
    # HTTP helpers (stdlib only, no external dependencies)
    # ------------------------------------------------------------------

    def _get(self, url: str) -> object:
        req = urllib.request.Request(url, method="GET")
        req.add_header("Content-Type", "application/json")
        return self._send(req)

    def _post(self, url: str, body: dict) -> object:
        payload = json.dumps(body).encode()
        req = urllib.request.Request(url, data=payload, method="POST")
        req.add_header("Content-Type", "application/json")
        return self._send(req)

    def _send(self, req: urllib.request.Request) -> object:
        try:
            with urllib.request.urlopen(req, timeout=5) as resp:
                return json.loads(resp.read().decode())
        except urllib.error.HTTPError as exc:
            body = exc.read().decode(errors="replace")
            raise FeatureClientError(
                f"HTTP {exc.code} from {req.full_url}: {body}"
            ) from exc
        except Exception as exc:
            raise FeatureClientError(f"Request failed: {exc}") from exc

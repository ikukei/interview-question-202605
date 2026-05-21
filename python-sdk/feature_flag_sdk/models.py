from __future__ import annotations

from dataclasses import dataclass, field
from typing import Optional


@dataclass
class FeatureContext:
    subject_key: str
    region: Optional[str] = None
    subject: Optional[str] = None
    release_key: Optional[str] = None
    attributes: dict[str, str] = field(default_factory=dict)

    def to_dict(self) -> dict:
        return {
            "subjectKey": self.subject_key,
            "region": self.region,
            "subject": self.subject,
            "releaseKey": self.release_key,
            "attributes": self.attributes,
        }


@dataclass
class FeatureEvaluation:
    flag_key: str
    enabled: bool
    reason_code: str
    snapshot_version: int
    matched_rule_id: Optional[str] = None
    release_key: Optional[str] = None

    @classmethod
    def from_dict(cls, data: dict) -> "FeatureEvaluation":
        return cls(
            flag_key=data["flagKey"],
            enabled=data["enabled"],
            reason_code=data["reasonCode"],
            snapshot_version=data["snapshotVersion"],
            matched_rule_id=data.get("matchedRuleId"),
            release_key=data.get("releaseKey"),
        )

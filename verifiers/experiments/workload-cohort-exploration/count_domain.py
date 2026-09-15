#!/usr/bin/env python3
"""Count a fixed workload's cap-limited candidate domain through ordinary requests."""
import argparse
import itertools
import json
from pathlib import Path
import sys

FIXED_GA = Path(__file__).resolve().parents[1] / "fixed-workload-ga"
sys.path.insert(0, str(FIXED_GA))

from runtime import prepare, read, save  # noqa: E402
from search import stable  # noqa: E402


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    config = read(args.config.resolve())
    runtime, domain = prepare(config, args.output.resolve())
    rows = []
    keys = set()
    for genes in itertools.product(*domain.coordinates):
        vector = "".join("1" if slot in genes else "0" for slot in range(domain.width))
        candidates = domain.resolve(vector)
        request = domain.requests[-1]
        response = request.get("response") or {}
        candidate_keys = sorted(candidate["key"] for candidate in candidates)
        keys.update(candidate_keys)
        rows.append({
            "genes": list(genes),
            "vector": vector,
            "status": response.get("status"),
            "uncappedScheduleCount": response.get("uncappedScheduleCount"),
            "writtenScheduleCount": response.get("writtenScheduleCount"),
            "uniqueCandidateCount": len(candidate_keys),
            "truncated": request.get("truncated"),
            "generatorWallSeconds": request["process"]["wallSeconds"],
        })
    domain.verify()
    runtime.verify()
    count_complete = all(row["uncappedScheduleCount"] is not None for row in rows)
    result = {
        "schemaVersion": "microservices-simulator.workload-domain-inventory.v1",
        "workload": domain.workload,
        "coordinates": domain.coordinates,
        "vectorDomainSize": len(rows),
        "recoveryCap": domain.cap,
        "vectors": rows,
        "capLimitedUniqueCandidateCount": len(keys),
        "sumUncappedScheduleCounts": sum(int(row["uncappedScheduleCount"]) for row in rows
                                           if row["uncappedScheduleCount"] is not None),
        "sumWrittenScheduleCounts": sum(int(row["writtenScheduleCount"]) for row in rows
                                        if row["writtenScheduleCount"] is not None),
        "countComplete": count_complete,
        "anyTruncated": any(row["truncated"] for row in rows),
        "generatorWallSeconds": sum(row["generatorWallSeconds"] for row in rows),
        "note": "Exact for this fixed workload when countComplete=true and anyTruncated=false; "
                "otherwise the successful requests describe only partial or cap-limited evidence.",
    }
    save(args.output.resolve() / "domain-count.json", result)
    print(stable({key: result[key] for key in (
        "vectorDomainSize", "recoveryCap", "capLimitedUniqueCandidateCount",
        "sumUncappedScheduleCounts", "sumWrittenScheduleCounts", "anyTruncated",
        "generatorWallSeconds")}))


if __name__ == "__main__":
    main()

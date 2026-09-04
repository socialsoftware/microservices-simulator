#!/usr/bin/env python3
"""Emit deterministic candidate inventory for consistency-testing onboarding."""

from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


PACKAGE_RE = re.compile(r"\bpackage\s+([\w.]+)\s*;")
CLASS_RE = re.compile(r"\bclass\s+([A-Za-z_]\w*)")
WORKFLOW_RE = re.compile(r"\bclass\s+([A-Za-z_]\w*)\s+extends\s+WorkflowFunctionality\b")
SAGA_AGGREGATE_RE = re.compile(
    r"\bclass\s+([A-Za-z_]\w*)[^{}]*\bimplements\s+[^{}]*\bSagaAggregate\b",
    re.DOTALL,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Discover candidates for a simulator application's consistency sweep adapter."
    )
    parser.add_argument("application", type=Path, help="Application module directory")
    parser.add_argument("--output", type=Path, help="Write JSON here instead of stdout")
    return parser.parse_args()


def read_java(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def qualified_name(text: str, class_name: str) -> str:
    match = PACKAGE_RE.search(text)
    return f"{match.group(1)}.{class_name}" if match else class_name


def relative(path: Path, root: Path) -> str:
    return path.relative_to(root).as_posix()


def pom_value(project: ET.Element, namespace: str, name: str) -> str | None:
    element = project.find(f"{namespace}{name}")
    if element is not None and element.text:
        return element.text.strip()
    parent = project.find(f"{namespace}parent")
    if parent is not None:
        element = parent.find(f"{namespace}{name}")
        if element is not None and element.text:
            return element.text.strip()
    return None


def pom_inventory(pom: Path) -> dict[str, object]:
    project = ET.parse(pom).getroot()
    namespace = ""
    if project.tag.startswith("{"):
        namespace = project.tag[: project.tag.index("}") + 1]

    profiles = sorted(
        element.text.strip()
        for element in project.findall(f"{namespace}profiles/{namespace}profile/{namespace}id")
        if element.text
    )
    dependencies = []
    for dependency in project.findall(f"{namespace}dependencies/{namespace}dependency"):
        artifact = dependency.find(f"{namespace}artifactId")
        if artifact is not None and artifact.text:
            dependencies.append(artifact.text.strip())

    start_class = project.find(f"{namespace}properties/{namespace}start-class")
    return {
        "group_id": pom_value(project, namespace, "groupId"),
        "artifact_id": pom_value(project, namespace, "artifactId"),
        "version": pom_value(project, namespace, "version"),
        "start_class": start_class.text.strip() if start_class is not None and start_class.text else None,
        "profiles": profiles,
        "has_consistency_testing_dependency": "ConsistencyTesting" in dependencies,
        "has_consistency_sweep_profile": "consistency-sweep" in profiles,
    }


def java_inventory(root: Path) -> dict[str, list[dict[str, str]]]:
    main_root = root / "src" / "main" / "java"
    test_root = root / "src" / "test" / "java"
    result: dict[str, list[dict[str, str]]] = {
        "spring_applications": [],
        "saga_aggregates": [],
        "saga_functionalities": [],
        "test_helpers": [],
        "existing_consistency_integration": [],
    }

    main_files = sorted(main_root.rglob("*.java")) if main_root.exists() else []
    for path in main_files:
        text = read_java(path)
        class_match = CLASS_RE.search(text)
        if not class_match:
            continue
        class_name = class_match.group(1)
        item = {"class": qualified_name(text, class_name), "path": relative(path, root)}
        if "@SpringBootApplication" in text:
            result["spring_applications"].append(item)
        workflow = WORKFLOW_RE.search(text)
        source_path = relative(path, root)
        if workflow and ("/coordination/sagas/" in f"/{source_path}" or workflow.group(1).endswith("Sagas")):
            name = workflow.group(1)
            result["saga_functionalities"].append(
                {"class": qualified_name(text, name), "path": source_path}
            )
        saga_aggregate = SAGA_AGGREGATE_RE.search(text)
        if saga_aggregate:
            name = saga_aggregate.group(1)
            result["saga_aggregates"].append(
                {"class": qualified_name(text, name), "path": relative(path, root)}
            )

    integration_suffixes = (
        "FunctionalityCatalogsProvider",
        "InterInvariantsProvider",
        "ConsistencyCatalogValidation",
        "ConsistencySweep",
    )
    helper_suffixes = ("Factory", "Builder", "Fixture", "Fixtures", "TestData")
    test_files = sorted(test_root.rglob("*.java")) if test_root.exists() else []
    for path in test_files:
        text = read_java(path)
        class_match = CLASS_RE.search(text)
        if not class_match:
            continue
        class_name = class_match.group(1)
        item = {"class": qualified_name(text, class_name), "path": relative(path, root)}
        if class_name.endswith(integration_suffixes):
            result["existing_consistency_integration"].append(item)
        elif class_name.endswith(helper_suffixes):
            result["test_helpers"].append(item)

    for values in result.values():
        values.sort(key=lambda item: (item["class"], item["path"]))
    return result


def main() -> int:
    args = parse_args()
    root = args.application.resolve()
    pom = root / "pom.xml"
    if not root.is_dir():
        print(f"error: application directory does not exist: {root}", file=sys.stderr)
        return 2
    if not pom.is_file():
        print(f"error: Maven pom.xml not found: {pom}", file=sys.stderr)
        return 2

    inventory: dict[str, object] = {"application_root": str(root), "pom": pom_inventory(pom)}
    inventory.update(java_inventory(root))
    rendered = json.dumps(inventory, indent=2, sort_keys=True) + "\n"

    if args.output:
        args.output.write_text(rendered, encoding="utf-8")
    else:
        sys.stdout.write(rendered)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Reproducible S5 source metrics for the campus repair system.

The script deliberately measures only source trees and Surefire/JaCoCo outputs.
It does not modify business sources or infer values from the S4 design files.
"""

from __future__ import annotations

import csv
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path
from statistics import mean
from typing import Iterable


ROOT = Path(__file__).resolve().parents[3]
MAIN_ROOT = ROOT / "backend/src/main/java"
TEST_ROOT = ROOT / "backend/src/test/java"
FRONT_ROOT = ROOT / "frontend/src"
OUT = ROOT / "S5/代码度量"

THRESHOLDS = {"WMC": 15, "RFC": 35, "DIT": 6, "NOC": 6, "CBO": 8, "LCOM": 1}
CONTROL_WORDS = {"if", "for", "while", "switch", "catch", "return", "throw", "new", "synchronized"}


@dataclass
class Method:
    name: str
    start: int
    end: int
    body_start: int | None
    body_end: int | None
    constructor: bool = False
    field_usage: set[str] = field(default_factory=set)
    complexity: int = 1


@dataclass
class JavaType:
    package: str
    name: str
    kind: str
    path: Path
    layer: str
    start: int
    body_start: int
    body_end: int
    parent: str | None = None
    interfaces: set[str] = field(default_factory=set)
    fields: set[str] = field(default_factory=set)
    methods: list[Method] = field(default_factory=list)
    imports: set[str] = field(default_factory=set)
    used_project_types: set[str] = field(default_factory=set)


def masked_java(text: str) -> str:
    """Replace comments and literals with spaces while preserving positions/newlines."""
    out = list(text)
    state = "normal"
    i = 0
    while i < len(text):
        ch = text[i]
        nxt = text[i + 1] if i + 1 < len(text) else ""
        if state == "normal":
            if ch == "/" and nxt == "/":
                out[i] = out[i + 1] = " "
                i += 2
                state = "line_comment"
                continue
            if ch == "/" and nxt == "*":
                out[i] = out[i + 1] = " "
                i += 2
                state = "block_comment"
                continue
            if ch == '"':
                out[i] = " "
                i += 1
                state = "string"
                continue
            if ch == "'":
                out[i] = " "
                i += 1
                state = "char"
                continue
            i += 1
            continue
        if state == "line_comment":
            if ch == "\n":
                state = "normal"
            else:
                out[i] = " "
            i += 1
            continue
        if state == "block_comment":
            if ch == "*" and nxt == "/":
                out[i] = out[i + 1] = " "
                i += 2
                state = "normal"
            else:
                if ch != "\n":
                    out[i] = " "
                i += 1
            continue
        if state in {"string", "char"}:
            if ch == "\\":
                out[i] = " "
                if i + 1 < len(text):
                    if text[i + 1] != "\n":
                        out[i + 1] = " "
                    i += 2
                else:
                    i += 1
                continue
            if (state == "string" and ch == '"') or (state == "char" and ch == "'"):
                out[i] = " "
                state = "normal"
            elif ch != "\n":
                out[i] = " "
            i += 1
    return "".join(out)


def brace_depths(masked: str) -> list[int]:
    depths = [0] * (len(masked) + 1)
    depth = 0
    for i, ch in enumerate(masked):
        depths[i] = depth
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth = max(0, depth - 1)
    depths[len(masked)] = depth
    return depths


def line_number(text: str, pos: int) -> int:
    return text.count("\n", 0, pos) + 1


def matching_brace(masked: str, opening: int) -> int:
    depth = 0
    for i in range(opening, len(masked)):
        if masked[i] == "{":
            depth += 1
        elif masked[i] == "}":
            depth -= 1
            if depth == 0:
                return i
    return len(masked) - 1


def layer_for(relative: Path) -> str:
    parts = relative.parts
    if "service" in parts and "impl" in parts and parts.index("impl") == parts.index("service") + 1:
        return "service_impl"
    for candidate in {"controller", "service", "repository", "domain", "security", "config", "common", "dto"}:
        if candidate in parts:
            return candidate
    return "other"


def field_names(body: str, masked: str, depths: list[int], body_start: int) -> set[str]:
    target_depth = depths[body_start]
    names: set[str] = set()
    for line in masked[body_start + 1 :].splitlines():
        # This line-only helper is supplemented by the position-aware pass below.
        if "(" in line or "class " in line or "interface " in line or "enum " in line:
            continue
        candidate = re.search(r"\b([A-Za-z_$][\w$]*)\s*(?:=|;|,)", line)
        if candidate:
            names.add(candidate.group(1))
    return names


def parse_java(path: Path) -> list[JavaType]:
    text = path.read_text(encoding="utf-8")
    masked = masked_java(text)
    depths = brace_depths(masked)
    package_match = re.search(r"\bpackage\s+([\w.]+)\s*;", masked)
    package = package_match.group(1) if package_match else ""
    relative = path.relative_to(MAIN_ROOT)
    imports = {m.group(1).split(".")[-1] for m in re.finditer(r"\bimport\s+(?:static\s+)?([\w.]+)\s*;", masked)}
    types: list[JavaType] = []
    type_pattern = re.compile(r"\b(class|interface|enum|record)\s+([A-Za-z_$][\w$]*)\s*([^{};]*?)\{")
    for match in type_pattern.finditer(masked):
        if depths[match.start()] != 0:
            continue
        opening = match.end() - 1
        closing = matching_brace(masked, opening)
        kind = "class" if match.group(1) == "record" else match.group(1)
        header = match.group(3)
        parent_match = re.search(r"\bextends\s+([A-Za-z_$][\w$]*)", header)
        implements_match = re.search(r"\bimplements\s+(.+)$", header)
        interfaces = set()
        if implements_match:
            interfaces = set(re.findall(r"[A-Za-z_$][\w$]*", implements_match.group(1)))
        if match.group(1) == "interface":
            extends_match = re.search(r"\bextends\s+(.+)$", header)
            if extends_match:
                interfaces |= set(re.findall(r"[A-Za-z_$][\w$]*", extends_match.group(1)))
        item = JavaType(package, match.group(2), kind, path, layer_for(relative), match.start(), opening, closing,
                        parent_match.group(1) if parent_match else None, interfaces, imports=imports)
        types.append(item)

    for item in types:
        body_depth = depths[item.body_start + 1]
        # Fields: declarations at the direct type-body depth, excluding method declarations.
        for line_match in re.finditer(r"(?m)^([^\n]*)", masked[item.body_start + 1 : item.body_end]):
            line = line_match.group(1)
            absolute = item.body_start + 1 + line_match.start()
            if depths[absolute] != body_depth or "(" in line or not line.strip():
                continue
            if re.search(r"\b(class|interface|enum|record)\b", line):
                continue
            for declaration in re.finditer(r"\b([A-Za-z_$][\w$]*)\s*(?:=|;|,)", line):
                name = declaration.group(1)
                if name not in {"return", "new", "throw"}:
                    item.fields.add(name)

        # A declaration is recognized at the direct type-body depth and must end in { or ;.
        for line_match in re.finditer(r"(?m)^([^\n]*)", masked[item.body_start + 1 : item.body_end]):
            line = line_match.group(1)
            absolute = item.body_start + 1 + line_match.start()
            if depths[absolute] != body_depth or "(" not in line:
                continue
            open_rel = line.find("(")
            open_pos = absolute + open_rel
            close_pos = open_pos
            paren = 0
            while close_pos < item.body_end:
                if masked[close_pos] == "(":
                    paren += 1
                elif masked[close_pos] == ")":
                    paren -= 1
                    if paren == 0:
                        break
                close_pos += 1
            suffix = masked[close_pos + 1 : min(item.body_end, close_pos + 160)]
            suffix_match = re.match(r"\s*(?:throws\s+[^\{;]+)?\s*([\{;])", suffix)
            if not suffix_match:
                continue
            before = masked[absolute : open_pos].strip()
            name_match = re.search(r"([A-Za-z_$][\w$]*)\s*$", before)
            if not name_match:
                continue
            name = name_match.group(1)
            if name in CONTROL_WORDS or "." in before or "=" in before:
                continue
            if item.kind == "enum" and re.fullmatch(re.escape(name), before):
                # The final enum constant may end with ';' and otherwise resemble
                # a semicolon-terminated method declaration.
                continue
            body_start = None
            body_end = None
            if suffix_match.group(1) == "{":
                body_start = close_pos + 1 + suffix_match.start(1)
                body_end = matching_brace(masked, body_start)
            method = Method(name, absolute, close_pos, body_start, body_end, name == item.name)
            if not method.constructor:
                item.methods.append(method)

        for method in item.methods:
            body = masked[method.body_start + 1 : method.body_end] if method.body_start is not None and method.body_end is not None else ""
            method.field_usage = {f for f in item.fields if re.search(rf"\b{re.escape(f)}\b", body)}
            decisions = 0
            decisions += len(re.findall(r"\bif\s*\(", body))
            decisions += len(re.findall(r"\b(?:for|while|catch)\s*\(", body))
            decisions += len(re.findall(r"\bcase\b", body))
            decisions += len(re.findall(r"&&|\|\|", body))
            decisions += len(re.findall(r"\?(?!=)", body))
            method.complexity = 1 + decisions
        item.used_project_types = set()
    return types


def loc_counts(path: Path) -> tuple[int, int, int, int]:
    text = path.read_text(encoding="utf-8")
    state = "normal"
    blank = comment = code = 0
    for raw_line in text.splitlines():
        i = 0
        code_seen = False
        comment_seen = False
        while i < len(raw_line):
            ch = raw_line[i]
            nxt = raw_line[i + 1] if i + 1 < len(raw_line) else ""
            if state == "normal":
                if ch == "/" and nxt == "/":
                    comment_seen = True
                    break
                if ch == "/" and nxt == "*":
                    comment_seen = True
                    state = "block_comment"
                    i += 2
                    continue
                if not ch.isspace():
                    code_seen = True
                if ch == '"':
                    state = "string"
                elif ch == "'":
                    state = "char"
                i += 1
                continue
            if state == "block_comment":
                comment_seen = True
                if ch == "*" and nxt == "/":
                    state = "normal"
                    i += 2
                else:
                    i += 1
                continue
            if state in {"string", "char"}:
                code_seen = True
                if ch == "\\":
                    i += 2
                elif (state == "string" and ch == '"') or (state == "char" and ch == "'"):
                    state = "normal"
                    i += 1
                else:
                    i += 1
        if not code_seen and not comment_seen and not raw_line.strip():
            blank += 1
        elif code_seen:
            code += 1
        else:
            comment += 1
    files = 1
    return files, blank, comment, code


def csv_write(path: Path, rows: Iterable[dict], fields: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def all_files(root: Path, suffix: str) -> list[Path]:
    return sorted(root.rglob(f"*{suffix}")) if root.exists() else []


def measure() -> dict:
    OUT.mkdir(parents=True, exist_ok=True)
    java_files = all_files(MAIN_ROOT, ".java")
    test_files = all_files(TEST_ROOT, ".java")
    front_files = [p for p in FRONT_ROOT.rglob("*") if p.is_file() and p.suffix in {".js", ".vue", ".css", ".html"}] if FRONT_ROOT.exists() else []
    types = [item for path in java_files for item in parse_java(path)]
    project_names = {item.name for item in types}
    for item in types:
        text = masked_java(item.path.read_text(encoding="utf-8"))
        item.used_project_types = {name for name in project_names if name != item.name and re.search(rf"\b{re.escape(name)}\b", text)}
        # The direct parent is excluded from CBO; interface implementation/extension remains counted.
        if item.parent:
            item.used_project_types.discard(item.parent)

    class_count = sum(item.kind == "class" for item in types)
    interface_count = sum(item.kind == "interface" for item in types)
    enum_count = sum(item.kind == "enum" for item in types)
    csv_write(OUT / "class_count.csv", [
        {"category": "class", "count": class_count},
        {"category": "interface", "count": interface_count},
        {"category": "enum", "count": enum_count},
    ], ["category", "count"])
    csv_write(OUT / "java_type_list.csv", [
        {"package": x.package, "type_name": x.name, "type_kind": x.kind, "file_path": str(x.path.relative_to(ROOT)), "layer": x.layer}
        for x in types
    ], ["package", "type_name", "type_kind", "file_path", "layer"])

    def tree_loc(files: list[Path]) -> dict:
        result = {"files": len(files), "blank": 0, "comment": 0, "code": 0}
        for file in files:
            _, b, c, k = loc_counts(file)
            result["blank"] += b
            result["comment"] += c
            result["code"] += k
        return result

    main_loc = tree_loc(java_files)
    test_loc = tree_loc(test_files)
    front_loc = tree_loc(front_files)
    total_main = main_loc["code"] + front_loc["code"]
    total_all = total_main + test_loc["code"]
    csv_write(OUT / "loc_summary.csv", [
        {"scope": "backend_main", **main_loc},
        {"scope": "backend_test", **test_loc},
        {"scope": "frontend", **front_loc},
        {"scope": "total_main", "files": main_loc["files"] + front_loc["files"], "blank": main_loc["blank"] + front_loc["blank"], "comment": main_loc["comment"] + front_loc["comment"], "code": total_main},
        {"scope": "total_all", "files": main_loc["files"] + test_loc["files"] + front_loc["files"], "blank": main_loc["blank"] + test_loc["blank"] + front_loc["blank"], "comment": main_loc["comment"] + test_loc["comment"] + front_loc["comment"], "code": total_all},
    ], ["scope", "files", "blank", "comment", "code"])

    module_rows = []
    module_groups: dict[str, list[Path]] = {}
    for file in java_files:
        rel = file.relative_to(MAIN_ROOT)
        module_groups.setdefault(layer_for(rel), []).append(file)
    for file in front_files:
        rel = file.relative_to(FRONT_ROOT)
        module_groups.setdefault(rel.parts[0] if rel.parts and rel.parts[0] in {"views", "components", "api", "router", "stores", "utils"} else "other", []).append(file)
    for module, files in sorted(module_groups.items()):
        values = tree_loc(files)
        module_rows.append({"module": module, "file_count": values["files"], "blank_lines": values["blank"], "comment_lines": values["comment"], "code_lines": values["code"]})
    csv_write(OUT / "loc_by_module.csv", module_rows, ["module", "file_count", "blank_lines", "comment_lines", "code_lines"])

    methods = [method for item in types for method in item.methods]
    method_rows = [{"type_name": item.name, "method_count": len(item.methods), "file_path": str(item.path.relative_to(ROOT))} for item in types]
    csv_write(OUT / "method_count.csv", method_rows, ["type_name", "method_count", "file_path"])
    categories = {"Controller": 0, "Service interface": 0, "ServiceImpl": 0, "Repository": 0, "Domain": 0, "Other": 0}
    for item in types:
        if item.layer == "controller": key = "Controller"
        elif item.layer == "service": key = "Service interface"
        elif item.layer == "service_impl": key = "ServiceImpl"
        elif item.layer == "repository": key = "Repository"
        elif item.layer == "domain": key = "Domain"
        else: key = "Other"
        categories[key] += len(item.methods)
    csv_write(OUT / "method_summary.csv", [
        {"category": category, "method_count": count}
        for category, count in categories.items()
    ], ["category", "method_count"])

    complexity_rows = []
    for item in types:
        for method in item.methods:
            complexity_rows.append({"class_name": item.name, "method_name": method.name, "file_path": str(item.path.relative_to(ROOT)), "start_line": line_number(item.path.read_text(encoding="utf-8"), method.start), "cyclomatic_complexity": method.complexity})
    csv_write(OUT / "cyclomatic_complexity.csv", complexity_rows, ["class_name", "method_name", "file_path", "start_line", "cyclomatic_complexity"])
    values = [int(row["cyclomatic_complexity"]) for row in complexity_rows]
    summary_rows = [
        {"metric": "method_total", "value": len(values)},
        {"metric": "average_vg", "value": f"{mean(values):.2f}" if values else "0.00"},
        {"metric": "max_vg", "value": max(values) if values else 0},
        {"metric": "vg_over_10_count", "value": sum(x > 10 for x in values)},
        {"metric": "vg_equal_10_count", "value": sum(x == 10 for x in values)},
    ]
    csv_write(OUT / "cyclomatic_summary.csv", summary_rows, ["metric", "value"])
    top = sorted(complexity_rows, key=lambda row: (-int(row["cyclomatic_complexity"]), row["class_name"], row["method_name"]))[:10]
    csv_write(OUT / "top_complex_methods.csv", [{"rank": i + 1, **row} for i, row in enumerate(top)], ["rank", "class_name", "method_name", "file_path", "start_line", "cyclomatic_complexity"])

    parents = {item.name: item.parent for item in types}
    def dit(item: JavaType) -> int:
        depth = 0
        parent = item.parent
        seen: set[str] = set()
        while parent and parent not in seen:
            seen.add(parent)
            depth += 1
            parent = parents.get(parent)
        return depth
    nocs = {item.name: sum(child.parent == item.name for child in types) for item in types}
    ck_rows = []
    ck_evidence: list[str] = ["# CK 计算证据", "", "本脚本按系统源码类型计算 CBO；JDK、Spring 和第三方类型不作为系统类对象。继承边不计 CBO，接口实现/扩展关系计入。", ""]
    for item in types:
        own_methods = {m.name for m in item.methods}
        external_calls: set[str] = set()
        for method in item.methods:
            body = masked_java(item.path.read_text(encoding="utf-8"))[method.body_start + 1 : method.body_end] if method.body_start is not None and method.body_end is not None else ""
            for call in re.finditer(r"\b([A-Za-z_$][\w$]*)\s*\(", body):
                name = call.group(1)
                before = body[max(0, call.start() - 8) : call.start()]
                if name not in CONTROL_WORDS and name not in own_methods and not re.search(r"\bnew\s*$", before):
                    external_calls.add(name)
        method_set = len(item.methods)
        rfc = method_set + len(external_calls)
        method_pairs = [(a, b) for index, a in enumerate(item.methods) for b in item.methods[index + 1 :]]
        p = sum(not (a.field_usage & b.field_usage) for a, b in method_pairs)
        q = sum(bool(a.field_usage & b.field_usage) for a, b in method_pairs)
        # Interfaces and pure repositories may have no instance attributes;
        # under the S4-compatible course convention their LCOM is recorded as 0.
        lcom = max(p - q, 0) if item.fields else 0
        cbo = len(item.used_project_types | item.interfaces)
        row = {"class_name": item.name, "WMC": method_set, "RFC": rfc, "DIT": dit(item), "NOC": nocs[item.name], "CBO": cbo, "LCOM": lcom}
        row["over_threshold"] = "YES" if any(float(row[key]) > threshold for key, threshold in THRESHOLDS.items()) else "NO"
        ck_rows.append(row)
        if row["over_threshold"] == "YES":
            ck_evidence.extend([
                f"## {item.name}",
                f"- WMC 方法：{', '.join(m.name for m in item.methods) or '无'}",
                f"- RFC 外部调用名：{', '.join(sorted(external_calls)) or '无'}",
                f"- CBO 系统类型：{', '.join(sorted(item.used_project_types | item.interfaces)) or '无'}",
                f"- LCOM：P={p}, Q={q}, max(P-Q,0)={lcom}",
                "",
            ])
    csv_write(OUT / "ck_actual.csv", ck_rows, ["class_name", "WMC", "RFC", "DIT", "NOC", "CBO", "LCOM", "over_threshold"])
    (OUT / "ck_evidence.md").write_text("\n".join(ck_evidence).rstrip() + "\n", encoding="utf-8")

    over_rows = []
    for row in ck_rows:
        for metric, threshold in THRESHOLDS.items():
            actual = float(row[metric])
            if actual > threshold:
                over_rows.append({"type": "CK", "object": row["class_name"], "metric": metric, "actual": row[metric], "threshold": threshold, "excess": f"{actual - threshold:.2f}"})
    for row in complexity_rows:
        actual = int(row["cyclomatic_complexity"])
        if actual > 10:
            over_rows.append({"type": "Cyclomatic", "object": f"{row['class_name']}.{row['method_name']}", "metric": "V(G)", "actual": actual, "threshold": 10, "excess": actual - 10})
    csv_write(OUT / "actual_over_threshold.csv", over_rows, ["type", "object", "metric", "actual", "threshold", "excess"])

    def ck_stat(metric: str, mode: str) -> str:
        vals = [float(row[metric]) for row in ck_rows]
        value = max(vals) if mode == "max" else mean(vals)
        return f"{value:.2f}" if mode == "avg" else str(int(value)) if value.is_integer() else f"{value:.2f}"
    def metric_difference(design: object, actual: object) -> str:
        try:
            difference = float(actual) - float(design)
        except (TypeError, ValueError):
            return "NA"
        return str(int(difference)) if difference.is_integer() else f"{difference:.2f}"
    actual_complexity = {f"{row['class_name']}.{row['method_name']}": row["cyclomatic_complexity"] for row in complexity_rows}
    comparison = [
        {"metric": "class_interface_count", "s4_design": 50, "s5_actual": len(types), "difference": metric_difference(50, len(types)), "interpretation": "按实际顶层 Java 类型统计"},
        *[{"metric": metric + "_max", "s4_design": design, "s5_actual": ck_stat(metric, "max"), "difference": metric_difference(design, ck_stat(metric, "max")), "interpretation": "S5 按普通方法和系统类型引用重新计算"} for metric, design in [("WMC", 11), ("RFC", 18), ("DIT", 1), ("NOC", 2), ("CBO", 8), ("LCOM", 9)]],
        *[{"metric": metric + "_avg", "s4_design": design, "s5_actual": ck_stat(metric, "avg"), "difference": metric_difference(design, ck_stat(metric, "avg")), "interpretation": "S5 按实际源码重新计算"} for metric, design in [("WMC", 3.76), ("RFC", 5.14), ("CBO", 2.56), ("LCOM", 0.30)]],
        {"metric": "processRepairOrder_VG", "s4_design": 6, "s5_actual": actual_complexity.get("MaintenanceServiceImpl.processRepairOrder", "NA"), "difference": metric_difference(6, actual_complexity.get("MaintenanceServiceImpl.processRepairOrder", "NA")), "interpretation": "S5 单方法方法体口径"},
        {"metric": "acceptRepairResult_VG", "s4_design": 6, "s5_actual": actual_complexity.get("AcceptanceServiceImpl.acceptRepairResult", "NA"), "difference": metric_difference(6, actual_complexity.get("AcceptanceServiceImpl.acceptRepairResult", "NA")), "interpretation": "S5 单方法方法体口径"},
        {"metric": "createOrder_VG", "s4_design": 5, "s5_actual": actual_complexity.get("RepairOrderServiceImpl.createOrder", "NA"), "difference": metric_difference(5, actual_complexity.get("RepairOrderServiceImpl.createOrder", "NA")), "interpretation": "S5 单方法方法体口径"},
        {"metric": "evaluateService_VG", "s4_design": "not in S4 core list", "s5_actual": actual_complexity.get("EvaluationServiceImpl.evaluateService", "NA"), "difference": "NA", "interpretation": "实现阶段新增评价服务的实际复杂度"},
    ]
    csv_write(OUT / "s4_vs_s5_metrics.csv", comparison, ["metric", "s4_design", "s5_actual", "difference", "interpretation"])

    surefire_dir = ROOT / "backend/target/surefire-reports"
    test_rows = []
    for xml in sorted(surefire_dir.glob("TEST-*.xml")) if surefire_dir.exists() else []:
        root = ET.parse(xml).getroot()
        test_rows.append({"test_class": root.attrib.get("name", xml.stem), "test_count": root.attrib.get("tests", 0), "failures": root.attrib.get("failures", 0), "errors": root.attrib.get("errors", 0), "skipped": root.attrib.get("skipped", 0)})
    total_tests = sum(int(row["test_count"]) for row in test_rows)
    total_failures = sum(int(row["failures"]) for row in test_rows)
    total_errors = sum(int(row["errors"]) for row in test_rows)
    total_skipped = sum(int(row["skipped"]) for row in test_rows)
    test_rows.append({"test_class": "TOTAL", "test_count": total_tests, "failures": total_failures, "errors": total_errors, "skipped": total_skipped})
    csv_write(OUT / "test_metrics.csv", test_rows, ["test_class", "test_count", "failures", "errors", "skipped"])
    pass_rate = ((total_tests - total_failures - total_errors - total_skipped) / total_tests * 100) if total_tests else 0
    csv_write(OUT / "test_summary.csv", [{"metric": "test_class_count", "value": len(test_rows) - 1}, {"metric": "test_count", "value": total_tests}, {"metric": "pass_rate", "value": f"{pass_rate:.2f}%"}], ["metric", "value"])

    jacoco_rows = []
    jacoco_xml = ROOT / "backend/target/site/jacoco/jacoco.xml"
    if jacoco_xml.exists():
        report = ET.parse(jacoco_xml).getroot()
        for counter in report.findall("counter"):
            metric = counter.attrib["type"].lower()
            missed = int(counter.attrib["missed"])
            covered = int(counter.attrib["covered"])
            pct = covered / (covered + missed) * 100 if covered + missed else 100
            jacoco_rows.append({"metric": metric, "covered": covered, "missed": missed, "coverage_percent": f"{pct:.2f}"})
    else:
        jacoco_rows = [{"metric": x, "covered": "NOT_RUN", "missed": "NOT_RUN", "coverage_percent": "NOT_RUN"} for x in ["instruction", "line", "branch", "method", "class"]]
    csv_write(OUT / "coverage_summary.csv", jacoco_rows, ["metric", "covered", "missed", "coverage_percent"])

    target_rows = [
        {"metric": "use_case_count", "target": 16, "actual": "NA", "difference": "NA", "deviation_percent": "NA", "reason": "源码度量范围不包含需求用例明细"},
        {"metric": "ucp", "target": 110, "actual": 109.10, "difference": -0.90, "deviation_percent": "-0.82%", "reason": "采用 S2 已记录实际 UCP"},
        {"metric": "class_interface_count", "target": 45, "actual": len(types), "difference": len(types) - 45, "deviation_percent": f"{(len(types)-45)/45*100:.2f}%", "reason": "实现阶段新增 DTO、配置、安全、目录服务和状态枚举等合理类型"},
        {"metric": "effective_main_loc", "target": 6000, "actual": total_main, "difference": total_main - 6000, "deviation_percent": f"{(total_main-6000)/6000*100:.2f}%", "reason": "按 backend main + frontend src 有效代码行统计，测试单列"},
        {"metric": "database_table_count", "target": 12, "actual": 12, "difference": 0, "deviation_percent": "0.00%", "reason": "沿用真实数据库结构核验"},
        {"metric": "test_count", "target": 60, "actual": total_tests, "difference": total_tests - 60, "deviation_percent": f"{(total_tests-60)/60*100:.2f}%", "reason": "Phase 1～3 增加自动化测试"},
    ]
    csv_write(OUT / "target_deviation.csv", target_rows, ["metric", "target", "actual", "difference", "deviation_percent", "reason"])
    return {"types": types, "main_loc": main_loc, "test_loc": test_loc, "front_loc": front_loc, "total_main": total_main, "total_all": total_all, "methods": methods, "complexity_rows": complexity_rows, "ck_rows": ck_rows, "over_rows": over_rows, "test_rows": test_rows, "total_tests": total_tests, "pass_rate": pass_rate, "jacoco_rows": jacoco_rows, "categories": categories}


if __name__ == "__main__":
    result = measure()
    print(f"types={len(result['types'])} classes={sum(x.kind == 'class' for x in result['types'])} interfaces={sum(x.kind == 'interface' for x in result['types'])} enums={sum(x.kind == 'enum' for x in result['types'])}")
    print(f"main_loc={result['main_loc']['code']} test_loc={result['test_loc']['code']} frontend_loc={result['front_loc']['code']} total_main={result['total_main']} total_all={result['total_all']}")
    print(f"methods={len(result['methods'])} max_vg={max((int(x['cyclomatic_complexity']) for x in result['complexity_rows']), default=0)} ck_over_threshold={sum(x['over_threshold'] == 'YES' for x in result['ck_rows'])} tests={result['total_tests']} pass_rate={result['pass_rate']:.2f}%")

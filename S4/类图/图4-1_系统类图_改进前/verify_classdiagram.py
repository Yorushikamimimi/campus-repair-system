#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""S4 图4-1 系统类图（改进前）机器自检。

检查项（对应任务书第十二节 1-16 条）：
  1  类/接口总数恰好 45
  2  AbstractAuditableEntity 存在且为 abstract
  3  9 个 Service 接口全部存在
  4  9 个 Service 实现类全部存在
  5  6 个 Controller 全部存在
  6  12 张数据库表对应实体全部存在
  7  QueryService 与 StatisticsService 明确拆开（未被合并）
  8  9 条接口实现关系正确
  9  2 条继承关系正确
 10  不存在重复类
 11  不存在额外第 46 个类
 12  无支付/采购/库存/GPS/短信/微信/AI派单等边界外内容
 13  所有方法可见性、参数、参数类型、返回类型可见
 14  PNG 与 SVG 成功生成（SVG 尺寸/可读性、PNG 宽度 >= 5000）
 15  未修改任何 Word/docx
 16  未修改 S0～S3

另附几何检查：类框互不覆盖、依赖线不穿越类框（不压类名/字段/方法文本）、
多重度与角色标注不重叠、接口与实现上下对应、横向画布。

用法：python3 verify_classdiagram.py [--dir <图4-1目录>] [--repo <仓库根>]
"""
from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import re
import subprocess
import sys

# ----------------------------------------------------------------------------
# 规范基线（任务书第四 ~ 九节）
# ----------------------------------------------------------------------------
DOMAIN = ["AbstractAuditableEntity", "SysUser", "SysRole", "SysUserRole", "RepairOrder",
          "RepairType", "RepairLocation", "RepairImage", "DispatchRecord", "RepairRecord",
          "AcceptanceRecord", "EvaluationRecord", "OrderStatusLog"]
CONTROLLERS = ["UserController", "RepairOrderController", "DispatchController",
               "MaintenanceController", "AcceptanceController", "QueryStatisticsController"]
SERVICES = ["UserPermissionService", "RepairOrderService", "DispatchService", "MaintenanceService",
            "AcceptanceService", "QueryService", "StatisticsService", "OrderStateService",
            "FileStorageService"]
IMPLS = ["UserPermissionServiceImpl", "RepairOrderServiceImpl", "DispatchServiceImpl",
         "MaintenanceServiceImpl", "AcceptanceServiceImpl", "QueryServiceImpl",
         "StatisticsServiceImpl", "OrderStateServiceImpl", "LocalFileStorageServiceImpl"]
REPOS = ["UserPermissionRepository", "RepairOrderRepository", "DispatchRepository",
         "MaintenanceRepository", "AcceptanceRepository", "EvaluationRepository",
         "OrderStatusRepository"]
INFRA = ["JwtTokenProvider"]
# 12 张数据库表对应的领域实体
TABLES_12 = ["SysUser", "SysRole", "SysUserRole", "RepairOrder", "RepairType", "RepairLocation",
             "RepairImage", "DispatchRecord", "RepairRecord", "AcceptanceRecord",
             "EvaluationRecord", "OrderStatusLog"]

EXPECTED_IMPL = {
    "UserPermissionService": "UserPermissionServiceImpl",
    "RepairOrderService": "RepairOrderServiceImpl",
    "DispatchService": "DispatchServiceImpl",
    "MaintenanceService": "MaintenanceServiceImpl",
    "AcceptanceService": "AcceptanceServiceImpl",
    "QueryService": "QueryServiceImpl",
    "StatisticsService": "StatisticsServiceImpl",
    "OrderStateService": "OrderStateServiceImpl",
    "FileStorageService": "LocalFileStorageServiceImpl",
}
EXPECTED_INHERIT = [("AbstractAuditableEntity", "SysUser"),
                    ("AbstractAuditableEntity", "RepairOrder")]

FORBIDDEN = ["支付", "采购", "库存", "GPS", "gps", "短信", "微信", "AI派单", "支付宝",
             "Payment", "Purchase", "Inventory", "Sms", "WeChat", "AiDispatch"]

# ----------------------------------------------------------------------------
# PUML 解析
# ----------------------------------------------------------------------------
CLS_RE = re.compile(r"^\s*(abstract class|class|interface)\s+(\w+)[^{]*\{", re.M)


def parse_body(text: str):
    """Return ({class name: raw body text}, [declaration order]) using brace matching."""
    bodies, order = {}, []
    for m in re.finditer(r"(?m)^\s*(abstract class|class|interface)\s+(\w+)[^{]*\{", text):
        depth, i = 1, m.end()
        while depth and i < len(text):
            if text[i] == "{":
                depth += 1
            elif text[i] == "}":
                depth -= 1
            i += 1
        bodies[m.group(2)] = text[m.end():i - 1]
        order.append(m.group(2))
    return bodies, order


def parse_puml(text: str):
    classes, relations, order = {}, [], []
    for m in CLS_RE.finditer(text):
        kind, name = m.group(1), m.group(2)
        depth, i = 1, m.end()
        while i < len(text) and depth:
            if text[i] == "{":
                depth += 1
            elif text[i] == "}":
                depth -= 1
            i += 1
        body = text[m.end():i - 1]
        attrs, methods = [], []
        for line in body.splitlines():
            line = line.strip()
            if not line or line == "--":
                continue
            (attrs if re.match(r"^[#+\-~]", line) and "(" not in line else methods).append(line)
        classes[name] = dict(kind=kind, attrs=attrs, methods=methods)
        order.append(name)
    for raw in text.splitlines():
        line = raw.split("'")[0].strip() if raw.strip().startswith("'") else raw.strip()
        if not line or line.startswith(("@", "title", "skinparam", "package", "}", "class",
                                        "interface", "abstract")):
            continue
        m = re.match(r"^(\w+)\s+(\S+?)\s+(--|\.\.|\*--|o--|<\|--|<\|\.\.|---|\.\.>|-->|\*\.\.|o\.\.)"
                     r"\s*(\S+?)\s*(\w+)?\s*(?::\s*(.*))?$", line)
        if m:
            relations.append(dict(left=m.group(1), lmult=m.group(2), op=m.group(3),
                                  rmult=m.group(4), right=m.group(5), label=(m.group(6) or "").strip()))
            continue
        m = re.match(r"^(\w+)\s*(<\|--|<\|\.\.|\.\.>)\s*(\w+)\s*$", line)
        if m:
            relations.append(dict(left=m.group(1), op=m.group(2), right=m.group(3),
                                  lmult="", rmult="", label=""))
    return classes, relations, order


# ----------------------------------------------------------------------------
# SVG 解析与几何检查
# ----------------------------------------------------------------------------
def _bezier(p0, p1, p2, p3, n=24):
    out = []
    for i in range(n + 1):
        t = i / n
        u = 1 - t
        out.append((u * u * u * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t * t * t * p3[0],
                    u * u * u * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t * t * t * p3[1]))
    return out


def _path_samples(d):
    toks = re.findall(r"([MLC])([-\d.,\s]*?)(?=[MLC]|$)", d)
    segs, cur = [], None
    for cmd, arg in toks:
        nums = [float(v) for v in re.findall(r"-?\d+\.?\d*(?:e-?\d+)?", arg)]
        pairs = list(zip(nums[0::2], nums[1::2]))
        if cmd == "M":
            for p in pairs:
                cur = p
                segs.append([cur])
        elif cmd == "L":
            for p in pairs:
                segs.append([cur, p])
                cur = p
        elif cmd == "C":
            for i in range(0, len(pairs) - 2, 3):
                segs.append(_bezier(cur, pairs[i], pairs[i + 1], pairs[i + 2]))
                cur = pairs[i + 2]
    return segs


def parse_svg(path):
    t = pathlib.Path(path).read_text(encoding="utf-8")
    boxes, clusters, edges, labels = {}, {}, [], []
    for m in re.finditer(r'<g id="elem_(\w+)">\s*<rect[^>]*?height="([\d.]+)"[^>]*?width="([\d.]+)"'
                         r'[^>]*?x="(-?[\d.]+)" y="(-?[\d.]+)"', t):
        n, h, w, x, y = m.group(1), *map(float, m.groups()[1:])
        boxes[n] = (x, y, x + w, y + h)
    for m in re.finditer(r'<g id="cluster_(\w+)">\s*<rect[^>]*?height="([\d.]+)"[^>]*?width="([\d.]+)"'
                         r'[^>]*?x="(-?[\d.]+)" y="(-?[\d.]+)"', t):
        n, h, w, x, y = m.group(1), *map(float, m.groups()[1:])
        clusters[n] = (x, y, x + w, y + h)
    for tag in re.findall(r"<path[^>]*>", t):
        if 'fill="none"' not in tag:
            continue
        eid, d = re.search(r'id="([^"]+)"', tag), re.search(r'd="([^"]+)"', tag)
        if eid and d:
            s = _path_samples(d.group(1))
            if s:
                edges.append((eid.group(1), s))
    for tag in re.findall(r"<text[^>]*>.*?</text>", t, re.S):
        x = float(re.search(r'\sx="(-?[\d.]+)"', tag).group(1))
        y = float(re.search(r'\sy="(-?[\d.]+)"', tag).group(1))
        fs = float(re.search(r'font-size="([\d.]+)"', tag).group(1))
        tl = re.search(r'textLength="([\d.]+)"', tag)
        raw = re.sub(r"<[^>]+>", "", tag)
        if not raw.strip():
            continue
        labels.append(dict(text=raw.strip(), x=x, y=y, fs=fs,
                           w=float(tl.group(1)) if tl else 0.0,
                           box=(x, y - 0.72 * fs, x + (float(tl.group(1)) if tl else 0.0), y + 0.05 * fs)))
    vb = re.search(r'viewBox="0 0 ([\d.]+) ([\d.]+)"', t)
    W, H = (float(vb.group(1)), float(vb.group(2))) if vb else (0.0, 0.0)
    return boxes, clusters, edges, labels, W, H


class Report:
    def __init__(self):
        self.rows = []

    def check(self, no, title, ok, detail=""):
        self.rows.append((no, title, bool(ok), detail))
        return bool(ok)

    def dump(self):
        width = max(len(t) for _, t, _, _ in self.rows)
        failed = 0
        for no, title, ok, detail in self.rows:
            mark = "PASS" if ok else "FAIL"
            if not ok:
                failed += 1
            print(f"[{mark}] {no:>2}. {title:<{width}}  {detail}")
        print()
        print(f"合计 {len(self.rows)} 项，通过 {len(self.rows) - failed} 项，失败 {failed} 项")
        return failed


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", default=".")
    ap.add_argument("--repo", default="../..")
    args = ap.parse_args()

    d = pathlib.Path(args.dir).resolve()
    repo = (d / args.repo).resolve()
    puml = d / "S4_图4-1_系统类图_改进前.puml"
    svg = d / "S4_图4-1_系统类图_改进前.svg"
    png = d / "S4_图4-1_系统类图_改进前.png"

    r = Report()
    text = puml.read_text(encoding="utf-8")
    classes, relations, order = parse_puml(text)
    names = list(classes)

    # 1 类/接口总数
    r.check(1, "类/接口总数恰好 45", len(classes) == 45, f"实际 {len(classes)}")
    # 2 抽象基类
    aae = classes.get("AbstractAuditableEntity")
    r.check(2, "AbstractAuditableEntity 存在且为 abstract",
            aae is not None and aae["kind"] == "abstract class",
            f"kind={aae['kind'] if aae else 'missing'}")
    # 3 / 4 / 5 / 6 各组存在性
    r.check(3, "9 个 Service 接口全部存在",
            all(n in classes and classes[n]["kind"] == "interface" for n in SERVICES),
            f"{sum(1 for n in SERVICES if n in classes)}/9")
    r.check(4, "9 个 Service 实现类全部存在",
            all(n in classes and classes[n]["kind"] == "class" for n in IMPLS),
            f"{sum(1 for n in IMPLS if n in classes)}/9")
    r.check(5, "6 个 Controller 全部存在",
            all(n in classes for n in CONTROLLERS),
            f"{sum(1 for n in CONTROLLERS if n in classes)}/6")
    r.check(6, "12 张数据库表对应实体全部存在",
            all(n in classes for n in TABLES_12),
            f"{sum(1 for n in TABLES_12 if n in classes)}/12")
    # 7 Query / Statistics 拆分：两个接口、两个实现类各自独立；
    #   QueryStatisticsController 只做 Web 接入（仅持有两个 Service 引用、无私有业务方法）；
    #   不存在把查询与统计合二为一的业务 Service。
    merged = [n for n in classes
              if n != "QueryStatisticsController" and "Statistic" in n and "Query" in n]
    qsc = classes.get("QueryStatisticsController", {})
    thin_gateway = (qsc.get("attrs") == ["- queryService: QueryService",
                                         "- statisticsService: StatisticsService"]
                    and not [m for m in qsc.get("methods", []) if m.lstrip().startswith("-")])
    r.check(7, "QueryService 与 StatisticsService 明确拆开",
            "QueryService" in classes and "StatisticsService" in classes
            and "QueryServiceImpl" in classes and "StatisticsServiceImpl" in classes
            and not merged and thin_gateway,
            f"接口 2/2、实现类 2/2，合并业务 Service={merged or '无'}，"
            f"Controller 仅做 Web 接入={'是' if thin_gateway else '否'}")
    # 8 实现关系
    got_impl = {(x["left"], x["right"]) for x in relations if x["op"] == "<|.."}
    want_impl = set(EXPECTED_IMPL.items())
    r.check(8, "9 条接口实现关系正确", got_impl == want_impl,
            f"正确 {len(got_impl & want_impl)}/9" + (f" 缺失{sorted(want_impl - got_impl)}" if got_impl != want_impl else "")
            + (f" 多余{sorted(got_impl - want_impl)}" if got_impl - want_impl else ""))
    # 9 继承关系
    got_inh = {(x["left"], x["right"]) for x in relations if x["op"] == "<|--"}
    want_inh = set(EXPECTED_INHERIT)
    r.check(9, "2 条继承关系正确", got_inh == want_inh,
            f"实际 {sorted(got_inh)}")
    # 10 / 11 重复与多余
    dups = [n for n in set(order) if order.count(n) > 1]
    expected_all = set(DOMAIN + CONTROLLERS + SERVICES + IMPLS + REPOS + INFRA)
    extra = sorted(set(names) - expected_all)
    missing = sorted(expected_all - set(names))
    r.check(10, "不存在重复类", not dups, f"重复={dups or '无'}")
    r.check(11, "不存在额外第 46 个类", not extra and not missing,
            f"额外={extra or '无'} 缺失={missing or '无'}")

    # 12 边界外内容
    hits = [w for w in FORBIDDEN if w in text]
    r.check(12, "无支付/采购/库存/GPS/短信/微信/AI派单等边界外内容", not hits,
            f"命中={hits or '无'}")

    # 13 方法签名完整性
    bad = []
    for n, c in classes.items():
        for mline in c["methods"]:
            mm = re.match(r"^([#+\-~])\s*(\w+)\s*\((.*)\)\s*:\s*([^:]+?)\s*$", mline)
            if not mm:
                bad.append(f"{n}.{mline}")
                continue
            params = [p for p in mm.group(3).split(",") if p.strip()]
            for p in params:
                if not re.match(r"^\s*\w+\s*:\s*\S+", p):
                    bad.append(f"{n}.{mline} :: 参数缺类型 [{p.strip()}]")
    r.check(13, "所有方法可见性/参数/参数类型/返回类型可见", not bad,
            f"异常 {len(bad)} 处" + (f" 例:{bad[:2]}" if bad else ""))
    # 14 渲染产物
    ok_svg = svg.exists() and svg.stat().st_size > 10000
    ok_png = png.exists() and png.stat().st_size > 10000
    r.check(14, "PNG 与 SVG 成功生成", ok_svg and ok_png,
            f"svg={svg.stat().st_size if ok_svg else 0}B png={png.stat().st_size if ok_png else 0}B")

    # 15 / 16 未改动 docx、未改动 S0～S3（优先 git，非 git 仓库退回 mtime 比对）
    selftest_dir = d.relative_to(repo).as_posix() if str(d).startswith(str(repo)) else ""

    def changed_since(since_ts):
        docs, frozen = [], []
        for p in repo.rglob("*"):
            if not p.is_file():
                continue
            rp = p.relative_to(repo).as_posix()
            if rp.startswith(selftest_dir):
                continue
            if p.stat().st_mtime <= since_ts:
                continue
            if p.suffix.lower() in (".docx", ".doc"):
                docs.append(rp)
            if re.match(r"^S[0-3]/", rp):
                frozen.append(rp)
        return docs, frozen

    def mtime_verdict():
        marks = [m for m in (d / "archive",) if m.exists()]
        ts = min(m.stat().st_mtime for m in marks) if marks else puml.stat().st_mtime
        docs, frozen = changed_since(ts)
        return docs, frozen, ts

    docs_changed, frozen_changed = [], []
    try:
        out = subprocess.run(["git", "status", "--porcelain"], cwd=repo,
                             capture_output=True, text=True, timeout=60)
        if out.returncode != 0:
            raise RuntimeError("not a git repository")
        for line in out.stdout.splitlines():
            p = line[3:].strip().strip('"')
            if p.lower().endswith((".docx", ".doc")):
                docs_changed.append(p)
            if re.match(r"^S[0-3]/", p):
                frozen_changed.append(p)
        detail = f"模式=git；docx 变更 {len(docs_changed)} 个，S0～S3 变更 {len(frozen_changed)} 个"
    except Exception:
        import datetime
        docs_changed, frozen_changed, ts = mtime_verdict()
        detail = ("模式=mtime（非 git 仓库）；参照时刻="
                  f"{datetime.datetime.fromtimestamp(ts):%Y-%m-%d %H:%M:%S}；"
                  f"docx 晚于参照 {len(docs_changed)} 个，S0～S3 晚于参照 {len(frozen_changed)} 个")
    r.check(15, "未修改任何 Word/docx", not docs_changed,
            detail if not docs_changed else f"{detail} 变更={docs_changed}")
    r.check(16, "未修改 S0～S3 已冻结文件", not frozen_changed,
            detail if not frozen_changed else f"{detail} 变更={frozen_changed}")

    # 17 属性可见性与类型
    bad_attr = [f"{n}.{a}" for n, c in classes.items() for a in c["attrs"]
                if not re.match(r"^[#+\-~]\s*\w+\s*:\s*\S+", a)]
    r.check(17, "所有属性可见性与类型可见", not bad_attr, f"异常 {len(bad_attr)} 处")

    # 几何检查
    if ok_svg:
        boxes, clusters, edges, labels, W, H = parse_svg(svg)
        r.check(18, "SVG 中类框数量 = 45", len(boxes) == 45, f"实际 {len(boxes)}")
        ov = [(a, b) for i, a in enumerate(boxes) for b in list(boxes)[i + 1:]
              if boxes[a][0] < boxes[b][2] and boxes[b][0] < boxes[a][2]
              and boxes[a][1] < boxes[b][3] and boxes[b][1] < boxes[a][3]]
        r.check(19, "类框互不覆盖", not ov, f"重叠对={len(ov)}")
        crossed = {}
        eps = 6.0
        for eid, segs in edges:
            bad_box = set()
            for s in segs:
                for x, y in s:
                    for n, (x1, y1, x2, y2) in boxes.items():
                        if x1 + eps < x < x2 - eps and y1 + eps < y < y2 - eps:
                            bad_box.add(n)
            if bad_box:
                crossed[eid] = sorted(bad_box)
        r.check(20, "依赖/关联线未穿越任何类框（不压类名与字段）", not crossed,
                f"穿越边数={len(crossed)}/{len(edges)}")
        r.check(21, "关系线总数 = 72（2 继承 + 9 实现 + 17 领域 + 7 控制器 + 25 实现类依赖 + 12 仓储依赖）",
                len(edges) == 72, f"实际 {len(edges)}")
        # 多重度/角色标注（字体 < 20 的文本）：
        #   注意必须先取全量标注，再判定"是否压到类框"——不能先把压框标注过滤掉，
        #   否则该检查会恒真（曾出现此缺陷，导致 6 处压框标注未被发现）。
        small = [l for l in labels if l["fs"] < 20]
        lab_ov = 0
        for i in range(len(small)):
            for j in range(i + 1, len(small)):
                a, b = small[i]["box"], small[j]["box"]
                if a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]:
                    lab_ov += 1
        on_box = {}
        for l in small:
            for n, (bx1, by1, bx2, by2) in boxes.items():
                ox = min(l["box"][2], bx2) - max(l["box"][0], bx1)
                oy = min(l["box"][3], by2) - max(l["box"][1], by1)
                if ox > 2 and oy > 2:
                    on_box.setdefault(l["text"], []).append((n, round(ox, 1), round(oy, 1)))
        n_on = sum(len(v) for v in on_box.values())
        r.check(22, "多重度/角色标注互不重叠、且不压在类框文字上",
                lab_ov == 0 and n_on == 0,
                f"标注总数={len(small)} 标注间重叠={lab_ov} 压框={n_on}"
                + (f" 例:{list(on_box.items())[:3]}" if n_on else ""))
        # 层带（package）归属：用于判断"接口层带"与"实现层带"是否相邻（与布局方向无关）
        def owner(box):
            best = None
            for name, (cx1, cy1, cx2, cy2) in clusters.items():
                if cx1 - 2 <= box[0] and box[2] <= cx2 + 2 and cy1 - 2 <= box[1] and box[3] <= cy2 + 2:
                    area = (cx2 - cx1) * (cy2 - cy1)
                    if best is None or area < best[1]:
                        best = (name, area)
            return best[0] if best else None

        def band_gap(b1, b2):
            dx = max(0.0, max(b1[0] - b2[2], b2[0] - b1[2]))
            dy = max(0.0, max(b1[1] - b2[3], b2[1] - b1[3]))
            return (dx * dx + dy * dy) ** 0.5

        wrong, gaps = [], []
        for iface, impl in EXPECTED_IMPL.items():
            if iface not in boxes or impl not in boxes:
                continue
            oi, om = owner(boxes[iface]), owner(boxes[impl])
            gap = band_gap(clusters[oi], clusters[om]) if oi and om and oi != om else float("inf")
            gaps.append((iface, gap))
            # 实现类必须与其接口分处相邻的两个层带（LR 为左右相邻列，TB 为上下相邻带）
            if not (oi and om and oi != om and gap <= 0.05 * min(W, H)):
                wrong.append(f"{iface}->{om}")
        maxgap = max((g for _, g in gaps if g != float("inf")), default=0)
        r.check(23, "Service 接口与其实现类位于相邻层带（接口层带紧邻实现层带）",
                not wrong,
                f"{len(gaps)}/9 成对，层带间距最大 {maxgap:.0f}px（阈值 {0.05 * min(W, H):.0f}px），"
                f"未对齐={wrong or '无'}")
        r.check(24, "大画布且宽高比落在 1.6:1 ~ 2:1（适配 Word A4 横向整页插图）",
                1.6 <= W / H <= 2.0 and W >= 5000,
                f"canvas={W:.0f}x{H:.0f} ratio={W / H:.2f}")
        if ok_png:
            try:
                from PIL import Image
                im = Image.open(png)
                r.check(25, "PNG 宽度 >= 5000px 且与 SVG 尺寸一致",
                        im.size[0] >= 5000 and abs(im.size[0] - W) <= 2 and abs(im.size[1] - H) <= 2,
                        f"png={im.size[0]}x{im.size[1]} svg={W:.0f}x{H:.0f}")
            except Exception as e:  # pragma: no cover
                r.check(25, "PNG 尺寸检查", False, repr(e))

    # 26 内容冻结：与 archive/v1 基线逐字节/逐关系比对（只允许版式差异）
    v1p = d / "archive" / "v1_S4_图4-1_系统类图_改进前.puml"
    if v1p.exists():
        old = v1p.read_text(encoding="utf-8")
        _c, _o = parse_body(old)
        _cn, _on = parse_body(text)

        def _norm(s):
            s = re.sub(r"\bo--\w+--", "o--", s)
            s = re.sub(r"\*--\w+--", "*--", s)
            s = re.sub(r"--\w+--", "--", s)
            s = re.sub(r"(?<![\w*])-\w+-(?= )", "--", s)
            return re.sub(r"\s+", " ", s).strip()

        def _rels(t):
            i = t.rindex("\n}")
            return [_norm(x.strip()) for x in t[i:].splitlines()
                    if x.strip() and not x.strip().startswith(("}", "'", "@"))]

        ro, rn = _rels(old), _rels(text)
        same_bodies = _c == _cn and len(_cn) == 45
        same_order = _o == _on
        same_rels = ro == rn and len(rn) == 72
        diff = [k for k in _c if _c.get(k) != _cn.get(k)]
        r.check(26, "内容 100% 冻结（对比 archive/v1：45 个类体逐字节一致、声明顺序一致、72 条关系语义一致）",
                same_bodies and same_order and same_rels,
                f"类体一致={same_bodies} 顺序一致={same_order} 关系一致={same_rels}"
                + (f" 差异类={diff}" if diff else ""))

    # 27 语义指纹：箭头/菱形/三角等装饰图元计数与 archive/v1 完全一致（继承/实现/聚合/组合/依赖未被改动）
    v1svg = d / "archive" / "v1_S4_图4-1_系统类图_改进前.svg"
    if v1svg.exists():
        import collections

        def fingerprint(p):
            c = collections.Counter()
            for tag, attrs in re.findall(r"<(rect|ellipse|polygon|path|line)\b([^>]*)>",
                                         pathlib.Path(p).read_text(encoding="utf-8")):
                fill = (re.search(r'fill="([^"]*)"', attrs) or [None, "-"])[1]
                c[(tag, fill)] += 1
            return c
        fp_new, fp_old = fingerprint(svg), fingerprint(v1svg)
        r.check(27, "关系装饰语义指纹与 v1 一致（空心菱形=聚合4 / 实心菱形=组合2 / 三角=继承2+实现9 / 箭头=依赖44）",
                fp_new == fp_old and fp_new[("polygon", "none")] == 15 and fp_new[("polygon", "#181818")] == 46,
                f"空心多边形={fp_new[('polygon', 'none')]} 实心多边形={fp_new[('polygon', '#181818')]} 指纹一致={fp_new == fp_old}")

    # 28 版式收紧效果（信息项：相对 v1 的宽度与留白改善）
    v1svg = d / "archive" / "v1_S4_图4-1_系统类图_改进前.svg"
    if ok_svg and v1svg.exists():
        _b1, _c1, _e1, _l1, W1, H1 = parse_svg(v1svg)
        if _b1:
            r.check(28, "版式重排效果：宽度显著收窄（整页插图字号变大）",
                    W < W1,
                    f"宽度 {W1:.0f}px -> {W:.0f}px ({(1 - W / W1) * 100:.0f}%)，"
                    f"宽高比 {W1 / H1:.2f} -> {W / H:.2f}，等效 A4 横向字号 {26.25 * 297 / W1:.3f}mm -> {26.25 * 297 / W:.3f}mm")

    return r.dump()


if __name__ == "__main__":
    sys.exit(1 if main() else 0)

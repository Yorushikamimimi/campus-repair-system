#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""校验 S4 控制流图的 n / e / 判定节点数 / V(G)，并检测边的交叉（区域数法一致性）。"""
import re
import subprocess
import sys
from itertools import combinations

BASE = "S4/控制流图"
TARGETS = {
    "acceptRepairResult": {
        "dot": f"{BASE}/acceptRepairResult/S4_图4-2b_acceptRepairResult控制流图.dot",
        "n": 13, "e": 17, "d": 5, "vg": 6,
    },
    "createOrder": {
        "dot": f"{BASE}/createOrder/S4_图4-2c_createOrder控制流图.dot",
        "n": 12, "e": 15, "d": 4, "vg": 5,
    },
}


def parse_dot(path):
    src = open(path, encoding="utf-8").read()
    body = src[src.index("{") + 1:]
    nodes, shapes = {}, {}
    for m in re.finditer(r'^\s*(N\d+)\s*\[([^\]]*)\];', body, re.M):
        name, attrs = m.group(1), m.group(2)
        nodes[name] = attrs
        s = re.search(r'shape=(\w+)', attrs)
        shapes[name] = s.group(1) if s else "box"
    edges = []
    for m in re.finditer(r'^\s*(N\d+)\s*->\s*(N\d+)\s*(\[[^\]]*\])?;', body, re.M):
        edges.append((m.group(1), m.group(2)))
    return nodes, shapes, edges


def seg_inter(p1, p2, p3, p4):
    def cross(o, a, b):
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])

    def on(a, b, c):
        return min(a[0], b[0]) - 1e-6 <= c[0] <= max(a[0], b[0]) + 1e-6 and \
               min(a[1], b[1]) - 1e-6 <= c[1] <= max(a[1], b[1]) + 1e-6

    d1, d2 = cross(p3, p4, p1), cross(p3, p4, p2)
    d3, d4 = cross(p1, p2, p3), cross(p1, p2, p4)
    if ((d1 > 0) != (d2 > 0)) and ((d3 > 0) != (d4 > 0)):
        return True
    for (a, b, c, d) in ((p1, p2, p3, d3), (p1, p2, p4, d4), (p3, p4, p1, d1), (p3, p4, p2, d2)):
        if abs(d) < 1e-9 and on(a, b, c):
            return True
    return False


def layout(dot_path):
    """用 graphviz -Tplain 取得节点中心与边折线，用于交叉检测。"""
    plain = subprocess.run(["dot", "-Tplain", dot_path], capture_output=True, text=True).stdout
    centers, splines = {}, []
    for line in plain.splitlines():
        f = line.split()
        if not f:
            continue
        if f[0] == "node":
            centers[f[1]] = (float(f[2]), float(f[3]))
        elif f[0] == "edge":
            npts = int(f[3])
            nums = [float(x) for x in f[4:4 + 2 * npts]]
            pts = list(zip(nums[0::2], nums[1::2]))
            splines.append((f[1], f[2], pts))
    return centers, splines


def crossing_count(splines):
    def polyline(pts):
        out = [pts[0]]
        for i in range(1, len(pts) - 2, 3):
            out.append(pts[i + 2])
        return out

    segs, crosses = [], 0
    detail = []
    for src, dst, pts in splines:
        poly = polyline(pts)
        for a, b in zip(poly, poly[1:]):
            segs.append((src, dst, a, b))
    for (s1, d1, a1, b1), (s2, d2, a2, b2) in combinations(segs, 2):
        if {s1, d1} == {s2, d2}:          # 同一对节点之间（回边/平行边）不算
            continue
        if len({s1, d1, s2, d2}) < 4:      # 共享端点的相邻边不算交叉
            continue
        if seg_inter(a1, b1, a2, b2):
            crosses += 1
            detail.append(f"{s1}->{d1} × {s2}->{d2}")
    return crosses, detail


ok_all = True
for name, t in TARGETS.items():
    nodes, shapes, edges = parse_dot(t["dot"])
    n = len(nodes)
    e = len(edges)
    d = sum(1 for s in shapes.values() if s == "diamond")
    print(f"===== {name} =====")
    print(f"  n（节点数）        = {n}  (目标 {t['n']})  {'OK' if n == t['n'] else '✗'}")
    print(f"  e（边数）          = {e}  (目标 {t['e']})  {'OK' if e == t['e'] else '✗'}")
    print(f"  判定节点数         = {d}  (目标 {t['d']})  {'OK' if d == t['d'] else '✗'}")
    print(f"  判定节点法 V(G)    = {d} + 1 = {d + 1}  (目标 {t['vg']})  {'OK' if d + 1 == t['vg'] else '✗'}")
    print(f"  边点差法   V(G)    = {e} - {n} + 2 = {e - n + 2}  (目标 {t['vg']})  {'OK' if e - n + 2 == t['vg'] else '✗'}")

    centers, splines = layout(t["dot"])
    crosses, detail = crossing_count(splines)
    print(f"  区域数法   V(G)    = e-n+2 = {e - n + 2}（需图形平面无交叉：交叉数 = {crosses}）"
          f"  {'OK' if crosses == 0 else '✗'}")
    for line in detail[:10]:
        print(f"      crossing: {line}")

    # 结构校验：唯一汇点、可达性
    outdeg = {k: 0 for k in nodes}
    for s, dd in edges:
        outdeg[s] += 1
    sinks = [k for k, v in outdeg.items() if v == 0]
    reach, stack = set(), ["N1"]
    adj = {}
    for s, dd in edges:
        adj.setdefault(s, []).append(dd)
    while stack:
        cur = stack.pop()
        if cur in reach:
            continue
        reach.add(cur)
        stack.extend(adj.get(cur, []))
    print(f"  汇点（出度0）      = {sinks}  起始节点可达全部节点：{len(reach) == n}")
    if len(sinks) != 1 or len(reach) != n:
        ok_all = False

    three = (d + 1 == e - n + 2 == t["vg"] and crosses == 0 and
             n == t["n"] and e == t["e"] and d == t["d"])
    print(f"  三法是否一致       = {'是' if three else '否'}")
    ok_all = ok_all and three

print("\n总体：", "全部与目标值一致 ✔" if ok_all else "存在不一致 ✗")
sys.exit(0 if ok_all else 1)

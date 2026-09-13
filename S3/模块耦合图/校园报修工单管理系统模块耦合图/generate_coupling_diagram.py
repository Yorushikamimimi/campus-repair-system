from math import atan2, cos, sin, pi
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


OUT = Path(__file__).parent
S = 2
W, H = 3200, 2000

BG = "#ffffff"
INK = "#27323a"
MUTED = "#66737d"
HEADER = "#e8eef7"
ID = "#214d78"
TEXT = "#18232b"

FONT_REG = "/System/Library/Fonts/STHeiti Light.ttc"
FONT_MED = "/System/Library/Fonts/STHeiti Medium.ttc"


def font(size, medium=False):
    return ImageFont.truetype(FONT_MED if medium else FONT_REG, size * S)


def sc_points(points):
    return [(int(x * S), int(y * S)) for x, y in points]


def draw_arrow(draw, points, width=3):
    pts = sc_points(points)
    draw.line(pts, fill=INK, width=width * S, joint="curve")
    x1, y1 = pts[-2]
    x2, y2 = pts[-1]
    angle = atan2(y2 - y1, x2 - x1)
    length = 18 * S
    spread = 7 * S
    left = (x2 - length * cos(angle) + spread * sin(angle),
            y2 - length * sin(angle) - spread * cos(angle))
    right = (x2 - length * cos(angle) - spread * sin(angle),
             y2 - length * sin(angle) + spread * cos(angle))
    draw.polygon([(x2, y2), left, right], fill=INK)


def centered(draw, box, value, fnt, fill=TEXT):
    x0, y0, x1, y1 = box
    b = draw.textbbox((0, 0), value, font=fnt)
    draw.text(((x0 + x1 - (b[2] - b[0])) / 2,
               (y0 + y1 - (b[3] - b[1])) / 2 - b[1]), value,
              font=fnt, fill=fill)


def module(draw, x, y, w, h, mid, name):
    # A consistent header band makes the module hierarchy readable at report scale.
    draw.rectangle((x * S, y * S, (x + w) * S, (y + h) * S),
                   fill=BG, outline=INK, width=3 * S)
    draw.rectangle((x * S, y * S, (x + w) * S, (y + 58) * S), fill=HEADER)
    draw.line((x * S, (y + 58) * S, (x + w) * S, (y + 58) * S),
              fill=INK, width=1 * S)
    draw.text(((x + 24) * S, (y + 14) * S), mid,
              font=font(28, medium=True), fill=ID)
    draw.text(((x + 95) * S, (y + 14) * S), name,
              font=font(28, medium=True), fill=TEXT)


def make_png():
    image = Image.new("RGB", (W * S, H * S), BG)
    draw = ImageDraw.Draw(image)

    centered(draw, (0, 28, W, 92), "校园报修工单管理系统模块耦合图", font(48, medium=True))

    # Layer labels
    for y, label in [(117, "接入层"), (377, "业务模块"),
                     (787, "公共业务层"), (1237, "底层数据访问层")]:
        draw.text((40 * S, y * S), label, font=font(24, medium=True), fill=MUTED)

    # Exact 22 direct-call relationships.
    edges = [
        # M1 -> M2..M7
        [(1370, 270), (300, 390)], [(1460, 270), (820, 390)],
        [(1550, 270), (1340, 390)], [(1640, 270), (1860, 390)],
        [(1730, 270), (2380, 390)], [(1820, 270), (2900, 390)],
        # M2 -> M11
        [(300, 610), (20, 610), (20, 1180), (1980, 1180), (1980, 1250)],
        # M3..M6 -> M8
        [(820, 610), (1050, 700), (1390, 800)],
        [(1340, 610), (1340, 800)],
        [(1860, 610), (1700, 700), (1580, 800)],
        [(2380, 610), (2140, 700), (1690, 800)],
        # Business modules -> data/storage modules
        [(700, 610), (600, 1000), (400, 1250)],
        [(950, 610), (2700, 720), (2775, 1250)],
        [(1200, 610), (1000, 1040), (500, 1250)],
        [(1450, 610), (2100, 1060), (2000, 1250)],
        [(1700, 610), (900, 1110), (600, 1250)],
        [(1750, 610), (1400, 1110), (1200, 1250)],
        [(2200, 610), (1100, 1140), (700, 1250)],
        [(2250, 610), (1500, 1130), (1300, 1250)],
        [(2750, 610), (1800, 1040), (650, 1250)],
        [(2920, 610), (2000, 1080), (1400, 1250)],
        # M8 -> M9
        [(1400, 990), (900, 1120), (500, 1250)],
    ]
    for points in edges:
        draw_arrow(draw, points)

    # Module boxes: exactly M1-M12, with names unchanged.
    module(draw, 1300, 90, 600, 180, "M1", "Web接入模块")
    for x, mid, name in [
        (50, "M2", "用户与权限模块"), (570, "M3", "报修申请模块"),
        (1090, "M4", "审核派单模块"), (1610, "M5", "维修处理模块"),
        (2130, "M6", "验收返修与评价模块"), (2650, "M7", "查询统计模块"),
    ]:
        module(draw, x, 390, 500, 220, mid, name)
    module(draw, 1300, 800, 600, 190, "M8", "工单状态流转模块")
    for x, mid, name in [
        (80, "M9", "工单数据访问模块"), (870, "M10", "维修验收数据访问模块"),
        (1660, "M11", "用户权限数据访问模块"), (2450, "M12", "文件存储模块"),
    ]:
        module(draw, x, 1250, 650, 230, mid, name)

    centered(draw, (0, 1560, W, 1615), "实线箭头表示模块之间的直接调用关系", font(22), MUTED)
    image.resize((W, H), Image.Resampling.LANCZOS).save(OUT / "图3-3_模块耦合图.png", dpi=(300, 300))


def make_svg():
    # Keep a vector companion for editing and lossless report insertion.
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs><marker id="a" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto"><path d="M0,0 L12,6 L0,12 Z" fill="{INK}"/></marker>
<style>text{{font-family:"PingFang SC","Heiti SC","Microsoft YaHei",sans-serif;fill:{TEXT}}}.title{{font-size:48px;font-weight:700}}.layer{{font-size:24px;font-weight:600;fill:{MUTED}}}.id{{font-size:28px;font-weight:700;fill:{ID}}}.name{{font-size:28px;font-weight:700}}.edge{{fill:none;stroke:{INK};stroke-width:3;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#a)}}.box{{fill:#fff;stroke:{INK};stroke-width:3}}.head{{fill:{HEADER}}}</style></defs>
<rect width="{W}" height="{H}" fill="#fff"/><text x="1600" y="70" text-anchor="middle" class="title">校园报修工单管理系统模块耦合图</text>
<text x="40" y="135" class="layer">接入层</text><text x="40" y="395" class="layer">业务模块</text><text x="40" y="805" class="layer">公共业务层</text><text x="40" y="1255" class="layer">底层数据访问层</text>
'''
    edges = [
        [(1370,270),(300,390)],[(1460,270),(820,390)],[(1550,270),(1340,390)],[(1640,270),(1860,390)],[(1730,270),(2380,390)],[(1820,270),(2900,390)],
        [(300,610),(20,610),(20,1180),(1980,1180),(1980,1250)],[(820,610),(1050,700),(1390,800)],[(1340,610),(1340,800)],[(1860,610),(1700,700),(1580,800)],[(2380,610),(2140,700),(1690,800)],
        [(700,610),(600,1000),(400,1250)],[(950,610),(2700,720),(2775,1250)],[(1200,610),(1000,1040),(500,1250)],[(1450,610),(2100,1060),(2000,1250)],[(1700,610),(900,1110),(600,1250)],[(1750,610),(1400,1110),(1200,1250)],[(2200,610),(1100,1140),(700,1250)],[(2250,610),(1500,1130),(1300,1250)],[(2750,610),(1800,1040),(650,1250)],[(2920,610),(2000,1080),(1400,1250)],[(1400,990),(900,1120),(500,1250)],
    ]
    for p in edges:
        svg += '<path class="edge" d="M ' + ' L '.join(f'{x} {y}' for x,y in p) + '"/>\n'
    modules = [(1300,90,600,180,"M1","Web接入模块"),(50,390,500,220,"M2","用户与权限模块"),(570,390,500,220,"M3","报修申请模块"),(1090,390,500,220,"M4","审核派单模块"),(1610,390,500,220,"M5","维修处理模块"),(2130,390,500,220,"M6","验收返修与评价模块"),(2650,390,500,220,"M7","查询统计模块"),(1300,800,600,190,"M8","工单状态流转模块"),(80,1250,650,230,"M9","工单数据访问模块"),(870,1250,650,230,"M10","维修验收数据访问模块"),(1660,1250,650,230,"M11","用户权限数据访问模块"),(2450,1250,650,230,"M12","文件存储模块")]
    for x,y,w,h,mid,name in modules:
        svg += f'<rect class="box" x="{x}" y="{y}" width="{w}" height="{h}"/><rect class="head" x="{x}" y="{y}" width="{w}" height="58"/><text class="id" x="{x+24}" y="{y+39}">{mid}</text><text class="name" x="{x+95}" y="{y+39}">{name}</text>\n'
    svg += '<text x="1600" y="1595" text-anchor="middle" class="layer">实线箭头表示模块之间的直接调用关系</text>\n</svg>\n'
    (OUT / "图3-3_模块耦合图.svg").write_text(svg, encoding="utf-8")


if __name__ == "__main__":
    make_png()
    make_svg()

# 图4-1 系统类图（改进前）— 交付说明

本目录是 S4 第 1 节的唯一基线图，后续 WMC / RFC / DIT / NOC / CBO / LCOM / LK / MOOD 全部以本图为准。

## 一、产物

| 文件 | 说明 |
|---|---|
| `S4_图4-1_系统类图_改进前.puml` | 唯一可编辑源文件（PlantUML） |
| `S4_图4-1_系统类图_改进前.svg` | 矢量图（可无限放大，属性/方法清晰） |
| `S4_图4-1_系统类图_改进前.png` | 位图，白底，9105 × 4653，供插入 Word |
| `verify_classdiagram.py` | 机器自检脚本（28 项） |
| `archive/` | 历史版本，不删除 |

## 二、内容基线（已验收，冻结）

- 类/接口 **恰好 45 个**，全部成员（属性可见性+类型、方法可见性+参数+参数类型+返回类型）完整可见
- **继承 2 条**：`AbstractAuditableEntity <|-- SysUser`、`AbstractAuditableEntity <|-- RepairOrder`
- **接口实现 9 条**：9 个 Service 接口 ↔ 9 个实现类
- **领域关系 17 条**：4 聚合（`o--`）、2 组合（`*--`）、其余普通关联，多重度与角色标注齐全
- **依赖**：Controller→Service 7 条、Service 实现类→仓储/其他 Service 25 条、Repository→实体 12 条
- S3 改进保留：`QueryService` / `StatisticsService` 职责拆分，`QueryStatisticsController` 仅做统一 Web 接入

## 三、版式（2026-09 重排，仅动布局）

| 指标 | 重排前 | 重排后 |
|---|---|---|
| 画布 | 12181 × 4081 | 9105 × 4653 |
| 宽高比 | 2.98 : 1 | 1.96 : 1 |
| 等效 A4 横向整页字号 | 0.640 mm | 0.856 mm（≈1.34 倍）|
| 空白格占比 | 0.552 | 0.492 |

布局要点：

- `left to right direction`：分层由"横排带"改为**纵向列带**
  controller → service → service.impl → repository/infrastructure → domain
  这样 9 个宽类框由单行横排变为竖排，画布宽度由 12181px 降到 9105px，插入 A4 横向整页后文字明显变大。
- `skinparam nodesep 25`、`skinparam ranksep 80`：控制列内/列间距。
- 2 条 `--down--` 方向提示（`RepairOrder` 的 AcceptanceRecord、OrderStatusLog 关联）：
  **仅影响走线与标注落点**，不改变关系类型、多重度、箭头与菱形，用于避免多重度标注互叠或压到类框文字。

**注意**：字体类 `skinparam`（`classFontSize` / `classAttributeFontSize` / `ArrowFontSize`）必须写在 `skinparam dpi 180` **之后**，否则会被 dpi 缩放覆盖而失效。

## 四、复现命令

```bash
cd S4/类图/图4-1_系统类图_改进前

# 1) 渲染 SVG（headless，避免弹出窗口抢焦点）
java -Djava.awt.headless=true -DPLANTUML_LIMIT_SIZE=30000 -jar plantuml.jar \
     -charset UTF-8 -tsvg S4_图4-1_系统类图_改进前.puml

# 2) 由同一 SVG 生成白底 PNG（保证 SVG/PNG 完全一致）
rsvg-convert --background-color=white \
     -o S4_图4-1_系统类图_改进前.png S4_图4-1_系统类图_改进前.svg

# 3) 机器自检（纯 Python，28 项）
python3 verify_classdiagram.py --dir . --repo ../../..
```

## 五、自检覆盖（28 项，全部通过）

- 结构：45 个类、9 接口、9 实现、6 Controller、12 张表对应实体、无重复、无第 46 个类、无边界外内容
- 关系：2 继承、9 实现、72 条关系总数、装饰语义指纹（空心菱形 15 / 实心 46）与验收基线一致
- 版式：类框零重叠、关系线零穿越类框、多重度/角色标注零互叠且零压框、宽高比 1.6~2.0、PNG 宽度 ≥5000 且与 SVG 尺寸一致
- 冻结：与 `archive/v1_*` 对比，45 个类体逐字节一致、声明顺序一致、72 条关系语义一致；未改动任何 Word/docx 与 S0～S3

## 六、archive 内容

| 文件 | 说明 |
|---|---|
| `prev_*.puml.broken-relations` / `prev_*.png` / `prev_*.svg` / `prev_generate_diagram.py` | 最初半成品：关系段丢失右端类名、PNG/SVG 由 PIL 手绘且连线穿框 |
| `v1_*` | 内容已验收、版式为 12181×4081 的版本（本次被版式重排替换） |

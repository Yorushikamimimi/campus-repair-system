# S5 Git 过程度量：改进阶段

## 1. 统计范围

- 基线 commit：`80a524e75e415f8762e3d50cc64cee8fe8edec2e`
- 改进阶段终点：`d6899c297da083ffcf2eb2bcb44d31e5c29ee518`
- 当前分支：`feature/s5-metrics`
- 改进提交数：5
- 统计范围：`80a524e75e415f8762e3d50cc64cee8fe8edec2e..HEAD`
- 说明：Git author 反映实际仓库维护者，不直接等同于四名成员工作量。

## 2. A. Git 真实 author 口径

### 2.1 改进阶段

| 真实 author | commit count | added | deleted | net | first date | last date | span |
|---|---:|---:|---:|---:|---|---|---:|
| `yang <yangmingtian377@gmail.com>` | 5 | 69 | 18 | 51 | 2026-09-15 11:15:24 +08:00 | 2026-09-15 11:18:12 +08:00 | 0.12 天（同日，约 2 分 48 秒） |

说明：该表来自 `git log --numstat -- 80a524e..HEAD`，只反映 Git 真实 author，不伪造成四名成员。

### 2.2 全仓累计参考

| 真实 author | commit count | added | deleted | net | first date | last date | span |
|---|---:|---:|---:|---:|---|---|---:|
| `yang <yangmingtian377@gmail.com>` | 9 | 17,342 | 47 | 17,295 | 2026-09-13 22:21:17 +08:00 | 2026-09-15 11:18:12 +08:00 | 1.54 天 |

说明：全仓累计表包含初始基线、度量文档提交和本轮五条代码改进提交。Git numstat 对二进制文件不产生可计行数，本表按 Git 可计文本行统计。

## 3. B. 责任模块归属口径

该表为责任模块归属统计，不是 Git author 统计。责任归属根据 commit message 中的 R1、R2、R3、R4 标识划分。

| 责任人 | 改进 commit 数 | 新增行 | 删除行 | 净增行 | 对应 commit |
|---|---:|---:|---:|---:|---|
| R1 邓鸿翔 | 2 | 20 | 1 | 19 | `7cb9cb7`、`d6899c2` |
| R2 陈语 | 1 | 8 | 1 | 7 | `eb69f63` |
| R3 羊鸣天 | 1 | 19 | 7 | 12 | `b89f964` |
| R4 赵俊超 | 1 | 22 | 9 | 13 | `d747a32` |
| 合计 | 5 | 69 | 18 | 51 |  |

## 4. 按 commit 的过程数据

| 编号 | 责任归属 | commit hash | commit message | 新增行 | 删除行 | 净增行 | 修改文件 |
|---|---|---|---:|---:|---:|---|
| C1 | R1 邓鸿翔 | `7cb9cb7accc9845793b8cb11d6c0d7070aff29ac` | `refactor(R1): simplify current user validation` | 4 | 1 | 3 | `backend/src/main/java/com/campusrepair/security/CurrentUser.java` |
| C2 | R2 陈语 | `eb69f63567506c6a4880677d61bf6fbf22935ea6` | `refactor(R2): simplify repair status normalization` | 8 | 1 | 7 | `backend/src/main/java/com/campusrepair/service/impl/QueryServiceImpl.java` |
| C3 | R3 羊鸣天 | `b89f964d77d96671b9558f354b7e2d703b94712f` | `refactor(R3): split maintenance record validation` | 19 | 7 | 12 | `backend/src/main/java/com/campusrepair/service/impl/MaintenanceServiceImpl.java` |
| C4 | R4 赵俊超 | `d747a32adf2d01ab17be712e7d95bcbfa59ac299` | `refactor(R4): simplify repair duration calculation` | 22 | 9 | 13 | `backend/src/main/java/com/campusrepair/service/impl/StatisticsServiceImpl.java` |
| C5 | R1 邓鸿翔 | `d6899c297da083ffcf2eb2bcb44d31e5c29ee518` | `refactor(R1): split file validation checks` | 16 | 0 | 16 | `backend/src/main/java/com/campusrepair/storage/LocalFileStorageServiceImpl.java` |

## 5. 可直接填入报告的文字

本组代码仓库采用统一维护方式，由主笔负责版本合并与Git提交，因此Git author主要反映仓库维护过程，不能直接等同于成员工作量。代码改进仍按照前期模块分工落实责任人，并通过commit message中的R1～R4标识对应成员负责模块。各项改动均保留独立commit，可根据commit号追溯实际代码变化。

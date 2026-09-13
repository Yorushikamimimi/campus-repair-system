# Phase 1 集成复验报告

## 1 环境

- JDK: `java 21.0.2`
- Maven: `3.9.14`（运行时 Java = 21.0.2）
- Node: `v24.14.1`
- MySQL: `8.0.46`

## 2 Git状态

- 当前目录：`/Users/yang/Workspace/school/软件度量大作业`
- `git rev-parse --show-toplevel`：失败（`fatal: not a git repository`）
- `git ls-remote https://github.com/Yorushikamimimimi/campus-repair-system`：返回 `Repository not found`。
- 说明：本地工作目录与官方仓库均无法形成可追踪状态，版本控制接入未完成。

## 3 数据库真实验证

- `SHOW TABLES`：12
  - `acceptance_record`
  - `dispatch_record`
  - `evaluation_record`
  - `order_status_log`
  - `repair_image`
  - `repair_location`
  - `repair_order`
  - `repair_record`
  - `repair_type`
  - `sys_role`
  - `sys_user`
  - `sys_user_role`
- `sys_user` 表无 `role_id` 字段（`role_id_exists=0`）
- `seed.sql` 仅维护基础字典（`sys_role`、`repair_type`、`repair_location`）；`phase1_admin`、`phase1_reporter`、`phase1_maintainer`、`phase1_reporter_2` 为本地复验阶段补充插入账号，密码保存为 BCrypt hash。
- 目标12表已满足。

## 4 本地测试账号

- `phase1_reporter`
- `phase1_admin`
- `phase1_maintainer`
- `phase1_reporter_2`（越权复验新增）

## 5 后端构建

- `mvn clean test`：`Tests run=24`，`Failures=0`，`Errors=0`，`Skipped=0`

测试清单（真实 surefire 统计）：

- `com.campusrepair.controller.RepairOrderControllerTest`：2
- `com.campusrepair.controller.UserControllerTest`：3
- `com.campusrepair.service.OrderStateServiceImplTest`：2
- `com.campusrepair.service.QueryServiceImplTest`：2
- `com.campusrepair.service.RepairOrderServiceImplTest`：8
- `com.campusrepair.service.UserPermissionServiceImplTest`：5
- `com.campusrepair.storage.LocalFileStorageServiceImplTest`：2

合计：24

## 6 后端MySQL启动

- `GET /api/health`：HTTP 200, `code=200`
- MySQL 连接与写入：正常（通过应用 DB 用户与查询验证）

## 7 HTTP Smoke

| 测试项 | 实际HTTP状态 | 结果 | 备注 |
|---|---:|---|---|
| health | 200 | PASS | `code=200` |
| reporter login | 200 | PASS | JWT 正常返回（仅记录 `token returned = YES`） |
| types | 200 | PASS | 至少 2 条 |
| locations | 200 | PASS | 至少 2 条 |
| create order | 200 | PASS | 成功创建 orderId |
| my orders | 200 | PASS | 可见本人新建工单 |
| detail | 200 | PASS | reporterId/status/title/description 与DB一致 |
| update | 200 | PASS | 返回 `code=200`，成功更新数据库 `title/description` |
| anonymous reject | 401 | PASS | 匿名请求被拦截 |
| cross-user reject | 403 | PASS | Reporter2 访问其他用户工单被拒 |
| admin login | 200 | PASS | 登录成功 |
| ADMIN /api/users | 200 | PASS | ADMIN 可访问 |
| REPORTER /api/users | 403 | PASS | `REPORTER` 拒绝访问用户管理列表 |
| anonymous /api/users | 401 | PASS | 未登录拒绝访问用户管理列表 |
| maintainer login | 200 | PASS | 登录成功 |
| image upload | 200 | PASS | 文件落盘且 DB 有记录 |

## 8 数据库落库证据

### 8.1 `repair_order`

示例（latest）

- `order_id`: 14
- `reporter_id`: 7
- `type_id`: 1
- `location_id`: 1
- `title`: `smoke with image`
- `status`: `SUBMITTED`
- `submit_time`: `2026-09-13 16:23:30`

### 8.2 `order_status_log`

示例（latest）

- `log_id`: 4
- `order_id`: 12
- `operator_id`: 7
- `old_status`: `NULL`
- `new_status`: `SUBMITTED`
- `change_reason`: `提交报修`

### 8.3 `repair_image`

- `order_id`: 14
- `file_url`: `/uploads/repair/14/cb060f88-ebcd-4db6-a44d-185786e4389c.png`
- `file_name`: `f651b3e0-2927-45dd-a173-2868ead4ed4f.png`
- `upload`：本地存在文件，文件名为随机 UUID 生成

## 9 前端构建

- `npm run build`：PASS

## 10 遗留问题

- `PUT /api/repair-orders/{orderId}` 与 `REPORTER /api/users` 已修复，当前复验 PASS
- `git ls-remote` 在本环境返回 `Repository not found`，远端仓库接入与身份校验与接入无法确认

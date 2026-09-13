# Phase 2 审核派单维修开发报告

## 1 开发范围

- 实现并打通“管理员审核 → 派单 → 维修人员接单/处理 → 维修记录持久化”的完整主链路
- 保持 Phase 1 流程与功能不回退（创建、列表、详情、状态基础能力）
- 在不新增数据库表的前提下复用既有 `dispatch_record`、`repair_record`、`order_status_log`
- 补齐控制器、服务、仓储边界与接口权限，并补充单元测试
- 进行真实 MySQL + 真实 HTTP 的链路验收
- 前端补齐管理员审核派单与维修人员任务页面（不包含验收、评价、统计）

## 2 新增/完善类

后端新增/完善的关键类：

- `com.campusrepair.service.DispatchService`
- `com.campusrepair.service.impl.DispatchServiceImpl`
- `com.campusrepair.service.MaintenanceService`
- `com.campusrepair.service.impl.MaintenanceServiceImpl`
- `com.campusrepair.controller.DispatchController`
- `com.campusrepair.controller.MaintenanceController`
- `backend/src/test/java/com/campusrepair/service/DispatchServiceImplTest.java`
- `backend/src/test/java/com/campusrepair/service/MaintenanceServiceImplTest.java`
- `backend/src/test/java/com/campusrepair/controller/DispatchControllerTest.java`
- `backend/src/test/java/com/campusrepair/controller/MaintenanceControllerTest.java`

前端补齐页面与接口：

- `frontend/src/views/AdminDispatch.vue`
- `frontend/src/views/MaintenanceTasks.vue`
- `frontend/src/api/repair.js`（新增管理员审核/派单与维修人员任务/处理接口封装）
- `frontend/src/router/index.js`（新增管理员与维修角色页面路由）

## 3 API清单

### 管理员

- `GET /api/admin/repair-orders/pending`  获取待审核 + 待处理工单
- `POST /api/admin/repair-orders/{orderId}/audit`  审核通过 / 退回
- `POST /api/admin/repair-orders/{orderId}/dispatch`  派单
- `GET /api/admin/repair-orders/{orderId}/history`  获取维修历史（仅管理员）

### 维修人员

- `GET /api/maintenance/tasks`  我的维修任务
- `POST /api/maintenance/tasks/{orderId}/accept`  接单
- `POST /api/maintenance/tasks/{orderId}/process`  提交维修过程/结果
- `POST /api/maintenance/tasks/{orderId}/continue`  继续维修
- `GET /api/maintenance/tasks/{orderId}/history`  维修历史

权限约束：

- 管理员接口 `@PreAuthorize("hasRole('ADMIN')")`
- 维修接口 `@PreAuthorize("hasRole('MAINTAINER')")`
- 匿名访问统一返回 401；角色越权返回 403。

## 4 管理员审核流程

`DispatchService.auditOrder(adminId, orderId, approved, comment)` 执行逻辑：

1. 读取工单，必须存在
2. 校验当前状态为 `SUBMITTED`
3. `approved=true`：进入 `OrderStateService.approve(orderId, adminId)`，变更为 `PENDING_PROCESS`
4. `approved=false`：不新增新状态，保留 `SUBMITTED`，追加 `order_status_log`（`old_status='SUBMITTED'`, `new_status='SUBMITTED'`, `change_reason` = 审核意见，缺省值 `退回补充`）
5. 无法审核时返回业务冲突/未授权

## 5 派单流程

`DispatchService.dispatchOrder(adminId, orderId, maintainerId, note)` 执行逻辑：

1. 校验工单存在且状态为 `PENDING_PROCESS`
2. 校验管理员 ID 非空
3. 校验 `maintainerId` 对应用户存在、启用、具备 `MAINTAINER` 角色
4. 防止同单存在进行中派单（`ASSIGNED/TAKEN`）
5. 新建 `dispatch_record`：`order_id/admin_id/maintainer_id/dispatch_time/dispatch_status 设置为 ASSIGNED`，`dispatch_note` 可空
6. 调用 `OrderStateService.dispatch` 记录状态日志（不改工单主状态值，保持 `PENDING_PROCESS`）
7. 重复派单冲突返回 409

## 6 维修任务与权限

- `MaintenanceService.listTasks(maintainerId)`：先查 `dispatch_record` 最新记录，再按 `latestByOrder` 去重，聚合到 `MaintenanceTaskItem`
- 可见性仅允许 `PENDING_PROCESS / PROCESSING / REWORK`
- 排序按 `dispatchTime` 倒序
- 通过 `listTasks` 和接单/处理接口的 `@PreAuthorize` + `validateAssignment` 双重确保：维修人员只能操作自己被派工单
- `admin` 访问维修接口返回 403；`reporter` 访问维修接口返回 403；匿名返回 401

## 7 processRepairOrder实现

`MaintenanceServiceImpl.processRepairOrder(maintainerId, orderId, request)` 采用 S4 冻结流程拆分：

1. 校验派单归属与派单状态（`ASSIGNED/TAKEN`）
2. 校验工单当前状态（必须 `PROCESSING`）
3. 校验完成标记不为空
4. 校验维修记录输入完整性（完成与未完成分支）
5. 计算下一状态：完成→`PENDING_ACCEPTANCE`，未完成→`PROCESSING`
6. 事务内保存维修记录：
   - 完成：必须 `repairResult`，写 `endTime`
   - 未完成：必须 `unfinishedReason`，`repair_result` 空
7. 完成时更新工单到 `PENDING_ACCEPTANCE` 并将派单状态置 `COMPLETED`
8. 持久化失败统一包装为 `INTERNAL_ERROR`

S4 的幂等与分支控制保持，未采用 if-else 直接拼贴式实现；实际重算后，`MaintenanceServiceImpl.processRepairOrder` 的控制流图为：`V(G)=3`（单方法口径、排除子方法判定）。

## 8 数据库落库

已实际落库表：

- `dispatch_record`
- `repair_record`
- `order_status_log`

真实链路核验（以下为样本）：

- 完成链路工单（`order_id=23`）：
  - `repair_order.status = PENDING_ACCEPTANCE`
  - `dispatch_record.dispatch_status = COMPLETED`
  - `repair_record` 累计 1 条，`repair_result='已修复'`
  - 最新状态日志链路：
    - `NULL -> SUBMITTED`，原因 `提交报修`
    - `SUBMITTED -> PENDING_PROCESS`，原因 `审核通过`
    - `PENDING_PROCESS -> PENDING_PROCESS`，原因 `完成派单`
    - `PENDING_PROCESS -> PROCESSING`，原因 `开始维修`
    - `PROCESSING -> PENDING_ACCEPTANCE`，原因 `维修完成，等待验收`
  - 未出现 `PENDING_PROCESS -> PENDING_ACCEPTANCE` 的直接跳转。
- 未完成链路工单（`order_id=24`）：
  - `repair_order.status = PROCESSING`
  - `dispatch_record.dispatch_status = TAKEN`
  - `repair_record` 累计 1 条，`unfinished_reason='零件未到'`，`repair_result` 为空
  - 状态日志覆盖 `提交报修`、`审核通过`、`完成派单`、`开始维修`

数据库 schema 未新增表，保持 12 表设计不变。

## 9 前端页面

### 管理员

- `AdminDispatch.vue`：
  - 待处理列表（待审核 + 待处理）
  - 审核通过 / 退回补充
  - 派单到 `maintainerId` + 派单说明

### 维修人员

- `MaintenanceTasks.vue`：
  - 我的任务列表（仅当前绑定任务）
  - 接单
  - 提交维修结果（完成/未完成）
  - 未完成必须填写未完成原因
  - 查看维修历史

### 接口对接

- `frontend/src/api/repair.js` 新增 admin 与 maintenance 统一入口
- `router/index.js` 增加 `/admin/dispatch` 与 `/maintenance/tasks`

## 10 测试

Phase 2 新增/更新测试（Mock/Mvc）：

- `DispatchServiceImplTest`
  - 审核通过
  - 非待审核不能审核
  - 正常派单
  - 非维修人员拒绝派单
  - 重复派单冲突
  - 待处理列表去重

- `MaintenanceServiceImplTest`
  - 只能看到自己的任务
  - 他人任务拒绝
  - 正常接单
  - 重复接单幂等（不重复更新）
  - 完成维修进入待验收
  - 未完成保持处理中
  - 持久化异常回滚为 `INTERNAL_ERROR`
  - `continue` 场景
  - 维修历史查询

- `DispatchControllerTest`（角色鉴权与接口返回）
- `MaintenanceControllerTest`（角色鉴权与接口返回）

## 11 构建结果

### 后端

- 命令：`cd backend && mvn clean test`
- 结果：PASS
- `Tests run = 88`
- `Failures = 0`
- `Errors = 0`
- `Skipped = 0`

### 前端

- 命令：`cd frontend && npm run build`
- 结果：PASS
- 构建产物正常产出；含大 chunk warning，但构建成功

## 12 真实端到端Smoke

使用端口：`http://localhost:8085`

- 完成链路：
  - Reporter 创建 `order_id=23`
  - Admin 审核通过
  - Admin 派单到 maintainer 9
  - Maintainer 任务列表显示该订单
  - Maintainer 接单
  - Maintainer 提交完成（`completed=true`）
  - 结果：`repair_order.status=PENDING_ACCEPTANCE`

- 未完成链路：
  - Reporter 创建 `order_id=24`
  - Admin 审核通过
  - Admin 派单到 maintainer 9
  - Maintainer 接单
  - Maintainer 提交未完成（`completed=false`, `unfinishedReason='零件未到'`）
  - 结果：`repair_order.status=PROCESSING`

- 实体核验 SQL 已确认 `dispatch_record`、`repair_record`、`order_status_log` 均有记录

## 13 与S4设计偏差

- 无新增“待补充/退回”状态；退回保留 `SUBMITTED`
- 验收、返修、评价、统计接口与基础实现已在当前代码中存在（当前任务不新增阶段性逻辑，仅更新阶段度量口径）
- 前端 `OrderDetail.vue` 中尚有说明文本提示验收/维修在后续阶段实现（不影响本阶段接口与后端链路）

## 14 未完成事项

- 继续修正 `continue` 仍保持与 S4 约定：仅 `REWORK` 可重开（当前接口已就位）
- 阶段性收口：不修改数据库结构、不修改 S4 已冻结设计文档、不给 `Phase 3` 功能打补丁

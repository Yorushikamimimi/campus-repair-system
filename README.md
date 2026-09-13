# 校园报修工单管理系统

## 项目简介
校园报修工单管理系统的基础工程仓库，课程资料 S0-S9 与正式代码共存。

## 技术栈
前端 Vue 3、Vite、Element Plus、Axios、Pinia、Vue Router；后端 Java 17、Spring Boot 3.3、Spring Security、MyBatis-Plus、JWT、Maven；数据库 MySQL 8.0。

## 目录结构
`backend/` 后端工程；`frontend/` 前端工程；`database/` DDL 与种子数据；`S0/` 至 `S9/` 课程资料。

## 环境要求
JDK 17、Node.js 20 LTS、Maven 3.9.x、MySQL 8.0，以及 Chrome/Edge。

## 启动方式
后端：`cd backend && mvn spring-boot:run`。前端：`cd frontend && npm install && npm run dev`。

## 数据库初始化
在 MySQL 8.0 中依次执行 `database/schema.sql` 和 `database/seed.sql`。当前种子数据不含账号密码。

## 环境变量
后端读取 `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`、`JWT_SECRET`；前端读取 `VITE_API_BASE_URL`（默认 `http://localhost:8080/api`）。

## 当前开发状态
已完成项目基础工程初始化，S4详细设计终版冻结后进入业务功能开发。

## Git 分支约定
功能开发使用 `feature/*`，集成使用 `dev`，稳定版本合并到 `main`。

## Git 分支

- `main`：Phase 1～3 已验收的稳定基线
- `dev`：日常集成开发
- `feature/*`：具体功能或度量任务

首次基线已接入正式远端；后续功能和 S5 度量改动应基于 `dev` 或对应 `feature/*` 分支进行。

USE campus_repair;
INSERT INTO sys_role (role_name, role_code, remark) VALUES ('报修人','REPORTER','提交和验收报修'),('管理员','ADMIN','管理工单和派单'),('维修人员','MAINTAINER','处理维修工单');
INSERT INTO repair_type (type_name, type_code, description) VALUES ('水电维修','WATER_ELECTRIC','校园水电设施'),('设施维修','FACILITY','校园公共设施');
INSERT INTO repair_location (building_name, area_name, room_no, description) VALUES ('教学楼','A区','101','示例地点'),('学生宿舍','1号楼','201','示例地点');
-- 不写入初始账号；管理员密码应通过后续初始化流程生成 BCrypt 哈希。

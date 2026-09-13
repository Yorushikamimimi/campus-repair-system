# CK 计算证据

本脚本按系统源码类型计算 CBO；JDK、Spring 和第三方类型不作为系统类对象。继承边不计 CBO，接口实现/扩展关系计入。

## AcceptanceController
- WMC 方法：acceptRepair, requestRework, getAcceptanceHistory, evaluate, getEvaluation, hasAdminAuthority
- RFC 外部调用名：acceptRepairResult, anyMatch, equals, evaluateService, getAuthorities, getAuthority, getComment, getPassed, getReturnReason, getScore, requireId, stream, success
- CBO 系统类型：AcceptanceRecord, AcceptanceRequest, AcceptanceService, ApiResponse, CurrentUser, EvaluationRecord, EvaluationRequest, EvaluationService, ReworkRequest
- LCOM：P=9, Q=6, max(P-Q,0)=3

## RepairOrder
- WMC 方法：getCreateTime, setCreateTime, touchUpdate, updateContent, isOwnedBy
- RFC 外部调用名：equals, isBlank, now, trim
- CBO 系统类型：无
- LCOM：P=9, Q=1, max(P-Q,0)=8

## JwtTokenProvider
- WMC 方法：generateToken, generateToken, validateAndGetSubject, parseUserId, parseRoles, validateToken, parseClaims
- RFC 外部调用名：build, builder, claim, compact, emptyList, expiration, get, getPayload, getSubject, getTime, issuedAt, map, of, parseSignedClaims, parser, signWith, stream, subject, toList, valueOf, verifyWith
- CBO 系统类型：无
- LCOM：P=20, Q=1, max(P-Q,0)=19

## RepairOrderService
- WMC 方法：createOrder, updateOrder, getMyOrders, getProgress
- RFC 外部调用名：无
- CBO 系统类型：RepairOrder
- LCOM：P=6, Q=0, max(P-Q,0)=6

## AcceptanceServiceImpl
- WMC 方法：acceptRepairResult, requestRework, getAcceptanceHistory, resolveOrderOwnership, loadOrder, requirePendingAcceptance, ensureReturnReason
- RFC 外部调用名：accept, code, equals, findById, findByOrder, getOrderId, getStatus, isBlank, isOwnedBy, pass, reject, saveAcceptance, setOrderId, setReporterId
- CBO 系统类型：AcceptanceRecord, AcceptanceRepository, AcceptanceService, BusinessException, ErrorCode, OrderStateService, OrderStatus, RepairOrder, RepairOrderRepository
- LCOM：P=20, Q=1, max(P-Q,0)=19

## DispatchServiceImpl
- WMC 方法：auditOrder, dispatchOrder, listPendingOrders, requireOrder, requireAdmin, validateMaintainer, validateNoActiveDispatch, appendLog, findPendingByStatus, isActiveDispatchStatus, normalizeAuditComment, normalizeNote
- RFC 外部调用名：add, anyMatch, approve, changeStatus, code, contains, dispatch, equals, findByFilters, findById, findLatestByOrder, findRolesByUserId, findUserById, getDispatchStatus, getOrderId, getStatus, isBlank, isEnabled, map, now, of, save, setAdminId, setChangeReason, setChangeTime, setDispatchNote, setDispatchTime, setMaintainerId, setNewStatus, setOldStatus, setOperatorId, setOrderId, stream, trim
- CBO 系统类型：BusinessException, DispatchRecord, DispatchRepository, DispatchService, ErrorCode, OrderStateService, OrderStatus, OrderStatusLog, OrderStatusRepository, RepairOrder, RepairOrderRepository, SysRole, SysUser, UserPermissionRepository
- LCOM：P=63, Q=3, max(P-Q,0)=60

## EvaluationServiceImpl
- WMC 方法：evaluateService, getEvaluation, requireOwnedOrder, requireOrder, requireCompleted, validateScore
- RFC 外部调用名：equals, existsByOrder, findById, findByOrder, get, getStatus, isEmpty, isOwnedBy, saveEvaluation, setComment, setOrderId, setReporterId, setScore, size
- CBO 系统类型：BusinessException, ErrorCode, EvaluationRecord, EvaluationRepository, EvaluationService, RepairOrder, RepairOrderRepository
- LCOM：P=14, Q=1, max(P-Q,0)=13

## MaintenanceServiceImpl
- WMC 方法：listTasks, acceptTask, processRepairOrder, continueRepair, getRepairHistory, validateAssignment, isActiveAssignmentStatusForFlow, validateRepairState, ensureRecordComplete, normalizeText, resolveNextState, saveResult, handlePersistenceFailure, isCurrentTaskVisible, requireOrder, requireUserId, requireCurrentState
- RFC 外部调用名：add, collect, compareTo, dispatchTime, equals, findById, findByOrder, findLatestByOrder, findTasksByMaintainer, finishRepair, fromCode, getCompleted, getDispatchId, getDispatchNote, getDispatchStatus, getDispatchTime, getLocationId, getMaintainerId, getOrderId, getProcessDesc, getRepairResult, getStatus, getTitle, getTypeId, getUnfinishedReason, isBlank, isEmpty, now, of, putIfAbsent, recordUnfinished, reopen, run, saveRepairRecord, setEndTime, setMaintainerId, setOrderId, setProcessDesc, setRepairResult, setStartTime, sorted, startRepair, stream, toList, trim, updateStatus, values
- CBO 系统类型：BusinessException, DispatchRecord, DispatchRepository, ErrorCode, MaintenanceProcessRequest, MaintenanceRepository, MaintenanceService, MaintenanceTaskItem, OrderStateService, OrderStatus, RepairOrder, RepairOrderRepository, RepairRecord
- LCOM：P=118, Q=18, max(P-Q,0)=100

## OrderStateServiceImpl
- WMC 方法：submit, approve, dispatch, startRepair, finishRepair, accept, reject, reopen, transition, requireCurrent, requireOrder, appendLog
- RFC 外部调用名：code, contains, equals, findById, findLatest, fromCode, getStatus, isBlank, now, of, setChangeReason, setChangeTime, setNewStatus, setOldStatus, setOperatorId, setOrderId, setStatus, trim, updateOrder
- CBO 系统类型：BusinessException, ErrorCode, OrderStateService, OrderStatus, OrderStatusLog, OrderStatusRepository, RepairOrder, RepairOrderRepository
- LCOM：P=61, Q=5, max(P-Q,0)=56

## QueryServiceImpl
- WMC 方法：queryOrderProgress, queryOrders, requireOrder, checkQueryPermission, normalizeStatus
- RFC 外部调用名：code, displayName, equals, filter, findById, findByReporter, fromCode, getStatus, getTypeId, isBlank, isOwnedBy, stream, toList, trim, values
- CBO 系统类型：BusinessException, ErrorCode, OrderStatus, QueryService, RepairOrder, RepairOrderRepository
- LCOM：P=9, Q=1, max(P-Q,0)=8

## RepairOrderServiceImpl
- WMC 方法：createOrder, updateOrder, getMyOrders, getProgress, validateCreate, saveImages, cleanupFiles, requireOwnedOrder, requireUserId, normalizeText
- RFC 外部调用名：add, code, delete, emptyList, equals, findById, findByReporter, getBytes, getOrderId, getOriginalFilename, getStatus, insertOrder, isBlank, isOwnedBy, length, now, save, setDescription, setFileName, setFileUrl, setLocationId, setOrderId, setReporterId, setStatus, setSubmitTime, setTitle, setTypeId, setUploadTime, submit, trim, updateContent, valueOf, warn
- CBO 系统类型：BusinessException, ErrorCode, FileStorageService, OrderStateService, OrderStatus, RepairImage, RepairImageRepository, RepairLocation, RepairLocationRepository, RepairOrder, RepairOrderRepository, RepairOrderService, RepairType, RepairTypeRepository
- LCOM：P=38, Q=7, max(P-Q,0)=31

## StatisticsServiceImpl
- WMC 方法：countByStatus, countByType, countByPeriod, averageRepairDuration
- RFC 外部调用名：add, atStartOfDay, between, code, collect, comparingByKey, counting, doubleValue, entrySet, filter, findCompletedRecords, forEach, ge, get, getEndTime, getKey, getOrDefault, getStartTime, getStatus, getTypeId, getValue, groupingBy, isAfter, isBefore, isEmpty, lambdaQuery, lt, plusDays, put, selectCount, selectList, setScale, sorted, stream, toMap, toMinutes, valueOf, values
- CBO 系统类型：BusinessException, ErrorCode, MaintenanceRepository, OrderStatus, RepairOrder, RepairOrderRepository, RepairRecord, RepairType, RepairTypeRepository, StatisticsService, TypeCountItem
- LCOM：P=3, Q=3, max(P-Q,0)=0

## LocalFileStorageServiceImpl
- WMC 方法：save, load, delete, validateFile, resolveStoredPath, extensionOf
- RFC 外部调用名：contains, createDirectories, deleteIfExists, equals, getFileName, isBlank, lastIndexOf, length, normalize, of, randomUUID, readAllBytes, resolve, startsWith, substring, toLowerCase, toString, valueOf, write
- CBO 系统类型：BusinessException, ErrorCode, FileStorageService
- LCOM：P=14, Q=1, max(P-Q,0)=13

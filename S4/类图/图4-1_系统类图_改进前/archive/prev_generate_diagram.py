from pathlib import Path
from html import escape
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).parent
PUML = OUT / 'S4_图4-1_系统类图_改进前.puml'
SVG = OUT / 'S4_图4-1_系统类图_改进前.svg'
PNG = OUT / 'S4_图4-1_系统类图_改进前.png'

classes = {}
def add(name, kind, attrs=(), methods=(), package='domain', abstract=False):
    classes[name] = dict(name=name, kind=kind, attrs=list(attrs), methods=list(methods), package=package, abstract=abstract)

add('AbstractAuditableEntity', 'class', ['# createTime: LocalDateTime', '# updateTime: LocalDateTime'], ['+ touchUpdate(): void'], 'domain', True)
add('SysUser', 'class', ['- userId: Long','- username: String','- passwordHash: String','- realName: String','- phone: String','- status: String'], ['+ changeStatus(status: String): void','+ updateProfile(realName: String, phone: String): void','+ isEnabled(): boolean'], 'domain')
add('SysRole', 'class', ['- roleId: Long','- roleName: String','- roleCode: String','- remark: String'], ['+ rename(roleName: String): void','+ changeRemark(remark: String): void'], 'domain')
add('SysUserRole', 'class', ['- id: Long','- userId: Long','- roleId: Long','- assignTime: LocalDateTime'], ['+ matches(userId: Long, roleId: Long): boolean'], 'domain')
add('RepairOrder', 'class', ['- orderId: Long','- reporterId: Long','- typeId: Long','- locationId: Long','- title: String','- description: String','- status: String'], ['+ changeStatus(status: String): void','+ updateContent(title: String, description: String): void','+ isOwnedBy(userId: Long): boolean','+ canOperate(): boolean'], 'domain')
add('RepairType', 'class', ['- typeId: Long','- typeName: String','- typeCode: String','- description: String','- status: String'], ['+ enable(): void','+ disable(): void'], 'domain')
add('RepairLocation', 'class', ['- locationId: Long','- buildingName: String','- areaName: String','- roomNo: String','- description: String','- status: String'], ['+ enable(): void','+ disable(): void'], 'domain')
add('RepairImage', 'class', ['- imageId: Long','- orderId: Long','- fileUrl: String','- fileName: String','- uploadTime: LocalDateTime'], ['+ getExtension(): String'], 'domain')
add('DispatchRecord', 'class', ['- dispatchId: Long','- orderId: Long','- adminId: Long','- maintainerId: Long','- dispatchTime: LocalDateTime','- dispatchNote: String','- dispatchStatus: String'], ['+ changeStatus(status: String): void'], 'domain')
add('RepairRecord', 'class', ['- repairId: Long','- orderId: Long','- maintainerId: Long','- startTime: LocalDateTime','- endTime: LocalDateTime','- processDesc: String','- repairResult: String','- unfinishedReason: String'], ['+ finish(result: String): void','+ recordUnfinished(reason: String): void'], 'domain')
add('AcceptanceRecord', 'class', ['- acceptanceId: Long','- orderId: Long','- reporterId: Long','- acceptResult: String','- returnReason: String','- acceptTime: LocalDateTime'], ['+ pass(): void','+ reject(reason: String): void'], 'domain')
add('EvaluationRecord', 'class', ['- evaluationId: Long','- orderId: Long','- reporterId: Long','- score: Integer','- comment: String','- evaluateTime: LocalDateTime'], ['+ validateScore(): boolean'], 'domain')
add('OrderStatusLog', 'class', ['- logId: Long','- orderId: Long','- operatorId: Long','- oldStatus: String','- newStatus: String','- changeReason: String','- changeTime: LocalDateTime'], ['+ describeTransition(): String'], 'domain')

add('UserController','class',['- userPermissionService: UserPermissionService'],['+ login(username: String, password: String): String','+ listUsers(roleCode: String, status: String): List<SysUser>','+ updateUserStatus(userId: Long, status: String): void','+ assignRole(userId: Long, roleId: Long): void'],'web')
add('RepairOrderController','class',['- repairOrderService: RepairOrderService'],['+ createOrder(reporterId: Long, typeId: Long, locationId: Long, title: String, description: String, files: List<String>): RepairOrder','+ updateOrder(reporterId: Long, orderId: Long, title: String, description: String): RepairOrder','+ getMyOrders(reporterId: Long): List<RepairOrder>','+ getProgress(reporterId: Long, orderId: Long): RepairOrder'],'web')
add('DispatchController','class',['- dispatchService: DispatchService'],['+ auditOrder(adminId: Long, orderId: Long, approved: boolean, comment: String): void','+ dispatchOrder(adminId: Long, orderId: Long, maintainerId: Long, note: String): DispatchRecord','+ listPendingOrders(): List<RepairOrder>'],'web')
add('MaintenanceController','class',['- maintenanceService: MaintenanceService'],['+ getTasks(maintainerId: Long): List<RepairOrder>','+ acceptTask(maintainerId: Long, orderId: Long): void','+ processOrder(maintainerId: Long, orderId: Long, processDesc: String, repairResult: String, unfinishedReason: String): RepairRecord','+ continueRepair(maintainerId: Long, orderId: Long): RepairRecord'],'web')
add('AcceptanceController','class',['- acceptanceService: AcceptanceService'],['+ acceptResult(reporterId: Long, orderId: Long, passed: boolean, returnReason: String): AcceptanceRecord','+ requestRework(reporterId: Long, orderId: Long, reason: String): AcceptanceRecord','+ evaluateService(reporterId: Long, orderId: Long, score: Integer, comment: String): EvaluationRecord'],'web')
add('QueryStatisticsController','class',['- queryService: QueryService','- statisticsService: StatisticsService'],['+ queryProgress(userId: Long, orderId: Long): RepairOrder','+ queryOrders(userId: Long, status: String, typeId: Long): List<RepairOrder>','+ statisticsSummary(): Map<String, Object>'],'web')

add('UserPermissionService','interface',[],['+ login(username: String, password: String): String','+ listUsers(roleCode: String, status: String): List<SysUser>','+ updateUserStatus(userId: Long, status: String): void','+ assignRole(userId: Long, roleId: Long): void','+ getUserRoles(userId: Long): List<SysRole>'],'service')
add('RepairOrderService','interface',[],['+ createOrder(reporterId: Long, typeId: Long, locationId: Long, title: String, description: String, files: List<String>): RepairOrder','+ updateOrder(reporterId: Long, orderId: Long, title: String, description: String): RepairOrder','+ getMyOrders(reporterId: Long): List<RepairOrder>','+ getProgress(reporterId: Long, orderId: Long): RepairOrder'],'service')
add('DispatchService','interface',[],['+ auditOrder(adminId: Long, orderId: Long, approved: boolean, comment: String): void','+ dispatchOrder(adminId: Long, orderId: Long, maintainerId: Long, note: String): DispatchRecord','+ listPendingOrders(): List<RepairOrder>'],'service')
add('MaintenanceService','interface',[],['+ listTasks(maintainerId: Long): List<RepairOrder>','+ acceptTask(maintainerId: Long, orderId: Long): void','+ processRepairOrder(maintainerId: Long, orderId: Long, processDesc: String, repairResult: String, unfinishedReason: String): RepairRecord','+ continueRepair(maintainerId: Long, orderId: Long): RepairRecord','+ getRepairHistory(orderId: Long): List<RepairRecord>'],'service')
add('AcceptanceService','interface',[],['+ acceptRepairResult(reporterId: Long, orderId: Long, passed: boolean, returnReason: String): AcceptanceRecord','+ requestRework(reporterId: Long, orderId: Long, reason: String): AcceptanceRecord','+ evaluateService(reporterId: Long, orderId: Long, score: Integer, comment: String): EvaluationRecord','+ getAcceptanceHistory(orderId: Long): List<AcceptanceRecord>'],'service')
add('QueryService','interface',[],['+ queryOrderProgress(userId: Long, orderId: Long): RepairOrder','+ queryOrders(userId: Long, status: String, typeId: Long): List<RepairOrder>','+ queryRepairHistory(orderId: Long): List<RepairRecord>'],'service')
add('StatisticsService','interface',[],['+ countByStatus(): Map<String, Long>','+ countByType(): Map<String, Long>','+ countByPeriod(start: LocalDate, end: LocalDate): Map<String, Long>','+ averageRepairDuration(): Double'],'service')
add('OrderStateService','interface',[],['+ submit(orderId: Long, operatorId: Long): void','+ approve(orderId: Long, operatorId: Long): void','+ dispatch(orderId: Long, operatorId: Long): void','+ startRepair(orderId: Long, operatorId: Long): void','+ finishRepair(orderId: Long, operatorId: Long): void','+ accept(orderId: Long, operatorId: Long): void','+ reject(orderId: Long, operatorId: Long, reason: String): void','+ reopen(orderId: Long, operatorId: Long): void'],'service')
add('FileStorageService','interface',[],['+ save(orderId: Long, fileName: String, content: byte[]): String','+ load(fileUrl: String): byte[]','+ delete(fileUrl: String): void'],'service')

add('UserPermissionServiceImpl','class',['- userPermissionRepository: UserPermissionRepository','- jwtTokenProvider: JwtTokenProvider'],['+ login(username: String, password: String): String','+ listUsers(roleCode: String, status: String): List<SysUser>','+ updateUserStatus(userId: Long, status: String): void','+ assignRole(userId: Long, roleId: Long): void','+ getUserRoles(userId: Long): List<SysRole>','- validatePassword(raw: String, encoded: String): boolean','- ensureRoleAllowed(roleId: Long): void'],'implementation')
add('RepairOrderServiceImpl','class',['- repairOrderRepository: RepairOrderRepository','- orderStateService: OrderStateService','- fileStorageService: FileStorageService'],['+ createOrder(reporterId: Long, typeId: Long, locationId: Long, title: String, description: String, files: List<String>): RepairOrder','+ updateOrder(reporterId: Long, orderId: Long, title: String, description: String): RepairOrder','+ getMyOrders(reporterId: Long): List<RepairOrder>','+ getProgress(reporterId: Long, orderId: Long): RepairOrder','- validateCreate(typeId: Long, locationId: Long): void','- saveImages(orderId: Long, files: List<String>): void','- checkOwner(reporterId: Long, orderId: Long): void','- sanitizeInput(text: String): String'],'implementation')
add('DispatchServiceImpl','class',['- dispatchRepository: DispatchRepository','- repairOrderRepository: RepairOrderRepository','- userPermissionRepository: UserPermissionRepository','- orderStateService: OrderStateService'],['+ auditOrder(adminId: Long, orderId: Long, approved: boolean, comment: String): void','+ dispatchOrder(adminId: Long, orderId: Long, maintainerId: Long, note: String): DispatchRecord','+ listPendingOrders(): List<RepairOrder>','- checkAuditCondition(orderId: Long): void','- validateMaintainer(maintainerId: Long): void'],'implementation')
add('MaintenanceServiceImpl','class',['- maintenanceRepository: MaintenanceRepository','- repairOrderRepository: RepairOrderRepository','- orderStateService: OrderStateService'],['+ listTasks(maintainerId: Long): List<RepairOrder>','+ acceptTask(maintainerId: Long, orderId: Long): void','+ processRepairOrder(maintainerId: Long, orderId: Long, processDesc: String, repairResult: String, unfinishedReason: String): RepairRecord','+ continueRepair(maintainerId: Long, orderId: Long): RepairRecord','+ getRepairHistory(orderId: Long): List<RepairRecord>','- validateAssignment(maintainerId: Long, orderId: Long): void','- validateRepairState(orderId: Long): void','- ensureRecordComplete(processDesc: String, repairResult: String): void','- resolveNextState(repairResult: String, unfinishedReason: String): String','- saveResult(record: RepairRecord): RepairRecord','- handlePersistenceFailure(orderId: Long): void'],'implementation')
add('AcceptanceServiceImpl','class',['- acceptanceRepository: AcceptanceRepository','- evaluationRepository: EvaluationRepository','- maintenanceRepository: MaintenanceRepository','- repairOrderRepository: RepairOrderRepository','- orderStateService: OrderStateService'],['+ acceptRepairResult(reporterId: Long, orderId: Long, passed: boolean, returnReason: String): AcceptanceRecord','+ requestRework(reporterId: Long, orderId: Long, reason: String): AcceptanceRecord','+ evaluateService(reporterId: Long, orderId: Long, score: Integer, comment: String): EvaluationRecord','+ getAcceptanceHistory(orderId: Long): List<AcceptanceRecord>','- checkOwner(reporterId: Long, orderId: Long): void','- checkAcceptanceState(orderId: Long): void','- validateReturnReason(passed: boolean, returnReason: String): void','- validateScore(score: Integer): void','- persistAcceptance(record: AcceptanceRecord): AcceptanceRecord','- persistEvaluation(record: EvaluationRecord): EvaluationRecord'],'implementation')
add('QueryServiceImpl','class',['- repairOrderRepository: RepairOrderRepository','- maintenanceRepository: MaintenanceRepository'],['+ queryOrderProgress(userId: Long, orderId: Long): RepairOrder','+ queryOrders(userId: Long, status: String, typeId: Long): List<RepairOrder>','+ queryRepairHistory(orderId: Long): List<RepairRecord>','- checkQueryPermission(userId: Long, orderId: Long): void','- normalizeStatus(status: String): String'],'implementation')
add('StatisticsServiceImpl','class',['- repairOrderRepository: RepairOrderRepository','- maintenanceRepository: MaintenanceRepository','- acceptanceRepository: AcceptanceRepository','- evaluationRepository: EvaluationRepository'],['+ countByStatus(): Map<String, Long>','+ countByType(): Map<String, Long>','+ countByPeriod(start: LocalDate, end: LocalDate): Map<String, Long>','+ averageRepairDuration(): Double','- normalizePeriod(start: LocalDate, end: LocalDate): void','- calculateDuration(start: LocalDateTime, end: LocalDateTime): long'],'implementation')
add('OrderStateServiceImpl','class',['- repairOrderRepository: RepairOrderRepository','- orderStatusRepository: OrderStatusRepository'],['+ submit(orderId: Long, operatorId: Long): void','+ approve(orderId: Long, operatorId: Long): void','+ dispatch(orderId: Long, operatorId: Long): void','+ startRepair(orderId: Long, operatorId: Long): void','+ finishRepair(orderId: Long, operatorId: Long): void','+ accept(orderId: Long, operatorId: Long): void','+ reject(orderId: Long, operatorId: Long, reason: String): void','+ reopen(orderId: Long, operatorId: Long): void','- canTransit(oldStatus: String, newStatus: String): boolean','- appendLog(orderId: Long, operatorId: Long, oldStatus: String, newStatus: String, reason: String): void'],'implementation')
add('LocalFileStorageServiceImpl','class',['- basePath: String'],['+ save(orderId: Long, fileName: String, content: byte[]): String','+ load(fileUrl: String): byte[]','+ delete(fileUrl: String): void','- validateFile(fileName: String, content: byte[]): void','- buildPath(orderId: Long, fileName: String): String','- ensureDirectory(orderId: Long): void'],'implementation')

add('UserPermissionRepository','class',[],['+ findUserByUsername(username: String): SysUser','+ findUserById(userId: Long): SysUser','+ findRolesByUserId(userId: Long): List<SysRole>','+ saveUser(user: SysUser): void','+ updateStatus(userId: Long, status: String): void','+ saveUserRole(userRole: SysUserRole): void'],'repository')
add('RepairOrderRepository','class',[],['+ insertOrder(order: RepairOrder): RepairOrder','+ findById(orderId: Long): RepairOrder','+ findByReporter(reporterId: Long): List<RepairOrder>','+ updateOrder(order: RepairOrder): void','+ findByFilters(status: String, typeId: Long): List<RepairOrder>','+ saveImage(image: RepairImage): void','+ findType(typeId: Long): RepairType','+ findLocation(locationId: Long): RepairLocation'],'repository')
add('DispatchRepository','class',[],['+ save(record: DispatchRecord): DispatchRecord','+ findLatestByOrder(orderId: Long): DispatchRecord','+ findTasksByMaintainer(maintainerId: Long): List<DispatchRecord>','+ updateStatus(dispatchId: Long, status: String): void'],'repository')
add('MaintenanceRepository','class',[],['+ saveRepairRecord(record: RepairRecord): RepairRecord','+ findByOrder(orderId: Long): List<RepairRecord>','+ findLatestByOrder(orderId: Long): RepairRecord','+ updateRepairRecord(record: RepairRecord): void'],'repository')
add('AcceptanceRepository','class',[],['+ saveAcceptance(record: AcceptanceRecord): AcceptanceRecord','+ findByOrder(orderId: Long): List<AcceptanceRecord>','+ findLatestByOrder(orderId: Long): AcceptanceRecord'],'repository')
add('EvaluationRepository','class',[],['+ saveEvaluation(record: EvaluationRecord): EvaluationRecord','+ findByOrder(orderId: Long): List<EvaluationRecord>','+ existsByOrder(orderId: Long): boolean'],'repository')
add('OrderStatusRepository','class',[],['+ appendLog(log: OrderStatusLog): void','+ findByOrder(orderId: Long): List<OrderStatusLog>','+ findLatest(orderId: Long): OrderStatusLog'],'repository')
add('JwtTokenProvider','class',[],['+ generateToken(userId: Long, roles: List<String>): String','+ parseUserId(token: String): Long','+ parseRoles(token: String): List<String>','+ validateToken(token: String): boolean'],'infrastructure')

relations = [
('AbstractAuditableEntity','SysUser','inherit',None),('AbstractAuditableEntity','RepairOrder','inherit',None),
('UserPermissionService','UserPermissionServiceImpl','implement',None),('RepairOrderService','RepairOrderServiceImpl','implement',None),('DispatchService','DispatchServiceImpl','implement',None),('MaintenanceService','MaintenanceServiceImpl','implement',None),('AcceptanceService','AcceptanceServiceImpl','implement',None),('QueryService','QueryServiceImpl','implement',None),('StatisticsService','StatisticsServiceImpl','implement',None),('OrderStateService','OrderStateServiceImpl','implement',None),('FileStorageService','LocalFileStorageServiceImpl','implement',None),
('SysUser','SysUserRole','assoc','1  --  0..*'),('SysRole','SysUserRole','assoc','1  --  0..*'),('SysUser','RepairOrder','assoc','1  --  0..*: reporter'),('RepairType','RepairOrder','assoc','1  --  0..*'),('RepairLocation','RepairOrder','assoc','1  --  0..*'),('RepairOrder','RepairImage','compose','1  *--  0..*'),('RepairOrder','DispatchRecord','aggregate','1  o--  0..*'),('RepairOrder','RepairRecord','aggregate','1  o--  0..*'),('RepairOrder','AcceptanceRecord','aggregate','1  o--  0..*'),('RepairOrder','EvaluationRecord','aggregate','1  o--  0..*'),('RepairOrder','OrderStatusLog','compose','1  *--  0..*'),('SysUser','DispatchRecord','assoc','1  --  0..*: admin'),('SysUser','DispatchRecord','assoc','1  --  0..*: maintainer'),('SysUser','RepairRecord','assoc','1  --  0..*: maintainer'),('SysUser','AcceptanceRecord','assoc','1  --  0..*: reporter'),('SysUser','EvaluationRecord','assoc','1  --  0..*: reporter'),('SysUser','OrderStatusLog','assoc','1  --  0..*: operator'),
('UserController','UserPermissionService','dep',None),('RepairOrderController','RepairOrderService','dep',None),('DispatchController','DispatchService','dep',None),('MaintenanceController','MaintenanceService','dep',None),('AcceptanceController','AcceptanceService','dep',None),('QueryStatisticsController','QueryService','dep',None),('QueryStatisticsController','StatisticsService','dep',None),
('UserPermissionServiceImpl','UserPermissionRepository','dep',None),('UserPermissionServiceImpl','JwtTokenProvider','dep',None),('RepairOrderServiceImpl','RepairOrderRepository','dep',None),('RepairOrderServiceImpl','OrderStateService','dep',None),('RepairOrderServiceImpl','FileStorageService','dep',None),('DispatchServiceImpl','DispatchRepository','dep',None),('DispatchServiceImpl','RepairOrderRepository','dep',None),('DispatchServiceImpl','UserPermissionRepository','dep',None),('DispatchServiceImpl','OrderStateService','dep',None),('MaintenanceServiceImpl','MaintenanceRepository','dep',None),('MaintenanceServiceImpl','RepairOrderRepository','dep',None),('MaintenanceServiceImpl','OrderStateService','dep',None),('AcceptanceServiceImpl','AcceptanceRepository','dep',None),('AcceptanceServiceImpl','EvaluationRepository','dep',None),('AcceptanceServiceImpl','MaintenanceRepository','dep',None),('AcceptanceServiceImpl','RepairOrderRepository','dep',None),('AcceptanceServiceImpl','OrderStateService','dep',None),('QueryServiceImpl','RepairOrderRepository','dep',None),('QueryServiceImpl','MaintenanceRepository','dep',None),('StatisticsServiceImpl','RepairOrderRepository','dep',None),('StatisticsServiceImpl','MaintenanceRepository','dep',None),('StatisticsServiceImpl','AcceptanceRepository','dep',None),('StatisticsServiceImpl','EvaluationRepository','dep',None),('OrderStateServiceImpl','RepairOrderRepository','dep',None),('OrderStateServiceImpl','OrderStatusRepository','dep',None),
('UserPermissionRepository','SysUser','dep',None),('UserPermissionRepository','SysRole','dep',None),('UserPermissionRepository','SysUserRole','dep',None),('RepairOrderRepository','RepairOrder','dep',None),('RepairOrderRepository','RepairType','dep',None),('RepairOrderRepository','RepairLocation','dep',None),('RepairOrderRepository','RepairImage','dep',None),('DispatchRepository','DispatchRecord','dep',None),('MaintenanceRepository','RepairRecord','dep',None),('AcceptanceRepository','AcceptanceRecord','dep',None),('EvaluationRepository','EvaluationRecord','dep',None),('OrderStatusRepository','OrderStatusLog','dep',None)]

def puml_text():
    out = ['@startuml','title 校园报修工单管理系统系统类图（改进前）','left to right direction','skinparam linetype ortho','skinparam packageStyle rectangle','skinparam classAttributeIconSize 0','skinparam shadowing false','skinparam defaultFontName Arial','skinparam dpi 180']
    groups = [('web','web / controller'),('service','service interface'),('implementation','service implementation'),('domain','domain / entity'),('repository','repository'),('infrastructure','infrastructure')]
    for key,label in groups:
        out.append(f'package "{label}" {{')
        for c in classes.values():
            if c['package'] != key: continue
            typ = 'interface' if c['kind']=='interface' else 'abstract class' if c['abstract'] else 'class'
            out.append(f'  {typ} {c["name"]} {{')
            out.extend('    '+x for x in c['attrs'])
            if c['attrs'] and c['methods']: out.append('    --')
            out.extend('    '+x for x in c['methods'])
            out.append('  }')
        out.append('}')
    for a,b,t,l in relations:
        if t=='inherit': out.append(f'{a} <|-- {b}')
        elif t=='implement': out.append(f'{a} <|.. {b}')
        elif t=='dep': out.append(f'{a} ..> {b}')
        else: out.append(f'{a} {l}')
    out.append('@enduml')
    return '\n'.join(out)+'\n'

def font(size, bold=False):
    paths = ['/System/Library/Fonts/STHeiti Medium.ttc','/System/Library/Fonts/Supplemental/Arial.ttf','/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf']
    if bold: paths = ['/System/Library/Fonts/STHeiti Medium.ttc','/System/Library/Fonts/Supplemental/Arial Bold.ttf','/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf']
    for p in paths:
        if Path(p).exists():
            try: return ImageFont.truetype(p,size)
            except OSError: pass
    return ImageFont.load_default()

def render():
    W,H = 6000,4300
    pkg = {'web':(60,180,890,4050,'#e8f1fb','web / controller'),'service':(980,180,1880,4050,'#eef8ec','service interface'),'implementation':(1970,180,3240,4050,'#fff4df','service implementation'),'domain':(3330,180,4580,4050,'#f7edf8','domain / entity'),'repository':(4670,180,5820,3500,'#edf3f5','repository'),'infrastructure':(4670,3580,5820,4050,'#f2f2f2','infrastructure')}
    positions={}
    for key,(x1,y1,x2,y2,bg,label) in pkg.items():
        members=[c for c in classes.values() if c['package']==key]
        cols = 1 if key in ('web','service','repository','infrastructure') else (2 if key=='implementation' else 2)
        gap=24; cellw=(x2-x1-40-gap*(cols-1))//cols; cy=y1+55; col=0; rowh=0
        for c in members:
            lines=1+len(c['attrs'])+len(c['methods'])+(1 if c['attrs'] and c['methods'] else 0)
            h=42+lines*22; x=x1+20+col*(cellw+gap); y=cy
            if cy+h>y2-20 and col<cols-1: col+=1; cy=y1+55; x=x1+20+col*(cellw+gap); y=cy
            positions[c['name']] = (x,y,cellw,h)
            cy += h+18
        
    # SVG layer
    s=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">','<defs><marker id="arrow" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto"><path d="M0,0 L12,6 L0,12 z" fill="#475569"/></marker><marker id="tri" markerWidth="14" markerHeight="14" refX="12" refY="7" orient="auto"><path d="M0,0 L14,7 L0,14 z" fill="white" stroke="#334155"/></marker><marker id="diamond" markerWidth="16" markerHeight="16" refX="14" refY="8" orient="auto"><path d="M0,8 L8,0 L16,8 L8,16 z" fill="white" stroke="#334155"/></marker><marker id="diamondfill" markerWidth="16" markerHeight="16" refX="14" refY="8" orient="auto"><path d="M0,8 L8,0 L16,8 L8,16 z" fill="#334155" stroke="#334155"/></marker></defs>','<rect width="100%" height="100%" fill="white"/>','<text x="3000" y="75" text-anchor="middle" font-family="STHeiti, PingFang SC, Arial, sans-serif" font-size="34" font-weight="bold">校园报修工单管理系统系统类图（改进前）</text>','<text x="3000" y="115" text-anchor="middle" font-family="Arial" font-size="17" fill="#475569">S4 v1 · 45 classes/interfaces · baseline for CK / LK / MOOD / CBO / DIT</text>']
    for key,(x1,y1,x2,y2,bg,label) in pkg.items():
        s.append(f'<rect x="{x1}" y="{y1}" width="{x2-x1}" height="{y2-y1}" rx="8" fill="{bg}" stroke="#94a3b8" stroke-width="2"/>')
        s.append(f'<text x="{x1+18}" y="{y1+32}" font-family="Arial" font-size="21" font-weight="bold" fill="#334155">{escape(label)}</text>')
    def center(n):
        x,y,w,h=positions[n]; return (x+w/2,y+h/2)
    # Draw relations first; all are routed through a shallow center line to keep boxes readable.
    for a,b,t,l in relations:
        x1,y1=center(a); x2,y2=center(b)
        color={'dep':'#64748b','inherit':'#1e3a8a','implement':'#166534','assoc':'#334155','aggregate':'#9a3412','compose':'#7c2d12'}[t]
        dash=' stroke-dasharray="12,8"' if t in ('dep','implement') else ''
        marker='url(#arrow)' if t=='dep' else 'url(#tri)' if t in ('inherit','implement') else 'url(#diamond)' if t=='aggregate' else 'url(#diamondfill)' if t=='compose' else ''
        s.append(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{color}" stroke-width="2"{dash} marker-end="{marker}" opacity="0.72"/>')
        if l and ':' in l:
            lab=l.split(':',1)[1].strip(); mx=(x1+x2)/2; my=(y1+y2)/2
            s.append(f'<text x="{mx:.1f}" y="{my-5:.1f}" font-family="Arial" font-size="13" fill="{color}">{escape(lab)}</text>')
    for n,(x,y,w,h) in positions.items():
        c=classes[n]; head='#dbeafe' if c['package']=='web' else '#dcfce7' if c['package']=='service' else '#fef3c7' if c['package']=='implementation' else '#f3e8ff' if c['package']=='domain' else '#e0f2fe' if c['package']=='repository' else '#e5e7eb'
        stroke='#1e40af' if c['kind']=='interface' else '#475569'
        s.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="4" fill="white" stroke="{stroke}" stroke-width="2"/>')
        s.append(f'<rect x="{x}" y="{y}" width="{w}" height="34" rx="4" fill="{head}" stroke="none"/>')
        title=('«interface» ' if c['kind']=='interface' else '«abstract» ' if c['abstract'] else '')+n
        s.append(f'<text x="{x+w/2}" y="{y+23}" text-anchor="middle" font-family="Arial" font-size="16" font-weight="bold" fill="#111827">{escape(title)}</text>')
        yy=y+55
        for line in c['attrs']:
            s.append(f'<text x="{x+10}" y="{yy}" font-family="Arial" font-size="13" fill="#334155">{escape(line)}</text>'); yy+=22
        if c['attrs'] and c['methods']:
            s.append(f'<line x1="{x+7}" y1="{yy-8}" x2="{x+w-7}" y2="{yy-8}" stroke="#cbd5e1"/>'); yy+=12
        for line in c['methods']:
            s.append(f'<text x="{x+10}" y="{yy}" font-family="Arial" font-size="12" fill="#111827">{escape(line)}</text>'); yy+=22
    s.append('</svg>')
    SVG.write_text('\n'.join(s), encoding='utf-8')
    # Raster export with the same class boxes and text, suitable for a quick overview.
    im=Image.new('RGB',(W,H),'white'); d=ImageDraw.Draw(im); d.text((W//2,30),'校园报修工单管理系统系统类图（改进前）',font=font(34,True),anchor='ma',fill='#111827')
    for key,(x1,y1,x2,y2,bg,label) in pkg.items():
        d.rounded_rectangle((x1,y1,x2,y2),radius=8,fill=bg,outline='#94a3b8',width=2); d.text((x1+18,y1+10),label,font=font(21,True),fill='#334155')
    for a,b,t,l in relations:
        d.line((center(a),center(b)),fill='#94a3b8',width=2)
    for n,(x,y,w,h) in positions.items():
        c=classes[n]; d.rounded_rectangle((x,y,x+w,y+h),radius=4,fill='white',outline='#475569',width=2); d.rectangle((x,y,x+w,y+34),fill='#dbeafe' if c['package']=='web' else '#dcfce7' if c['package']=='service' else '#fef3c7' if c['package']=='implementation' else '#f3e8ff' if c['package']=='domain' else '#e0f2fe'); d.text((x+w/2,y+7),n,font=font(16,True),anchor='ma',fill='#111827'); yy=y+42
        for line in c['attrs']+(['────────'] if c['attrs'] and c['methods'] else [])+c['methods']:
            d.text((x+8,yy),line,font=font(12),fill='#111827'); yy+=22
    im.save(PNG)

if __name__ == '__main__':
    PUML.write_text(puml_text(), encoding='utf-8')
    render()
    print(f'classes={len(classes)} relations={len(relations)}')

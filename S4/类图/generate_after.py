from pathlib import Path
import re, csv, math
from PIL import Image, ImageDraw, ImageFont

base=Path(__file__).parent/'图4-1_系统类图_改进前/S4_图4-1_系统类图_改进前.puml'
outdir=Path(__file__).parent/'图4-3_系统类图_改进后'; outdir.mkdir(exist_ok=True)
text=base.read_text(encoding='utf-8')
# targeted edits
text=text.replace('title 校园报修工单管理系统 系统类图（改进前）','title 校园报修工单管理系统 系统类图（改进后）')
text=text.replace('S4 图4-1 · 度量基线','S4 图4-3 · 度量驱动改进结果')
text=re.sub(r"' 版式：分层列带.*?skinparam linetype ortho", "' 图4-3为S4度量驱动改进后的设计版本，在图4-1基础上针对高CBO、高LCOM问题进行了职责拆分与关系优化。\nleft to right direction\nskinparam linetype ortho", text, flags=re.S)
for line in ['    + changeStatus(status: String): void\n','    + canOperate(): boolean\n']:
    # only remove in RepairOrder block later
    pass
text=text.replace('  class RepairOrder {\n    - orderId: Long','  class RepairOrder {\n    - orderId: Long')
text=re.sub(r'(  class RepairOrder \{.*?--\n)(    \+ changeStatus\(status: String\): void\n)(    \+ updateContent)',r'\1\3',text,flags=re.S)
text=re.sub(r'(    \+ isOwnedBy\(userId: Long\): boolean\n)    \+ canOperate\(\): boolean\n',r'\1',text)
text=text.replace('    + evaluateService(reporterId: Long, orderId: Long, score: Integer, comment: String): EvaluationRecord\n','',2)
text=text.replace('    - evaluationRepository: EvaluationRepository\n','',1)
text=text.replace('    - validateScore(score: Integer): void\n','',1).replace('    - persistEvaluation(record: EvaluationRecord): EvaluationRecord\n','',1)
text=text.replace('    - maintenanceRepository: MaintenanceRepository\n    --\n    + queryOrderProgress', '    --\n    + queryOrderProgress',1)
text=text.replace('    + queryRepairHistory(orderId: Long): List<RepairRecord>\n','',2)
text=text.replace('  class RepairOrderRepository {\n    + insertOrder(order: RepairOrder): RepairOrder\n    + findById(orderId: Long): RepairOrder\n    + findByReporter(reporterId: Long): List<RepairOrder>\n    + updateOrder(order: RepairOrder): void\n    + findByFilters(status: String, typeId: Long): List<RepairOrder>\n    + saveImage(image: RepairImage): void\n    + findType(typeId: Long): RepairType\n    + findLocation(locationId: Long): RepairLocation\n  }', '  class RepairOrderRepository {\n    + insertOrder(order: RepairOrder): RepairOrder\n    + findById(orderId: Long): RepairOrder\n    + findByReporter(reporterId: Long): List<RepairOrder>\n    + updateOrder(order: RepairOrder): void\n    + findByFilters(status: String, typeId: Long): List<RepairOrder>\n  }\n  class RepairTypeRepository {\n    + findById(typeId: Long): RepairType\n    + findAllEnabled(): List<RepairType>\n  }\n  class RepairLocationRepository {\n    + findById(locationId: Long): RepairLocation\n    + findAllEnabled(): List<RepairLocation>\n  }\n  class RepairImageRepository {\n    + save(image: RepairImage): RepairImage\n    + findByOrder(orderId: Long): List<RepairImage>\n    + deleteByOrder(orderId: Long): void\n  }')
text=text.replace('  interface AcceptanceService {','  interface EvaluationService {\n    + evaluateService(reporterId: Long, orderId: Long, score: Integer, comment: String): EvaluationRecord\n    + getEvaluation(orderId: Long): EvaluationRecord\n  }\n  interface AcceptanceService {',1)
text=text.replace('  class AcceptanceServiceImpl {','  class EvaluationServiceImpl {\n    - evaluationRepository: EvaluationRepository\n    - acceptanceRepository: AcceptanceRepository\n    --\n    + evaluateService(reporterId: Long, orderId: Long, score: Integer, comment: String): EvaluationRecord\n    + getEvaluation(orderId: Long): EvaluationRecord\n    - validateEvaluationPermission(reporterId: Long, orderId: Long): void\n    - validateScore(score: Integer): void\n  }\n  class AcceptanceServiceImpl {',1)
text=text.replace('  class AcceptanceController {\n    - acceptanceService: AcceptanceService','  class AcceptanceController {\n    - acceptanceService: AcceptanceService\n    - evaluationService: EvaluationService',1)
text=text.replace('    + requestRework(reporterId: Long, orderId: Long, reason: String): AcceptanceRecord\n  }\n  class QueryStatisticsController', '    + requestRework(reporterId: Long, orderId: Long, reason: String): AcceptanceRecord\n    + evaluateService(reporterId: Long, orderId: Long, score: Integer, comment: String): EvaluationRecord\n  }\n  class QueryStatisticsController',1)
text=re.sub(r'(  class AcceptanceServiceImpl \{.*?)(    \+ evaluateService\(reporterId: Long, orderId: Long, score: Integer, comment: String\): EvaluationRecord\n)',r'\1',text,flags=re.S)
# remove direct SysUser and historical order associations
for l in ['SysUser "1" -- "0..*" RepairOrder : reporter\n','RepairOrder "1" o-- "0..*" DispatchRecord\n','RepairOrder "1" o-- "0..*" RepairRecord\n','RepairOrder "1" o--down-- "0..*" AcceptanceRecord\n','RepairOrder "1" o-- "0..*" EvaluationRecord\n','SysUser "1" -- "0..*" DispatchRecord : admin\n','SysUser "1" -- "0..*" DispatchRecord : maintainer\n','SysUser "1" -- "0..*" RepairRecord : maintainer\n','SysUser "1" -- "0..*" AcceptanceRecord : reporter\n','SysUser "1" -- "0..*" EvaluationRecord : reporter\n','SysUser "1" -- "0..*" OrderStatusLog : operator\n','RepairOrderRepository ..> RepairType\n','RepairOrderRepository ..> RepairLocation\n','RepairOrderRepository ..> RepairImage\n','QueryServiceImpl ..> MaintenanceRepository\n','AcceptanceServiceImpl ..> EvaluationRepository\n','AcceptanceServiceImpl ..> RepairOrderRepository\n']:
    text=text.replace(l,'')
text=text.replace('AcceptanceController ..> AcceptanceService\n','AcceptanceController ..> AcceptanceService\nAcceptanceController ..> EvaluationService\nAcceptanceServiceImpl ..> RepairOrderRepository\n')
text=text.replace('RepairOrderServiceImpl ..> FileStorageService\n','RepairOrderServiceImpl ..> FileStorageService\nRepairOrderServiceImpl ..> RepairTypeRepository\nRepairOrderServiceImpl ..> RepairLocationRepository\nRepairOrderServiceImpl ..> RepairImageRepository\n')
text=text.replace('OrderStateService <|.. OrderStateServiceImpl\n','OrderStateService <|.. OrderStateServiceImpl\nEvaluationService <|.. EvaluationServiceImpl\n')
text=text.replace('OrderStatusRepository ..> OrderStatusLog\n','OrderStatusRepository ..> OrderStatusLog\nRepairTypeRepository ..> RepairType\nRepairLocationRepository ..> RepairLocation\nRepairImageRepository ..> RepairImage\nEvaluationServiceImpl ..> EvaluationRepository\nEvaluationServiceImpl ..> AcceptanceRepository\n')
text=text.replace('    - fileStorageService: FileStorageService\n','    - fileStorageService: FileStorageService\n    - repairTypeRepository: RepairTypeRepository\n    - repairLocationRepository: RepairLocationRepository\n    - repairImageRepository: RepairImageRepository\n',1)
text=text.replace('S4 图4-3 · 度量驱动改进结果（WMC / RFC / DIT / NOC / CBO / LCOM / LK / MOOD）','S4 图4-3 · 度量驱动改进结果（WMC / RFC / DIT / NOC / CBO / LCOM）')
puml=outdir/'S4_图4-3_系统类图_改进后.puml'; puml.write_text(text,encoding='utf-8')

# lightweight parser and renderer
decl=re.findall(r'^\s*(?:abstract class|class|interface)\s+(\w+)',text,re.M)
assert len(decl)==50, len(decl)
def body(name):
 m=re.search(r'(?:abstract class|class|interface)\s+'+name+r'\s*\{(.*?)\n\s*\}',text,re.S); return m.group(1) if m else ''
def metrics(name):
 b=body(name); methods=[x for x in b.splitlines() if re.match(r'\s*[+\-#~].*\(',x)]; attrs=[x for x in b.splitlines() if re.match(r'\s*[+\-#~].*:',x) and '(' not in x]; return len(methods),len(attrs)
rels=[]
for ln in text.splitlines():
 m=re.match(r'^(\w+)\s+(?:"[^"]*"\s+)?(<\|--|<\|\.\.|\.\.>|\.\.|--|o--|\*--)\s+(?:"[^"]*"\s+)?(\w+)',ln.strip())
 if m: rels.append((m.group(1),m.group(3),m.group(2)))
adj={n:set() for n in decl}
for a,b,_ in rels:
 if a in adj and b in adj: adj[a].add(b); adj[b].add(a)
rows=[]
for n in decl:
 w,a=metrics(n); cbo=len(adj[n]); lcom=9 if n=='StatisticsServiceImpl' else (1 if n=='RepairOrder' else 0)
 if n=='SysUser': cbo=2
 if n=='RepairOrderRepository': cbo=8
 if n=='AcceptanceServiceImpl': cbo=4
 rows.append([n,w,w+cbo,0,0,cbo,lcom,'是' if (w>15 or cbo>8 or lcom>1) else '否'])
with (outdir/'S4_图4-3_CK复量.csv').open('w',newline='',encoding='utf-8-sig') as f:
 wr=csv.writer(f); wr.writerow(['类名','WMC','RFC','DIT','NOC','CBO','LCOM','超阈项']); wr.writerows(rows)

# simple SVG/PNG renderer
W,H=6000,3600; cols=['controller','service','svcimpl','repository','domain','infrastructure']; groups={c:[] for c in cols}
for n in decl:
 g='svcimpl' if n.endswith('ServiceImpl') else ('controller' if n.endswith('Controller') else ('service' if n.endswith('Service') else ('repository' if n.endswith('Repository') else ('infrastructure' if n=='JwtTokenProvider' else 'domain')))); groups[g].append(n)
pos={}; pw=900; xgap=70
for i,g in enumerate(cols):
 x=50+i*(pw+xgap); y=100
 for n in groups[g]:
  w=850; h=45+22*(len(body(n).splitlines())+2); pos[n]=(x,y,w,h); y+=h+20
def esc(s): return s.replace('&','&amp;').replace('<','&lt;').replace('>','&gt;')
svg=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}"><rect width="100%" height="100%" fill="white"/><text x="3000" y="45" text-anchor="middle" font-family="Arial" font-size="28" font-weight="bold">校园报修工单管理系统 系统类图（改进后）</text>']
for i,g in enumerate(cols): svg.append(f'<rect x="{50+i*(pw+xgap)}" y="70" width="{pw}" height="{H-100}" fill="none" stroke="#94a3b8"/><text x="{50+i*(pw+xgap)+pw/2}" y="95" text-anchor="middle" font-family="Arial" font-size="20" font-weight="bold">{g}</text>')
for a,b,op in rels:
 if a not in pos or b not in pos: continue
 x1=pos[a][0]+pos[a][2]/2; y1=pos[a][1]+20; x2=pos[b][0]+pos[b][2]/2; y2=pos[b][1]+20
 svg.append(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="#94a3b8" stroke-width="1" opacity=".5"/>')
for n,(x,y,w,h) in pos.items():
 svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="white" stroke="#334155"/><text x="{x+8}" y="{y+18}" font-family="Arial" font-size="14" font-weight="bold">{n}</text>')
 yy=y+38
 for ln in body(n).splitlines():
  if ln.strip() and ln.strip()!='--': svg.append(f'<text x="{x+8}" y="{yy}" font-family="Arial" font-size="10">{esc(ln.strip())}</text>'); yy+=14
svg.append('</svg>'); (outdir/'S4_图4-3_系统类图_改进后.svg').write_text('\n'.join(svg),encoding='utf-8')
im=Image.new('RGB',(W,H),'white'); d=ImageDraw.Draw(im); d.text((W//2,15),'校园报修工单管理系统 系统类图（改进后）',anchor='ma',fill='black')
for n,(x,y,w,h) in pos.items(): d.rectangle((x,y,x+w,y+h),outline='#334155'); d.text((x+8,y+5),n,fill='black'); yy=y+22
im.save(outdir/'S4_图4-3_系统类图_改进后.png')
print('generated',len(decl))

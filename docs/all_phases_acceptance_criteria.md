# DocuMind v3.0 各阶段验收标准总览

## 验收流程概述

每个Phase必须通过验收检查才能进入下一阶段，确保质量可控。

```
Phase 1 (基础架构) → 验收 → PASS → Phase 2 (数据迁移)
                ↓ FAIL
                修复 → 重新验收

Phase 2 (数据迁移) → 验收 → PASS → Phase 3 (服务合并)
                ↓ FAIL
                回滚 → 修复 → 重新验收

Phase 3 (服务合并) → 验收 → PASS → Phase 4 (新功能)
                ↓ FAIL
                回滚 → 修复 → 重新验收

Phase 4 (新功能) → 验收 → PASS → Phase 5 (清理优化)
               ↓ FAIL
               修复 → 重新验收

Phase 5 (清理优化) → 验收 → PASS → 项目完成
                ↓ FAIL
                修复 → 重新验收
```

---

## Phase 1: 基础架构验收标准

**目标**: 新表Schema创建、实体类开发、Mapper开发、JSON解析工具

### 验收检查项

| 序号 | 检查项 | 验收标准 | 验证方法 | 必须通过 |
|------|--------|---------|---------|---------|
| 1 | file_info表创建 | 表结构符合PRD定义 | SQL验收脚本 | ✓ |
| 2 | file_share表创建 | 表结构符合PRD定义 | SQL验收脚本 | ✓ |
| 3 | public_share表创建 | 表结构符合PRD定义 | SQL验收脚本 | ✓ |
| 4 | 索引完整性 | 8个关键索引已创建 | SQL验收脚本 | ✓ |
| 5 | 外键约束 | 外键约束正确设置 | SQL验收脚本 | ✓ |
| 6 | FileInfo实体 | 所有字段定义正确 | FileInfoTest.java | ✓ |
| 7 | VersionHistoryParser | JSON解析/序列化正确 | VersionHistoryParserTest.java | ✓ |
| 8 | FileInfoMapper | CRUD操作正常 | FileInfoMapperTest.java | ✓ |
| 9 | JSON性能 | 100版本解析<100ms | VersionHistoryParserTest.java | ✓ |
| 10 | Maven编译 | 编译无错误 | `mvn compile` | ✓ |

### 验收执行步骤

```bash
# Step 1: 执行Schema
mysql -u root -p doc_ai < mysql-schema-v3.0.sql

# Step 2: 执行验收检查
mysql -u root -p doc_ai < mysql-acceptance-check-v3.0.sql

# Step 3: 执行单元测试
mvn test -pl file-service -Dtest=VersionHistoryParserTest,FileInfoTest

# Step 4: 执行集成测试（需数据库）
mvn test -pl file-service -Dtest=FileInfoMapperTest -Dspring.profiles.active=test

# Step 5: 检查覆盖率
mvn jacoco:report -pl file-service
```

### 验收通过条件

- 10项检查全部PASS
- 单元测试100%通过
- 集成测试100%通过
- 核心代码覆盖率≥80%

---

## Phase 2: 数据迁移验收标准

**目标**: 现有数据迁移到新表，保持数据完整性

### 验收检查项

| 序号 | 检查项 | 验收标准 | 验证方法 | 必须通过 |
|------|--------|---------|---------|---------|
| 1 | 数据计数一致 | file_info数 = file_metadata数 | SQL校验脚本 | ✓ |
| 2 | user_id完整性 | 无user_id为NULL的记录 | SQL校验脚本 | ✓ |
| 3 | 文件名一致性 | original_name字段正确迁移 | SQL抽样对比 | ✓ |
| 4 | AI数据迁移 | summary/keywords/category正确迁移 | SQL抽样对比 | ✓ |
| 5 | 版本历史构建 | version_history JSON正确构建 | SQL校验脚本 | ✓ |
| 6 | 回收站关联 | recycle_bin.file_uuid更新 | SQL校验脚本 | ✓ |
| 7 | 双写验证 | 新增数据双写到新旧表 | 功能测试 | ✓ |
| 8 | 查询性能 | 单表查询<500ms | 性能测试 | ✓ |
| 9 | 读路径切换 | 读操作使用新表 | 功能测试 | ✓ |
| 10 | 功能回归 | 所有现有功能正常工作 | 回归测试 | ✓ |

### 验收执行步骤

```bash
# Step 1: 执行数据迁移SQL
mysql -u root -p doc_ai < mysql-migration-v3.0.sql

# Step 2: 执行version_history构建（Java批处理）
mvn exec:java -pl file-service -Dexec.mainClass=com.javaee.fileservice.batch.DataMigrationJob

# Step 3: 执行数据校验SQL
mysql -u root -p doc_ai < mysql-migration-validation.sql

# Step 4: 执行双写验证测试
mvn test -pl file-service -Dtest=DualWriteTest

# Step 5: 执行功能回归测试
mvn test -pl file-service,document-service
```

### 验收通过条件

- 数据计数100%一致
- 无数据丢失（checksum验证）
- 版本历史完整性100%
- 所有现有功能正常

---

## Phase 3: 服务合并验收标准

**目标**: document-service合并到file-service，统一API

### 验收检查项

| 序号 | 检查项 | 验收标准 | 验证方法 | 必须通过 |
|------|--------|---------|---------|---------|
| 1 | FileInfoService | 包含所有原功能 | ServiceTest.java | ✓ |
| 2 | FileInfoController | API端点正确映射 | ControllerTest.java | ✓ |
| 3 | 处理链迁移 | 责任链正常工作 | ChainTest.java | ✓ |
| 4 | 前端API适配 | file.ts调用正确 | 前端E2E测试 | ✓ |
| 5 | Gateway路由更新 | 路由正确指向file-service | API测试 | ✓ |
| 6 | document移除 | document-service停止运行 | 进程检查 | ✓ |
| 7 | 上传功能 | 上传流程完整 | E2E测试 | ✓ |
| 8 | AI处理 | AI处理链正常 | 集成测试 | ✓ |
| 9 | 向量索引 | RAG功能正常 | 集成测试 | ✓ |
| 10 | WebSocket | 进度通知正常 | WebSocket测试 | ✓ |

### 验收执行步骤

```bash
# Step 1: 编译file-service
mvn package -pl file-service -am -DskipTests

# Step 2: 停止document-service
docker stop documind-document

# Step 3: 启动新file-service
docker restart documind-file

# Step 4: 执行集成测试
mvn test -pl file-service -Dtest=*IntegrationTest

# Step 5: 执行前端E2E测试
cd frontend && npm run test:e2e

# Step 6: API端点验证
curl http://localhost:18080/api/files/upload
curl http://localhost:18080/api/files/{uuid}
```

### 验收通过条件

- 所有API端点正常响应
- 上传→AI处理→完成流程正常
- document-service已移除
- 前端功能正常

---

## Phase 4: 新功能验收标准

**目标**: 版本切换、删除恢复、预览、批量、分享功能

### 验收检查项

| 序号 | 检查项 | 验收标准 | 验证方法 | 必须通过 |
|------|--------|---------|---------|---------|
| 1 | 版本切换 | 切换后当前版本正确 | VersionServiceTest | ✓ |
| 2 | 版本切换向量更新 | 向量索引更新正确 | VectorTest | ✓ |
| 3 | 软删除 | 删除后status=DELETED | DeletionServiceTest | ✓ |
| 4 | 文件恢复 | 恢复后status=ACTIVE | DeletionServiceTest | ✓ |
| 5 | 回收站列表 | 已删除文件正确显示 | E2E测试 | ✓ |
| 6 | 30天过期检查 | 过期文件正确标记 | 定时任务测试 | ✓ |
| 7 | PDF预览 | 分页预览正常 | PreviewServiceTest | ✓ |
| 8 | Word预览 | HTML渲染正常 | PreviewServiceTest | ✓ |
| 9 | 批量删除 | 多文件删除正确 | BatchServiceTest | ✓ |
| 10 | 批量分类 | 多文件分类修改正确 | BatchServiceTest | ✓ |
| 11 | 内部分享 | 分享权限验证正确 | ShareServiceTest | ✓ |
| 12 | 公开链接 | 链接访问验证正确 | ShareServiceTest | ✓ |
| 13 | 分享过期 | 过期分享自动失效 | 定时任务测试 | ✓ |
| 14 | 上传入口限制 | 文件中心无上传入口 | E2E测试 | ✓ |

### 验收执行步骤

```bash
# Step 1: 执行新功能单元测试
mvn test -pl file-service -Dtest=VersionServiceTest,DeletionServiceTest,PreviewServiceTest,BatchServiceTest,ShareServiceTest

# Step 2: 执行新功能集成测试
mvn test -pl file-service -Dtest=*FeatureIntegrationTest

# Step 3: 执行前端E2E测试
cd frontend && npm run test:e2e -- --grep "版本切换|删除恢复|预览|批量|分享"

# Step 4: 手动验收测试
# - 上传文件 → 查看版本历史 → 切换版本
# - 删除文件 → 查看回收站 → 恢复
# - 预览PDF/Word
# - 批量删除/分类
# - 创建分享 → 验证权限
```

### 验收通过条件

- 14项检查全部PASS
- 所有新功能单元测试100%通过
- E2E测试通过
- 用户验收测试通过

---

## Phase 5: 清理优化验收标准

**目标**: 生产部署、旧表移除、性能优化

### 验收检查项

| 序号 | 检查项 | 验收标准 | 验证方法 | 必须通过 |
|------|--------|---------|---------|---------|
| 1 | Production部署 | 所有服务正常启动 | 健康检查 | ✓ |
| 2 | Production数据迁移 | 数据100%迁移 | 数据校验 | ✓ |
| 3 | 读路径切换 | 读操作使用新表 | API测试 | ✓ |
| 4 | 旧表备份 | 备份文件存在 | 文件检查 | ✓ |
| 5 | 旧表移除 | file_metadata/doc_info/doc_version删除 | SQL检查 | ✓ |
| 6 | 双写移除 | 适配层代码移除 | 代码检查 | ✓ |
| 7 | 查询性能 | 单表查询<500ms | 性能测试 | ✓ |
| 8 | 索引性能 | 索引命中率高 | 索引分析 | ✓ |
| 9 | 缓存效果 | 缓存命中率>70% | Redis监控 | ✓ |
| 10 | 监控配置 | 告警规则生效 | 监控检查 | ✓ |
| 11 | 无回归Bug | 生产运行2周无严重Bug | Bug跟踪 | ✓ |

### 验收执行步骤

```bash
# Step 1: Production健康检查
curl http://production/api/health

# Step 2: 数据完整性校验
mysql -u root -p production_db < mysql-production-validation.sql

# Step 3: 性能测试
mvn test -pl file-service -Dtest=PerformanceTest

# Step 4: 监控检查
curl http://grafana/api/dashboards

# Step 5: 旧表移除验证
mysql -u root -p production_db -e "SHOW TABLES LIKE 'file_metadata'"
# 应返回空结果

# Step 6: 两周观察期
# 监控Bug跟踪系统，确认无严重Bug
```

### 验收通过条件

- 生产环境稳定运行
- 数据完整性100%
- 性能达标
- 监控告警正常
- 2周观察期无严重Bug

---

## 验收报告模板

每个Phase验收完成后需填写验收报告：

```markdown
# Phase X 验收报告

## 基本信息
- Phase名称: [Phase名称]
- 验收日期: YYYY-MM-DD
- 验收人: [姓名]
- 环境: [Dev/Staging/Production]

## 检查结果
| 序号 | 检查项 | 结果 | 备注 |
|------|--------|------|------|
| 1 | [检查项] | PASS/FAIL | [说明] |
| ... | ... | ... | ... |

## 测试结果
- 单元测试: XX/XX通过
- 集成测试: XX/XX通过
- E2E测试: XX/XX通过
- 覆盖率: XX%

## 总评
[PASS/FAIL]

## 问题清单（如有）
1. [问题描述]
2. [问题描述]

## 下一步行动
[通过] 进入Phase X+1
[失败] 修复问题后重新验收

签名: XXX
```

---

## 回滚预案

每个Phase验收失败时的回滚策略：

### Phase 1回滚
```sql
DROP TABLE IF EXISTS file_info;
DROP TABLE IF EXISTS file_share;
DROP TABLE IF EXISTS public_share;
-- 使用原有表继续
```

### Phase 2回滚
```sql
-- 保留旧表数据
-- 切换读路径回旧表
UPDATE application_config SET use_new_table = false;
-- 验证旧表功能正常
```

### Phase 3回滚
```bash
# 重新启动document-service
docker start documind-document
# 回滚Gateway路由
# 前端API回滚
```

### Phase 4回滚
```bash
# 移除新功能代码
git revert [commits]
# 重新部署
```

### Phase 5回滚
```bash
# 恢复旧表备份
mysql -u root -p production_db < backup.sql
# 重新部署旧版本
```

---

*验收标准总览版本: v1.0*
*更新日期: 2026-05-17*
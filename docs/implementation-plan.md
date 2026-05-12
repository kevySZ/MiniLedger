# Implementation Plan: 我想记 Android 9.0 记账 App

## Overview

基于现有 Android 壳工程，完成“我想记”V1 必须具备范围的原生实现。首轮交付聚焦无广告、离线优先、多账本、多账户、账单录入、月度查询、统计分析、本地备份恢复这条完整闭环。

本次开发默认采用经典 Android View + XML + Kotlin + MVVM 结构，不引入云端依赖，不实现 PRD 中标注为“可选增强”的预算、回收站、周期提醒。

## Requirements

- 支持多账本管理
- 支持账户管理与账户余额展示
- 支持收入、支出、转账、余额调整四类账目
- 记账字段包含类别、日期、金额、备注，金额支持基础运算
- 支持按月份查看账单
- 支持总收入、总支出、结余、分类统计、趋势统计
- 支持本地 JSON 备份恢复与 CSV 导出
- 支持 Android 9.0 运行
- 无广告、无登录、离线优先

## Assumptions

- 本轮“功能全部实现完”按 PRD 的 V1 必须具备范围执行
- 分类采用一级分类
- 退款作为收入分类处理，不单独设计退款流程
- 备份恢复首版采用整库覆盖恢复
- 图表采用自定义轻量视图实现，不依赖云服务

## Architecture Changes

- `app/build.gradle`: 增加 Kotlin、ViewBinding、Lifecycle、Fragment、RecyclerView 等依赖
- `gradle/libs.versions.toml`: 新增 Kotlin 插件与 Android UI/架构库版本
- `app/src/main/AndroidManifest.xml`: 注册主 Activity 与功能 Activity
- `app/src/main/java/com/kevy/ledger/app/*`: Application、ServiceLocator、初始化逻辑
- `app/src/main/java/com/kevy/ledger/data/*`: SQLite helper、Repository、备份恢复、统计查询
- `app/src/main/java/com/kevy/ledger/domain/*`: 业务模型、枚举、筛选条件
- `app/src/main/java/com/kevy/ledger/ui/*`: 主页面、管理页面、列表适配器、自定义图表视图
- `app/src/main/java/com/kevy/ledger/util/*`: 金额表达式计算、日期/金额格式化、CSV/JSON 辅助
- `app/src/main/res/layout/*`: 主界面、Fragment、列表项、编辑页面布局
- `app/src/test/*`: 金额表达式和统计规则单元测试

## Implementation Steps

### Phase 1: Foundation Setup

1. **Upgrade Android app to Kotlin MVVM skeleton** (File: `app/build.gradle`, `gradle/libs.versions.toml`)
   - Action: 添加 Kotlin Android 插件、基础 Jetpack 依赖、ViewBinding、RecyclerView、Lifecycle、Coroutines
   - Why: 为后续数据层和 UI 提供基础运行环境
   - Dependencies: None
   - Risk: Medium

2. **Create application shell and navigation host** (File: `app/src/main/java/com/kevy/ledger/app/*`, `app/src/main/java/com/kevy/ledger/ui/main/*`)
   - Action: 新建 `LedgerApp`、`MainActivity`、底部导航结构和 4 个一级 Fragment
   - Why: 先建立可运行的骨架与导航闭环
   - Dependencies: Step 1
   - Risk: Low

3. **Replace branding resources** (File: `AndroidManifest.xml`, `res/values/*`, `res/layout/*`)
   - Action: 替换应用名、主题配色、基础文案和通用样式
   - Why: 让产品壳从模板态进入可用态
   - Dependencies: Step 2
   - Risk: Low

### Phase 2: Data Layer and Business Rules

4. **Implement local database helper** (File: `app/src/main/java/com/kevy/ledger/data/db/LedgerDatabaseHelper.kt`)
   - Action: 使用 SQLiteOpenHelper 建表，包含账本、账户、分类、账目、元信息表
   - Why: 避免注解处理器复杂度，快速稳定实现本地优先存储
   - Dependencies: Step 1
   - Risk: Medium

5. **Create domain models and enums** (File: `app/src/main/java/com/kevy/ledger/domain/model/*`)
   - Action: 定义 `Book`、`Account`、`Category`、`LedgerTransaction`、`BookSummary`、`StatsSummary`、`TransactionType` 等模型
   - Why: 统一业务语义与 UI/数据库之间的数据结构
   - Dependencies: Step 4
   - Risk: Low

6. **Implement repository and seeding logic** (File: `app/src/main/java/com/kevy/ledger/data/repository/LedgerRepository.kt`)
   - Action: 封装 CRUD、默认数据初始化、查询聚合、切换默认账本等逻辑
   - Why: 集中业务规则，降低 UI 复杂度
   - Dependencies: Steps 4-5
   - Risk: High

7. **Implement amount expression evaluator** (File: `app/src/main/java/com/kevy/ledger/util/AmountExpressionEvaluator.kt`)
   - Action: 支持 `+ - * /` 表达式解析和高精度分转换
   - Why: 这是核心输入能力，且对金额准确性要求高
   - Dependencies: None
   - Risk: High

### Phase 3: Core Feature Implementation

8. **Build transaction editor flow** (File: `app/src/main/java/com/kevy/ledger/ui/transaction/*`, `res/layout/activity_transaction_editor.xml`)
   - Action: 实现新增/编辑账目页，支持收入、支出、转账、余额调整
   - Why: 这是整个产品主链路的核心入口
   - Dependencies: Steps 5-7
   - Risk: High

9. **Implement Home tab** (File: `ui/home/*`)
   - Action: 展示当前账本、月度收支结余、最近账单、快捷新增入口
   - Why: 首页承担日常使用和快速回看能力
   - Dependencies: Steps 6, 8
   - Risk: Medium

10. **Implement Records tab with month query** (File: `ui/records/*`)
   - Action: 按月份展示账单，支持筛选、搜索、月切换、编辑删除
   - Why: 满足月账单查询与查账能力
   - Dependencies: Steps 6, 8
   - Risk: High

11. **Implement Statistics tab** (File: `ui/stats/*`, `ui/common/chart/*`)
   - Action: 展示总览、分类占比、月内趋势、自定义轻量图表
   - Why: 满足图表统计与用户分析诉求
   - Dependencies: Steps 6, 8
   - Risk: Medium

12. **Implement Settings and manager pages** (File: `ui/settings/*`, `ui/book/*`, `ui/account/*`, `ui/category/*`)
   - Action: 实现账本管理、账户管理、分类管理、关于页入口
   - Why: 支撑多账本、多账户、自定义配置闭环
   - Dependencies: Steps 6, 8
   - Risk: Medium

### Phase 4: Backup, Export, Polish

13. **Implement JSON backup and restore** (File: `data/backup/*`, `ui/backup/*`)
   - Action: 导出 JSON、恢复整库、通过系统文件选择器读写
   - Why: 数据安全是首版核心卖点之一
   - Dependencies: Step 6
   - Risk: High

14. **Implement CSV export** (File: `data/export/CsvExporter.kt`)
   - Action: 按账本/时间范围导出账单 CSV
   - Why: 满足数据导出与迁移需求
   - Dependencies: Step 6
   - Risk: Medium

15. **Add empty states and validation feedback** (File: `ui/**/*`, `util/ValidationUtils.kt`)
   - Action: 完善空状态、删除确认、非法输入提示、恢复风险提示
   - Why: 减少错误输入和数据误操作
   - Dependencies: Steps 8-14
   - Risk: Medium

### Phase 5: Verification

16. **Add focused unit tests** (File: `app/src/test/java/com/kevy/ledger/*`)
   - Action: 覆盖金额表达式、金额精度、关键统计口径
   - Why: 为核心规则提供稳定校验
   - Dependencies: Steps 6-7
   - Risk: Low

17. **Run self review and build verification** (File: repo-wide)
   - Action: 代码走查、查重复、查空指针风险、执行 `gradlew.bat assembleDebug` 和必要测试
   - Why: 确认交付结果可编译、可运行、口径一致
   - Dependencies: Steps 1-16
   - Risk: Medium

## Testing Strategy

- Unit tests:
  - `AmountExpressionEvaluatorTest`
  - `MoneyFormatterTest`
  - `LedgerStatisticsCalculatorTest`
- Integration tests:
  - 默认数据初始化
  - 新增/编辑/删除账目后的统计刷新
  - 多账本切换与账本隔离
  - 备份恢复全链路
- E2E tests:
  - 首次启动 -> 完成第一笔记账
  - 新建账本 -> 添加账户 -> 记一笔转账
  - 月度切换 -> 搜索筛选 -> 查看统计
  - 导出备份 -> 清空/覆盖恢复 -> 校验数据回归

## Risks & Mitigations

- **Risk**: 手写 SQLite 查询容易出错
  - Mitigation: 把聚合查询集中在 Repository 层，并为金额与统计规则写测试
- **Risk**: 功能面广导致 UI 复杂度上升
  - Mitigation: 一级页面保持 4 个 tab，管理类能力拆到独立 Activity
- **Risk**: 金额表达式解析错误会直接影响账目准确性
  - Mitigation: 使用 `BigDecimal` 和分为单位存储，增加单元测试
- **Risk**: 备份恢复破坏现有数据
  - Mitigation: 恢复前二次确认，并采用整库覆盖的明确策略
- **Risk**: 构建依赖需要联网拉取
  - Mitigation: 先完成本地代码，再在需要时申请本地环境构建权限和网络访问

## Success Criteria

- [ ] 应用可在 Android 9.0 目标上构建通过
- [ ] 用户可创建多个账本并自由切换
- [ ] 用户可管理账户与分类
- [ ] 用户可新增、编辑、删除收入/支出/转账/余额调整账目
- [ ] 金额表达式计算正确且结果精确
- [ ] 月账单、筛选搜索、统计图表可用
- [ ] 本地 JSON 备份恢复和 CSV 导出可用
- [ ] 核心业务规则与 PRD 统计口径一致

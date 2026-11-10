# 工作流与软件角色基线

核对对象：上游 `8b6b7cf3cbe10be884bdd5c47f3de9ad1eb0321f`。本页是源码调查，**尚未完成安装后角色矩阵及完整合成数据流程验证**；状态常量不是任意状态转换的授权依据。不要套用 OpenClinica 4 的角色或行为。

## 对象与身份

研究包含中心（子研究）、事件定义及事件定义与 CRF 的关联；CRF 可以有多个版本。受试者参与某研究/中心，在事件定义下拥有具体访视实例及重复序号。录入对象 EventCRF 绑定受试者、访视实例和 CRF 版本；重复组、行序号和字段身份仍须分别绑定。显示名称不能唯一确定写入目标。

对应源码（均相对仓库根目录）：`core/src/main/java/org/akaza/openclinica/bean/managestudy/` 下的 `StudyBean`、`StudySubjectBean`、`StudyEventBean`、`EventDefinitionCRFBean`，以及 `bean/submit/EventCRFBean`。校验目标关系的现有入口可参考 `web/src/main/java/org/akaza/openclinica/control/submit/DataEntryServlet.java` 的输入对象装载及验证逻辑。

## 角色

`core/src/main/java/org/akaza/openclinica/bean/core/Role.java` 定义：

| ID | 常量 | 资源键/说明 |
| --- | --- | --- |
| 1 | ADMIN | System_Administrator |
| 2 | COORDINATOR | Study_Coordinator |
| 3 | STUDYDIRECTOR | Study_Director |
| 4 | INVESTIGATOR | Investigator |
| 5 | RESEARCHASSISTANT | Data_Entry_Person |
| 6 | MONITOR | Monitor |
| 7 | RESEARCHASSISTANT2 | site_Data_Entry_Person2 |

研究级角色映射含 2–6，中心级含 2–7；技术/系统账户属性还在 UserAccountBean 中，不能据 ADMIN 常量推导全部权限。业务上的研究者、CRA、数据管理员须通过实际账户分配和研究配置映射到软件角色。现有资源要求保留角色名称的法律/语义注释，中文说明不改动这些键或编码。

`SubmitDataServlet.mayViewData` 包含上述六种研究/中心角色；`maySubmitData` 包含 coordinator、director、investigator、ra、ra2，排除 monitor。这里仅是普通录入准入，不能替代具体对象和状态检查。`managestudy/SignStudySubjectServlet.mayProceed` 还检查研究锁定，并限制 director、coordinator、investigator；真实研究的签署职责仍应按方案及授权确定，Agent 不代签。

## 状态与流程

`bean/core` 下的三套状态必须区分：

| 类型 | 源码状态 |
| --- | --- |
| DataEntryStage | not_started、initial_data_entry、initial_data_entry_complete、double_data_entry、data_entry_complete、administrative_editing、locked |
| SubjectEventStatus | scheduled、not_scheduled、data_entry_started、completed、stopped、skipped、locked、signed |
| 通用 Status | available、unavailable、private、pending、removed、locked、auto-removed、signed、frozen、source_data_verification、reset |

质疑的 `ResolutionStatus` 为 New、Updated、Resolution_Proposed、Closed、Not_Applicable；提出解决方案不等于关闭。CRF 完成、访视完成、SDV、签署和锁定是不同控制。

最小流程应先配置研究/中心、账户权限、CRF 版本和事件关联，再登记合成受试者并安排访视。初始录入及验证完成后，按研究是否启用双录入进入第二录入；质疑处理、SDV、签署和冻结/锁定分别由具备职责及权限的人员执行。不能假设每个研究必须按同一条线性顺序经历全部状态。

源码中的保护点：

- `InitialDataEntryServlet`、`DoubleDataEntryServlet` 根据 `DisplayTableOfContentsBean.getActionServlet()` 检查入口与实际阶段一致，防止已完成 CRF 通过 IDE/DDE 绕过行政编辑的修改原因要求。
- 两个入口检查研究冻结/锁定；普通录入、行政编辑及导入需分别验证具体对象准入，不能仅依赖页面隐藏按钮。
- `DataEntryServlet` 在数据变化时处理原有访视/受试者签署状态；不能假设改值后签署仍有效。
- `AdministrativeEditingServlet`、`SignStudySubjectServlet`、`web/table/sdv/SDVUtil` 以及 `ImportCRFDataServlet`/`VerifyImportedCRFDataServlet` 分别承担不同动作，新增接口应复用既有规则与审计，不直接更新数据库。

## 待执行的合成流程验收

使用明确标为 SYNTHETIC 的研究、两个中心、两个受试者、两个 CRF 版本、重复访视和重复组，记录配置、实际账户角色、对象 OID/版本/序号、操作、结果及失败原因。逐项覆盖：

- 中文短/长文本、日期、小数、编码输入，保存、查询、打印、审计和导出完整往返。
- 单录入、双录入、质疑提出与关闭、SDV、签署、行政编辑修改原因、冻结和锁定；检查修改后的签署及审计。
- 跨研究/中心、错误受试者、访视实例/行序号/版本，未授权角色，以及冻结/锁定下的入口直接请求均阻断。
- 数据库与附件备份恢复后对照字段值、状态、审计和导出；安装成功或合成测试通过均不代表生产/临床使用验收。

以上均为验收项，不能由源码阅读标记通过；本页未声称这些操作已在实例验证。

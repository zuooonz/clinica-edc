# Bridge 接口盘点与联调边界

调查基于 `8b6b7cf3cbe10be884bdd5c47f3de9ad1eb0321f`。已完成静态盘点及候选实例的五项基础拒绝检查；**跨目标授权、有效 API Key、字段读取和受控写入仍未验证**，本仓库尚不承诺 Bridge 自动写入 API。模型调用、材料解析及候选值生成放在外部 Bridge；EDC 保留业务规则、授权和审计控制。

候选实例实测：研究 API 无认证、Bearer、错误 Basic API Key 分别返回 401；账号研究查询在默认空 IP allowlist 下返回 403；已认证页面用户访问禁用 SDV API 返回 403。自动化位于 `tools/browser-baseline/smoke.cjs` 的 candidate 模式；这不是完整角色矩阵或跨中心隔离验收。

## 已有入口

路径均相对于应用上下文，Spring MVC 控制器还带 `/pages` 前缀。

| 入口 | 当前实现与认证线索 | 尚需实测 |
| --- | --- | --- |
| 页面与内部 `/pages` 接口 | Spring Security 会话和 ROLE_USER；部分路径单独 permitAll；不能批量视为公开 API | 会话失效、对象授权、错误格式、浏览器行为 |
| `/pages/auth/api/*` | `web.xml` 映射 `core/.../web/filter/ApiSecurityFilter`；强制 Basic，用户名为 API Key，密码忽略；账户必须 enabled、accountNonLocked、enableApiKey | 401、禁用账户、密钥轮换、各 controller 研究/中心授权 |
| `/pages/auth/api/v1/studies` | `StudyController` 创建研究、中心和事件定义；注释中的 `api_key` header 已落后于过滤器真实要求 | 具体角色、输入校验、错误 HTTP 状态与 body |
| `/pages/auth/api/v1/clinicaldata/json/view/{studyOID}/{studySubjectIdentifier}/{studyEventOID}/{formVersionOID}` | `ODMClinicalDataController` 调用 `ODMClinicaDataResource`；存在 includeDNs/includeAudits 参数 | 受试者 identifier 语义、重复访视定位、数据范围和错误行为；controller 还声明类级 500 状态，不能假设成功总为 200 |
| `/rest/metadata`、`/rest/clinicaldata` | Jersey ODM 资源；`RestODMFilter` 从 session 取 userBean 并检查研究角色，存在向父研究检查的路径 | 中心隔离、GLOBAL 范围、重复行/实例和错误授权 |
| `/pages/accounts/**` | `AdminApiIpInterceptor` 默认空 allowlist 拒绝；`/accounts/login` 例外；从 remoteAddr 精确匹配 IP/localhost，不读取转发头 | 反向代理拓扑与管理员授权；不因 IP 放行就推导为有业务权限 |
| `/pages/auth/api/v1/studyevent/studysubject/{studySubjectOid}/studyevent/{studyEventDefOid}/ordinal/{ordinal}/complete` | `StudyEventController` 的 Participate 完成动作 | 目标及状态授权；它不是通用字段写入或签署接口 |
| `/pages/auth/api/itemdata/**` | `applicationContext-security.xml` 明确 denyAll，禁用未使用且缺少研究检查的旧 SDV API | 保持拒绝；不为联调重新开放 |
| `/pages/openrosa/{studyOID}/submission?ecid=...` | 安全配置 permitAll；`OpenRosaSubmissionController` 使用 PFormCache 的 ecid 上下文和 participantPortal 条件，调用事务性服务 | 专用上下文的创建/寿命、跨目标隔离、事务和文件失败；不能当通用 Bridge API |
| SOAP，独立 ws 模块 | 上游 README 标 legacy、未持续测试和开发；资源副本独立于 core | 必须独立核验实际认证、授权、版本和错误，不能继承 REST 结论 |

实现路径前缀：controller 位于 `web/src/main/java/org/akaza/openclinica/controller/`，ODM/Jersey 资源及过滤器位于同模块 `web/restful/`、`web/filter/rest/`，安全配置位于 `web/src/main/resources/org/akaza/openclinica/applicationContext-security.xml` 及 `web/src/main/webapp/WEB-INF/{web.xml,pages-servlet.xml}`。permitAll 描述 Spring Security 这一层；不能据此推导端点没有其他校验，也不能据 API Key 过滤器推导端点已经正确实施对象授权。

## 可以先联调的人工 ODM 闭环

以下是待在隔离实例验证的操作协议，不是已验证自动能力：

1. 由有权限的人从目标研究导出实际元数据/表单定义，保存 OID、版本、访视重复序号、重复组/行和字段编码。外部候选值与 EDC 当前值分开存放。
2. Bridge 为合成材料生成候选值、证据位置、源材料版本和目标身份；审核者在其受控系统中完成身份认证及内容审核。不能只接收任意 reviewer 字符串。
3. 导出明确标为待导入的 ODM，绑定审核内容摘要及版本；源资料、候选值或目标版本改变时重新审核。人工再次确认目标与 EDC 当前值。
4. 有授权的 EDC 用户使用现有 `ImportCRFDataServlet` 和 `VerifyImportedCRFDataServlet` 流程预览/校验，并处理实际版本支持的校验、规则、状态及修改原因。若入口无法满足审计要求则停止该目标的导入，不能改用数据库写入。
5. 留存 EDC 导入结果并逐字段回读、核对重复行、状态、审计和错误。超时或结果不确定时先查询核对，不盲目重放。

人工写前确认和写后回读仍不能提供自动提交的并发控制，也不保证批次原子性或幂等性。ODM 导入是否可用于已完成数据、是否完整遵循修改原因与规则、部分失败如何恢复，都必须先实例验证；未验证时仅用于联调文件生成和人工审阅。

## 后续版本化契约要求（未实现）

候选契约使用独立版本命名，不扩张旧 API 的已保证范围。至少区分读取上下文、字段定义及当前值、预校验、人工审核授权后的提交、结果查询及逐字段回读。

每次提交必须明确研究/中心、受试者、访视定义与实例重复序号、CRF 版本、重复组/行和字段 OID；审查记录绑定候选内容、确认值、源材料版本及目标版本。服务端在提交事务中检查审核有效性、授权、状态及并发版本；写前读取和写后读取不是并发控制。需要可验证的 EDC 当前版本或同等可靠条件写入机制，不能只使用客户端摘要。

幂等键必须限定授权主体及目标，并绑定请求内容；同键异内容拒绝。结果应区分未执行、成功、失败和结果不确定，记录逐字段结果、原子性边界、可重试范围及恢复步骤。提交复用 EDC 类型/编码/日期/单位/完整性校验、规则和审计，已完成数据修改遵循修改原因。签署、CRA 核查及锁定不交由 Agent 自动完成。

发布自动写入能力前，需可执行测试阻断跨研究/中心/受试者、错误访视/行/版本、未审核/过期/替换内容、禁止状态、无效值；并验证并发修改、重复提交、超时、部分失败、恢复及源材料变更重审。以上是设计门槛，尚未实现或验证。

## Clinica EDC — Unreleased

- 新增简体中文九类资源包，覆盖全部 3,764 个上游键，保留角色/法律/编码值及格式契约；增加原文保留清单、中文日历和资源/日期回归。
- 修复差异记录汇总列固定英文名称导致中文表头与计数键不匹配的问题；类型编码与编号不变。
- 新增中文候选浏览器验收：合成受试者中文保存/搜索/回读、审计当前值与创建事件、浏览器打印 PDF 中文提取；英文界面回归通过。
- 合并旧 clinica/baseline 分支历史，保留开发 Compose 命名持久卷和迁移说明；候选验收仍使用隔离配置。

- 以固定 MIT 上游源码构建独立坐标的 PDFBox Layout，保留逐文件来源及法律文本，移除原 JitPack 依赖与仓库；严格 Maven 构建恢复通过。
- 增加源码篡改/生成转换校验、PDF 排版及实际 2FA 证书二维码回归；显式声明原有 ZXing 依赖并隔离测试日志。
- 增加 PostgreSQL 18 候选 WAR 私有部署准备、产物/工作树身份分离记录、候选浏览器与基础接口拒绝检查；Docker 支持显式源码标识而不复制 .git。
- 固定 LibreClinica `8b6b7cf3cbe10be884bdd5c47f3de9ad1eb0321f` 为候选开发基线，保留发布线原版对照与适用声明。
- Docker 改用摘要固定的 Maven 3.9.9 / JDK 11、Tomcat 9 / JDK 11；web WAR 名与 Docker COPY 对齐，严格检查依赖 checksum，支持代理 settings/信任库 BuildKit secrets。
- 新增实际 LocaleResolver/ResourceBundleProvider 的 13 项独立回归、PostgreSQL 18 中文/持久化/恢复探针及 CI。
- 新增校验官方 WAR 的本地原版实例准备、浏览器冒烟及停写备份恢复脚本，全部使用合成数据与私有测试目录。
- 补充项目定位、基线、工作流角色、本地化术语、接口联调、部署回退、状态验收、贡献与第三方许可记录。
- 首批曾阻塞：上游 JitPack `pdfbox2-layout:1.0.1` 的实际下载 SHA-1 与公布摘要不一致；本批已用受控源码构建替代，未关闭校验，未发布 Release。

## 1.4.0 (June 13, 2025)

**bugfix:**

* fixed textareas dropping entries > 2000 characters (#391)
* AuditLogEvent value length harmonisation (#431)
* Username DB column length hramonisation (#429)
* fixed data extraction SQL failing on PostgreSQL >= 15
* make SOAP study listAllRequest showing also child studies/sites for monitor users (#406)
* allow discrepancy notes to be added to empty fields (#404)
* fixed inconsistent preselection of discrepancy note type in repeating groups (#343) by setting it to Annotation for data entry (#414)
* make flags for discrepancy notes change to correct color (#417)
* not offer datamanagers with disabled accounts for configuration of new studies (#422)

**maintenance:**

* link for unsupported rule designer removed (#128)
* removed unused logos (#403, #410)

**enhancement:**

* ListUserAccountsView: provide information on parent study (#419)

**tests:**

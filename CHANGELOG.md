## Clinica EDC — Unreleased

- 固定 LibreClinica `8b6b7cf3cbe10be884bdd5c47f3de9ad1eb0321f` 为候选开发基线，保留发布线原版对照与适用声明。
- Docker 改用摘要固定的 Maven 3.9.9 / JDK 11、Tomcat 9 / JDK 11；web WAR 名与 Docker COPY 对齐，严格检查依赖 checksum，支持代理 settings/信任库 BuildKit secrets。
- 新增实际 LocaleResolver/ResourceBundleProvider 的 13 项独立回归、PostgreSQL 18 中文/持久化/恢复探针及 CI。
- 新增校验官方 WAR 的本地原版实例准备、浏览器冒烟及停写备份恢复脚本，全部使用合成数据与私有测试目录。
- 补充项目定位、基线、工作流角色、本地化术语、接口联调、部署回退、状态验收、贡献与第三方许可记录。
- 已知阻塞：上游 JitPack `pdfbox2-layout:1.0.1` 的实际下载 SHA-1 与公布摘要不一致，严格源码构建拒绝该依赖；未发布可信发行包。

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

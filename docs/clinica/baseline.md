# Clinica EDC 开发基线

Clinica EDC 是独立维护的 LibreClinica 中文增强发行版，不代表上游或获得其背书。Bridge 负责外部材料处理与候选值审核；EDC 负责研究数据和自身权限、校验、审计。这里尚未实现 Bridge 自动写入。

## 基线选择（2026-11-10）

开发起点固定为上游 `lc-develop` 的 `8b6b7cf3cbe10be884bdd5c47f3de9ad1eb0321f`。远程 Git 的默认分支也是 `lc-develop`；本仓库 GitHub 页面显示 forked from reliatec-gmbh/LibreClinica。上游 `lc-release-1.4.0` 和 `v1.4.0rc1` 均指向 `dd8c98ce237125b55ff8ca356a55f98ef55d6d39`；GitHub Release 标题为 LibreClinica v1.4.0，标签仍为 rc1。官网安装包与源码的对应关系尚未核实。

开发线包含发布线及其后的国际化资源整合、现代浏览器适配、已完成 CRF 的录入状态保护和 API 访问限制等修复；也包含更大变更，回归风险更高。选择它作为**研发候选基线**，以便使用这些修复和现有语言机制；不是稳定版本或生产部署推荐。发布线更接近上游发布内容，但缺少上述后续修复。`master` 不是默认开发线。固定提交不等于依赖、容器镜像和构建结果均已锁定；完整构建、安装和流程验证前不能宣称可重复部署。上游 README 推荐开发线 PostgreSQL 18、1.4.0 PostgreSQL 16；本仓库开发 Compose 使用 PostgreSQL 14，尚未验证其与该提交的兼容性。SOAP 被上游标记为 legacy、未持续测试和开发。

## 当前状态与验收缺口

- 已实现：开发 Compose 显式命名 PostgreSQL 与应用文件卷。仅用于合成数据的本地开发；默认数据库凭据不可用于公网或生产。
- 已测试：`docker compose config --quiet` 通过；独立 Compose 项目中 PostgreSQL 14 合成表在强制重建后保留，`pg_dump -Fc` 导出并恢复到另一数据库后中文值一致。这不验证 EDC 数据或应用文件恢复。
- 构建失败：原始 `docker build -t clinica-baseline:local .` 在 ODM 阶段解析 Maven Central 的 `jaxb2-maven-plugin:2.5.0` 时容器 DNS 失败。宿主的 Maven Central 请求成功，但 OpenClinica JFrog 请求被环境策略以 403 拒绝。尚未跑到应用编译、测试或安装。已按上游容器说明固定 web WAR 文件名以匹配 Dockerfile，尚未完成构建验证。
- 尚未验证：应用安装和完整研究流程、中文资源和往返、接口鉴权与写入、临床业务负向用例、应用级备份恢复、升级和回退。禁止将合成测试视为临床或生产验收。
- 下一步：在隔离环境建立原版研究、中心、用户、CRF、受试者、重复访视、录入/双录入、质疑、SDV、签署、冻结/锁定、审计和导出基线，逐项记录角色、状态、命令及结果；然后开展中文和接口工作。
- 外部依赖：Maven 包含 OpenClinica JFrog 和 JBoss 仓库；镜像标签和依赖未完全固定。构建网络、许可证及安全检查仍需逐版本核实。

上游 `LICENSE` 为 GPL-3.0 文本，`COPYING.LESSER` 为 LGPL-3.0 补充文本；具体文件和第三方依赖的许可及分发义务仍需逐项核对。保留上游版权和法律声明，不把许可合规等同研究使用验收。

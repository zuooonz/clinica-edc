# 上游与候选基线

核对日期：北京时间 2026-11-10。实际仓库 `/workspace/clinica-edc`，初始分支
`work`、干净工作区；未找到适用 AGENTS.md 或本项目先前开发记录，未见同仓库开发
进程、其他 worktree 或开发锁持有者。主执行随后持有 POSIX flock 开发锁。
没有创建、恢复或修改定时任务。

GitHub `repos/zuooonz/clinica-edc` 返回 `fork: true`，parent/source 均为
`reliatec-gmbh/LibreClinica`；双方默认分支均为 `lc-develop`。
保留 `origin`，新增 `upstream` 指向该上游，保留其开发/发布/master 跟踪分支和标签。
本项目工作分支为 `clinica/baseline-validation`。

| 候选 | 本次核实的精确提交 | 取舍 |
| --- | --- | --- |
| lc-develop | `8b6b7cf3cbe10be884bdd5c47f3de9ad1eb0321f` | 选择为候选开发/集成基线；较发布线多 54 提交，含 i18n 集中、日语实现、现代浏览器、已完成 CRF 的 IDE/DDE 准入、账户 API IP 默认拒绝及 Basic 认证修复；变更面更大，未经本项目完整回归 |
| lc-release-1.4.0 / v1.4.0rc1 | `dd8c98ce237125b55ff8ca356a55f98ef55d6d39` | GitHub Release 名为 LibreClinica v1.4.0，标签/资产名含 rc1；用于原版对照，缺开发线后续修复，不能直接当最终安全基线 |
| master | `bff9a39a6836cee1d4882e60f4e594bf49efccf0` | 保留上游，不因名称推定它最适合开发 |

关键后续提交：i18n 集中 `24c4a0b71`、已完成 CRF IDE/DDE 保护
`90af2cbe6` / `e8d5eb27a`、账户 API `0324ca2f3`、Basic API `45d546161`。
上游 Actions workflows/runs 及当前提交 check-runs 查询均未发现执行记录，不能说上游 CI 通过。

官网 [download.html](https://libreclinica.org/download.html) 标稳定版 1.4，要求 Java 11、
Tomcat 9、PostgreSQL 16。官网 `LibreClinica-web-1.4.0.war` 已实际下载并核对公布的
SHA-256：`25378635ab396195d2bc8d58ee2988383fccf0699d2c5222800c8a37524179c7`。
尚未与 GitHub rc1 资产做字节比较；不根据文件名认定完全相同。
开发线 README 要求 PostgreSQL 18，与原有 Compose 的 14 不一致。

本批将 Docker 构建改为摘要固定的 Maven 3.9.9/Java 11，Tomcat 固定到已实际拉取的
9.0.123 / Java 11.0.33 镜像摘要，稳定 web WAR 名以匹配已有 Docker COPY。
可复现工具链不代表依赖安全：完整可信构建、安装及业务回归的实际结果见
[状态](status.md)。上游版本号仍保持原值，不用重命名或改版本号掩盖来源。

本项目的后续差异包括源码供应替代：`org.clinica.vendor:pdfbox2-layout:1.0.1-clinica.1`
从 MIT 上游提交 `6902dd0568061afff4da1d05a4cbd5a7f2b9dc8f` 构建；详情见
[来源说明](../../vendor/pdfbox2-layout/README.md)。这不代表 JitPack 原二进制已被验证，
也不改变 LibreClinica 原有版本/法律文本。候选部署单独记录 WAR 摘要、内嵌版本、
changeset 及准备时的 Git SHA/dirty，不能用准备时工作树身份冒认产物来源。

本项目已整合 `clinica/baseline` 的两个历史提交（截至 `7e5c3ff6ae1b4045325811bf4d1b9eb653ca886b`），
保留根 Compose 命名数据卷、既有卷不自动迁移的说明，以及相同 WAR 名称修复。旧网络失败
记录属于历史环境，当前构建结果以本项目 CI 和开发状态为准；旧分支保留，不删除或强推。

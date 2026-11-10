# 开发状态与验收记录

记录日期：北京时间 2026-11-10。本批分支 `clinica/baseline-validation`；
基线选择与精确上游提交见[baseline.md](baseline.md)。所有数据均为合成或上游默认空研究。

## 接续批次：可信源码供应与候选安装

首批提交 `9f9a776acfeebfe543cbcabafd27c21f756cfa79` 的
[CI](https://github.com/zuooonz/clinica-edc/actions/runs/42789188383) 已完成：i18n、database 成功，
package 因下述 JitPack checksum 不一致失败。本批不接受或重新标记该异常二进制，
改为从 MIT 上游固定源码构建独立内部坐标，保留完整许可与来源。

| 本批项目 | 实际结果与范围 |
| --- | --- |
| 来源核验 | `ralfstuckert/pdfbox-layout@6902dd0568061afff4da1d05a4cbd5a7f2b9dc8f`；80 个原始文件保留原样；76 个 Java 的生成转换严格复现两处 import 替换及一个兼容类覆盖 |
| 自动来源门禁 | 固定 Git 树、blob、SHA-256、MIT 许可、文件集合及生成结果核验通过；7 项 Python 测试通过，包括篡改、删除、增加、清单改写、来源替换与生成源码变化 |
| 证书回归 | 3 项库排版/分页/位置/图像测试，1 项实际 TwoFactorService PDF 文本及嵌入二维码解码测试，14 项既有 TwoFactorServiceTest；合计 18 项零失败/错误/跳过；合成秘密与 PDF 仅在内存 |
| 模块重复构建 | 两次 clean package 的 binary JAR SHA-256 都为 `68f313136255f86f24d88888eb94372bab1b139c565846c75795bc40212e0b35`，sources JAR 都为 `a0b29245cb480ac09aef99ee49b7a9591d6e77e9028e6643250a32c15871f082`；不等于全应用字节可重复 |
| 候选产物门禁 | 5 项合成 ZIP 测试验证元数据解析，以及拒绝混入旧依赖、缺失/变化的 MIT 许可、错误源码提交和错误内部坐标；元数据解析不等于证明构建来源 |
| 严格源码 package | 本地 7 个模块成功，包括依赖分析；已去除原 JitPack 坐标/仓库，未放宽 checksum。新增测试直接使用的 ZXing 维持原 3.4.0 compile 版本/作用域并显式声明，测试日志落 target |
| 候选部署 | 本项目 source-built WAR，在 Tomcat 9.0.123/JDK 11.0.33/PostgreSQL 18.1 上新建隔离数据库并启动成功；loopback 18082、随机密码、私有配置和持久卷 |
| 候选浏览器 | 首次登录与强制改密、版本核对、首页、受试者矩阵、质疑与研究审计页面通过；截图只含默认空合成研究，zh-CN 仍英文回退 |
| 候选基础 API 拒绝 | 无凭据、Bearer、错误 Basic API Key 均 401；默认空账号 IP allowlist 为 403；已认证用户访问禁用 SDV API 为 403。未测试有效密钥、跨研究/中心或写入 |
| 产物身份 | 首次候选 WAR SHA-256 `efc700b540e373b9d03cd970e7a242b3a64b2c2fe4cb9250d349629e188edf12`，内嵌 changeset `9f9a776acfee`；构建发生在该父提交的修改工作树，prepare 时 dirty=true，不能宣称它是父提交原样产物或已发布版本 |
| Docker 完整构建 | 已成功生成本地镜像 `sha256:d00b848884a56b855a80888bc91a6157ecad5142d0db4e25b227aee960229c58`；源码标识显式 `9f9a776acfeebfe543cbcabafd27c21f756cfa79-dirty`，未复制 .git，严格校验和依赖分析均通过；未发布镜像或 Release |

本批准确提交的远程 CI 在提交推送后单独核对，以最终简报为准。
独立审查已覆盖供应来源、许可证、生成转换、内部坐标、CI 和镜像元数据；未运行上游
全部 PDF 示例/像素基准，也未完成 EDC 旧数据库测试或完整研究业务回归。

## 已实现

- 固定上游候选开发版本并保留发布线；补齐项目边界、源码工作流/角色、接口盘点、术语、许可及贡献要求。
- 修正 Docker 的 Java 8 构建工具链为 JDK 11，固定构建/运行镜像摘要，稳定 web WAR 名，加入严格 checksum 与可选 BuildKit 网络配置 secrets。
- 独立 i18n 测试直接编译两个实际生产类，不重写生产逻辑，不连接数据库。
- PostgreSQL 18.1 无端口临时探针：独立预期值、中文长文本、数值/日期/时区、持久化和恢复。
- 官方 1.4 WAR 校验与原版测试实例准备（随机数据库密码、私有文件、唯一项目和持久卷）。
- 原版浏览器登录/改密/只读页面测试、停写备份与独立恢复项目的表/序列/文件一致性及恢复后浏览器测试。
- GitHub Actions 三个独立 job：i18n、database、package。package 不跳过 checksum 错误；旧 reactor 测试默认被父 POM 跳过。

## 本地测试通过

| 验证 | 实际结果与范围 |
| --- | --- |
| JDK 11 / Maven 3.9.9 独立 i18n | 13 tests，0 failures/errors/skips；浏览器语言顺序、日语地区、session、整包/单键回退、Unicode、法律声明和日期配置 |
| PostgreSQL 18.1 探针 | 中文、长文本外部长度/MD5 预期、小数、日期、时刻、重启持久化、独立数据库 dump/restore 均一致；本次容器/卷已清理 |
| 官网原版 WAR | SHA-256 与官网公布值一致；安装后页面版本 `1.4.0rc1` / changeset `dd8c98ce2371` |
| 原版隔离安装 | Tomcat 9.0.123 / JDK 11.0.33 / PostgreSQL 16.15；自动初始化数据库（databasechangelog 1010 条），默认空研究可登录，初始 root 密码已经正常 UI 随机改密 |
| 原版 Chromium 冒烟 | zh-CN 浏览器下英文回退；登录、研究首页、受试者矩阵、质疑、研究审计页面通过；截图已检查，只含空合成研究 |
| 应用停写备份恢复 | 新项目/空卷恢复；110 张 public 表的行数/排序内容指纹、81 个序列状态、应用文件 SHA-256 相同（原版文件目录有 17 个文件）；恢复实例浏览器检查通过。临时恢复项目及卷已删除 |
| 普通诊断源码构建 | 全部 6 个模块完成 package，生成稳定路径 `web/target/LibreClinica-web.war`；**有依赖 checksum 警告，不作为可信构建通过** |
| 私有文件/审查 | 生成配置、compose 和凭据文件 0600，work/、备份、node_modules 不入 Git；独立代码审查意见已修正容器清理、外部长文本预期与项目隔离等问题 |
| 审查后验证 | 冲突 `COMPOSE_PROJECT_NAME=clinica-original` 下再次恢复通过，原项目未被覆盖/删除；Python/Node 语法、POM XML、文档链接、staged diff 与 Dockerfile build check 通过（后者零警告） |

运行证据在本会话 ignored `work/`；不提交日志、截图、数据库或备份。截图、探针与空研究
不证明临床准确性、医院兼容性、生产就绪或完整业务验收。

## 首批失败证据与处理

严格构建 `mvn -B -ntp -C -U package` 在 Core 依赖解析阶段按预期阻断：

```
com.github.ralfstuckert.pdfbox-layout:pdfbox2-layout:1.0.1
JitPack published SHA-1: 3950e41c0b58188f4ea4d52119679baeb8f10d49
Downloaded SHA-1:       52c6d78a9761001a60ca7f7580221261d68cd600
```

JFrog 备用来源返回 401，JBoss 与 Maven Central 未找到相同坐标。曾下载的该 JAR
已隔离至私有 work/；没有关闭 checksum、伪造远端摘要或部署诊断产物。
本批已完成固定源码提交的独立核验与受控内部模块构建，移除旧依赖，不再等待 JitPack
修正。异常 JAR 的远端 checksum 问题本身仍未被解释或修复；不能因此重新信任该旧制品。

原环境 Maven 未自动读取 HTTP 代理；显式 settings 后，提取的 JDK 又需使用环境系统
信任库。两项已在本会话配置，不将环境 secrets 写入仓库，不关闭 TLS。
GitHub Actions 权限设置查询返回 HTTP 403，未重试/绕过；不影响本地验证，
远程 Actions 是否触发及结果须以准确提交对应运行核对。

## 尚未验证与优先级

- [x] P0：建立可核验的源码依赖供应，本地严格 package 和 PostgreSQL 18 候选应用安装通过。
- [x] P0：完整 Docker build 通过。
- [ ] P0：核对本批准确提交的 CI，处理发现的失败。
- [ ] P0：在候选源码实例建立虚构研究/两个中心、角色账户、表单多版本及重复访视/重复行，走完整录入、双录入、质疑、SDV、签署、冻结/锁定、审计与导出流程。
- [ ] P0：逐项阻断跨研究/中心/受试者、错误访视/重复行/表单版本、非法状态、编码/日期/单位和不完整提交；验证原有规则与修改原因审计。
- [ ] P1：中文核心九包、占位符/法律保护/缺键检查、页面显示与中文输入保存/查询/导入导出/打印/审计端到端验证。
- [ ] P1：实测所有现有接口认证、授权及错误行为，先完成授权人工 ODM 导入/逐字段回读闭环；源码调查不等于接口实例测试通过。
- [ ] P1：Bridge 版本化预校验/受控提交契约与可执行负向测试：可信审核身份、内容/来源/目标版本绑定、审核过期、提交时并发控制、幂等/超时/部分失败恢复。当前未提供自动写入能力。
- [ ] P1：依赖/镜像/前端资源 SBOM 与可复现漏洞检查、跨版本迁移/回退、含附件的完整研究备份恢复。

本批不改变录入业务逻辑、签署/SDV/锁定权限或 ODM 语义；不发布 Release。
本批测试 app/db 已停止，恢复测试容器/卷已清理；原版与候选持久卷、私有配置及原版合成备份保留在本地供接续。
开发锁持有至提交推送结束后释放。GitHub 提交/PR/CI 以会话最终简报和远程记录为准，
CI 未完成或失败不得记作通过。没有创建后台常驻开发或定时通知任务。

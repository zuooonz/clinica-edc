# 第三方来源与许可证记录

Clinica EDC 基于 LibreClinica 独立维护，不代表 LibreClinica、OpenClinica 或 ReliaTec 的官方发行、认证、赞助或背书。保留上游版权、商标说明、免责声明和许可证。软件许可合规不等于临床研究使用验收。

## 当前源码中的实际声明

核对基线：`8b6b7cf3cbe10be884bdd5c47f3de9ad1eb0321f`。

| 位置 | 当前内容 |
| --- | --- |
| 根目录 `LICENSE` | GNU GPL version 3 完整文本 |
| 根目录 `COPYING.LESSER` | GNU LGPL version 3 补充文本，明确纳入 GPL v3 条款 |
| `core/LICENSE.txt`、`web/LICENSE.txt`、`ws/LICENSE.txt` | LGPL v2.1 “or (at your option) any later version” 声明及文本 |
| Java 文件头 | 通常声明 GNU LGPL，并列出 Akaza Research/OpenClinica/LibreClinica 年份；具体文件仍按各自声明核对 |
| `core/src/main/resources/org/akaza/openclinica/i18n/licensing.properties` | 有 DO NOT CHANGE 标记；同时出现 LGPLv2.1 字样与 version 3.0 链接、ReliaTec 注册商标和独立实例免责声明 |
| [上游下载页](https://libreclinica.org/download.html) | 声明 LGPL version 3.0；不能代替逐文件及依赖许可核验 |

这些不同层次的历史声明尚未由本项目统一或重写。LGPL v2.1-or-later 与仓库 v3 文本不能简单概括成“全部 GPLv3”或擅自改为其他许可证；遇到不一致应保留证据、按实际分发组件及版权来源核实，通用修复优先向上游报告。本批不更改这些文件或法律文本。

## 分发与公开材料

分发修改过的 LGPL 组件或组合产物时，按适用具体版本保留许可和版权、注明修改，提供相应源码及必要构建/安装材料，并核对接收者修改/重新链接所需机制等义务。镜像/WAR 分发同样需要检查；不能因为在公开 GitHub 存有部分源码就默认完成全部交付义务。此处记录工程检查要求，不声称已经完成发行许可审查。

公开仓库、截图、测试日志、CI 附件和示例只能含允许公开的代码、配置模板及明确标为合成的数据。禁止提交真实病例、报告、生产数据库/备份、凭据、证书私钥和非公开研究材料。真实数据不发送外部模型。

## 依赖和资源清单状态

Maven 依赖声明位于根目录及 `core/odm/web/ws/docs` 的 POM；前端另有直接纳入仓库的脚本、图片、字体和日历语言资源，不能只扫描 Maven。当前**尚未完成**解析后的依赖版本、传递依赖、SBOM、全部资源许可及可再分发性检查，也未形成漏洞不存在的结论。

后续每个新增/升级依赖或翻译来源须同批记录组件、精确版本/提交、来源 URL、许可证及 NOTICE、修改与分发方式。术语优先自行编写并保留出处；不复制未核实许可的临床词表。发行前对构建实际使用的 Maven/JDK/Tomcat/PostgreSQL/镜像版本分别记录来源、摘要和许可，而不是使用浮动版本名称作为证据。

## 本批测试/构建工具

| 组件 | 精确版本/来源 | 许可与用途 |
| --- | --- | --- |
| Maven | 3.9.9，Apache Maven / 官方 Maven 镜像（摘要见 Dockerfile） | Apache-2.0；构建工具 |
| Eclipse Temurin | 构建镜像 JDK 11.0.27；运行镜像 JDK 11.0.33 | OpenJDK GPL-2.0 with Classpath Exception；不修改 JDK |
| Tomcat | 9.0.123，官方镜像摘要见部署文档 | Apache-2.0；镜像其他层需独立核实 |
| PostgreSQL | 探针 18.1、原版镜像 PostgreSQL 16 系列，摘要见脚本 | PostgreSQL License；镜像其他层需独立核实 |
| Spring WebMVC / Test | 5.1.4.RELEASE，Maven Central | Apache-2.0；与应用 Spring 版本一致，仅独立测试引入 spring-test |
| Servlet API | javax.servlet-api 3.0.1，Maven Central | CDDL-1.0 / GPL-2.0 with Classpath Exception；独立测试编译依赖 |
| JUnit | 4.13.2，Maven Central | EPL-1.0；独立测试依赖 |
| Playwright / playwright-core | 1.62.1，registry.npmjs.org；完整性锁定于 package-lock | Apache-2.0；开发测试依赖，不加入 EDC WAR |
| fsevents | 2.3.2，可选 macOS npm 传递依赖 | MIT；Linux 不安装 |

本表记录新增工具的已知直接许可，不是全部传递依赖或镜像 SBOM。自动下载 Chromium 时还须
保留浏览器自身的 LICENSE/credits；本次使用云端系统 Chromium，仅生成合成测试截图，未分发浏览器。

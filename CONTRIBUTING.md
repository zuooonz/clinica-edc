# 贡献 Clinica EDC

本项目独立维护 LibreClinica 中文增强与受控接口。开发前阅读
[基线](docs/clinica/baseline.md)、[状态](docs/clinica/status.md)和
[许可](docs/clinica/third-party.md)。上游通用修复优先考虑独立贡献。

保留上游分支与法律声明，从项目开发分支创建小范围功能分支。先检查
`git status --short --branch`、worktree、进程和实际持有的锁，不覆盖他人工作。
本地协作统一使用 `flock -n .git/clinica-development.lock <command>`；
长任务须在工作期间持有文件描述符，状态文件或时间戳不构成互斥。
该锁仅协调共享此 Git 目录的参与者，不能替代 GitHub 上的分支协作。

验证命令：

```sh
mvn -B -ntp -C -f tools/i18n-baseline/pom.xml clean test
python3 tools/check_database_baseline.py
python3 tools/verify_pdfbox_layout.py
mvn -B -ntp -C -pl core -am -DskipTests=false \
  -Dtest=PdfBoxLayoutTest,TwoFactorServiceTest,TwoFactorCertificateTest \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test
python3 tools/verify_pdfbox_layout.py --generated vendor/pdfbox2-layout/target/generated-sources/pdfbox2
python3 -m unittest discover -s tools/tests -v
mvn -B -ntp -C package
git diff --check
```

使用 JDK 11、Maven 3.9.9；Docker 测试仅使用合成数据。父 POM 默认跳过旧数据库测试，
`package` 成功不能写成旧测试通过；完整工作流须另行验收。
云端代理须配置 Maven settings 和受信 CA，不提交环境 settings、凭据或关闭 TLS 校验。
依赖校验不一致应停止可信构建并调查，不能用关闭 checksum 校验消除失败。
`vendor/pdfbox2-layout/upstream` 是保留原样的固定 MIT 来源，更新必须同时核实精确提交、
原始 Git 树、清单、生成转换、法律文本和证书回归；不得手工修改 vendored 源码来绕过校验。

行为、依赖、配置、命令和接口变化须同批更新文档、CHANGELOG 及相关可执行测试。
提交前检查 staged diff，确保无病例、报告、数据库、备份、凭据、私钥及非公开材料。
记录通过/失败/未验证/外部阻塞，普通推送后核对**准确提交 SHA**的 Actions。
不强推、不擅自发 Release、不把合成演练描述为生产或临床验收。

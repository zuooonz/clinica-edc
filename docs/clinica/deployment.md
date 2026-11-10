# 隔离部署、备份恢复与回退

仅使用合成数据。原有 `compose.yaml` 是上游开发示例，仍含固定演示数据库密码和
PostgreSQL 14，不作为本项目验收部署。本批提供精确固定的**原版对照实例**，
与候选源码构建分开。它不是生产部署，也不包含已知后续开发线修复。

## 原版对照实例

要求 Docker/Compose、Python 3、curl、Node 与浏览器。执行前检查 18080 端口及现有
容器/开发进程。脚本生成随机数据库密码、唯一 Compose 项目名、私有配置和持久卷；
不发布数据库端口，应用仅绑定 `127.0.0.1:18080`，邮件指向本机无服务端口。

```sh
python3 tools/prepare_original_baseline.py
docker compose -f work/original/compose.json up -d
docker compose -f work/original/compose.json ps
curl --max-time 15 -L http://127.0.0.1:18080/LibreClinica/
```

脚本拒绝覆盖已有 `work/original`，下载失败或摘要不符也停止；保留失败目录供检查，
不要在其中已有实例时重新生成密码。只读挂载校验过的 WAR 和私有配置；数据库、
应用文件和日志分别用命名卷持久化。`down` 保留卷；不要对需保留的实例执行 `down -v`。
本次最初手动使用的项目名为 `clinica-original`，已写入该会话私有 compose 文件；
后续新生成项目使用随机名称。

官网 1.4 WAR 校验摘要见[基线](baseline.md)。其页面实际显示
`1.4.0rc1 / dd8c98ce2371`。原版使用 PostgreSQL 16 镜像摘要
`aa6cb33cf4c19c765cefcf48d96176258800870a9a1e930ad126200f9d825139`，
Tomcat 9.0.123 / Java 11.0.33 摘要
`cf13b145adf437e0c30ff3238016b09f9e904d2d75a714894ced0c8b7ea61554`。
这些固定值须受控更新，不能据此宣称没有漏洞。

## 浏览器冒烟检查

```sh
npm --prefix tools/browser-baseline ci --ignore-scripts
npm --prefix tools/browser-baseline exec -- playwright install chromium
node tools/browser-baseline/smoke.cjs
```

已有系统 Chromium 可设置 `CLINICA_CHROMIUM=/usr/bin/chromium`。
本云端沙箱如需 `CLINICA_BROWSER_NO_SANDBOX=1`，仅用于隔离合成测试，不能推广为生产配置。
测试只允许 loopback HTTP 目标。首次用上游默认 root 登录并完成强制改密，将随机新密码
写入 `work/original/admin.json`（0600），不输出秘密；之后复用该文件。
首次改密超时需人工核实是否成功，私有文件记录的是拟提交的新密码，不代表服务端已成功。
浏览器检查原版 changeset、角色、首页、受试者矩阵、质疑和研究审计页面；
`zh-CN` 当前回退英文，不能记录为中文 UI 已通过。
截图仅存 ignored work/，公开前须逐张审查。

## 数据库探针

```sh
python3 tools/check_database_baseline.py
```

精确固定 PostgreSQL 18.1 镜像（开发候选线要求），创建本次独有容器/卷且不映射端口，
以独立预期值验证 UTF-8 中文长文本、日期、UTC/+08 时刻和小数；重启后及独立数据库
恢复后逐值一致。正常退出清理本次资源，清理失败明确报错。
这项测试不涉及 EDC 表结构、权限、表单或审计，不代替应用恢复。
两项 Python Docker 检查固定使用本机 `/var/run/docker.sock`，清除远程 context/TLS
选择变量但保留 registry 配置，避免误操作远程 daemon；其他 socket 的环境须先适配并审查。

## 应用级备份与恢复演练

每次备份先停所有可能写入的应用/导入任务，保持数据库运行；在同一停写窗口内备份
数据库、`libreclinica.data`、任何额外附件目录、配置与确切 WAR/镜像标识。
私有备份目录先 `umask 077`，绝不提交仓库或上传 CI。
下面以脚本生成的实例为例，Compose app 也可在停止后用一次性容器读取其数据卷：

```sh
umask 077
mkdir -p work/original/backup
docker compose -f work/original/compose.json stop app
docker compose -f work/original/compose.json exec -T db \
  pg_dump -U clinica -d libreclinica -Fc --no-owner --no-acl \
  > work/original/backup/database.dump
docker compose -f work/original/compose.json run --rm -T --no-deps \
  --entrypoint tar app -C /usr/local/tomcat/libreclinica.data -czf - . \
  > work/original/backup/files.tar.gz
docker compose -f work/original/compose.json start app
```

恢复必须在**另一唯一 Compose 项目/全新卷**先启动 db，确认健康；同版本 PostgreSQL
用 `pg_restore --exit-on-error --no-owner --no-acl` 恢复到空数据库，再用临时 app 容器
`tar -xzf -` 恢复文件卷，最后启动 app。私有配置要保持数据库身份和文件路径匹配，
恢复实例换用另一个 loopback 端口，禁用真实邮件、外部接口和定时业务导出。
不得向正在运行的 EDC 数据库覆盖恢复。

本项目准备的空合成实例可执行完整的停写/指纹比较/恢复浏览器检查：

```sh
python3 tools/check_original_restore.py
```

它要求原实例 app/db 已运行且浏览器已初始化密码，使用另一个随机项目及 18081 端口，
结束时清理本次恢复项目/卷并重新启动原实例。备份保留在私有 work/original 下。
失败时会尽力清理和重启并报告错误，不保证异常后服务仍健康，须检查 `compose ps`。
源/目标均以显式 `-p` 固定项目，拒绝外部卷和额外挂载，不受 `COMPOSE_PROJECT_NAME` 覆盖。

本次实际停写备份与恢复结果见[状态](status.md)：比较应用 public 全表行内容指纹、
序列状态及文件 SHA-256 后，再浏览器登录恢复实例。该空合成研究演练不能证明真实
研究的大文件、外部存储、跨版本迁移或并发在线备份一致性。

## 源码构建、升级和回退

源码构建使用 JDK 11 / Maven 3.9.9，`mvn -B -ntp -C package`。
父 POM 默认跳过旧数据库测试，必须另行选择和配置测试数据库。
Docker 构建支持可选 BuildKit `maven_settings`、`java_truststore` secret：

```sh
docker build --secret id=maven_settings,src=work/container-maven-settings.xml \
  --secret id=java_truststore,src=/etc/ssl/certs/java/cacerts -t clinica-edc:test .
```

仅在网络确需时提供 secrets，settings 中的代理必须能从构建容器访问；
保持环境继承的代理和 CA，不提交凭据，不关闭 TLS。
当前严格构建受上游 JitPack checksum 问题阻断；不得把普通 Maven 警告后的成功包当可信包部署。
WAR 名固定为 `web/target/LibreClinica-web.war`，已验证普通诊断构建会生成此路径。

升级前保存匹配的 WAR、配置、数据库与文件快照，并在隔离副本验证 Liquibase 和完整业务回归。
不能假定旧 WAR 可直接连接新 schema；回退须恢复成对快照并验证审计、附件和导出一致。
本批未验证候选开发线升级/降级，不发布 Release，也不保留会话结束后常驻服务的承诺。

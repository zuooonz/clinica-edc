# PDFBox Layout 1.0.1 — 本项目源码构建

来源：<https://github.com/ralfstuckert/pdfbox-layout>，标签 `1.0.1`，精确提交
`6902dd0568061afff4da1d05a4cbd5a7f2b9dc8f`。作者 Ralf Stuckert，MIT 许可，完整文本在
`upstream/LICENSE`，随二进制和源码包保留。`provenance.json` 记录原始路径、Git blob 与 SHA-256。

`upstream/` 的 80 个文件保持原样：76 个公共 Java 源文件、PDFBox 2 兼容类、LICENSE
及两份构建脚本。没有引入上游 wrapper、二进制示例或 PDFBox 1 实现。独立审查确认
main 没有额外资源、网络访问或后台任务；WordBreakerFactory 原有 JVM 属性反射行为未改变。
`.gitattributes` 仅对这个 frozen upstream 目录保留原始换行/空白，避免格式化改变来源字节；
该目录以来源校验代替空白整理，其他项目代码继续接受正常的空白检查。

本模块使用独立坐标 `org.clinica.vendor:pdfbox2-layout:1.0.1-clinica.1`，不冒充上游/JitPack
二进制。其原因是旧 `com.github.ralfstuckert.pdfbox-layout:pdfbox2-layout:1.0.1` 的远端
checksum 与下载内容不一致，严格 Maven 构建拒绝该包。保留严格校验，不覆盖旧坐标缓存。

Maven 只复现上游 `pdfbox2-layout/build.gradle` 的 main 源码步骤：

1. 复制公共源码到 target/generated-sources/pdfbox2。
2. 替换 PDPageContentStream 和 PDXObject 两处旧包路径为 PDFBox 2 包路径。
3. 用原版 PDFBox 2 CompatibilityHelper 覆盖公共兼容类。

输出仍为 76 个 Java 文件；使用与 EDC 当前相同的 PDFBox 2.0.31（替代上游构建的 2.0.4）。
不改 EDC 证书文字、字体、二维码或身份验证流程。只构建普通 WAR classpath 所需 JAR，
不复制上游 Gradle 的 OSGi bundle 元数据、examples 生成或完整图像比较测试。
二进制含 META-INF/pdfbox-layout 下的许可、来源说明和清单；同时生成 sources JAR。

```sh
python3 tools/verify_pdfbox_layout.py
mvn -B -ntp -C -f vendor/pdfbox2-layout/pom.xml clean verify
python3 tools/verify_pdfbox_layout.py --generated vendor/pdfbox2-layout/target/generated-sources/pdfbox2
```

单独模块测试和实际 TwoFactorService 证书回归覆盖合成 PDF 的文字/图像/分页与二维码。
它们不是上游完整示例测试或中文证书验收；Courier 的现有字符覆盖限制仍需后续处理。
升级必须重新核实提交、许可、清单、生成转换和 EDC 证书行为，不手工修改 frozen upstream 源码。

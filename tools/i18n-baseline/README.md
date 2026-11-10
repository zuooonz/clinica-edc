# 数据库无关的国际化行为基线

在仓库根目录使用 JDK 11、Maven 3.9.9 运行：

```sh
mvn -B -ntp -C -f tools/i18n-baseline/pom.xml clean test
```

这个独立模块直接编译仓库中的 `LocaleResolver` 和 `ResourceBundleProvider`，加载 core 中未经过 Maven 过滤的原始资源。它没有复制生产逻辑，不加入应用 reactor，也不连接数据库。Spring 版本与当前应用一致。

测试覆盖浏览器语言顺序、英语回退、日语地区变体、session 优先级、九包资格检查、单键回退、Unicode 解码、法律声明回退和现有日期配置保持。`zz` 是只在测试 classpath 中使用的合成不完整语言包（缺少 workflow），不会进入应用 WAR。

中文测试额外核对 zh/zh-CN/zh-Hans-CN 的真实语言选择、3,764 个自身键、占位符/HTML/实体、
非空及原文保留清单、格式配置、11 个受保护角色与法律/版本值、ASCII Unicode 转义。
`zh-preserved-keys.txt` 仅用于测试；不能通过新增豁免掩盖应翻译的自然语言回退。

通过仅表明这些 Java 类和资源的行为符合断言；不代表应用构建成功、资源过滤后 WAR 验证、中文包可用、中文输入保存/导出、完整日期校验、数据库迁移、权限或临床工作流验收通过。日期测试使用显式严格解析，不能代替各页面真实的验证器检查。

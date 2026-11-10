# 中文术语与本地化基线

本项目已提供简体中文 Web 界面预览。浏览器首选语言设为简体中文（Accept-Language
`zh-CN`），使用现有 LocaleResolver 自动选择；`zh`、`zh-CN`、`zh-Hans-CN` 均有自动测试。
英文和日语保留。本批不改变数据库身份、类型编码、研究规则或时间语义。

当前 core 的 `org/akaza/openclinica/i18n` 是 web 使用的九类资源包规范位置：
admin、audit_events、exceptions、format、notes、page_messages、terms、words、workflow。
`LocaleResolver` 要求九包均能解析到相同语言，缺包会整语言回退；缺单键则回退英文。
`ws` 的独立历史资源被冻结；页面仍有 JSP/JavaScript/图片/日历和硬编码来源，不能只统计 properties。
详见 core 资源目录 README。中文包参考现有日语包的集成方式，所有键按英文实际资源逐项翻译，
不通过空包或英文父包回退凑覆盖。全部 3,764 个唯一键在中文文件中直接存在：3,632 个值含中文，
130 个值经审查与英文相同（角色、法律、技术/产品标识、配置和遗留源条目）；另两项为日历路径等差异。
保留项清单在 `tools/i18n-baseline/src/test/resources/zh-preserved-keys.txt`，增减需要复核。

| English | 中文术语 | 区分 |
| --- | --- | --- |
| Study / Site | 研究 / 中心 | 子研究关系不等于独立租户保证 |
| Subject / Study Subject | 受试者 / 研究受试者 | 绑定研究内身份及 OID |
| Study Event / Event Definition | 访视实例 / 访视定义 | 重复序号须单独保留 |
| CRF / CRF Version | 病例报告表 / 表单版本 | 显示名不替代版本身份 |
| Initial / Double Data Entry | 初次录入 / 双录入 | 第二录入岗位与账户必须另核实 |
| Discrepancy Note / Query | 差异记录 / 质疑 | 注释、质疑、修改原因有不同类型 |
| SDV | 源数据核查 | 不由 Agent 自动确认 |
| Sign / Freeze / Lock | 签署 / 冻结 / 锁定 | 不混用不同实体的状态码 |
| Audit Trail / Reason for Change | 审计追踪 / 修改原因 | UI 翻译不改变审计语义 |

新增中文资源使用无歧义 `\uXXXX` 编码，保留占位符、HTML/格式标记、编码值与构建占位符。
licensing 全文、要求保留的角色名称和页脚法律文本不得翻译。
日期/电话配置保持英文原值，仅日历脚本指向 `calendar-zh.js`。原应用按请求语言生成/解析
MMM；中文月份为 `1月` 至 `12月`，日历与 Java 实际 formatter 一致，仍使用 `dd-MMM-yyyy`。
不修改研究编码、单位、数据库标识、ODM 命名空间、时区或所代表的日期/时刻。

## 已验证

- 独立资源测试：中文语言选择、完整自身键集合、非空、占位符、HTML 标签/属性/实体、Unicode 转义、受保护值及原文保留清单。
- 实际 I18nFormatUtil：12 个月日期往返、闰日、时间戳与 ODM 模式；实际日历脚本：中英文月份、三个时区、字典及许可片段。
- PostgreSQL 18 候选：中文登录/首页/矩阵/质疑/审计；通过 UI 创建唯一合成受试者，中文次要标识、出生/入组日期经搜索查看页回读。
- 审计页显示中文当前值与创建事件；创建事件未提供逐字段旧/新值，不扩大为字段修改审计已验收。
- 浏览器打印 PDF 的中文可经 pdftotext 提取，截图已检查；英文页面及五项基础 API 拒绝回归通过。
- 差异记录汇总使用当前语言的显示名，计数列不再因固定英文表头而丢失；英文机器映射保持不变。

运行：

```sh
mvn -B -ntp -C -f tools/i18n-baseline/pom.xml clean test
node tools/browser-baseline/calendar-test.cjs
node tools/browser-baseline/chinese.cjs
```

浏览器验收需先准备/启动隔离候选并完成 root 初始改密，安装 Playwright、Chromium 与 pdftotext，
且浏览器环境须有中文字形字体。`CLINICA_CHROMIUM` 可指定系统 Chromium；云端隔离沙箱如需
`CLINICA_BROWSER_NO_SANDBOX=1`，仅用于该合成测试。运行物都留在 ignored work/。

## 验收边界

建库、CRF 多版本、重复访视/行、录入/双录入、SDV、签署、锁定、导入导出等资源已翻译，
但完整业务及权限负向矩阵尚未全部实例验收。长文本、所有弹窗/小屏、CRF 导出和服务器生成的
2FA PDF 中文姓名仍需验证；本批打印证明限于浏览器页面 PDF。SOAP 独立资源未翻译或验收。
业务中已有的研究名/编码等数据不自动翻译；英语角色及法律页脚是有意保留。

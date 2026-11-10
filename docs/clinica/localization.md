# 中文术语与本地化基线

本批没有激活中文包，也没有修改业务规则。原版行为先由
[独立测试](../../tools/i18n-baseline/README.md)固定。

当前 core 的 `org/akaza/openclinica/i18n` 是 web 使用的九类资源包规范位置：
admin、audit_events、exceptions、format、notes、page_messages、terms、words、workflow。
`LocaleResolver` 要求九包均能解析到相同语言，缺包会整语言回退；缺单键则回退英文。
`ws` 的独立历史资源被冻结；页面仍有 JSP/JavaScript/图片/日历和硬编码来源，不能只统计 properties。
详见 core 资源目录 README。现有参考翻译是日语；没有预先创建九个中文空包来宣称中文可用。

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
日期配置先保持英文原值，不自行修改时间语义、数据库标识、研究编码、单位、ODM 命名空间。

后续优先覆盖建库、受试者、访视、录入、质疑、SDV、签署、锁定和导出，增加资源缺键/
占位符检查，再逐页验收英文回退、长文本溢出与错误提示。中文输入保存、查询、导入导出、
打印、审计及长文本往返、日期/时区/小数仍需真实应用实例测试；数据库探针不能替代这些验收。

/* Loopback-only synthetic candidate UI and Chinese text round-trip. */
'use strict';
const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const { execFileSync } = require('node:child_process');

const root = path.resolve(__dirname, '../..');
const target = path.join(root, 'work/candidate');
const output = path.join(root, 'work/zh');
const base = new URL(process.env.CLINICA_BASE_URL || 'http://127.0.0.1:18082/LibreClinica/');
assert.equal(base.protocol, 'http:');
assert(['127.0.0.1', 'localhost', '[::1]'].includes(base.hostname), 'Only loopback is allowed');
const info = JSON.parse(fs.readFileSync(path.join(target, 'build-info.json'), 'utf8'));
assert.equal(crypto.createHash('sha256').update(fs.readFileSync(path.join(target, 'LibreClinica.war'))).digest('hex'), info.war_sha256);
const auth = JSON.parse(fs.readFileSync(path.join(target, 'admin.json'), 'utf8'));
const label = 'SYNTH-ZH-001';
const secondary = '合成中文往返甲';
process.umask(0o077);
fs.mkdirSync(output, { recursive: true, mode: 0o700 });
fs.chmodSync(output, 0o700);
const localTarget = value => {
  const url = new URL(value, base);
  assert.equal(url.protocol, 'http:', 'Synthetic checks only allow local HTTP');
  assert(['127.0.0.1', 'localhost', '[::1]'].includes(url.hostname), 'Non-loopback target rejected');
  assert.equal(url.port, base.port, 'Other local services are outside the synthetic candidate');
  return url.href;
};

(async () => {
  const browser = await chromium.launch({ headless: true,
    ...(process.env.CLINICA_CHROMIUM ? { executablePath: process.env.CLINICA_CHROMIUM } : {}),
    ...(process.env.CLINICA_BROWSER_NO_SANDBOX === '1' ? { args: ['--no-sandbox'] } : {}) });
  const page = await browser.newPage({ locale: 'zh-CN' });
  page.setDefaultTimeout(30000);
  await page.context().route('**/*', async route => {
    const url = route.request().url();
    if (url.startsWith('data:') || url.startsWith('about:')) return route.continue();
    try { localTarget(url); } catch { return route.abort('blockedbyclient'); }
    return route.continue();
  });
  const goto = async route => {
    const response = await page.goto(localTarget(route));
    localTarget(page.url());
    assert(response && response.status() < 400, `${route}: HTTP failure`);
    assert.equal(await page.locator('#j_password').count(), 0, `${route}: session lost`);
  };
  try {
    await page.goto(localTarget(base.href));
    await page.locator('#username').fill(auth.username);
    await page.locator('#j_password').fill(auth.password);
    await page.locator('input.loginbutton').click();
    await page.getByText('Default Study (default-study)', { exact: false }).first().waitFor();
    await page.waitForFunction(({version, release}) => document.body.innerText.includes(version) && document.body.innerText.includes(release), { version: info.version, release: info.artifact_release });
    let body = await page.locator('body').innerText();
    for (const text of ['首页', '受试者矩阵', '研究审计追踪', 'Study Director']) assert(body.includes(text), text);
    assert.match(body, /差异记录\s*(?:与|&)\s*质疑/);
    await page.screenshot({ path: path.join(output, 'chinese-home.png'), fullPage: true });
    for (const [route, text] of [['ListStudySubjects', '以下研究的受试者矩阵'], ['ViewNotes?module=submit', '解决状态'], ['StudyAuditLog', '查看研究追踪']]) {
      await goto(route);
      assert((await page.locator('body').innerText()).includes(text), `${route}: missing ${text}`);
    }
    await goto('ViewNotes?module=submit');
    const summary = page.locator('table.summaryTable');
    for (const text of ['质疑', '校验未通过', '修改原因', '注释', '总计']) {
      assert((await summary.innerText()).includes(text), `Localized summary missing ${text}`);
    }
    const countRows = summary.locator('tr').filter({ has: page.locator('td img') });
    assert.equal(await countRows.count(), 5, 'All five resolution statuses must retain count columns');
    for (const row of await countRows.all()) {
      for (const cell of (await row.locator('td').all()).slice(1)) {
        assert.match((await cell.innerText()).trim(), /^(?:--|[0-9]+)$/, 'Summary counts must not disappear in Chinese');
      }
    }
    console.log('PASS: 中文登录、首页、受试者矩阵、差异记录、研究审计和构建版本');
    // The navbar performs the application's own subject-label filter.
    const search = async () => {
      await goto('ListStudySubjects');
      await page.locator('input[name="findSubjects_f_studySubject.label"]').fill(label);
      await Promise.all([page.waitForNavigation({ waitUntil: 'domcontentloaded' }), page.locator('input[name="findSubjects_f_studySubject.label"]').press('Enter')]);
    };
    await search();
    if (!(await page.locator('body').innerText()).includes(`查看受试者: ${label}`) && await page.locator('tbody.tbody tr td').getByText(label, { exact: true }).count() === 0) {
      assert((await page.locator('body').innerText()).includes('没有找到记录'), 'Ambiguous search result; refusing to create');
      await goto('AddNewSubject');
      await page.locator('input[name="label"]').fill(label);
      await page.locator('input[name="uniqueIdentifier"]').fill('SYNTH-PID-ZH-001');
      await page.locator('input[name="secondaryLabel"]').fill(secondary);
      // Fixed, clearly synthetic fixture date: independent of the day this runs.
      await page.locator('input[name="enrollmentDate"]').fill('10-11月-2026');
      await page.locator('select[name="gender"]').selectOption('m');
      await page.locator('input[name="dob"]').fill('15-1月-1990');
      await Promise.all([page.waitForNavigation({ waitUntil: 'domcontentloaded' }), page.locator('input[name="submitDone"]').click()]);
      assert((await page.locator('body').innerText()).includes('已成功创建'), 'Subject creation was not confirmed');
      console.log('PASS: synthetic subject created');
    } else console.log('Existing synthetic subject found; verifying without submitting again.');
    await search();
    if (!(await page.locator('body').innerText()).includes(`查看受试者: ${label}`)) {
      const row = page.locator('tbody.tbody tr').filter({ has: page.getByText(label, { exact: true }) });
      assert.equal(await row.count(), 1, 'Synthetic subject missing from filtered matrix');
      assert((await row.textContent()).includes(secondary), 'Matrix secondary label missing');
      await Promise.all([page.waitForNavigation({ waitUntil: 'domcontentloaded' }), row.locator('a[href^="ViewStudySubject?"]').click()]);
    }
    await page.getByText('研究受试者记录', { exact: true }).click();
    await page.getByText(secondary, { exact: true }).waitFor({ state: 'visible' });
    body = await page.locator('body').innerText();
    for (const value of [label, secondary, 'SYNTH-PID-ZH-001', '15-1月-1990', '10-11月-2026']) assert(body.includes(value), `Subject view missing ${value}`);
    await page.screenshot({ path: path.join(output, 'chinese-subject.png'), fullPage: true });
    await page.pdf({ path: path.join(output, 'chinese-subject.pdf'), printBackground: true, format: 'A4' });
    const pdfText = execFileSync('pdftotext', ['-layout', path.join(output, 'chinese-subject.pdf'), '-'], { encoding: 'utf8' });
    for (const value of [secondary, label, '研究受试者记录']) assert(pdfText.includes(value), `PDF text missing ${value}`);
    console.log('PASS: filtered matrix, subject details, Chinese PDF text extraction');
    await goto('StudyAuditLog');
    const auditRow = page.locator('tbody.tbody tr').filter({ has: page.getByText(label, { exact: true }) });
    assert.equal(await auditRow.count(), 1, 'Subject missing from study audit');
    for (const value of [secondary, '15-1月-1990']) assert((await auditRow.innerText()).includes(value), `Study audit missing ${value}`);
    const auditLink = await auditRow.locator('a').getAttribute('href');
    const match = auditLink.match(/openDocWindow\('([^']+)'\)/);
    assert(match, 'Subject audit link missing');
    await goto(match[1]);
    body = await page.locator('body').innerText();
    for (const value of [label, secondary, '15-1月-1990', '研究受试者已创建']) assert(body.includes(value), `Subject audit missing ${value}`);
    await page.screenshot({ path: path.join(output, 'chinese-audit.png'), fullPage: true });
    console.log('PASS: audit displays Chinese subject value and creation events; creation events do not contain individual field changes');
    await goto('AddNewSubject');
    await page.locator('input[name="label"]').fill('SYNTH-ZH-INVALID-DATE');
    await page.locator('input[name="uniqueIdentifier"]').fill('SYNTH-PID-ZH-INVALID');
    await page.locator('input[name="secondaryLabel"]').fill('合成无效日期：不得保存');
    await page.locator('input[name="enrollmentDate"]').fill('10-11月-2026');
    await page.locator('select[name="gender"]').selectOption('m');
    await page.locator('input[name="dob"]').fill('31-2月-1990');
    await page.locator('input[name="submitDone"]').click();
    await page.locator('#spanAlert-dob.alert').first().waitFor();
    assert.match(await page.locator('#spanAlert-dob.alert').first().innerText(), /日期/);
    assert.equal(await page.locator('input[name="dob"]').inputValue(), '31-2月-1990');
    await page.screenshot({ path: path.join(output, 'chinese-invalid-date.png'), fullPage: true });
    console.log('PASS: impossible Chinese date rejected by the actual form with a Chinese error');
  } catch (error) {
    await page.screenshot({ path: path.join(output, 'chinese-failure.png'), fullPage: true }).catch(() => {});
    throw error;
  } finally { await browser.close(); }
})().catch(error => { console.error('FAIL:', error.message); process.exitCode = 1; });

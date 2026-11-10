/* Synthetic upstream comparison smoke. No real study data or external targets. */
const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');

const root = path.resolve(__dirname, '../..');
const target = path.join(root, 'work/original');
const base = new URL(process.env.CLINICA_BASE_URL || 'http://127.0.0.1:18080/LibreClinica/');
assert(['localhost', '127.0.0.1', '[::1]'].includes(base.hostname), 'Only loopback synthetic instances are allowed');
assert.equal(base.protocol, 'http:');
assert(fs.existsSync(path.join(target, 'compose.json')), 'Prepare the original baseline first');
const credentials = path.join(target, 'admin.json');

(async () => {
  const browser = await chromium.launch({
    headless: true,
    ...(process.env.CLINICA_CHROMIUM ? { executablePath: process.env.CLINICA_CHROMIUM } : {}),
    ...(process.env.CLINICA_BROWSER_NO_SANDBOX === '1' ? { args: ['--no-sandbox'] } : {}),
  });
  try {
    const page = await browser.newPage({ locale: 'zh-CN' });
    page.setDefaultTimeout(30000);
    const existing = fs.existsSync(credentials);
    const auth = existing ? JSON.parse(fs.readFileSync(credentials, 'utf8'))
      : { username: 'root', password: '12345678' };
    await page.goto(base.href);
    await page.locator('#username').fill(auth.username);
    await page.locator('#j_password').fill(auth.password);
    await page.locator('input[value="Login"]').click();
    if (!existing) {
      await page.locator('input[name="oldPasswd"]').waitFor();
      const newAuth = { username: 'root', password: 'Cl!9' + crypto.randomBytes(24).toString('base64url') };
      // Keep the proposed secret privately before submission so a timeout does
      // not destroy recovery information. A failed reset must be investigated.
      fs.writeFileSync(credentials, JSON.stringify(newAuth), { flag: 'wx', mode: 0o600 });
      await page.locator('input[name="oldPasswd"]').fill(auth.password);
      await page.locator('input[name="passwd"]').fill(newAuth.password);
      await page.locator('input[name="passwd1"]').fill(newAuth.password);
      await page.locator('select[name="passwdChallengeQ"]').selectOption({ index: 1 });
      await page.locator('input[name="passwdChallengeA"]').fill('synthetic-baseline-only');
      await page.locator('input[name="submit"]').click();
    }
    await page.getByText('Default Study (default-study)', { exact: false }).first().waitFor();
    const home = await page.locator('body').innerText();
    assert(home.includes('dd8c98ce2371'), 'Expected official upstream baseline changeset');
    assert(home.includes('Study Director'), 'Expected synthetic root study role');
    // zh-CN currently falls back to English; do not label this a Chinese UI pass.
    assert(home.includes('Subject Matrix'));
    await page.screenshot({ path: path.join(target, `browser-home-${base.port || '80'}.png`), fullPage: true });
    for (const [route, expected] of [
      ['ListStudySubjects', 'Subject Matrix'],
      ['ViewNotes?module=submit', 'Notes & Discrepancies'],
      ['StudyAuditLog', 'Study Audit Log'],
    ]) {
      const response = await page.goto(new URL(route, base).href);
      assert(response && response.status() < 400, route + ' HTTP failure');
      assert((await page.locator('body').innerText()).includes(expected), route + ' page missing');
      assert.equal(await page.locator('#j_password').count(), 0, route + ' unexpectedly logged out');
    }
    console.log('PASS: login, authenticated home, subject matrix, notes and study audit pages' +
      (existing ? ' (existing test credentials)' : ' (first-run password initialized)'));
    console.log('Scope: empty synthetic upstream study; no CRF entry, SDV, signature, lock, export or Bridge acceptance.');
  } finally {
    await browser.close();
  }
})().catch(error => { console.error(error.message); process.exitCode = 1; });

'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const calendarDir = path.resolve(__dirname, '../../web/src/main/webapp/includes/new_cal');
function load(locale) {
  const context = vm.createContext({ navigator: { userAgent: '' }, window: {}, document: {} });
  for (const file of ['calendar.js', `lang/calendar-${locale}.js`]) {
    vm.runInContext(fs.readFileSync(path.join(calendarDir, file), 'utf8'), context, { filename: file });
  }
  return context;
}

const english = load('en');
const chinese = load('zh');
const en = english.Calendar;
const zh = chinese.Calendar;
const months = Array.from({ length: 12 }, (_, i) => `${i + 1}月`);
assert.deepEqual(Array.from(zh._MN), months);
assert.deepEqual(Array.from(zh._SMN), months);
assert.equal(zh._FD, en._FD);
assert.equal(zh._FD, 0);
assert.deepEqual(Object.keys(zh._TT).sort(), Object.keys(en._TT).sort());
for (const key of ['DEF_DATE_FORMAT', 'TT_DATE_FORMAT', 'WEEKEND']) {
  assert.equal(zh._TT[key], en._TT[key], key);
}
for (const names of [zh._DN, zh._SDN]) {
  assert.equal(names.length, 8);
  for (const name of names) assert.match(name, /[\u3400-\u9fff]/);
  assert.equal(names[0], names[7]);
}
for (const key of ['INFO', 'PREV_YEAR', 'PREV_MONTH', 'GO_TODAY', 'NEXT_MONTH',
  'NEXT_YEAR', 'SEL_DATE', 'DRAG_TO_MOVE', 'PART_TODAY', 'DAY_FIRST', 'CLOSE',
  'TODAY', 'TIME_PART', 'WK', 'TIME', 'ABOUT_TIME']) {
  assert.match(zh._TT[key], /[\u3400-\u9fff]/, key);
}
assert.match(zh._TT.DAY_FIRST, /%s/);
const license = en._TT.ABOUT.split('\n').slice(0, 4).join('\n');
assert.ok(zh._TT.ABOUT.includes(license), 'ABOUT must preserve the original copyright and license notice');

const format = '%d-%b-%Y';
const originalTZ = process.env.TZ;
try {
  for (const tz of ['UTC', 'America/Los_Angeles', 'Asia/Shanghai']) {
    process.env.TZ = tz;
    for (const [locale, context] of [['en', english], ['zh', chinese]]) {
      const DateInCalendar = vm.runInContext('Date', context);
      const cases = Array.from({ length: 12 }, (_, month) => [2024, month, 15]);
      cases.push([2024, 1, 29], [2023, 11, 31], [2024, 0, 1]);
      for (const [year, month, day] of cases) {
        const original = new DateInCalendar(year, month, day, 12);
        const printed = original.print(format);
        const monthName = locale === 'zh' ? months[month] : en._SMN[month];
        assert.equal(printed, `${String(day).padStart(2, '0')}-${monthName}-${year}`);
        const parsed = DateInCalendar.parseDate(printed, format);
        assert.deepEqual([parsed.getFullYear(), parsed.getMonth(), parsed.getDate()],
          [year, month, day], `${locale} ${tz}: ${printed}`);
      }
    }
  }
} finally {
  if (originalTZ === undefined) delete process.env.TZ;
  else process.env.TZ = originalTZ;
}
console.log('Calendar locale and date round-trip checks passed.');

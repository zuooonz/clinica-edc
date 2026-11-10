/* Clinica EDC localization regression tests. SPDX-License-Identifier: LGPL-2.1-or-later */
package org.akaza.openclinica.i18n.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Test;

/** Tests the application's real date formatter, not a second parsing implementation. */
public class ChineseDateFormatTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @After
    public void cleanup() {
        ResourceBundleProvider.localeMap.remove(Thread.currentThread());
    }

    @Test
    public void everyLocalizedMonthRoundTripsWithUnchangedDatePattern() throws Exception {
        ResourceBundleProvider.updateLocale(Locale.CHINESE);
        assertEquals("dd-MMM-yyyy", I18nFormatUtil.dateFormatString(Locale.CHINESE));
        SimpleDateFormat format = I18nFormatUtil.getDateFormat(Locale.CHINESE);
        format.setTimeZone(UTC);
        format.setLenient(false);
        Calendar expected = Calendar.getInstance(UTC, Locale.ROOT);
        for (int month = 0; month < 12; month++) {
            expected.clear();
            expected.set(2026, month, 10);
            String value = format.format(expected.getTime());
            assertEquals("10-" + (month + 1) + "月-2026", value);
            assertEquals(expected.getTime(), format.parse(value));
        }
    }

    @Test
    public void localizedTimestampPreservesTheInstantAndOdmPattern() throws Exception {
        ResourceBundleProvider.updateLocale(Locale.CHINESE);
        Date expected = Date.from(Instant.parse("2026-11-10T12:34:56Z"));
        SimpleDateFormat format = I18nFormatUtil.getDateTimeFormat(Locale.CHINESE);
        assertEquals("dd-MMM-yyyy HH:mm:ss", format.toPattern());
        format.setTimeZone(UTC);
        format.setLenient(false);
        assertEquals(expected, format.parse(format.format(expected)));
        assertEquals("MM/dd/yyyy", ResourceBundleProvider.getFormatBundle(Locale.CHINESE)
                .getString("oc_date_format_string"));
    }

    @Test
    public void chineseLeapDayIsAcceptedAndInvalidDateIsRejected() throws Exception {
        ResourceBundleProvider.updateLocale(Locale.CHINESE);
        SimpleDateFormat format = I18nFormatUtil.getDateFormat(Locale.CHINESE);
        format.setLenient(false);
        assertEquals("29-2月-2024", format.format(format.parse("29-2月-2024")));
        boolean rejected = false;
        try {
            format.parse("29-2月-2025");
        } catch (java.text.ParseException expected) {
            rejected = true;
        }
        assertTrue("Invalid leap day must fail strict application formatting", rejected);
    }
}

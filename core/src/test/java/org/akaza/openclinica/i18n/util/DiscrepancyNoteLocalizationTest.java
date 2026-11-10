/* Clinica EDC localization regression. SPDX-License-Identifier: LGPL-2.1-or-later */
package org.akaza.openclinica.i18n.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.akaza.openclinica.bean.core.DiscrepancyNoteType;
import org.akaza.openclinica.service.DiscrepancyNoteUtil;
import org.junit.After;
import org.junit.Test;

public class DiscrepancyNoteLocalizationTest {
    @After
    public void cleanup() {
        ResourceBundleProvider.localeMap.remove(Thread.currentThread());
    }

    @Test
    public void summaryHeadersFollowEachRequestAndMatchTheSummaryKeys() {
        for (Locale locale : new Locale[] { Locale.ENGLISH, Locale.CHINESE, Locale.JAPANESE, Locale.ENGLISH }) {
            ResourceBundleProvider.updateLocale(locale);
            String[] headers = DiscrepancyNoteUtil.getTypeNames();
            Set<String> summaryKeys = new HashSet<String>();
            for (DiscrepancyNoteType type : DiscrepancyNoteType.list) {
                summaryKeys.add(type.getName());
            }
            assertEquals("Every summary column resolves an existing localized count key",
                    summaryKeys, new HashSet<String>(Arrays.asList(headers)));
            if (Locale.CHINESE.equals(locale)) {
                assertArrayEquals(new String[] { "质疑", "校验未通过", "修改原因", "注释" }, headers);
            }
            if (Locale.ENGLISH.equals(locale)) {
                assertArrayEquals(new String[] { "Query", "Failed Validation Check", "Reason for Change", "Annotation" }, headers);
            }
        }
    }

    @Test
    public void machineTypeNamesAndIdsRemainUnchanged() {
        ResourceBundleProvider.updateLocale(Locale.CHINESE);
        assertArrayEquals(new String[] { "Query", "Failed Validation Check", "Reason for Change", "Annotation" },
                DiscrepancyNoteUtil.TYPE_NAMES);
        assertEquals(Integer.valueOf(3), DiscrepancyNoteUtil.TYPES.get("Query"));
        assertEquals(Integer.valueOf(1), DiscrepancyNoteUtil.TYPES.get("Failed Validation Check"));
        assertEquals(Integer.valueOf(4), DiscrepancyNoteUtil.TYPES.get("Reason for Change"));
        assertEquals(Integer.valueOf(2), DiscrepancyNoteUtil.TYPES.get("Annotation"));
        assertTrue(DiscrepancyNoteUtil.TYPES.size() == 4);
    }
}

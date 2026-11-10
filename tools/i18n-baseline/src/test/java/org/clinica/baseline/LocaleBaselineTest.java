/* Clinica EDC independent behavior tests, LGPL-2.1-or-later. */
package org.clinica.baseline;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

import org.akaza.openclinica.i18n.core.LocaleResolver;
import org.akaza.openclinica.i18n.util.ResourceBundleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/** Tests production locale selection and canonical resources, without a database or servlet container. */
public class LocaleBaselineTest {
    private static final String PREFIX = "org.akaza.openclinica.i18n.";
    private static final String[] BUNDLES = {
        "admin", "audit_events", "exceptions", "format", "notes", "page_messages", "terms", "words", "workflow"
    };
    private Locale originalDefault;

    @Before
    public void setUp() {
        originalDefault = Locale.getDefault();
        Locale.setDefault(Locale.US);
        ResourceBundle.clearCache();
    }

    @After
    public void tearDown() {
        ResourceBundleProvider.localeMap.remove(Thread.currentThread());
        Locale.setDefault(originalDefault);
        ResourceBundle.clearCache();
    }

    private MockHttpServletRequest request(Locale... locales) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setPreferredLocales(Arrays.asList(locales));
        return request;
    }

    @Test
    public void absentRequestUsesEnglish() {
        assertEquals(Locale.ENGLISH, LocaleResolver.resolveLocale(null));
    }

    @Test
    public void englishCountryVariantUsesEnglish() {
        assertEquals(Locale.ENGLISH, LocaleResolver.resolveLocale(request(Locale.US)));
    }

    @Test
    public void unsupportedLanguageFallsBackToEnglish() {
        assertEquals(Locale.ENGLISH, LocaleResolver.resolveLocale(request(Locale.forLanguageTag("eo"))));
    }

    @Test
    public void japaneseCountryVariantSelectsLanguageBundle() {
        assertEquals(Locale.JAPANESE, LocaleResolver.resolveLocale(request(Locale.JAPAN)));
    }

    @Test
    public void unavailablePreferenceDoesNotHideAvailableSecondLanguage() {
        assertEquals(Locale.JAPANESE,
                LocaleResolver.resolveLocale(request(Locale.forLanguageTag("eo"), Locale.JAPAN)));
    }

    @Test
    public void explicitEnglishPreferenceStopsBeforeJapanese() {
        assertEquals(Locale.ENGLISH, LocaleResolver.resolveLocale(request(Locale.US, Locale.JAPAN)));
    }

    @Test
    public void partialPackCannotActivateEvenWhenEightBundlesExist() {
        Locale partial = Locale.forLanguageTag("zz");
        for (String bundle : BUNDLES) {
            ResourceBundle loaded = ResourceBundle.getBundle(PREFIX + bundle, partial);
            assertEquals(bundle, "workflow".equals(bundle) ? "" : "zz", loaded.getLocale().getLanguage());
        }
        assertEquals(Locale.ENGLISH, LocaleResolver.resolveLocale(request(partial)));
        assertEquals(Locale.JAPANESE, LocaleResolver.resolveLocale(request(partial, Locale.JAPAN)));
    }

    @Test
    public void missingIndividualKeyFallsBackToEnglishParent() {
        ResourceBundle english = ResourceBundle.getBundle(PREFIX + "words", Locale.ROOT);
        ResourceBundle partial = ResourceBundle.getBundle(PREFIX + "words", Locale.forLanguageTag("zz"));
        assertEquals("synthetic partial pack", partial.getString("clinica_baseline_fixture"));
        String existingEnglishKey = english.keySet().iterator().next();
        assertEquals(english.getString(existingEnglishKey), partial.getString(existingEnglishKey));
    }

    @Test
    public void sessionLocaleTakesPrecedenceOverBrowserPreference() {
        MockHttpServletRequest request = request(Locale.US);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LocaleResolver.getLocaleSessionAttributeName(), Locale.JAPANESE);
        request.setSession(session);
        assertEquals(Locale.JAPANESE, LocaleResolver.getLocale(request));
    }

    @Test
    public void absentSessionLocaleUsesBrowserSelection() {
        MockHttpServletRequest request = request(Locale.JAPAN);
        assertEquals(Locale.JAPANESE, LocaleResolver.getLocale(request));
        request.setSession(new MockHttpSession());
        assertEquals(Locale.JAPANESE, LocaleResolver.getLocale(request));
    }

    @Test
    public void canonicalEnglishAndJapaneseBundlesAreReadableAndJapaneseIsDecoded() {
        for (String bundle : BUNDLES) {
            ResourceBundle english = ResourceBundle.getBundle(PREFIX + bundle, Locale.ROOT);
            ResourceBundle japanese = ResourceBundle.getBundle(PREFIX + bundle, Locale.JAPAN);
            assertFalse(bundle, english.keySet().isEmpty());
            assertEquals(bundle, Locale.JAPANESE, japanese.getLocale());
        }
        ResourceBundle words = ResourceBundle.getBundle(PREFIX + "words", Locale.JAPAN);
        assertTrue("Escaped Japanese must decode into actual Unicode characters",
                words.keySet().stream().map(words::getString).anyMatch(value -> value.codePoints()
                        .anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN
                                || Character.UnicodeScript.of(c) == Character.UnicodeScript.HIRAGANA)));
    }

    @Test
    public void licensingRemainsUntranslatedForJapanese() {
        ResourceBundle english = ResourceBundle.getBundle(PREFIX + "licensing", Locale.ROOT);
        ResourceBundle japanese = ResourceBundle.getBundle(PREFIX + "licensing", Locale.JAPAN);
        assertEquals(Locale.ROOT, japanese.getLocale());
        assertEquals(english.keySet(), japanese.keySet());
        for (String key : english.keySet()) {
            assertEquals(key, english.getString(key), japanese.getString(key));
        }
    }

    @Test
    public void localizationPreservesCanonicalDatePatternsAndOdmDateSemantics() throws Exception {
        ResourceBundle english = ResourceBundle.getBundle(PREFIX + "format", Locale.ROOT);
        ResourceBundle japanese = ResourceBundle.getBundle(PREFIX + "format", Locale.JAPAN);
        String[] keys = {"date_format", "date_format_calender", "date_format_string", "date_format_string_ds",
            "date_format_year", "date_format_year_month", "date_regexp", "date_time_format",
            "date_time_format_string", "date_time_regexp", "oc_date_format_string"};
        for (String key : keys) {
            assertEquals(key, english.getString(key).trim(), japanese.getString(key).trim());
        }
        assertEquals("MM/dd/yyyy", japanese.getString("oc_date_format_string"));
        SimpleDateFormat format = new SimpleDateFormat(japanese.getString("date_format_string"), Locale.ENGLISH);
        format.setLenient(false);
        assertEquals("29-Feb-2024", format.format(format.parse("29-Feb-2024")));
        try {
            format.parse("29-Feb-2025");
            fail("An impossible date must not parse with strict canonical date parsing");
        } catch (ParseException expected) {
            // Parser configuration is explicit; application-wide date validation is a separate acceptance step.
        }
    }
}

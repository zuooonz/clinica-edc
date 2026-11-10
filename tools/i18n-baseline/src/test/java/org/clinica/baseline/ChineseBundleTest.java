/* Clinica EDC independent behavior tests, LGPL-2.1-or-later. */
package org.clinica.baseline;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.akaza.openclinica.i18n.core.LocaleResolver;
import org.akaza.openclinica.i18n.util.ResourceBundleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Resource contracts for the actual Chinese pack, without a database or reimplemented locale logic. */
public class ChineseBundleTest {
    private static final String PREFIX = "org.akaza.openclinica.i18n.";
    private static final String PATH = "org/akaza/openclinica/i18n/";
    private static final String[] BUNDLES = {
        "admin", "audit_events", "exceptions", "format", "notes", "page_messages", "terms", "words", "workflow"
    };
    private static final String[] ROLE_KEYS = {
        "Data_Entry_Person", "Investigator", "Monitor", "Study_Coordinator", "Study_Director",
        "site_Data_Entry_Person", "site_Data_Entry_Person2", "site_Study_Coordinator",
        "site_Study_Director", "site_investigator", "site_monitor"
    };
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{[^}]+\\}|\\{[0-9]+(?:,[^{}]+)?\\}");
    // The letter requirement deliberately excludes literal '<' and '>' in validation messages.
    private static final Pattern HTML_TAG = Pattern.compile("</?[A-Za-z][^>]*>");
    private static final Pattern HTML_ENTITY = Pattern.compile("&(?:#[0-9]+|#[xX][0-9a-fA-F]+|[A-Za-z][A-Za-z0-9]*);");
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

    private byte[] bytes(String filename) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(PATH + filename)) {
            assertNotNull("Missing exact resource " + filename, stream);
            return stream.readAllBytes();
        }
    }

    private Properties ownProperties(String bundle, boolean chinese) throws IOException {
        Properties properties = new Properties();
        // Properties.load decodes Java escapes, with no ResourceBundle parent fallback.
        try (InputStream stream = new java.io.ByteArrayInputStream(
                bytes(bundle + (chinese ? "_zh" : "") + ".properties"))) {
            properties.load(stream);
        }
        return properties;
    }

    private List<String> tokens(Pattern pattern, String value) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private Map<String, Integer> multiset(Pattern pattern, String value) {
        Map<String, Integer> counts = new HashMap<>();
        for (String token : tokens(pattern, value)) {
            counts.merge(token, 1, Integer::sum);
        }
        return counts;
    }

    private boolean hasHan(String value) {
        return value.codePoints().anyMatch(codePoint ->
                Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private void assertSelectsChinese(String languageTag) {
        Locale requested = Locale.forLanguageTag(languageTag);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setPreferredLocales(Arrays.asList(requested, Locale.ENGLISH));
        assertEquals(languageTag, Locale.CHINESE, LocaleResolver.resolveLocale(request));
        assertEquals(languageTag + " without session", Locale.CHINESE, LocaleResolver.getLocale(request));
        for (String bundle : BUNDLES) {
            assertEquals(languageTag + ":" + bundle, Locale.CHINESE,
                    ResourceBundle.getBundle(PREFIX + bundle, requested).getLocale());
        }
    }

    @Test
    public void chineseLanguageSelectsCompleteChinesePack() {
        assertSelectsChinese("zh");
    }

    @Test
    public void chineseChinaSelectsCompleteChinesePack() {
        assertSelectsChinese("zh-CN");
    }

    @Test
    public void chineseHansChinaSelectsCompleteChinesePack() {
        assertSelectsChinese("zh-Hans-CN");
    }

    @Test
    public void everyChineseOwnKeyMatchesEnglishWithoutParentFallback() throws Exception {
        for (String bundle : BUNDLES) {
            Properties english = ownProperties(bundle, false);
            Properties chinese = ownProperties(bundle, true);
            assertFalse(bundle + " must not be empty", english.isEmpty());
            assertEquals(bundle + " own keys", english.stringPropertyNames(), chinese.stringPropertyNames());
            ResourceBundle runtime = ResourceBundle.getBundle(PREFIX + bundle, Locale.CHINESE);
            assertEquals(bundle, Locale.CHINESE, runtime.getLocale());
            for (String key : english.stringPropertyNames()) {
                assertEquals(bundle + ":" + key + " must be present in the Chinese file",
                        chinese.getProperty(key), runtime.getString(key));
            }
        }
    }

    @Test
    public void placeholdersMarkupAttributesAndEntitiesArePreserved() throws Exception {
        for (String bundle : BUNDLES) {
            Properties english = ownProperties(bundle, false);
            Properties chinese = ownProperties(bundle, true);
            for (String key : english.stringPropertyNames()) {
                String context = bundle + ":" + key;
                String original = english.getProperty(key);
                String translated = chinese.getProperty(key);
                assertNotNull(context, translated);
                assertEquals(context + " placeholders (including repetitions)",
                        multiset(PLACEHOLDER, original), multiset(PLACEHOLDER, translated));
                assertEquals(context + " HTML tags, attributes and nesting order",
                        tokens(HTML_TAG, original), tokens(HTML_TAG, translated));
                assertEquals(context + " HTML entities (including repetitions)",
                        multiset(HTML_ENTITY, original), multiset(HTML_ENTITY, translated));
            }
        }
    }

    @Test
    public void formatBehaviorOnlyChangesCalendarLanguageFile() throws Exception {
        Properties english = ownProperties("format", false);
        Properties chinese = ownProperties("format", true);
        assertEquals(english.stringPropertyNames(), chinese.stringPropertyNames());
        for (String key : english.stringPropertyNames()) {
            assertEquals("format:" + key,
                    "jscalendar_language_file".equals(key) ? "calendar-zh.js" : english.getProperty(key),
                    chinese.getProperty(key));
        }
    }

    @Test
    public void protectedRoleLabelsLegalFooterAndBuildVersionRemainUnchanged() throws Exception {
        Properties englishTerms = ownProperties("terms", false);
        Properties chineseTerms = ownProperties("terms", true);
        for (String key : ROLE_KEYS) {
            assertTrue("Expected protected role key " + key, englishTerms.containsKey(key));
            assertEquals(key, englishTerms.getProperty(key), chineseTerms.getProperty(key));
        }
        Properties englishWords = ownProperties("words", false);
        Properties chineseWords = ownProperties("words", true);
        for (String key : englishWords.stringPropertyNames()) {
            if (key.startsWith("footer.license.") || "footer.tooltip".equals(key)
                    || "version_number".equals(key)) {
                assertEquals(key, englishWords.getProperty(key), chineseWords.getProperty(key));
            }
        }
        assertTrue(englishWords.containsKey("footer.tooltip"));
        assertTrue(englishWords.containsKey("version_number"));
    }

    @Test
    public void chineseLicensingAlwaysFallsBackToOriginalLegalText() {
        assertNull("No translated legal resource may be added",
                getClass().getClassLoader().getResource(PATH + "licensing_zh.properties"));
        ResourceBundle original = ResourceBundle.getBundle(PREFIX + "licensing", Locale.ROOT);
        for (String languageTag : Arrays.asList("zh", "zh-CN", "zh-Hans-CN")) {
            ResourceBundle chinese = ResourceBundle.getBundle(PREFIX + "licensing", Locale.forLanguageTag(languageTag));
            assertEquals(languageTag, Locale.ROOT, chinese.getLocale());
            assertEquals(original.keySet(), chinese.keySet());
            for (String key : original.keySet()) {
                assertEquals(languageTag + ":" + key, original.getString(key), chinese.getString(key));
            }
        }
    }

    @Test
    public void chineseFilesUseAsciiBytesWithJavaUnicodeEscapes() throws Exception {
        for (String bundle : BUNDLES) {
            byte[] contents = bytes(bundle + "_zh.properties");
            for (int index = 0; index < contents.length; index++) {
                assertTrue(bundle + " non-ASCII byte at " + index, (contents[index] & 0xff) < 128);
            }
            if (!"format".equals(bundle)) {
                assertTrue(bundle + " must contain decoded Chinese text",
                        ownProperties(bundle, true).values().stream().map(Object::toString).anyMatch(this::hasHan));
            }
        }
    }

    @Test
    public void untranslatedValuesAreLimitedToReviewedPreservationKeys() throws Exception {
        Set<String> reviewed = new HashSet<String>();
        try (InputStream input = getClass().getResourceAsStream("/zh-preserved-keys.txt")) {
            assertNotNull("Preservation decisions must be explicit", input);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.US_ASCII))) {
                for (String line; (line = reader.readLine()) != null;) {
                    if (!line.trim().isEmpty() && !line.startsWith("#")) reviewed.add(line.trim());
                }
            }
        }
        Set<String> preserved = new HashSet<String>();
        for (String bundle : BUNDLES) {
            Properties english = ownProperties(bundle, false);
            Properties chinese = ownProperties(bundle, true);
            for (String key : english.stringPropertyNames()) {
                String original = english.getProperty(key);
                String translated = chinese.getProperty(key);
                if (!original.trim().isEmpty()) {
                    assertFalse(bundle + ":" + key + " must not be blank", translated.trim().isEmpty());
                }
                if (original.equals(translated)) preserved.add(bundle + ":" + key);
            }
        }
        assertEquals("New English fallback or changed technical/legal values require review", reviewed, preserved);
    }

    @Test
    public void coreWorkflowLabelsAndErrorsContainChinese() throws Exception {
        String[][] examples = {
            {"admin", "active"}, {"audit_events", "__added_a_study_event"},
            {"exceptions", "input_invalid"}, {"notes", "study_frozen_locked_note"},
            {"page_messages", "event_CRF_marked_complete"}, {"terms", "query"},
            {"words", "study"}, {"words", "subject"}, {"words", "data_entry"},
            {"workflow", "add_new_subject"}
        };
        for (String[] example : examples) {
            String value = ownProperties(example[0], true).getProperty(example[1]);
            assertNotNull(Arrays.toString(example), value);
            assertTrue(Arrays.toString(example) + " must contain Chinese", hasHan(value));
        }
    }
}

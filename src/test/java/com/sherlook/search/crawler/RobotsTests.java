package com.sherlook.search.crawler;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RobotsTests {

	@BeforeEach
	void clearRobotsCache() throws Exception {
		// Clear static robotsAllow and robotsDisallow maps using reflection
		Field allowField = Robots.class.getDeclaredField("robotsAllow");
		allowField.setAccessible(true);
		((Map<?, ?>) allowField.get(null)).clear();

		Field disallowField = Robots.class.getDeclaredField("robotsDisallow");
		disallowField.setAccessible(true);
		((Map<?, ?>) disallowField.get(null)).clear();
	}

	@Test
	void testIsAllowedDefaultTrueWhenNoRobotsTxt() {
		// Non-existent domain will simulate robots.txt not found
		String url = "http://nonexistent1234567890.com/page";
		boolean result = Robots.isAllowed(url);
		assertTrue(result);
	}

	@Test
	void testRuleToRegex() throws Exception {
		// Access private method via reflection
		var method = Robots.class.getDeclaredMethod("ruleToRegex", String.class);
		method.setAccessible(true);

		assertEquals("^/private/.*", method.invoke(null, "/private/"));
		assertEquals("^/path/.*file\\.html.*", method.invoke(null, "/path/*file.html"));
		assertEquals("^/admin/.*", method.invoke(null, "/admin/"));
		assertEquals("^/file\\.php\\?id=.*", method.invoke(null, "/file.php?id=*"));
		assertEquals("^/admin$", method.invoke(null, "/admin$"));
	}

	@Test
	void testIsAllowedWithStaticMockRules() throws Exception {
		String base = "http://test.com";
		String fullUrl = base + "/allowed/page";

		// Simulate already-fetched rules
		Field allowField = Robots.class.getDeclaredField("robotsAllow");
		Field disallowField = Robots.class.getDeclaredField("robotsDisallow");
		allowField.setAccessible(true);
		disallowField.setAccessible(true);
		@SuppressWarnings("unchecked")
		var allowMap = (Map<String, List<Pattern>>) allowField.get(null);
		@SuppressWarnings("unchecked")
		var disallowMap = (Map<String, List<Pattern>>) disallowField.get(null);

		allowMap.put(base, List.of(Pattern.compile("^/allowed/.*")));
		disallowMap.put(base, List.of(Pattern.compile("^/.*")));

		assertTrue(Robots.isAllowed(fullUrl));
	}

	@Test
	void testIsDisallowedWithStaticMockRules() throws Exception {
		String base = "http://blocked.com";
		String fullUrl = base + "/blocked/page";

		Field allowField = Robots.class.getDeclaredField("robotsAllow");
		Field disallowField = Robots.class.getDeclaredField("robotsDisallow");
		allowField.setAccessible(true);
		disallowField.setAccessible(true);

		@SuppressWarnings("unchecked")
		var allowMap = (Map<String, List<Pattern>>) allowField.get(null);
		@SuppressWarnings("unchecked")
		var disallowMap = (Map<String, List<Pattern>>) disallowField.get(null);

		disallowMap.put(base, List.of(Pattern.compile("^/blocked/.*")));
		allowMap.put(base, List.of());

		assertFalse(Robots.isAllowed(fullUrl));
	}

	@Test
	void testAllowOverridesDisallowWithLongerMatch() throws Exception {
		String base = "http://example.com";
		String fullUrl = base + "/folder/subfolder/file";

		Field allowField = Robots.class.getDeclaredField("robotsAllow");
		Field disallowField = Robots.class.getDeclaredField("robotsDisallow");
		allowField.setAccessible(true);
		disallowField.setAccessible(true);

		@SuppressWarnings("unchecked")
		var allowMap = (Map<String, List<Pattern>>) allowField.get(null);
		@SuppressWarnings("unchecked")
		var disallowMap = (Map<String, List<Pattern>>) disallowField.get(null);

		// Simulate Disallow: /folder/ Allow: /folder/subfolder/
		disallowMap.put(base, List.of(Pattern.compile("^/folder/.*")));
		allowMap.put(base, List.of(Pattern.compile("^/folder/subfolder/.*")));

		assertTrue(Robots.isAllowed(fullUrl)); // Allow wins due to longer match
	}
}

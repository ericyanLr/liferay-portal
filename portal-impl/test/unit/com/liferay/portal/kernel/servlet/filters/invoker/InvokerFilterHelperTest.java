/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.servlet.filters.invoker;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.module.util.SystemBundleUtil;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.UnsecureSAXReaderUtil;
import com.liferay.portal.servlet.filters.ignore.IgnoreFilter;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.portal.util.PortalImpl;
import com.liferay.portal.xml.SAXReaderImpl;

import java.io.ByteArrayInputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.ServletContext;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import org.mockito.Mockito;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Eric Yan
 */
public class InvokerFilterHelperTest {

	@ClassRule
	public static LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		PortalUtil portalUtil = new PortalUtil();

		portalUtil.setPortal(new PortalImpl());

		SAXReaderUtil saxReaderUtil = new SAXReaderUtil();

		SAXReaderImpl secureSAXReaderImpl = new SAXReaderImpl();

		secureSAXReaderImpl.setSecure(true);

		saxReaderUtil.setSAXReader(secureSAXReaderImpl);

		UnsecureSAXReaderUtil unsecureSAXReaderUtil =
			new UnsecureSAXReaderUtil();

		unsecureSAXReaderUtil.setSAXReader(new SAXReaderImpl());
	}

	@After
	public void tearDown() {
		_serviceRegistrations.forEach(ServiceRegistration::unregister);

		_invokerFilterHelper.destroy();
	}

	@Test
	public void testLiferayWebXml() throws Exception {
		String xml = _generateLiferayWebXmlWithTestFilter();

		Mockito.when(
			_servletContext.getResourceAsStream(
				Mockito.eq("/WEB-INF/liferay-web.xml"))
		).thenReturn(
			new ByteArrayInputStream(xml.getBytes())
		);

		_invokerFilterHelper.init(
			new InvokerFilterConfig(_servletContext, null, null));

		_assert(
			Arrays.asList(_TEST_FILTER_PROPERTIES.get("servlet-filter-name")));
	}

	@Test
	public void testOSGIComponent() throws Exception {
		BundleContext bundleContext = SystemBundleUtil.getBundleContext();

		_serviceRegistrations.add(
			bundleContext.registerService(
				Filter.class, _filter, _TEST_FILTER_PROPERTIES));

		Dictionary<String, String> testFilterBProperties =
			HashMapDictionaryBuilder.putAll(
				_TEST_FILTER_PROPERTIES
			).put(
				"before-filter",
				_TEST_FILTER_PROPERTIES.get("servlet-filter-name")
			).put(
				"servlet-context-name", ""
			).put(
				"servlet-filter-name", "Test Filter B"
			).build();

		_serviceRegistrations.add(
			bundleContext.registerService(
				Filter.class, _filter, testFilterBProperties));

		Dictionary<String, String> testFilterCProperties =
			HashMapDictionaryBuilder.putAll(
				_TEST_FILTER_PROPERTIES
			).put(
				"after-filter", "Test Filter B"
			).put(
				"servlet-context-name", ""
			).put(
				"servlet-filter-name", "Test Filter C"
			).build();

		_serviceRegistrations.add(
			bundleContext.registerService(
				Filter.class, _filter, testFilterCProperties));

		_invokerFilterHelper.init(
			new InvokerFilterConfig(_servletContext, null, null));

		_assert(
			Arrays.asList(
				testFilterBProperties.get("servlet-filter-name"),
				testFilterCProperties.get("servlet-filter-name"),
				_TEST_FILTER_PROPERTIES.get("servlet-filter-name")));
	}

	private void _assert(List<String> expectedFilterNames) {
		ConcurrentMap<String, FilterMapping[]> filterMappingsMap =
			ReflectionTestUtil.getFieldValue(
				_invokerFilterHelper, "_filterMappingsMap");

		FilterMapping filterMapping = (FilterMapping)ArrayUtil.getValue(
			filterMappingsMap.get(
				_TEST_FILTER_PROPERTIES.get("servlet-filter-name")),
			0);

		Set<Dispatcher> dispatchers = ReflectionTestUtil.getFieldValue(
			filterMapping, "_dispatchers");

		Iterator<Dispatcher> iterator = dispatchers.iterator();

		Assert.assertEquals(
			Dispatcher.valueOf(_TEST_FILTER_PROPERTIES.get("dispatcher")),
			iterator.next());

		Filter filter = filterMapping.getFilter();

		Assert.assertEquals(
			IgnoreFilter.class.getName(),
			filter.getClass(
			).getName());

		Assert.assertEquals(
			_TEST_FILTER_PROPERTIES.get("servlet-filter-name"),
			ReflectionTestUtil.getFieldValue(filterMapping, "_filterName"));

		Assert.assertEquals(
			_TEST_FILTER_PROPERTIES.get("url-pattern"),
			ArrayUtil.getValue(
				ReflectionTestUtil.getFieldValue(filterMapping, "_urlPatterns"),
				0));

		Pattern pattern = ReflectionTestUtil.getFieldValue(
			filterMapping, "_urlRegexIgnorePattern");

		Assert.assertEquals(
			_TEST_FILTER_PROPERTIES.get("init-param.url-regex-ignore-pattern"),
			pattern.toString());

		pattern = ReflectionTestUtil.getFieldValue(
			filterMapping, "_urlRegexPattern");

		Assert.assertEquals(
			_TEST_FILTER_PROPERTIES.get("init-param.url-regex-pattern"),
			pattern.toString());

		List<String> filterNames = ReflectionTestUtil.getFieldValue(
			_invokerFilterHelper, "_filterNames");

		Assert.assertEquals(expectedFilterNames, filterNames);
	}

	private String _generateLiferayWebXmlWithTestFilter() {
		StringBundler sb = new StringBundler(19);

		sb.append("<?xml version=\"1.0\"?><web-app>");
		sb.append("<filter>");
		sb.append(
			String.format(
				"<filter-name>%s</filter-name>",
				_TEST_FILTER_PROPERTIES.get("servlet-filter-name")));
		sb.append(
			String.format(
				"<filter-class>%s</filter-class>",
				_filter.getClass(
				).getName()));
		sb.append("<init-param>");
		sb.append("<param-name>url-regex-ignore-pattern</param-name>");
		sb.append(
			String.format(
				"<param-value>%s</param-value>",
				_TEST_FILTER_PROPERTIES.get(
					"init-param.url-regex-ignore-pattern")));
		sb.append("</init-param>");
		sb.append("<init-param>");
		sb.append("<param-name>url-regex-pattern</param-name>");
		sb.append(
			String.format(
				"<param-value>%s</param-value>",
				_TEST_FILTER_PROPERTIES.get("init-param.url-regex-pattern")));
		sb.append("</init-param>");
		sb.append("</filter>");
		sb.append("<filter-mapping>");
		sb.append(
			String.format(
				"<filter-name>%s</filter-name>",
				_TEST_FILTER_PROPERTIES.get("servlet-filter-name")));
		sb.append(
			String.format(
				"<dispatcher>%s</dispatcher>",
				_TEST_FILTER_PROPERTIES.get("dispatcher")));
		sb.append(
			String.format(
				"<url-pattern>%s</url-pattern>",
				_TEST_FILTER_PROPERTIES.get("url-pattern")));

		sb.append("</filter-mapping>");
		sb.append("</web-app>");

		return sb.toString();
	}

	private static final Dictionary<String, String> _TEST_FILTER_PROPERTIES =
		HashMapDictionaryBuilder.put(
			"dispatcher", "FORWARD"
		).put(
			"init-param.url-regex-ignore-pattern", "/test-regex-ignore-pattern"
		).put(
			"init-param.url-regex-pattern", "/test-regex-pattern"
		).put(
			"servlet-context-name", ""
		).put(
			"servlet-filter-name", "Test Filter"
		).put(
			"url-pattern", "/*"
		).build();

	private final Filter _filter = new IgnoreFilter();
	private final InvokerFilterHelper _invokerFilterHelper =
		new InvokerFilterHelper();
	private final List<ServiceRegistration<?>> _serviceRegistrations =
		new ArrayList<>();
	private final ServletContext _servletContext = Mockito.mock(
		ServletContext.class);

}
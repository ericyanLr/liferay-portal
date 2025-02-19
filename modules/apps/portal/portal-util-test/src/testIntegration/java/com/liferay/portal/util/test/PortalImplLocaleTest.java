/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.util.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.layout.test.util.LayoutTestUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.servlet.HttpMethods;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.CompanyTestUtil;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Props;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.language.LanguageResources;
import com.liferay.portal.servlet.I18nServlet;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.util.PortalImpl;
import com.liferay.portal.util.PortalInstances;
import com.liferay.portal.util.PropsValues;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * @author Filipe Afonso
 */
@RunWith(Arquillian.class)
public class PortalImplLocaleTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_availableLocales = _language.getAvailableLocales();

		PropsValues.LOCALES_ENABLED = new String[] {
			"ca_ES", "en_US", "fr_FR", "de_DE", "pt_BR", "es_ES", "en_GB"
		};

		_language.init();

		LanguageResources.getSuperLocale(LocaleUtil.GERMANY);
		LanguageResources.getSuperLocale(LocaleUtil.US);

		_group = GroupTestUtil.addGroup();

		_layout = LayoutTestUtil.addTypePortletLayout(_group);

		CompanyTestUtil.resetCompanyLocales(
			_group.getCompanyId(),
			Arrays.asList(
				LocaleUtil.fromLanguageId("ca_ES"), LocaleUtil.US,
				LocaleUtil.FRANCE, LocaleUtil.GERMANY, LocaleUtil.BRAZIL,
				LocaleUtil.SPAIN, LocaleUtil.UK),
			LocaleUtil.getDefault());

		GroupTestUtil.updateDisplaySettings(
			_group.getGroupId(),
			Arrays.asList(LocaleUtil.UK, LocaleUtil.GERMANY),
			LocaleUtil.GERMANY);
	}

	@After
	public void tearDown() throws Exception {
		PropsValues.LOCALES_ENABLED = _props.getArray(
			PropsKeys.LOCALES_ENABLED);

		_language.init();

		CompanyTestUtil.resetCompanyLocales(
			TestPropsValues.getCompanyId(), _availableLocales,
			LocaleUtil.getDefault());
	}

	@Test
	public void testInvalidResourceWithLocale() throws Exception {
		MockHttpServletResponse mockHttpServletResponse =
			_testLocaleForLanguageId(
				"localhost", "/en", "/WEB-INF/web.xml;.js", LocaleUtil.US);

		Assert.assertEquals(
			HttpServletResponse.SC_NOT_FOUND,
			mockHttpServletResponse.getStatus());
	}

	@Test
	public void testSiteAvailableLanguageId() throws Exception {
		_testLocaleForLanguageId("localhost", "/en", LocaleUtil.UK);
	}

	@Test
	public void testSiteAvailableLocale() throws Exception {
		_testLocaleForLanguageId("localhost", "/en_GB", LocaleUtil.UK);
	}

	@Test
	public void testSiteDefaultLanguageId() throws Exception {
		_testLocaleForLanguageId("localhost", "/de", LocaleUtil.GERMANY);
	}

	@Test
	public void testSiteDefaultLocale() throws Exception {
		_testLocaleForLanguageId("localhost", "", LocaleUtil.GERMANY);
		_testLocaleForLanguageId("localhost", "/de_DE", LocaleUtil.GERMANY);
	}

	private void _setRequestURI(
		MockHttpServletRequest mockHttpServletRequest, String requestURI) {

		mockHttpServletRequest.setPathInfo(
			requestURI.substring(requestURI.indexOf(CharPool.SLASH, 1)));
		mockHttpServletRequest.setRequestURI(requestURI);
		mockHttpServletRequest.setServletPath(
			requestURI.substring(0, requestURI.indexOf(CharPool.SLASH, 1)));
	}

	private void _testLocaleForLanguageId(
			String host, String i18nLanguageId, Locale expectedLocale)
		throws Exception {

		_testLocaleForLanguageId(
			host, i18nLanguageId,
			StringBundler.concat(
				PropsValues.LAYOUT_FRIENDLY_URL_PUBLIC_SERVLET_MAPPING,
				_group.getFriendlyURL(), _layout.getFriendlyURL()),
			expectedLocale);
	}

	private MockHttpServletResponse _testLocaleForLanguageId(
			String host, String i18nLanguageId, String pathInfo,
			Locale expectedLocale)
		throws Exception {

		MockServletContext mockServletContext = new MockServletContext() {
		};

		mockServletContext.setContextPath(StringPool.BLANK);
		mockServletContext.setServletContextName(StringPool.BLANK);

		_i18nServlet.init(new MockServletConfig(mockServletContext));

		MockHttpServletRequest mockHttpServletRequest =
			new MockHttpServletRequest(mockServletContext);

		mockHttpServletRequest.addHeader("Host", host);
		mockHttpServletRequest.setMethod(HttpMethods.GET);

		_setRequestURI(mockHttpServletRequest, i18nLanguageId + pathInfo);

		MockHttpServletResponse mockHttpServletResponse =
			new MockHttpServletResponse();

		mockHttpServletRequest.setCookies(mockHttpServletResponse.getCookies());

		if (Validator.isNull(i18nLanguageId)) {
			PortalInstances.getCompanyId(mockHttpServletRequest);

			ReflectionTestUtil.invoke(
				_i18nFilter, "processFilter",
				new Class<?>[] {
					HttpServletRequest.class, HttpServletResponse.class,
					FilterChain.class
				},
				mockHttpServletRequest, mockHttpServletResponse,
				new MockFilterChain());

			String redirect = mockHttpServletResponse.getHeader(
				HttpHeaders.LOCATION);

			if (Validator.isNotNull(redirect)) {
				i18nLanguageId = redirect.substring(
					0, redirect.indexOf(CharPool.SLASH, 1));

				_setRequestURI(
					mockHttpServletRequest, i18nLanguageId + pathInfo);

				mockHttpServletResponse = new MockHttpServletResponse();
			}
		}

		if (Validator.isNotNull(i18nLanguageId)) {
			_i18nServlet.service(
				mockHttpServletRequest, mockHttpServletResponse);

			String forwardedUrl = mockHttpServletResponse.getForwardedUrl();

			if (Validator.isNotNull(forwardedUrl)) {
				_setRequestURI(mockHttpServletRequest, forwardedUrl);
			}
		}

		_publicFriendlyURLServlet.service(
			mockHttpServletRequest, mockHttpServletResponse);

		Assert.assertEquals(
			expectedLocale,
			_portalImpl.getLocale(
				mockHttpServletRequest, mockHttpServletResponse, false));

		return mockHttpServletResponse;
	}

	private Set<Locale> _availableLocales;

	@DeleteAfterTestRun
	private Group _group;

	@Inject(filter = "servlet-filter-name=I18n Filter")
	private Filter _i18nFilter;

	private final I18nServlet _i18nServlet = new I18nServlet();

	@Inject
	private Language _language;

	@DeleteAfterTestRun
	private Layout _layout;

	private final PortalImpl _portalImpl = new PortalImpl();

	@Inject
	private Props _props;

	@Inject(
		filter = "component.name=com.liferay.friendly.url.internal.servlet.PublicFriendlyURLServlet"
	)
	private Servlet _publicFriendlyURLServlet;

}
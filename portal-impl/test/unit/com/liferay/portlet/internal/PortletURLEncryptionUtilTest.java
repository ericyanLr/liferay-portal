/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portlet.internal;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.encryptor.Encryptor;
import com.liferay.portal.kernel.encryptor.EncryptorUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.module.service.Snapshot;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import jakarta.servlet.http.HttpServletRequest;

import java.security.Key;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Eric Yan
 */
public class PortletURLEncryptionUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() throws Exception {
		Mockito.when(
			_company.getKeyObj()
		).thenReturn(
			_key
		);

		_companyLocalServiceUtilMockedStatic.when(
			() -> CompanyLocalServiceUtil.getCompanyById(_COMPANY_ID)
		).thenReturn(
			_company
		);

		Mockito.when(
			_encryptor.encrypt(Mockito.eq(_key), Mockito.anyString())
		).thenAnswer(
			invocation -> _encrypt(invocation.getArgument(1))
		);

		Snapshot<Encryptor> snapshot = Mockito.mock(Snapshot.class);

		Mockito.when(
			snapshot.get()
		).thenReturn(
			_encryptor
		);

		_autoCloseable = ReflectionTestUtil.setFieldValueWithAutoCloseable(
			EncryptorUtil.class, "_encryptorSnapshot", snapshot);
	}

	@After
	public void tearDown() throws Exception {
		_autoCloseable.close();

		_companyLocalServiceUtilMockedStatic.close();
	}

	@Test
	public void testEncryptParameters() {
		String authToken = RandomTestUtil.randomString();
		String name = RandomTestUtil.randomString();
		String value = RandomTestUtil.randomString();

		List<KeyValuePair> encryptedParameters =
			PortletURLEncryptionUtil.encryptParameters(
				_COMPANY_ID,
				Arrays.asList(
					new KeyValuePair("p_auth", authToken),
					new KeyValuePair(name, value)));

		Assert.assertEquals(
			encryptedParameters.toString(), 3, encryptedParameters.size());

		_assertKeyValuePair("p_auth", authToken, encryptedParameters.get(0));
		_assertKeyValuePair(name, _encrypt(value), encryptedParameters.get(1));
		_assertKeyValuePair(WebKeys.ENCRYPT, "1", encryptedParameters.get(2));
	}

	@Test
	public void testGetDecryptedHttpServletRequest() throws Exception {
		_mockDecrypt(_PORTLET_ID_ENCRYPTED, _PORTLET_ID);

		String authToken = RandomTestUtil.randomString();

		MockHttpServletRequest mockHttpServletRequest =
			_createMockHttpServletRequest(
				"p_auth=" + authToken, "p_p_id=" + _PORTLET_ID_ENCRYPTED);

		String plid = String.valueOf(RandomTestUtil.randomLong());

		mockHttpServletRequest.addParameter("p_l_id", plid);

		HttpServletRequest httpServletRequest =
			PortletURLEncryptionUtil.getDecryptedHttpServletRequest(
				_COMPANY_ID, mockHttpServletRequest);

		Assert.assertEquals(
			authToken, httpServletRequest.getParameter("p_auth"));
		Assert.assertEquals(plid, httpServletRequest.getParameter("p_l_id"));
		Assert.assertEquals(
			_PORTLET_ID, httpServletRequest.getParameter("p_p_id"));
	}

	@Test
	public void testGetDecryptedHttpServletRequestWhenParameterNameIsMalformed()
		throws Exception {

		_mockDecrypt(_PORTLET_ID_ENCRYPTED, _PORTLET_ID);

		HttpServletRequest httpServletRequest =
			PortletURLEncryptionUtil.getDecryptedHttpServletRequest(
				_COMPANY_ID,
				_createMockHttpServletRequest(
					RandomTestUtil.randomString() + "%=" +
						RandomTestUtil.randomString(),
					"p_p_id=" + _PORTLET_ID_ENCRYPTED));

		Assert.assertEquals(
			_PORTLET_ID, httpServletRequest.getParameter("p_p_id"));
	}

	@Test
	public void testGetDecryptedHttpServletRequestWhenParameterNamesAreEncodedDifferently()
		throws Exception {

		_mockDecrypt(_PORTLET_ID_ENCRYPTED, _PORTLET_ID);

		HttpServletRequest httpServletRequest =
			PortletURLEncryptionUtil.getDecryptedHttpServletRequest(
				_COMPANY_ID,
				_createMockHttpServletRequest(
					"p_p_id=" + _PORTLET_ID_ENCRYPTED,
					"p%5Fp%5Fid=" + RandomTestUtil.randomString()));

		Assert.assertEquals(
			_PORTLET_ID, httpServletRequest.getParameter("p_p_id"));
	}

	@Test
	public void testGetDecryptedHttpServletRequestWithMultipleValues()
		throws Exception {

		String encryptedValue1 = RandomTestUtil.randomString();
		String value1 = RandomTestUtil.randomString();

		_mockDecrypt(encryptedValue1, value1);

		String encryptedValue2 = RandomTestUtil.randomString();
		String value2 = RandomTestUtil.randomString();

		_mockDecrypt(encryptedValue2, value2);

		String encryptedValue3 = RandomTestUtil.randomString();
		String value3 = RandomTestUtil.randomString();

		_mockDecrypt(encryptedValue3, value3);

		String name = RandomTestUtil.randomString();

		HttpServletRequest httpServletRequest =
			PortletURLEncryptionUtil.getDecryptedHttpServletRequest(
				_COMPANY_ID,
				_createMockHttpServletRequest(
					name + "=" + encryptedValue1, name + "=" + encryptedValue3,
					name + "=" + encryptedValue2));

		String[] values = httpServletRequest.getParameterValues(name);

		Assert.assertArrayEquals(new String[] {value1, value3, value2}, values);

		Assert.assertEquals(value1, httpServletRequest.getParameter(name));

		Map<String, String[]> parameterMap =
			httpServletRequest.getParameterMap();

		Assert.assertArrayEquals(values, parameterMap.get(name));
	}

	private void _assertKeyValuePair(
		String expectedKey, String expectedValue, KeyValuePair keyValuePair) {

		Assert.assertEquals(expectedKey, keyValuePair.getKey());
		Assert.assertEquals(expectedValue, keyValuePair.getValue());
	}

	private MockHttpServletRequest _createMockHttpServletRequest(
		String... parameters) {

		MockHttpServletRequest mockHttpServletRequest =
			new MockHttpServletRequest();

		StringBundler sb = new StringBundler((parameters.length * 2) + 2);

		for (String parameter : parameters) {
			int index = parameter.indexOf('=');

			mockHttpServletRequest.addParameter(
				parameter.substring(0, index), parameter.substring(index + 1));

			sb.append(parameter);
			sb.append("&");
		}

		sb.append(WebKeys.ENCRYPT);
		sb.append("=1");

		mockHttpServletRequest.addParameter(WebKeys.ENCRYPT, "1");
		mockHttpServletRequest.setQueryString(sb.toString());

		return mockHttpServletRequest;
	}

	private String _encrypt(String value) {
		return "encrypted-" + value;
	}

	private void _mockDecrypt(String encryptedValue, String value)
		throws Exception {

		Mockito.doReturn(
			value
		).when(
			_encryptor
		).decrypt(
			_key, encryptedValue
		);
	}

	private static final long _COMPANY_ID = RandomTestUtil.randomLong();

	private static final String _PORTLET_ID = RandomTestUtil.randomString();

	private static final String _PORTLET_ID_ENCRYPTED =
		RandomTestUtil.randomString();

	private AutoCloseable _autoCloseable;
	private final Company _company = Mockito.mock(Company.class);
	private final MockedStatic<CompanyLocalServiceUtil>
		_companyLocalServiceUtilMockedStatic = Mockito.mockStatic(
			CompanyLocalServiceUtil.class);
	private final Encryptor _encryptor = Mockito.mock(Encryptor.class);
	private final Key _key = Mockito.mock(Key.class);

}
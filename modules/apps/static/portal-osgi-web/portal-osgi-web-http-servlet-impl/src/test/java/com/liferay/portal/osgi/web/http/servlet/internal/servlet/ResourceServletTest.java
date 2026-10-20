/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.osgi.web.http.servlet.internal.servlet;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.servlet.HttpMethods;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.osgi.service.http.context.ServletContextHelper;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author Eric Yan
 */
public class ResourceServletTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void test() throws Exception {
		String content = RandomTestUtil.randomString();
		long lastModified = System.currentTimeMillis();

		URL url = URL.of(
			URI.create("test://" + RandomTestUtil.randomString()),
			new URLStreamHandler() {

				@Override
				protected URLConnection openConnection(URL url) {
					return new URLConnection(url) {

						@Override
						public void connect() {
						}

						@Override
						public int getContentLength() {
							return content.length();
						}

						@Override
						public InputStream getInputStream() {
							return new ByteArrayInputStream(content.getBytes());
						}

						@Override
						public long getLastModified() {
							return lastModified;
						}

					};
				}

			});

		ResourceServlet resourceServlet = new ResourceServlet(
			StringPool.SLASH,
			new ServletContextHelper() {

				@Override
				public String getMimeType(String name) {
					return ContentTypes.TEXT_HTML;
				}

				@Override
				public URL getResource(String name) {
					return url;
				}

			});

		MockHttpServletResponse mockHttpServletResponse = _service(
			DispatcherType.REQUEST, null, null, resourceServlet);

		String etag = mockHttpServletResponse.getHeader(HttpHeaders.ETAG);

		_assert(
			StringPool.BLANK, HttpServletResponse.SC_NOT_MODIFIED,
			_service(
				DispatcherType.REQUEST, HttpHeaders.IF_MODIFIED_SINCE,
				lastModified, resourceServlet));
		_assert(
			StringPool.BLANK, HttpServletResponse.SC_NOT_MODIFIED,
			_service(
				DispatcherType.REQUEST, HttpHeaders.IF_NONE_MATCH, etag,
				resourceServlet));
		_assert(
			content, HttpServletResponse.SC_OK,
			_service(
				DispatcherType.INCLUDE, HttpHeaders.IF_MODIFIED_SINCE,
				lastModified, resourceServlet));
		_assert(
			content, HttpServletResponse.SC_OK,
			_service(
				DispatcherType.INCLUDE, HttpHeaders.IF_NONE_MATCH, etag,
				resourceServlet));
	}

	private void _assert(
			String expectedContent, int expectedStatus,
			MockHttpServletResponse mockHttpServletResponse)
		throws Exception {

		Assert.assertEquals(
			expectedContent, mockHttpServletResponse.getContentAsString());
		Assert.assertEquals(
			expectedStatus, mockHttpServletResponse.getStatus());
	}

	private MockHttpServletResponse _service(
			DispatcherType dispatcherType, String headerName,
			Object headerValue, ResourceServlet resourceServlet)
		throws Exception {

		MockHttpServletRequest mockHttpServletRequest =
			new MockHttpServletRequest();

		mockHttpServletRequest.setDispatcherType(dispatcherType);
		mockHttpServletRequest.setMethod(HttpMethods.GET);

		if (headerName != null) {
			mockHttpServletRequest.addHeader(headerName, headerValue);
		}

		MockHttpServletResponse mockHttpServletResponse =
			new MockHttpServletResponse();

		resourceServlet.service(
			mockHttpServletRequest, mockHttpServletResponse);

		return mockHttpServletResponse;
	}

}
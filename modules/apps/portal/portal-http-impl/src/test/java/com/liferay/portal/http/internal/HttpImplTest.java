/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.http.internal;

import com.liferay.petra.concurrent.DCLSingleton;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Tuple;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.portal.util.PortalImpl;

import java.lang.reflect.Field;

import java.util.concurrent.TimeUnit;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.DefaultManagedHttpClientConnection;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.impl.execchain.MainClientExec;
import org.apache.http.impl.pool.BasicPoolEntry;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Miguel Pastor
 */
public class HttpImplTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@BeforeClass
	public static void setUpClass() {
		PortalUtil portalUtil = new PortalUtil();

		portalUtil.setPortal(
			new PortalImpl() {

				@Override
				public String[] stripURLAnchor(String url, String separator) {
					return new String[] {url, StringPool.BLANK};
				}

			});
	}

	@Test
	public void testHttpKeepAlive() throws Exception {
		_testHttpKeepAlive(true, Long.MAX_VALUE, -1);
		_testHttpKeepAlive(true, Long.MAX_VALUE, 0);
		_testHttpKeepAlive(true, 300000, 300);
	}

	@Test
	public void testHttpKeepAliveWithRequestClose() throws Exception {
		HttpRequest httpRequest = new BasicHttpRequest("GET", "/");

		httpRequest.addHeader(
			HttpHeaders.CONNECTION, HttpHeaders.CONNECTION_CLOSE_VALUE);

		HttpContext httpContext = new BasicHttpContext(null);

		httpContext.setAttribute(HttpCoreContext.HTTP_REQUEST, httpRequest);

		HttpResponse httpResponse = new BasicHttpResponse(
			new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK"));

		httpResponse.setHeaders(
			new Header[] {
				new BasicHeader(HttpHeaders.CONNECTION, HttpHeaders.KEEP_ALIVE),
				new BasicHeader(HttpHeaders.CONTENT_LENGTH, "10")
			});

		_testHttpKeepAlive(false, -1, httpContext, httpResponse);
	}

	@Test
	public void testHttpKeepAliveWithResponseClose() throws Exception {
		HttpRequest httpRequest = new BasicHttpRequest("GET", "/");

		httpRequest.addHeader(HttpHeaders.CONNECTION, HttpHeaders.KEEP_ALIVE);

		HttpContext httpContext = new BasicHttpContext(null);

		httpContext.setAttribute(HttpCoreContext.HTTP_REQUEST, httpRequest);

		HttpResponse httpResponse = new BasicHttpResponse(
			new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK"));

		httpResponse.setHeaders(
			new Header[] {
				new BasicHeader(
					HttpHeaders.CONNECTION, HttpHeaders.CONNECTION_CLOSE_VALUE),
				new BasicHeader(HttpHeaders.CONTENT_LENGTH, "10")
			});

		_testHttpKeepAlive(false, -1, httpContext, httpResponse);
	}

	@Test
	public void testIsNonProxyHost() throws Exception {
		String domain = "foo.com";
		String ipAddress = "192.168.0.250";
		String ipAddressWithStarWildcard = "182.*.0.250";

		Field field = ReflectionTestUtil.getField(
			HttpImpl.class, "_NON_PROXY_HOSTS");

		Object value = field.get(null);

		try {
			field.set(
				null,
				new String[] {domain, ipAddress, ipAddressWithStarWildcard});

			Assert.assertTrue(_httpImpl.isNonProxyHost(domain));
			Assert.assertTrue(_httpImpl.isNonProxyHost(ipAddress));
			Assert.assertTrue(_httpImpl.isNonProxyHost("182.123.0.250"));
			Assert.assertFalse(_httpImpl.isNonProxyHost("182.100.1.250"));
			Assert.assertFalse(_httpImpl.isNonProxyHost("google.com"));
		}
		finally {
			field.set(null, value);
		}
	}

	private Tuple _getHttpImplConnectionStrategies() {
		CloseableHttpClient closeableHttpClient = ReflectionTestUtil.invoke(
			_httpImpl, "getCloseableHttpClient",
			new Class<?>[] {HttpHost.class}, new Object[] {null});

		ClientExecChain clientExecChain = ReflectionTestUtil.getFieldValue(
			closeableHttpClient, "execChain");

		while (true) {
			clientExecChain = ReflectionTestUtil.getFieldValue(
				clientExecChain, "requestExecutor");

			if (clientExecChain instanceof MainClientExec) {
				ConnectionKeepAliveStrategy connectionKeepAliveStrategy =
					ReflectionTestUtil.getFieldValue(
						clientExecChain, "keepAliveStrategy");
				ConnectionReuseStrategy connectionReuseStrategy =
					ReflectionTestUtil.getFieldValue(
						clientExecChain, "reuseStrategy");

				return new Tuple(
					connectionKeepAliveStrategy, connectionReuseStrategy);
			}
		}
	}

	private void _resetHttpImpl() {
		DCLSingleton<CloseableHttpClient> closeableHttpClientDCLSingleton =
			ReflectionTestUtil.getFieldValue(
				_httpImpl, "_closeableHttpClientDCLSingleton");

		ReflectionTestUtil.setFieldValue(
			closeableHttpClientDCLSingleton, "_singleton", null);

		DCLSingleton<PoolingHttpClientConnectionManager>
			poolingHttpClientConnectionManagerDCLSingleton =
				ReflectionTestUtil.getFieldValue(
					_httpImpl,
					"_poolingHttpClientConnectionManagerDCLSingleton");

		ReflectionTestUtil.setFieldValue(
			poolingHttpClientConnectionManagerDCLSingleton, "_singleton", null);
	}

	private void _testHttpKeepAlive(
		boolean expectedKeepAlive, long expectedKeepAliveTimeoutInMilliseconds,
		HttpContext httpContext, HttpResponse httpResponse) {

		try {
			Tuple connectionStrategiesTuple =
				_getHttpImplConnectionStrategies();

			ConnectionKeepAliveStrategy connectionKeepAliveStrategy =
				(ConnectionKeepAliveStrategy)
					connectionStrategiesTuple.getObject(0);
			ConnectionReuseStrategy connectionReuseStrategy =
				(ConnectionReuseStrategy)connectionStrategiesTuple.getObject(1);

			Assert.assertNotNull(connectionKeepAliveStrategy);
			Assert.assertNotNull(connectionReuseStrategy);

			Assert.assertEquals(
				expectedKeepAlive,
				connectionReuseStrategy.keepAlive(httpResponse, httpContext));

			long keepAliveTimeout = -1;

			if (expectedKeepAlive) {
				long keepAliveDuration =
					connectionKeepAliveStrategy.getKeepAliveDuration(
						httpResponse, new BasicHttpContext(null));

				BasicPoolEntry basicPoolEntry = new BasicPoolEntry(
					"id", new HttpHost("localhost", 8080),
					new DefaultManagedHttpClientConnection("id", 8 * 1024));

				basicPoolEntry.updateExpiry(
					keepAliveDuration, TimeUnit.MILLISECONDS);

				keepAliveTimeout = basicPoolEntry.getExpiry();

				if (keepAliveTimeout != Long.MAX_VALUE) {
					keepAliveTimeout -= basicPoolEntry.getUpdated();
				}
			}

			Assert.assertEquals(
				expectedKeepAliveTimeoutInMilliseconds, keepAliveTimeout);
		}
		finally {
			_resetHttpImpl();
		}
	}

	private void _testHttpKeepAlive(
		boolean expectedKeepAlive, long expectedKeepAliveTimeoutInMilliseconds,
		long keepAliveTimeoutHeaderValue) {

		HttpContext httpContext = new BasicHttpContext(null);

		HttpResponse httpResponse = new BasicHttpResponse(
			new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK"));

		httpResponse.setHeaders(
			new Header[] {
				new BasicHeader(HttpHeaders.CONNECTION, HttpHeaders.KEEP_ALIVE),
				new BasicHeader(HttpHeaders.CONTENT_LENGTH, "10")
			});

		if (keepAliveTimeoutHeaderValue > -1) {
			httpResponse.addHeader(
				new BasicHeader(
					HttpHeaders.KEEP_ALIVE,
					"timeout=" + keepAliveTimeoutHeaderValue));
		}

		_testHttpKeepAlive(
			expectedKeepAlive, expectedKeepAliveTimeoutInMilliseconds,
			httpContext, httpResponse);
	}

	private final HttpImpl _httpImpl = new HttpImpl();

}
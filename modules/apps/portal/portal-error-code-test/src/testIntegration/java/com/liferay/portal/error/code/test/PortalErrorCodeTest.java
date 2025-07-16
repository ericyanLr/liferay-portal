/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.error.code.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.URLUtil;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LogEntry;
import com.liferay.portal.test.log.LoggerTestUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.io.FileNotFoundException;

import java.net.URL;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author István András Dézsi
 */
@RunWith(Arquillian.class)
public class PortalErrorCodeTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_company = _companyLocalService.getCompany(
			TestPropsValues.getCompanyId());
	}

	@Test
	public void testAccessErrorsCodeJsp() throws Exception {
		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				"portal_web.docroot.errors.code_jsp", LoggerTestUtil.WARN)) {

			URLUtil.toString(
				new URL(
					"http://" + _company.getVirtualHostname() +
						":8080/errors/code.jsp"));

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 1, logEntries.size());

			LogEntry logEntry = logEntries.get(0);

			Assert.assertEquals(LoggerTestUtil.WARN, logEntry.getPriority());

			Assert.assertEquals(
				"{code=\"0\", msg=\"null\", uri=null}", logEntry.getMessage());
		}
	}

	@Test
	public void testAccessErrorsCodeJspViaErrorDispatchWithInvalidResource()
		throws Exception {

		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				"portal_web.docroot.errors.code_jsp", LoggerTestUtil.WARN)) {

			String url =
				"http://" + _company.getVirtualHostname() +
					":8080/.image/company_logo";

			try {
				URLUtil.toString(new URL(url));

				Assert.fail();
			}
			catch (FileNotFoundException fileNotFoundException) {
				Assert.assertEquals(url, fileNotFoundException.getMessage());
			}

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 1, logEntries.size());

			LogEntry logEntry = logEntries.get(0);

			Assert.assertEquals(LoggerTestUtil.WARN, logEntry.getPriority());

			Assert.assertEquals(
				"{code=\"404\", msg=\"The requested resource " +
					"[/.image/company_logo] is not available\", " +
						"uri=/.image/company_logo}",
				logEntry.getMessage());
		}
	}

	private Company _company;

	@Inject
	private CompanyLocalService _companyLocalService;

}
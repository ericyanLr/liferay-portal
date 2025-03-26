/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.glowroot.plugin.freemarker.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.journal.util.JournalHelper;
import com.liferay.layout.display.page.LayoutDisplayPageProviderRegistry;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.portlet.PortletRequestModel;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.spring.aop.AopInvocationHandler;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Eric Yan
 */
@RunWith(Arquillian.class)
public class TemplatesAspectTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testJournalTransformerAdviceWithNullArguments()
		throws Exception {

		long startTime = System.currentTimeMillis();

		AopInvocationHandler aopInvocationHandler =
			ProxyUtil.fetchInvocationHandler(
				_journalArticleLocalService, AopInvocationHandler.class);

		Object journalTransformer = ReflectionTestUtil.getFieldValue(
			aopInvocationHandler.getTarget(), "_journalTransformer");

		try {
			ReflectionTestUtil.invoke(
				journalTransformer, "transform",
				new Class<?>[] {
					JournalArticle.class, DDMTemplate.class,
					JournalHelper.class, String.class,
					LayoutDisplayPageProviderRegistry.class, List.class,
					PortletRequestModel.class, boolean.class, String.class,
					ThemeDisplay.class, String.class
				},
				null, null, null, null, null, Collections.emptyList(), null,
				false, null, null, null);
		}
		catch (Exception exception) {
		}

		_assertTransaction(
			"Journal Article FreeMarker Template (Company ID ?, Site Group " +
				"ID ?, and DDMTemplate ID ?)",
			startTime);
	}

	private void _assertTransaction(String expectedMessage, long startTime)
		throws Exception {

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject(
			HttpUtil.URLtoString(
				StringBundler.concat(
					"http://localhost:4000/o/glowroot/backend/transaction",
					"/summaries?agent-rollup-id=&transaction-type=",
					"FreeMarker%20Templates&sort-order=total-time&limit=10",
					"&from=", startTime, "&to=",
					System.currentTimeMillis() + 60000L)));

		JSONArray transactionsJSONArray = jsonObject.getJSONArray(
			"transactions");

		JSONObject transactionJSONObject = transactionsJSONArray.getJSONObject(
			0);

		Assert.assertEquals(
			expectedMessage,
			transactionJSONObject.getString("transactionName"));
	}

	@Inject
	private JournalArticleLocalService _journalArticleLocalService;

}
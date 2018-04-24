/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.journal.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.dynamic.data.mapping.test.util.DDMStructureTestUtil;
import com.liferay.dynamic.data.mapping.test.util.DDMTemplateTestUtil;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.model.JournalArticleConstants;
import com.liferay.journal.model.JournalFolderConstants;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Zsolt Oláh
 */
@RunWith(Arquillian.class)
public class JournalArticleExpirationTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();
	}

	@Test
	public void testExpireApprovedArticle() throws Exception {
		JournalArticle article = addArticle();

		article = expire(article);

		Assert.assertTrue(article.isExpired());
	}

	@Test
	public void testExpireApprovedArticlePostponeExpiration() throws Exception {
		JournalArticle article = addArticle();

		article = postponeExpiration(article);

		article = expire(article);

		Assert.assertFalse(article.isExpired());
	}

	@Test
	public void testExpireDraftArticle() throws Exception {
		draft = true;

		JournalArticle article = addArticle();

		article = expire(article);

		Assert.assertFalse(article.isExpired());
	}

	@Test
	public void testExpireDraftArticlePostponeExpiration() throws Exception {
		draft = true;

		JournalArticle article = addArticle();

		article = postponeExpiration(article);

		article = expire(article);

		Assert.assertFalse(article.isExpired());
	}

	protected JournalArticle addArticle() throws Exception {
		long groupId = _group.getGroupId();

		Map<Locale, String> titleMap = new HashMap<>();

		titleMap.put(LocaleUtil.getDefault(), RandomTestUtil.randomString());

		Map<Locale, String> descriptionMap = new HashMap<>();

		descriptionMap.put(
			LocaleUtil.getDefault(), RandomTestUtil.randomString());

		String content = DDMStructureTestUtil.getSampleStructuredContent();

		DDMStructure ddmStructure = DDMStructureTestUtil.addStructure(
			groupId, JournalArticle.class.getName());

		DDMTemplate ddmTemplate = DDMTemplateTestUtil.addTemplate(
			groupId, ddmStructure.getStructureId(),
			portal.getClassNameId(JournalArticle.class));

		Calendar displayDateCalendar = new GregorianCalendar();

		displayDateCalendar.setTime(new Date());

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(groupId);

		if (draft) {
			serviceContext.setWorkflowAction(
				WorkflowConstants.ACTION_SAVE_DRAFT);
		}
		else {
			serviceContext.setWorkflowAction(WorkflowConstants.ACTION_PUBLISH);
		}

		Calendar expirationDateCalendar = getExpirationCalendar(Time.HOUR, 1);

		return journalArticleLocalService.addArticle(
			TestPropsValues.getUserId(), groupId,
			JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			JournalArticleConstants.CLASSNAME_ID_DEFAULT, 0, StringPool.BLANK,
			true, JournalArticleConstants.VERSION_DEFAULT, titleMap,
			descriptionMap, content, ddmStructure.getStructureKey(),
			ddmTemplate.getTemplateKey(), null,
			displayDateCalendar.get(Calendar.MONTH),
			displayDateCalendar.get(Calendar.DAY_OF_MONTH),
			displayDateCalendar.get(Calendar.YEAR),
			displayDateCalendar.get(Calendar.HOUR_OF_DAY),
			displayDateCalendar.get(Calendar.MINUTE),
			expirationDateCalendar.get(Calendar.MONTH),
			expirationDateCalendar.get(Calendar.DAY_OF_MONTH),
			expirationDateCalendar.get(Calendar.YEAR),
			expirationDateCalendar.get(Calendar.HOUR_OF_DAY),
			expirationDateCalendar.get(Calendar.MINUTE), false, 0, 0, 0, 0, 0,
			true, true, false, null, null, null, null, serviceContext);
	}

	protected JournalArticle expire(JournalArticle article) throws Exception {
		Date expirationDate = article.getExpirationDate();

		article.setExpirationDate(
			new Date(expirationDate.getTime() - (Time.HOUR * 2)));

		journalArticleLocalService.updateJournalArticle(article);

		journalArticleLocalService.checkArticles();

		return journalArticleLocalService.getArticle(article.getId());
	}

	protected Calendar getExpirationCalendar(long timeUnit, int timeValue)
		throws Exception {

		Calendar calendar = new GregorianCalendar();

		calendar.setTime(
			new Date(System.currentTimeMillis() + timeUnit * timeValue));

		User user = userLocalService.getUser(TestPropsValues.getUserId());

		calendar.setTimeZone(user.getTimeZone());

		return calendar;
	}

	protected JournalArticle postponeExpiration(JournalArticle article)
		throws Exception {

		Calendar displayDateCalendar = new GregorianCalendar();

		displayDateCalendar.setTime(article.getDisplayDate());

		Calendar expirationDateCalendar = getExpirationCalendar(Time.YEAR, 1);

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(article.getGroupId());

		serviceContext.setWorkflowAction(WorkflowConstants.ACTION_PUBLISH);

		return journalArticleLocalService.updateArticle(
			TestPropsValues.getUserId(), article.getGroupId(),
			article.getFolderId(), article.getArticleId(), article.getVersion(),
			article.getTitleMap(), article.getDescriptionMap(),
			article.getContent(), article.getDDMStructureKey(),
			article.getDDMTemplateKey(), article.getLayoutUuid(),
			displayDateCalendar.get(Calendar.MONTH),
			displayDateCalendar.get(Calendar.DAY_OF_MONTH),
			displayDateCalendar.get(Calendar.YEAR),
			displayDateCalendar.get(Calendar.HOUR_OF_DAY),
			displayDateCalendar.get(Calendar.MINUTE),
			expirationDateCalendar.get(Calendar.MONTH),
			expirationDateCalendar.get(Calendar.DAY_OF_MONTH),
			expirationDateCalendar.get(Calendar.YEAR),
			expirationDateCalendar.get(Calendar.HOUR_OF_DAY),
			expirationDateCalendar.get(Calendar.MINUTE), false, 0, 0, 0, 0, 0,
			true, article.getIndexable(), article.isSmallImage(),
			article.getSmallImageURL(), null, null, null, serviceContext);
	}

	protected boolean draft;

	@Inject
	protected JournalArticleLocalService journalArticleLocalService;

	@Inject
	protected Portal portal;

	@Inject
	protected UserLocalService userLocalService;

	@DeleteAfterTestRun
	private Group _group;

}
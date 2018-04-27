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

package com.liferay.journal.search.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.journal.model.JournalArticle;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.Sync;
import com.liferay.portal.kernel.test.rule.SynchronousDestinationTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.search.test.util.FieldValuesAssert;
import com.liferay.portal.search.test.util.IdempotentRetryAssert;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerTestRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Wade Cao
 * @author Eric Yan
 */
@RunWith(Arquillian.class)
@Sync
public class JournalArticleIndexerIndexedFieldsTest
	extends BaseJournalIndexerTestCase {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerTestRule.INSTANCE,
			SynchronousDestinationTestRule.INSTANCE);

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		setGroup(journalFixture.addGroup());
		setIndexerClass(JournalArticle.class);
		setUser(journalFixture.addUser());
	}

	@Test
	public void testIndexedFields() throws Exception {
		String content = RandomTestUtil.randomString();
		Locale locale = LocaleUtil.BRAZIL;
		String title = RandomTestUtil.randomString();

		JournalArticle journalArticle = journalFixture.addJournalArticle(
			title, locale, content, journalFixture.getServiceContext());

		Map<String, String> documentMap = new HashMap<>();

		populateExpectedFieldValues(journalArticle, documentMap);

		assertIndexedFields(title, locale, documentMap);
	}

	protected void assertIndexedFields(
			String keywords, Locale locale, Map<String, String> expectedValues)
		throws Exception {

		IdempotentRetryAssert.retryAssert(
			3, TimeUnit.SECONDS,
			() -> doAssertIndexedFields(keywords, locale, expectedValues));
	}

	protected Void doAssertIndexedFields(
			String keywords, Locale locale, Map<String, String> expectedValues)
		throws Exception {

		Document document = journalSearchFixture.searchOnlyOne(
			keywords, locale);

		indexedFieldsFixture.postProcessDocument(document);

		FieldValuesAssert.assertFieldValues(expectedValues, document, keywords);

		return null;
	}

	protected void populateContent(
			JournalArticle journalArticle, Map<String, String> map)
		throws Exception {

		String content = journalArticle.getContent();

		com.liferay.portal.kernel.xml.Document document = SAXReaderUtil.read(
			content);

		Element rootElement = document.getRootElement();

		List<String> availableLocales = Arrays.asList(
			StringUtil.split(rootElement.attributeValue("available-locales")));

		String defaultLanguageId = LanguageUtil.getLanguageId(
			LocaleUtil.getDefault());

		DDMStructure ddmStructure = journalArticle.getDDMStructure();

		long ddmStructureId = ddmStructure.getStructureId();

		for (String languageId : journalArticle.getAvailableLanguageIds()) {
			if (availableLocales.contains(languageId)) {
				String actualContent = journalFixture.getActualContent(
					journalArticle, languageId);
				String fieldName = StringBundler.concat(
					Field.CONTENT, StringPool.UNDERLINE, languageId);

				if (languageId.equals(defaultLanguageId)) {
					fieldName = Field.CONTENT;
				}

				map.put(fieldName, actualContent);

				String key = StringBundler.concat(
					"ddm__text__", String.valueOf(ddmStructureId),
					StringPool.DOUBLE_UNDERLINE, fieldName);

				map.put(key, actualContent);
				map.put(
					key.concat("_String_sortable"),
					StringUtil.lowerCase(actualContent));
			}
		}
	}

	protected void populateDates(
		JournalArticle journalArticle, Map<String, String> map) {

		indexedFieldsFixture.populateDate(
			Field.CREATE_DATE, journalArticle.getCreateDate(), map);
		indexedFieldsFixture.populateDate(
			"displayDate", journalArticle.getDisplayDate(), map);
		indexedFieldsFixture.populateExpirationDateWithForever(map);
		indexedFieldsFixture.populateDate(
			Field.MODIFIED_DATE, journalArticle.getModifiedDate(), map);
		indexedFieldsFixture.populateDate(
			Field.PUBLISH_DATE, journalArticle.getDisplayDate(), map);
	}

	protected void populateExpectedFieldValues(
			JournalArticle journalArticle, Map<String, String> map)
		throws Exception {

		map.put(Field.ARTICLE_ID, journalArticle.getArticleId());
		map.put(
			Field.ARTICLE_ID.concat("_String_sortable"),
			journalArticle.getArticleId());
		map.put(Field.CLASS_NAME_ID, "0");
		map.put(Field.CLASS_PK, "0");

		DDMStructure ddmStructure = journalArticle.getDDMStructure();

		map.put(
			Field.CLASS_TYPE_ID, String.valueOf(ddmStructure.getStructureId()));

		map.put(
			Field.COMPANY_ID, String.valueOf(journalArticle.getCompanyId()));
		map.put("ddmStructureKey", journalArticle.getDDMStructureKey());
		map.put("ddmTemplateKey", journalArticle.getDDMTemplateKey());
		map.put(Field.ENTRY_CLASS_NAME, journalArticle.getModelClassName());
		map.put(
			Field.ENTRY_CLASS_PK,
			String.valueOf(journalArticle.getResourcePrimKey()));
		map.put(Field.FOLDER_ID, String.valueOf(journalArticle.getFolderId()));
		map.put(Field.GROUP_ID, String.valueOf(journalArticle.getGroupId()));
		map.put("head", "true");
		map.put("headListable", "true");
		map.put("latest", "true");
		map.put(Field.LAYOUT_UUID, journalArticle.getLayoutUuid());
		map.put(
			Field.ROOT_ENTRY_CLASS_PK,
			String.valueOf(journalArticle.getResourcePrimKey()));
		map.put(
			Field.SCOPE_GROUP_ID, String.valueOf(journalArticle.getGroupId()));
		map.put(Field.STAGING_GROUP, "false");
		map.put(Field.STATUS, String.valueOf(journalArticle.getStatus()));

		ArrayList<String> treePathValues = new ArrayList<>(
			Arrays.asList(
				StringUtil.split(
					journalArticle.getTreePath(), CharPool.SLASH)));

		if (treePathValues.size() == 1) {
			map.put(Field.TREE_PATH, treePathValues.get(0));
		}
		else if (treePathValues.size() > 1) {
			map.put(Field.TREE_PATH, treePathValues.toString());
		}

		map.put(Field.USER_ID, String.valueOf(journalArticle.getUserId()));
		map.put(
			Field.USER_NAME,
			StringUtil.lowerCase(journalArticle.getUserName()));
		map.put(Field.VERSION, String.valueOf(journalArticle.getVersion()));
		map.put("visible", "true");

		populateContent(journalArticle, map);
		populateDates(journalArticle, map);
		populateLocalizedTitleFields(journalArticle, map);
		populateTitleFields(journalArticle, map);

		indexedFieldsFixture.populatePriority("0.0", map);
		indexedFieldsFixture.populateRoleIdFields(
			journalArticle.getCompanyId(), journalArticle.getModelClassName(),
			journalArticle.getResourcePrimKey(), journalArticle.getGroupId(),
			null, map);
		indexedFieldsFixture.populateUID(
			journalArticle.getModelClassName(), journalArticle.getId(), map);
	}

	protected void populateLocalizedTitleFields(
		JournalArticle journalArticle, Map<String, String> map) {

		for (Locale locale :
				LanguageUtil.getAvailableLocales(journalArticle.getGroupId())) {

			String title = StringUtil.lowerCase(
				journalArticle.getTitle(locale));

			map.put("localized_title", title);

			String key = StringBundler.concat(
				"localized_title_", LanguageUtil.getLanguageId(locale));

			map.put(key, title);
			map.put(key.concat("_sortable"), title);
		}
	}

	protected void populateTitleFields(
		JournalArticle journalArticle, Map<String, String> map) {

		String[] languageIds = LocalizationUtil.getAvailableLanguageIds(
			journalArticle.getDocument());

		for (String languageId : languageIds) {
			map.put(
				LocalizationUtil.getLocalizedName(Field.TITLE, languageId),
				journalArticle.getTitle(languageId));
		}
	}

}
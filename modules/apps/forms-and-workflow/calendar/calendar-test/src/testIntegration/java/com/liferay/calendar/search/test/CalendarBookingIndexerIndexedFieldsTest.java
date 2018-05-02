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

package com.liferay.calendar.search.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.calendar.constants.CalendarActionKeys;
import com.liferay.calendar.model.Calendar;
import com.liferay.calendar.model.CalendarBooking;
import com.liferay.calendar.model.CalendarResource;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.settings.LocalizedValuesMap;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.Sync;
import com.liferay.portal.kernel.test.rule.SynchronousDestinationTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.search.test.util.FieldValuesAssert;
import com.liferay.portal.search.test.util.IdempotentRetryAssert;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerTestRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Wade Cao
 * @author André de Oliveira
 */
@RunWith(Arquillian.class)
@Sync
public class CalendarBookingIndexerIndexedFieldsTest
	extends BaseCalendarIndexerTestCase {

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

		setGroup(calendarFixture.addGroup());
		setIndexerClass(CalendarBooking.class);
	}

	@Test
	public void testIndexedFields() throws Exception {
		String originalTitle = "entity title";
		String translatedTitle = "entitas neve";

		String description = StringUtil.toLowerCase(
			RandomTestUtil.randomString());

		CalendarBooking calendarBooking = addCalendarBooking(
			new LocalizedValuesMap() {
				{
					put(LocaleUtil.US, originalTitle);
					put(LocaleUtil.HUNGARY, translatedTitle);
				}
			},
			new LocalizedValuesMap() {
				{
					put(LocaleUtil.US, originalTitle);
					put(LocaleUtil.HUNGARY, translatedTitle);
				}
			},
			new LocalizedValuesMap() {
				{
					put(LocaleUtil.US, description);
					put(LocaleUtil.HUNGARY, description);
				}
			});

		Map<String, String> map = new HashMap<>();

		populateExpectedFieldValues(
			calendarBooking, originalTitle, translatedTitle, map);

		String keywords = "nev";

		assertIndexedFields(keywords, LocaleUtil.HUNGARY, map);
	}

	@Test
	public void testIndexedPermissionFields() throws Exception {
		String originalTitle = "entity title";
		String translatedTitle = "entitas neve";

		String description = StringUtil.toLowerCase(
			RandomTestUtil.randomString());

		CalendarBooking calendarBooking = addCalendarBooking(
			new LocalizedValuesMap() {
				{
					put(LocaleUtil.US, originalTitle);
					put(LocaleUtil.HUNGARY, translatedTitle);
				}
			},
			new LocalizedValuesMap() {
				{
					put(LocaleUtil.US, originalTitle);
					put(LocaleUtil.HUNGARY, translatedTitle);
				}
			},
			new LocalizedValuesMap() {
				{
					put(LocaleUtil.US, description);
					put(LocaleUtil.HUNGARY, description);
				}
			});

		Map<String, String> map = new HashMap<>();

		populateExpectedFieldValues(
			calendarBooking, originalTitle, translatedTitle, map);

		String keywords = "nev";

		assertIndexedFields(keywords, LocaleUtil.HUNGARY, map);

		roleFixture.setResourcePermissions(
			calendarBooking.getCompanyId(), RoleConstants.GUEST,
			Calendar.class.getName(), ResourceConstants.SCOPE_INDIVIDUAL,
			String.valueOf(calendarBooking.getCalendarId()),
			new String[] {
				ActionKeys.VIEW, CalendarActionKeys.VIEW_BOOKING_DETAILS
			});

		String documentMapRoleId = map.get(Field.ROLE_ID);

		List<String> expectedRoleIds = Arrays.asList(
			documentMapRoleId,
			String.valueOf(
				roleFixture.getRoleId(
					calendarBooking.getCompanyId(), RoleConstants.GUEST)));

		Collections.sort(expectedRoleIds);

		Assert.assertNotEquals(documentMapRoleId, expectedRoleIds.toString());
		Assert.assertTrue(
			map.replace(
				Field.ROLE_ID, documentMapRoleId, expectedRoleIds.toString()));

		calendarSearchFixture.reindex(calendarBooking);

		assertIndexedFields(keywords, LocaleUtil.HUNGARY, map);

		roleFixture.removeResourcePermission(
			calendarBooking.getCompanyId(), RoleConstants.SITE_MEMBER,
			Calendar.class.getName(), ResourceConstants.SCOPE_INDIVIDUAL,
			String.valueOf(calendarBooking.getCalendarId()),
			CalendarActionKeys.VIEW_BOOKING_DETAILS);

		Assert.assertTrue(map.containsKey(Field.GROUP_ROLE_ID));

		map.remove(Field.GROUP_ROLE_ID);

		Assert.assertFalse(map.containsKey(Field.GROUP_ROLE_ID));

		calendarSearchFixture.reindex(calendarBooking);

		assertIndexedFields(keywords, LocaleUtil.HUNGARY, map);
	}

	@Test
	public void testReindex() throws Exception {
		String originalTitle = "entity title";
		String translatedTitle = "entitas neve";

		String description = StringUtil.toLowerCase(
			RandomTestUtil.randomString());

		CalendarBooking calendarBooking = addCalendarBooking(
			new LocalizedValuesMap() {
				{
					put(LocaleUtil.US, originalTitle);
					put(LocaleUtil.HUNGARY, translatedTitle);
				}
			},
			new LocalizedValuesMap() {
				{
					put(LocaleUtil.US, originalTitle);
					put(LocaleUtil.HUNGARY, translatedTitle);
				}
			},
			new LocalizedValuesMap() {
				{
					put(LocaleUtil.US, description);
					put(LocaleUtil.HUNGARY, description);
				}
			});

		Map<String, String> map = new HashMap<>();

		populateExpectedFieldValues(
			calendarBooking, originalTitle, translatedTitle, map);

		String keywords = "nev";

		assertIndexedFields(keywords, LocaleUtil.HUNGARY, map);

		calendarSearchFixture.reindex(calendarBooking);

		Thread.sleep(3000);

		assertIndexedFields(keywords, LocaleUtil.HUNGARY, map);
	}

	protected CalendarBooking addCalendarBooking(
			LocalizedValuesMap titleLocalizedValuesMap,
			LocalizedValuesMap nameLocalizedValuesMap,
			LocalizedValuesMap descriptionLocalizedValuesMap)
		throws PortalException {

		ServiceContext serviceContext = calendarFixture.getServiceContext();

		Calendar calendar = calendarFixture.addCalendar(
			nameLocalizedValuesMap, descriptionLocalizedValuesMap,
			serviceContext);

		return calendarFixture.addCalendarBooking(
			titleLocalizedValuesMap, calendar, serviceContext);
	}

	protected void assertIndexedFields(
			String keywords, Locale locale, Map<String, String> expectedValues)
		throws Exception {

		IdempotentRetryAssert.retryAssert(
			3, TimeUnit.SECONDS,
			() -> doAssertIndexedFields(keywords, locale, expectedValues));
	}

	protected Void doAssertIndexedFields(
		String keywords, Locale locale, Map<String, String> expectedValues) {

		Document document = calendarSearchFixture.searchOnlyOne(
			keywords, locale);

		indexedFieldsFixture.postProcessDocument(document);

		FieldValuesAssert.assertFieldValues(expectedValues, document, keywords);

		return null;
	}

	protected void populateCalendar(
		Calendar calendar, Map<String, String> map) {

		map.put(Field.DEFAULT_LANGUAGE_ID, calendar.getDefaultLanguageId());
		map.put(Field.USER_ID, String.valueOf(calendar.getUserId()));
		map.put(
			Field.USER_NAME, StringUtil.toLowerCase(calendar.getUserName()));
		map.put("visible", "true");
	}

	protected void populateCalendarBooking(
		CalendarBooking calendarBooking, Map<String, String> map) {

		map.put(
			Field.CLASS_PK, String.valueOf(calendarBooking.getCalendarId()));
		map.put(Field.ENTRY_CLASS_NAME, calendarBooking.getModelClassName());
		map.put(
			Field.ENTRY_CLASS_PK,
			String.valueOf(calendarBooking.getCalendarBookingId()));
		map.put(
			"calendarBookingId",
			String.valueOf(calendarBooking.getCalendarBookingId()));
		map.put("endTime", String.valueOf(calendarBooking.getEndTime()));
		map.put(
			"endTime_sortable", String.valueOf(calendarBooking.getEndTime()));
		map.put("startTime", String.valueOf(calendarBooking.getStartTime()));
		map.put(
			"startTime_sortable",
			String.valueOf(calendarBooking.getStartTime()));

		populateDates(calendarBooking, map);
	}

	protected void populateCalendarResource(
		CalendarResource calendarResource, Map<String, String> map) {

		map.put(
			Field.COMPANY_ID, String.valueOf(calendarResource.getCompanyId()));
		map.put(Field.GROUP_ID, String.valueOf(calendarResource.getGroupId()));
		map.put(
			Field.SCOPE_GROUP_ID,
			String.valueOf(calendarResource.getGroupId()));
	}

	protected void populateDates(
		CalendarBooking calendarBooking, Map<String, String> map) {

		indexedFieldsFixture.populateDate(
			Field.CREATE_DATE, calendarBooking.getCreateDate(), map);
		indexedFieldsFixture.populateDate(
			Field.MODIFIED_DATE, calendarBooking.getModifiedDate(), map);
		indexedFieldsFixture.populateDate(Field.PUBLISH_DATE, new Date(0), map);

		indexedFieldsFixture.populateExpirationDateWithForever(map);
	}

	protected void populateExpectedFieldValues(
			CalendarBooking calendarBooking, String originalTitle,
			String translatedTitle, Map<String, String> map)
		throws Exception {

		map.put(
			Field.CLASS_NAME_ID,
			String.valueOf(portal.getClassNameId(Calendar.class)));
		map.put(Field.RELATED_ENTRY, "true");
		map.put(Field.STAGING_GROUP, "false");
		map.put(Field.STATUS, "0");
		map.put("viewActionId", CalendarActionKeys.VIEW_BOOKING_DETAILS);

		populateTitle(originalTitle, map);
		populateTranslatedTitle(translatedTitle, map);

		CalendarResource calendarResource =
			calendarBooking.getCalendarResource();

		populateCalendarResource(calendarResource, map);

		Calendar calendar = calendarResource.getDefaultCalendar();

		populateCalendar(calendar, map);

		populateCalendarBooking(calendarBooking, map);

		indexedFieldsFixture.populatePriority("0.0", map);
		indexedFieldsFixture.populateRoleIdFields(
			calendarBooking.getCompanyId(), Calendar.class.getName(),
			calendarBooking.getCalendarId(), calendarBooking.getGroupId(),
			CalendarActionKeys.VIEW_BOOKING_DETAILS, map);
		indexedFieldsFixture.populateUID(
			calendarBooking.getModelClassName(),
			calendarBooking.getCalendarBookingId(), map);
	}

	protected void populateTitle(String title, Map<String, String> map) {
		map.put(Field.TITLE + "_en_US", title);

		map.put("localized_title", title);
		map.put("localized_title_ca_ES", title);
		map.put("localized_title_ca_ES_sortable", title);
		map.put("localized_title_de_DE", title);
		map.put("localized_title_de_DE_sortable", title);
		map.put("localized_title_en_US", title);
		map.put("localized_title_en_US_sortable", title);
		map.put("localized_title_es_ES", title);
		map.put("localized_title_es_ES_sortable", title);
		map.put("localized_title_fi_FI", title);
		map.put("localized_title_fi_FI_sortable", title);
		map.put("localized_title_fr_FR", title);
		map.put("localized_title_fr_FR_sortable", title);
		map.put("localized_title_iw_IL", title);
		map.put("localized_title_iw_IL_sortable", title);
		map.put("localized_title_ja_JP", title);
		map.put("localized_title_ja_JP_sortable", title);
		map.put("localized_title_nl_NL", title);
		map.put("localized_title_nl_NL_sortable", title);
		map.put("localized_title_pt_BR", title);
		map.put("localized_title_pt_BR_sortable", title);
		map.put("localized_title_zh_CN", title);
		map.put("localized_title_zh_CN_sortable", title);
	}

	protected void populateTranslatedTitle(
		String title, Map<String, String> map) {

		map.put(Field.TITLE + "_hu_HU", title);

		map.put("localized_title_hu_HU", title);
		map.put("localized_title_hu_HU_sortable", title);
	}

	@Inject
	protected Portal portal;

}
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

package com.liferay.portal.tools.service.builder.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.sql.dsl.DSLQueryFactoryUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.tools.service.builder.test.model.NullConvertibleEntry;
import com.liferay.portal.tools.service.builder.test.model.NullConvertibleEntryTable;
import com.liferay.portal.tools.service.builder.test.service.NullConvertibleEntryLocalService;

import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Kyle Miho
 */
@RunWith(Arquillian.class)
public class NullConvertibleEntryTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testDSLQuery() {
		_nullConvertibleEntry =
			_nullConvertibleEntryLocalService.addNullConvertibleEntry(
				null, null);

		long count = _nullConvertibleEntryLocalService.dslQuery(
			DSLQueryFactoryUtil.count(
			).from(
				NullConvertibleEntryTable.INSTANCE
			).where(
				NullConvertibleEntryTable.INSTANCE.convertedValue.isNull(
				).and(
					NullConvertibleEntryTable.INSTANCE.nonconvertedValue.
						isNull()
				)
			));

		Assert.assertEquals(1, count);
	}

	@Test
	public void testDSLQueryWithBlankString() {
		_nullConvertibleEntry =
			_nullConvertibleEntryLocalService.addNullConvertibleEntry(
				StringPool.BLANK, StringPool.BLANK);

		long count = _nullConvertibleEntryLocalService.dslQuery(
			DSLQueryFactoryUtil.count(
			).from(
				NullConvertibleEntryTable.INSTANCE
			).where(
				NullConvertibleEntryTable.INSTANCE.convertedValue.isNull(
				).and(
					NullConvertibleEntryTable.INSTANCE.nonconvertedValue.
						isNull()
				)
			));

		Assert.assertEquals(1, count);
	}

	@Test
	public void testDynamicQuery() {
		_nullConvertibleEntry =
			_nullConvertibleEntryLocalService.addNullConvertibleEntry(
				null, null);

		DynamicQuery dynamicQuery =
			_nullConvertibleEntryLocalService.dynamicQuery();

		Conjunction conjunction = RestrictionsFactoryUtil.conjunction();

		conjunction.add(RestrictionsFactoryUtil.isNull("convertedValue"));
		conjunction.add(RestrictionsFactoryUtil.isNull("nonconvertedValue"));

		dynamicQuery.add(conjunction);

		List<NullConvertibleEntry> entries =
			_nullConvertibleEntryLocalService.dynamicQuery(dynamicQuery);

		Assert.assertEquals(entries.toString(), 1, entries.size());
	}

	@Test
	public void testDynamicQueryWithBlankString() {
		_nullConvertibleEntry =
			_nullConvertibleEntryLocalService.addNullConvertibleEntry(
				StringPool.BLANK, StringPool.BLANK);

		DynamicQuery dynamicQuery =
			_nullConvertibleEntryLocalService.dynamicQuery();

		Conjunction conjunction = RestrictionsFactoryUtil.conjunction();

		conjunction.add(RestrictionsFactoryUtil.isNull("convertedValue"));
		conjunction.add(RestrictionsFactoryUtil.isNull("nonconvertedValue"));

		dynamicQuery.add(conjunction);

		List<NullConvertibleEntry> entries =
			_nullConvertibleEntryLocalService.dynamicQuery(dynamicQuery);

		Assert.assertEquals(entries.toString(), 1, entries.size());
	}

	@Test
	public void testFetchNullConvertibleEntry() {
		_nullConvertibleEntry =
			_nullConvertibleEntryLocalService.addNullConvertibleEntry(
				null, null);

		Assert.assertEquals(
			_nullConvertibleEntry,
			_nullConvertibleEntryLocalService.fetchNullConvertibleEntry(
				null, null));
	}

	@Test
	public void testFetchNullConvertibleEntryWithBlankString() {
		_nullConvertibleEntry =
			_nullConvertibleEntryLocalService.addNullConvertibleEntry(
				StringPool.BLANK, StringPool.BLANK);

		Assert.assertEquals(
			_nullConvertibleEntry,
			_nullConvertibleEntryLocalService.fetchNullConvertibleEntry(
				StringPool.BLANK, StringPool.BLANK));
	}

	@Test
	public void testGetNullConvertibleEntries() {
		int initialCount =
			_nullConvertibleEntryLocalService.getNullConvertibleEntries(
				null, null);

		_nullConvertibleEntry =
			_nullConvertibleEntryLocalService.addNullConvertibleEntry(
				null, null);

		Assert.assertEquals(
			initialCount + 1,
			_nullConvertibleEntryLocalService.getNullConvertibleEntries(
				null, null));

		_nullConvertibleEntryLocalService.deleteNullConvertibleEntry(
			_nullConvertibleEntry);

		Assert.assertEquals(
			initialCount,
			_nullConvertibleEntryLocalService.getNullConvertibleEntries(
				null, null));
	}

	@DeleteAfterTestRun
	private NullConvertibleEntry _nullConvertibleEntry;

	@Inject
	private NullConvertibleEntryLocalService _nullConvertibleEntryLocalService;

}
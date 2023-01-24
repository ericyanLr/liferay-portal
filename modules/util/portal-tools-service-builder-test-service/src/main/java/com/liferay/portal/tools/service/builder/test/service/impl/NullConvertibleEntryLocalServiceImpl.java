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

package com.liferay.portal.tools.service.builder.test.service.impl;

import com.liferay.portal.tools.service.builder.test.model.NullConvertibleEntry;
import com.liferay.portal.tools.service.builder.test.service.base.NullConvertibleEntryLocalServiceBaseImpl;

/**
 * @author Brian Wing Shun Chan
 */
public class NullConvertibleEntryLocalServiceImpl
	extends NullConvertibleEntryLocalServiceBaseImpl {

	public NullConvertibleEntry addNullConvertibleEntry(
		String convertedValue, String nonconvertedValue) {

		long nullConvertibleEntryId = counterLocalService.increment();

		NullConvertibleEntry nullConvertibleEntry =
			nullConvertibleEntryPersistence.create(nullConvertibleEntryId);

		nullConvertibleEntry.setConvertedValue(convertedValue);
		nullConvertibleEntry.setNonconvertedValue(nonconvertedValue);

		return nullConvertibleEntryPersistence.update(nullConvertibleEntry);
	}

	public NullConvertibleEntry fetchNullConvertibleEntry(
		String convertedValue, String nonconvertedValue) {

		return nullConvertibleEntryPersistence.fetchByC_N(
			convertedValue, nonconvertedValue);
	}

	public int getNullConvertibleEntries(
		String convertedValue, String nonconvertedValue) {

		return nullConvertibleEntryPersistence.countByC_N(
			convertedValue, nonconvertedValue);
	}

}
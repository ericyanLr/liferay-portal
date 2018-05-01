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

package com.liferay.calendar.internal.search;

import com.liferay.calendar.model.Calendar;
import com.liferay.calendar.model.CalendarBooking;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.DocumentHelper;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.search.spi.model.index.contributor.ModelDocumentContributor;
import com.liferay.portal.search.spi.model.index.contributor.SearchPermissionModelDocumentContributor;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Eric Yan
 */
@Component(
	immediate = true,
	property = "indexer.class.name=com.liferay.calendar.model.CalendarBooking",
	service = {
		ModelDocumentContributor.class,
		SearchPermissionModelDocumentContributor.class
	}
)
public class CalendarBookingRelatedEntryModelDocumentContributor
	implements ModelDocumentContributor<CalendarBooking>,
			   SearchPermissionModelDocumentContributor<CalendarBooking> {

	@Override
	public void contribute(Document document, CalendarBooking calendarBooking) {
		DocumentHelper documentHelper = new DocumentHelper(document);

		documentHelper.setAttachmentOwnerKey(
			portal.getClassNameId(Calendar.class),
			calendarBooking.getCalendarId());

		document.addKeyword(Field.RELATED_ENTRY, true);
	}

	@Reference
	protected Portal portal;

}
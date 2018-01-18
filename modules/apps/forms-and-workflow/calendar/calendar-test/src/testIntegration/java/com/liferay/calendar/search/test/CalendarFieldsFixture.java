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

import com.liferay.calendar.model.Calendar;
import com.liferay.calendar.model.CalendarBooking;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.SearchEngine;
import com.liferay.portal.kernel.search.SearchEngineHelperUtil;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Wade Cao
 * @author André de Oliveira
 */
public class CalendarFieldsFixture {

	public CalendarFieldsFixture(RoleLocalService roleLocalService) {
		_roleLocalService = roleLocalService;
	}

	public boolean isSearchEngineElasticsearch() {
		SearchEngine searchEngine = SearchEngineHelperUtil.getSearchEngine(
			SearchEngineHelperUtil.getDefaultSearchEngineId());

		String vendor = searchEngine.getVendor();

		return vendor.equals("Elasticsearch");
	}

	public boolean isSearchEngineSolr() {
		SearchEngine searchEngine = SearchEngineHelperUtil.getSearchEngine(
			SearchEngineHelperUtil.getDefaultSearchEngineId());

		String vendor = searchEngine.getVendor();

		return vendor.equals("Solr");
	}

	public void populateGroupRoleId(Map<String, String> fieldValues)
		throws PortalException {

		Role role = _roleLocalService.getDefaultGroupRole(_group.getGroupId());

		fieldValues.put(
			Field.GROUP_ROLE_ID,
			_group.getGroupId() + StringPool.DASH + role.getRoleId());
	}

	public void populateRoleId(
		long companyId, String entryClassName, long entryClassPK,
		String viewActionId, Map<String, String> fieldValues) {

		if (Validator.isNull(viewActionId)) {
			viewActionId = ActionKeys.VIEW;
		}

		List<Role> roles = _roleLocalService.getResourceRoles(
			companyId, entryClassName, ResourceConstants.SCOPE_INDIVIDUAL,
			Long.toString(entryClassPK), viewActionId);

		List<Long> roleIds = new ArrayList<>();

		for (Role role : roles) {
			if ((role.getType() == RoleConstants.TYPE_ORGANIZATION) ||
				(role.getType() == RoleConstants.TYPE_SITE)) {

				continue;
			}

			roleIds.add(role.getRoleId());
		}

		if (roleIds.size() == 1) {
			fieldValues.put(Field.ROLE_ID, String.valueOf(roleIds.get(0)));
		}
		else if (roleIds.size() > 1) {
			fieldValues.put(Field.ROLE_ID, roleIds.toString());
		}
	}

	public void populateUID(
		Calendar calendar, Map<String, String> fieldValues) {

		fieldValues.put(
			Field.UID,
			calendar.getModelClassName() + "_PORTLET_" +
				calendar.getCalendarId());
	}

	public void populateUID(
		CalendarBooking calendarBooking, Map<String, String> fieldValues) {

		fieldValues.put(
			Field.UID,
			calendarBooking.getModelClassName() + "_PORTLET_" +
				calendarBooking.getCalendarBookingId());
	}

	public void postProcessDocument(Document document) {
		if (isSearchEngineSolr()) {
			document.remove("score");
		}
	}

	public void setGroup(Group group) {
		_group = group;
	}

	private Group _group;
	private final RoleLocalService _roleLocalService;

}
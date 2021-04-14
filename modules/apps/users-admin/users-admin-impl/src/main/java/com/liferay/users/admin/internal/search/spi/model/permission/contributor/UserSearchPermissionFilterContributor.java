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

package com.liferay.users.admin.internal.search.spi.model.permission.contributor;

import com.liferay.petra.sql.dsl.DSLQueryFactoryUtil;
import com.liferay.petra.sql.dsl.query.DSLQuery;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ContactTable;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.filter.BooleanFilter;
import com.liferay.portal.kernel.search.filter.TermsFilter;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.service.ContactLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.search.spi.model.permission.SearchPermissionFilterContributor;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;

/**
 * @author Jesse Yeh
 */
@Component(
	immediate = true,
	property = "indexer.class.name=com.liferay.portal.kernel.model.User",
	service = SearchPermissionFilterContributor.class
)
public class UserSearchPermissionFilterContributor
	implements SearchPermissionFilterContributor {

	@Override
	public void contribute(
		BooleanFilter booleanFilter, long companyId, long[] groupIds,
		long userId, PermissionChecker permissionChecker, String className) {

		if (!className.equals(User.class.getName())) {
			return;
		}

		ServiceContext serviceContext =
			ServiceContextThreadLocal.getServiceContext();

		if ((serviceContext != null) &&
			permissionChecker.isGroupAdmin(serviceContext.getScopeGroupId())) {

			try {
				TermsFilter roleIdsTermsFilter = new TermsFilter(
					Field.ROLE_IDS);

				Role role = roleLocalService.getRole(
					companyId, RoleConstants.USER);

				roleIdsTermsFilter.addValue(String.valueOf(role.getRoleId()));

				booleanFilter.add(roleIdsTermsFilter);
			}
			catch (PortalException portalException) {
				_log.error(
					"Unable to get the User role for company " + companyId,
					portalException);
			}
		}
		else {
			DSLQuery dslQuery = DSLQueryFactoryUtil.select(
				ContactTable.INSTANCE.classPK
			).from(
				ContactTable.INSTANCE
			).where(
				ContactTable.INSTANCE.userId.eq(userId)
			);

			List<Long> createdUsersIds = contactLocalService.dslQuery(dslQuery);

			if (!createdUsersIds.isEmpty()) {
				TermsFilter userIdTermsFilter = new TermsFilter(
					Field.USER_ID);

				userIdTermsFilter.addValues(
					ArrayUtil.toStringArray(
						createdUsersIds.toArray(new Long[0])));

				booleanFilter.add(userIdTermsFilter);
			}
		}
	}

	@Reference
	protected ContactLocalService contactLocalService;

	@Reference
	protected RoleLocalService roleLocalService;

	private static final Log _log = LogFactoryUtil.getLog(
		UserSearchPermissionFilterContributor.class);

}
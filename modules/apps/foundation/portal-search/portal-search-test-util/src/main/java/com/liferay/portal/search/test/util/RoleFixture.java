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

package com.liferay.portal.search.test.util;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.service.ResourcePermissionLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;

/**
 * @author Eric Yan
 */
public class RoleFixture {

	public RoleFixture(
		ResourcePermissionLocalService resourcePermissionLocalService,
		RoleLocalService roleLocalService) {

		_resourcePermissionLocalService = resourcePermissionLocalService;
		_roleLocalService = roleLocalService;
	}

	public void addResourcePermission(
			long companyId, String roleName, String resourceName, int scope,
			String primKey, String actionId)
		throws PortalException {

		Role role = _roleLocalService.getRole(companyId, roleName);

		addResourcePermission(role, resourceName, scope, primKey, actionId);
	}

	public void addResourcePermission(
			Role role, String resourceName, int scope, String primKey,
			String actionId)
		throws PortalException {

		_resourcePermissionLocalService.addResourcePermission(
			role.getCompanyId(), resourceName, scope, primKey, role.getRoleId(),
			actionId);
	}

	public long getRoleId(long companyId, String name) throws PortalException {
		Role role = _roleLocalService.getRole(companyId, name);

		return role.getRoleId();
	}

	public void removeResourcePermission(
			long companyId, String roleName, String resourceName, int scope,
			String primKey, String actionId)
		throws PortalException {

		Role role = _roleLocalService.getRole(companyId, roleName);

		_resourcePermissionLocalService.removeResourcePermission(
			role.getCompanyId(), resourceName, scope, primKey, role.getRoleId(),
			actionId);
	}

	public void setResourcePermissions(
			long companyId, String roleName, String resourceName, int scope,
			String primKey, String[] actionIds)
		throws PortalException {

		Role role = _roleLocalService.getRole(companyId, roleName);

		_resourcePermissionLocalService.setResourcePermissions(
			role.getCompanyId(), resourceName, scope, primKey, role.getRoleId(),
			actionIds);
	}

	public void setResourcePermissions(
			Role role, String resourceName, int scope, String primKey,
			String[] actionIds)
		throws PortalException {

		_resourcePermissionLocalService.setResourcePermissions(
			role.getCompanyId(), resourceName, scope, primKey, role.getRoleId(),
			actionIds);
	}

	private final ResourcePermissionLocalService
		_resourcePermissionLocalService;
	private final RoleLocalService _roleLocalService;

}
/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.service.builder.test.service.impl;

import com.liferay.portal.tools.service.builder.test.model.PermissionCheckFinderEntry;
import com.liferay.portal.tools.service.builder.test.service.base.PermissionCheckFinderEntryLocalServiceBaseImpl;

/**
 * @author Brian Wing Shun Chan
 */
public class PermissionCheckFinderEntryLocalServiceImpl
	extends PermissionCheckFinderEntryLocalServiceBaseImpl {

	public PermissionCheckFinderEntry addPermissionCheckFinderEntry(
		long groupId, int integer, String name, String type) {

		PermissionCheckFinderEntry permissionCheckFinderEntry =
			permissionCheckFinderEntryPersistence.create(
				counterLocalService.increment());

		permissionCheckFinderEntry.setGroupId(groupId);

		permissionCheckFinderEntry.setInteger(integer);

		permissionCheckFinderEntry.setName(name);

		permissionCheckFinderEntry.setType(type);

		permissionCheckFinderEntry =
			permissionCheckFinderEntryPersistence.update(
				permissionCheckFinderEntry);

		return permissionCheckFinderEntry;
	}

	public java.util.List<PermissionCheckFinderEntry> filterFindByGroupId(
		long groupId) {

		return permissionCheckFinderEntryPersistence.filterFindByGroupId(
			groupId);
	}

	public java.util.List<PermissionCheckFinderEntry> filterFindByGroupId(
		long[] groupIds) {

		return permissionCheckFinderEntryPersistence.filterFindByGroupId(
			groupIds);
	}

}
/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.service.builder.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.ResourcePermission;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.ResourcePermissionLocalService;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.UserGroupRoleLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.RoleTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.tools.service.builder.test.model.PermissionCheckFinderEntry;
import com.liferay.portal.tools.service.builder.test.service.PermissionCheckFinderEntryLocalService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Eric Yan
 */
@RunWith(Arquillian.class)
public class PermissionCheckFinderEntryTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_adminGroupId = TestPropsValues.getGroupId();
		_adminUser = TestPropsValues.getUser();
		_group1 = GroupTestUtil.addGroup();
		_group2 = GroupTestUtil.addGroup();
		_ownerRole = RoleLocalServiceUtil.getRole(
			TestPropsValues.getCompanyId(), RoleConstants.OWNER);
		_permissionedUser = UserTestUtil.addUser(new long[0]);
		_user = UserTestUtil.addUser(new long[0]);
	}

	@Test
	public void testFilterFindByGroupId() throws Exception {
		_testFilterFindByGroupId(ResourceConstants.SCOPE_INDIVIDUAL);
	}

	@Test
	public void testFilterFindByGroupIdWithScopeCompany() throws Exception {
		_role = RoleTestUtil.addRole(RoleConstants.TYPE_REGULAR);

		_userGroupRoleLocalService.addUserGroupRole(
			_permissionedUser.getUserId(), _group1.getGroupId(),
			_role.getRoleId());
		_userGroupRoleLocalService.addUserGroupRole(
			_permissionedUser.getUserId(), _group2.getGroupId(),
			_role.getRoleId());

		_addResourcePermission(
			TestPropsValues.getCompanyId(),
			PermissionCheckFinderEntry.class.getName(),
			ResourceConstants.SCOPE_COMPANY,
			String.valueOf(TestPropsValues.getCompanyId()), _role.getRoleId(),
			ActionKeys.VIEW);

		_testFilterFindByGroupId(ResourceConstants.SCOPE_COMPANY);
	}

	@Test
	public void testFilterFindByGroupIdWithScopeGroup() throws Exception {
		_role = RoleTestUtil.addRole(RoleConstants.TYPE_REGULAR);

		_userGroupRoleLocalService.addUserGroupRole(
			_permissionedUser.getUserId(), _group1.getGroupId(),
			_role.getRoleId());
		_userGroupRoleLocalService.addUserGroupRole(
			_permissionedUser.getUserId(), _group2.getGroupId(),
			_role.getRoleId());

		_addResourcePermission(
			TestPropsValues.getCompanyId(),
			PermissionCheckFinderEntry.class.getName(),
			ResourceConstants.SCOPE_GROUP, String.valueOf(_group1.getGroupId()),
			_role.getRoleId(), ActionKeys.VIEW);
		_addResourcePermission(
			TestPropsValues.getCompanyId(),
			PermissionCheckFinderEntry.class.getName(),
			ResourceConstants.SCOPE_GROUP, String.valueOf(_group2.getGroupId()),
			_role.getRoleId(), ActionKeys.VIEW);

		_testFilterFindByGroupId(ResourceConstants.SCOPE_GROUP);
	}

	@Test
	public void testFilterFindByGroupIdWithScopeGroupTemplate()
		throws Exception {

		_role = RoleTestUtil.addRole(RoleConstants.TYPE_SITE);

		_userGroupRoleLocalService.addUserGroupRole(
			_permissionedUser.getUserId(), _group1.getGroupId(),
			_role.getRoleId());
		_userGroupRoleLocalService.addUserGroupRole(
			_permissionedUser.getUserId(), _group2.getGroupId(),
			_role.getRoleId());

		_addResourcePermission(
			TestPropsValues.getCompanyId(),
			PermissionCheckFinderEntry.class.getName(),
			ResourceConstants.SCOPE_GROUP_TEMPLATE,
			String.valueOf(GroupConstants.DEFAULT_PARENT_GROUP_ID),
			_role.getRoleId(), ActionKeys.VIEW);

		_testFilterFindByGroupId(ResourceConstants.SCOPE_GROUP_TEMPLATE);
	}

	private PermissionCheckFinderEntry _addPermissionCheckFinderEntry(
			long groupId, long userId)
		throws Exception {

		PermissionCheckFinderEntry permissionCheckFinderEntry =
			_permissionCheckFinderEntryLocalService.
				addPermissionCheckFinderEntry(
					TestPropsValues.getCompanyId(), groupId,
					RandomTestUtil.nextInt(), RandomTestUtil.randomString(),
					RandomTestUtil.randomString(), userId);

		_permissionCheckFinderEntries.add(permissionCheckFinderEntry);
		_resourcePermissions.add(
			_resourcePermissionLocalService.getResourcePermission(
				TestPropsValues.getCompanyId(),
				PermissionCheckFinderEntry.class.getName(),
				ResourceConstants.SCOPE_INDIVIDUAL,
				String.valueOf(permissionCheckFinderEntry.getPrimaryKey()),
				_ownerRole.getRoleId()));

		return permissionCheckFinderEntry;
	}

	private void _addResourcePermission(
			long companyId, String name, int scope, String primKey, long roleId,
			String actionId)
		throws Exception {

		_resourcePermissionLocalService.addResourcePermission(
			companyId, name, scope, primKey, roleId, actionId);

		_resourcePermissions.add(
			_resourcePermissionLocalService.getResourcePermission(
				companyId, name, scope, primKey, roleId));
	}

	private void _testFilterFindByGroupId(int permissionedUserScope)
		throws Exception {

		PermissionCheckFinderEntry permissionCheckFinderEntry1 =
			_addPermissionCheckFinderEntry(
				_group1.getGroupId(), _user.getUserId());
		PermissionCheckFinderEntry permissionCheckFinderEntry2 =
			_addPermissionCheckFinderEntry(
				_group2.getGroupId(), _user.getUserId());
		PermissionCheckFinderEntry permissionCheckFinderEntry3 =
			_addPermissionCheckFinderEntry(
				_adminGroupId, _adminUser.getUserId());

		PermissionChecker permissionChecker =
			PermissionThreadLocal.getPermissionChecker();

		try {
			PermissionThreadLocal.setPermissionChecker(
				PermissionCheckerFactoryUtil.create(_adminUser));

			Assert.assertEquals(
				Collections.singletonList(permissionCheckFinderEntry1),
				_permissionCheckFinderEntryLocalService.filterFindByGroupId(
					new long[] {_group1.getGroupId()}));
			Assert.assertEquals(
				Collections.singletonList(permissionCheckFinderEntry2),
				_permissionCheckFinderEntryLocalService.filterFindByGroupId(
					new long[] {_group2.getGroupId()}));
			Assert.assertEquals(
				Collections.singletonList(permissionCheckFinderEntry3),
				_permissionCheckFinderEntryLocalService.filterFindByGroupId(
					new long[] {_adminGroupId}));
			Assert.assertEquals(
				Arrays.asList(
					permissionCheckFinderEntry1, permissionCheckFinderEntry2),
				_permissionCheckFinderEntryLocalService.filterFindByGroupId(
					new long[] {_group1.getGroupId(), _group2.getGroupId()}));
			Assert.assertEquals(
				Arrays.asList(
					permissionCheckFinderEntry1, permissionCheckFinderEntry3),
				_permissionCheckFinderEntryLocalService.filterFindByGroupId(
					new long[] {_group1.getGroupId(), _adminGroupId}));

			PermissionThreadLocal.setPermissionChecker(
				PermissionCheckerFactoryUtil.create(_user));

			Assert.assertEquals(
				Collections.singletonList(permissionCheckFinderEntry1),
				_permissionCheckFinderEntryLocalService.filterFindByGroupId(
					new long[] {_group1.getGroupId()}));
			Assert.assertEquals(
				Collections.singletonList(permissionCheckFinderEntry2),
				_permissionCheckFinderEntryLocalService.filterFindByGroupId(
					new long[] {_group2.getGroupId()}));
			Assert.assertEquals(
				Collections.emptyList(),
				_permissionCheckFinderEntryLocalService.filterFindByGroupId(
					new long[] {_adminGroupId}));
			Assert.assertEquals(
				Arrays.asList(
					permissionCheckFinderEntry1, permissionCheckFinderEntry2),
				_permissionCheckFinderEntryLocalService.filterFindByGroupId(
					new long[] {_group1.getGroupId(), _group2.getGroupId()}));
			Assert.assertEquals(
				Collections.singletonList(permissionCheckFinderEntry1),
				_permissionCheckFinderEntryLocalService.filterFindByGroupId(
					new long[] {_group1.getGroupId(), _adminGroupId}));

			PermissionThreadLocal.setPermissionChecker(
				PermissionCheckerFactoryUtil.create(_permissionedUser));

			if (permissionedUserScope == ResourceConstants.SCOPE_COMPANY) {
				Assert.assertEquals(
					Collections.singletonList(permissionCheckFinderEntry1),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_group1.getGroupId()}));
				Assert.assertEquals(
					Collections.singletonList(permissionCheckFinderEntry2),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_group2.getGroupId()}));
				Assert.assertEquals(
					Collections.emptyList(),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_adminGroupId}));
				Assert.assertEquals(
					Arrays.asList(
						permissionCheckFinderEntry1,
						permissionCheckFinderEntry2),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {
							_group1.getGroupId(), _group2.getGroupId()
						}));
				Assert.assertEquals(
					Arrays.asList(
						permissionCheckFinderEntry1,
						permissionCheckFinderEntry3),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_group1.getGroupId(), _adminGroupId}));
			}
			else if (permissionedUserScope == ResourceConstants.SCOPE_GROUP) {
				Assert.assertEquals(
					Collections.singletonList(permissionCheckFinderEntry1),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_group1.getGroupId()}));
				Assert.assertEquals(
					Collections.singletonList(permissionCheckFinderEntry2),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_group2.getGroupId()}));
				Assert.assertEquals(
					Collections.emptyList(),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_adminGroupId}));
				Assert.assertEquals(
					Arrays.asList(
						permissionCheckFinderEntry1,
						permissionCheckFinderEntry2),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {
							_group1.getGroupId(), _group2.getGroupId()
						}));
				Assert.assertEquals(
					Collections.singletonList(permissionCheckFinderEntry1),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_group1.getGroupId(), _adminGroupId}));
			}
			else if (permissionedUserScope ==
						ResourceConstants.SCOPE_GROUP_TEMPLATE) {

				Assert.assertEquals(
					Collections.singletonList(permissionCheckFinderEntry1),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_group1.getGroupId()}));
				Assert.assertEquals(
					Collections.singletonList(permissionCheckFinderEntry2),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_group2.getGroupId()}));
				Assert.assertEquals(
					Collections.emptyList(),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_adminGroupId}));
				Assert.assertEquals(
					Arrays.asList(
						permissionCheckFinderEntry1,
						permissionCheckFinderEntry2),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {
							_group1.getGroupId(), _group2.getGroupId()
						}));
				Assert.assertEquals(
					Collections.singletonList(permissionCheckFinderEntry1),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_group1.getGroupId(), _adminGroupId}));
			}
			else {
				Assert.assertEquals(
					Collections.emptyList(),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_group1.getGroupId()}));
				Assert.assertEquals(
					Collections.emptyList(),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_group2.getGroupId()}));
				Assert.assertEquals(
					Collections.emptyList(),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_adminGroupId}));
				Assert.assertEquals(
					Collections.emptyList(),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {
							_group1.getGroupId(), _group2.getGroupId()
						}));
				Assert.assertEquals(
					Collections.emptyList(),
					_permissionCheckFinderEntryLocalService.filterFindByGroupId(
						new long[] {_group1.getGroupId(), _adminGroupId}));
			}
		}
		finally {
			PermissionThreadLocal.setPermissionChecker(permissionChecker);
		}
	}

	private long _adminGroupId;
	private User _adminUser;

	@DeleteAfterTestRun
	private Group _group1;

	@DeleteAfterTestRun
	private Group _group2;

	private Role _ownerRole;

	@DeleteAfterTestRun
	private List<PermissionCheckFinderEntry> _permissionCheckFinderEntries =
		new ArrayList<>();

	@Inject
	private PermissionCheckFinderEntryLocalService
		_permissionCheckFinderEntryLocalService;

	@DeleteAfterTestRun
	private User _permissionedUser;

	@Inject
	private ResourcePermissionLocalService _resourcePermissionLocalService;

	@DeleteAfterTestRun
	private List<ResourcePermission> _resourcePermissions = new ArrayList<>();

	@DeleteAfterTestRun
	private Role _role;

	@DeleteAfterTestRun
	private User _user;

	@Inject
	private UserGroupRoleLocalService _userGroupRoleLocalService;

}
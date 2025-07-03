package com.liferay.portal.tools.service.builder.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.UserGroupRole;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.InlineSQLHelperUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.ResourceLocalServiceUtil;
import com.liferay.portal.kernel.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.kernel.service.UserGroupRoleLocalServiceUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.RoleTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.security.permission.SimplePermissionChecker;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.tools.service.builder.test.model.PermissionCheckFinderEntry;
import com.liferay.portal.tools.service.builder.test.service.PermissionCheckFinderEntryLocalService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class PermissionCheckFinderEntryTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testFilterFindByGroupId() throws Exception {
		long[] roleIds = new long[1];

		PermissionThreadLocal.setPermissionChecker(
			new SimplePermissionChecker() {
				{
					init(TestPropsValues.getUser());
				}

				@Override
				public long[] getRoleIds(long userId, long groupId) {
					return roleIds;
				}

				@Override
				public boolean isCompanyAdmin(long companyId) {
					return false;
				}

				@Override
				public boolean isGroupAdmin(long groupId) {
					return false;
				}

				@Override
				public boolean isGroupOwner(long groupId) {
					return false;
				}

			});

		Assert.assertTrue(InlineSQLHelperUtil.isEnabled(0));

		_permissionCheckFinderEntryLocalService.filterFindByGroupId(0);

		// Test scope: GROUP

		Group group1 = GroupTestUtil.addGroup();

		_groups.add(group1);

		PermissionCheckFinderEntry newPermissionCheckFinderEntry1 =
			_permissionCheckFinderEntryLocalService.
				addPermissionCheckFinderEntry(
					group1.getGroupId(), 1, RandomTestUtil.randomString(),
					RandomTestUtil.randomString());

		ResourceLocalServiceUtil.addResources(
			TestPropsValues.getCompanyId(), group1.getGroupId(),
			TestPropsValues.getUserId(),
			PermissionCheckFinderEntry.class.getName(),
			newPermissionCheckFinderEntry1.getPrimaryKey(), false, true, false);

		Group group2 = GroupTestUtil.addGroup();

		_groups.add(group2);

		PermissionCheckFinderEntry newPermissionCheckFinderEntry2 =
			_permissionCheckFinderEntryLocalService.
				addPermissionCheckFinderEntry(
					group2.getGroupId(), 2, RandomTestUtil.randomString(),
					RandomTestUtil.randomString());

		ResourceLocalServiceUtil.addResources(
			TestPropsValues.getCompanyId(), group1.getGroupId(),
			TestPropsValues.getUserId(),
			PermissionCheckFinderEntry.class.getName(),
			newPermissionCheckFinderEntry2.getPrimaryKey(), false, true, false);

		Role role = RoleTestUtil.addRole(RoleConstants.TYPE_SITE);

		roleIds[0] = role.getRoleId();

		_roles.add(role);

		_userGroupRoles.addAll(
			UserGroupRoleLocalServiceUtil.addUserGroupRoles(
				new long[] {TestPropsValues.getUserId()}, group1.getGroupId(),
				role.getRoleId()));
		_userGroupRoles.addAll(
			UserGroupRoleLocalServiceUtil.addUserGroupRoles(
				new long[] {TestPropsValues.getUserId()}, group2.getGroupId(),
				role.getRoleId()));

		ResourcePermissionLocalServiceUtil.addResourcePermission(
			TestPropsValues.getCompanyId(),
			PermissionCheckFinderEntry.class.getName(),
			ResourceConstants.SCOPE_GROUP, String.valueOf(group1.getGroupId()),
			role.getRoleId(), ActionKeys.VIEW);
		ResourcePermissionLocalServiceUtil.addResourcePermission(
			TestPropsValues.getCompanyId(),
			PermissionCheckFinderEntry.class.getName(),
			ResourceConstants.SCOPE_GROUP, String.valueOf(group2.getGroupId()),
			role.getRoleId(), ActionKeys.VIEW);

		Assert.assertTrue(InlineSQLHelperUtil.isEnabled(group1.getGroupId()));
		Assert.assertTrue(InlineSQLHelperUtil.isEnabled(group2.getGroupId()));

		Assert.assertEquals(
			Arrays.asList(newPermissionCheckFinderEntry1),
			_permissionCheckFinderEntryLocalService.filterFindByGroupId(
				group1.getGroupId()));
		Assert.assertEquals(
			Arrays.asList(newPermissionCheckFinderEntry2),
			_permissionCheckFinderEntryLocalService.filterFindByGroupId(
				group2.getGroupId()));

		Assert.assertEquals(
			Arrays.asList(newPermissionCheckFinderEntry1),
			_permissionCheckFinderEntryLocalService.filterFindByGroupId(
				new long[] {group1.getGroupId()}));
		Assert.assertEquals(
			Arrays.asList(newPermissionCheckFinderEntry2),
			_permissionCheckFinderEntryLocalService.filterFindByGroupId(
				new long[] {group2.getGroupId()}));

		Assert.assertEquals(
			Arrays.asList(
				newPermissionCheckFinderEntry1, newPermissionCheckFinderEntry2),
			_permissionCheckFinderEntryLocalService.filterFindByGroupId(
				new long[] {group1.getGroupId(), group2.getGroupId()}));
	}

	@DeleteAfterTestRun
	private List<Group> _groups = new ArrayList<>();

	@Inject
	private PermissionCheckFinderEntryLocalService
		_permissionCheckFinderEntryLocalService;

	@DeleteAfterTestRun
	private List<Role> _roles = new ArrayList<>();

	@DeleteAfterTestRun
	private List<UserGroupRole> _userGroupRoles = new ArrayList<>();

}
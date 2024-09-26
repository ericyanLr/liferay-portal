/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.permission.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.model.Resource;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.ResourceActions;
import com.liferay.portal.kernel.service.ResourceLocalService;
import com.liferay.portal.kernel.service.ResourcePermissionLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.model.impl.PortletImpl;
import com.liferay.portal.model.impl.ResourceImpl;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Manuel de la Peña
 */
@RunWith(Arquillian.class)
public class ResourcePermissionLocalServiceTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testModelResourceActionsDefaults() throws Exception {
		_testModelResourceActionsDefaults(
			Arrays.asList(ActionKeys.VIEW), Arrays.asList(ActionKeys.VIEW),
			Arrays.asList(ActionKeys.VIEW),
			_RESOURCE_ACTIONS_DEFAULTS_MODEL_NAME, _RESOURCE_ACTIONS_XML_PATH);
	}

	@Test
	public void testModelResourceActionsDefaultsWithEmptyDefaultActions()
		throws Exception {

		_testModelResourceActionsDefaults(
			Collections.emptyList(), Collections.emptyList(),
			Collections.emptyList(),
			_RESOURCE_ACTIONS_EMPTY_DEFAULTS_MODEL_NAME,
			_RESOURCE_ACTIONS_XML_PATH);
	}

	@Test
	public void testModelResourceActionsDefaultsWithOverride()
		throws Exception {

		_testModelResourceActionsDefaults(
			Arrays.asList(ActionKeys.ADD_TO_PAGE),
			Arrays.asList(ActionKeys.ADD_TO_PAGE),
			Arrays.asList(ActionKeys.ADD_TO_PAGE),
			_RESOURCE_ACTIONS_DEFAULTS_MODEL_NAME, _RESOURCE_ACTIONS_XML_PATH,
			_RESOURCE_ACTIONS_OVERRIDE_XML_PATH);
	}

	@Test
	public void testModelResourceActionsDefaultsWithUndeclaredDefaultActions()
		throws Exception {

		_testModelResourceActionsDefaults(
			Collections.emptyList(),
			Arrays.asList(
				ActionKeys.ADD_TO_PAGE, ActionKeys.CONFIGURATION,
				ActionKeys.PERMISSIONS, ActionKeys.VIEW),
			Collections.emptyList(),
			_RESOURCE_ACTIONS_UNDECLARED_DEFAULTS_MODEL_NAME,
			_RESOURCE_ACTIONS_XML_PATH);
	}

	@Test
	public void testPortletResourceActionsDefaults() throws Exception {
		_testPortletResourceActionsDefaults(
			Arrays.asList(ActionKeys.VIEW), Arrays.asList(ActionKeys.VIEW),
			Arrays.asList(ActionKeys.VIEW),
			_RESOURCE_ACTIONS_DEFAULTS_PORTLET_NAME,
			_RESOURCE_ACTIONS_XML_PATH);
	}

	@Test
	public void testPortletResourceActionsDefaultsWithEmptyDefaultActions()
		throws Exception {

		_testPortletResourceActionsDefaults(
			Collections.emptyList(), Collections.emptyList(),
			Collections.emptyList(),
			_RESOURCE_ACTIONS_EMPTY_DEFAULTS_PORTLET_NAME,
			_RESOURCE_ACTIONS_XML_PATH);
	}

	@Test
	public void testPortletResourceActionsDefaultsWithOverride()
		throws Exception {

		_testPortletResourceActionsDefaults(
			Arrays.asList(ActionKeys.ADD_TO_PAGE),
			Arrays.asList(ActionKeys.ADD_TO_PAGE),
			Arrays.asList(ActionKeys.ADD_TO_PAGE),
			_RESOURCE_ACTIONS_DEFAULTS_PORTLET_NAME, _RESOURCE_ACTIONS_XML_PATH,
			_RESOURCE_ACTIONS_OVERRIDE_XML_PATH);
	}

	@Test
	public void testPortletResourceActionsDefaultsWithUndeclaredDefaultActions()
		throws Exception {

		_testPortletResourceActionsDefaults(
			Collections.emptyList(),
			Arrays.asList(
				ActionKeys.ADD_TO_PAGE, ActionKeys.CONFIGURATION,
				ActionKeys.PERMISSIONS, ActionKeys.PREFERENCES,
				ActionKeys.VIEW),
			Collections.emptyList(),
			_RESOURCE_ACTIONS_UNDECLARED_DEFAULTS_PORTLET_NAME,
			_RESOURCE_ACTIONS_XML_PATH);
	}

	@Test
	public void testShouldFailIfFirstResourceIsNotIndividual()
		throws Exception {

		_testResources(
			"The first resource must be an individual scope",
			Arrays.asList(
				_createResource(ResourceConstants.SCOPE_GROUP),
				_createResource(ResourceConstants.SCOPE_COMPANY)));
	}

	@Test
	public void testShouldFailIfLastResourceIsNotCompany() throws Exception {
		_testResources(
			"The last resource must be a company scope",
			Arrays.asList(
				_createResource(ResourceConstants.SCOPE_INDIVIDUAL),
				_createResource(ResourceConstants.SCOPE_GROUP)));
	}

	@Test
	public void testShouldFailIfResourcesIsLessThanTwo() throws Exception {
		_testResources(
			"The list of resources must contain at least two values",
			Arrays.asList(new ResourceImpl()));
	}

	private void _assertResourceActionsDefaults(
			List<String> expectedGuestDefaultActions,
			List<String> expectedOwnerDefaultActions,
			List<String> expectedSiteMemberDefaultActions, String primKey,
			String resourceName, List<String> supportActionIds)
		throws Exception {

		Role guestRole = _roleLocalService.getRole(
			TestPropsValues.getCompanyId(), RoleConstants.GUEST);

		List<String> actualGuestActions =
			_resourcePermissionLocalService.
				getAvailableResourcePermissionActionIds(
					TestPropsValues.getCompanyId(), resourceName,
					ResourceConstants.SCOPE_INDIVIDUAL, String.valueOf(primKey),
					guestRole.getRoleId(), supportActionIds);

		Collections.sort(actualGuestActions);

		Assert.assertEquals(expectedGuestDefaultActions, actualGuestActions);

		Role ownerRole = _roleLocalService.getRole(
			TestPropsValues.getCompanyId(), RoleConstants.OWNER);

		List<String> actualOwnerActions =
			_resourcePermissionLocalService.
				getAvailableResourcePermissionActionIds(
					TestPropsValues.getCompanyId(), resourceName,
					ResourceConstants.SCOPE_INDIVIDUAL, String.valueOf(primKey),
					ownerRole.getRoleId(), supportActionIds);

		Collections.sort(actualOwnerActions);

		Assert.assertEquals(expectedOwnerDefaultActions, actualOwnerActions);

		Role siteMemberRole = _roleLocalService.getRole(
			TestPropsValues.getCompanyId(), RoleConstants.SITE_MEMBER);

		List<String> actualSiteMemberActions =
			_resourcePermissionLocalService.
				getAvailableResourcePermissionActionIds(
					TestPropsValues.getCompanyId(), resourceName,
					ResourceConstants.SCOPE_INDIVIDUAL, String.valueOf(primKey),
					siteMemberRole.getRoleId(), supportActionIds);

		Collections.sort(actualSiteMemberActions);

		Assert.assertEquals(
			expectedSiteMemberDefaultActions, actualSiteMemberActions);
	}

	private Resource _createResource(int scope) {
		Resource resource = new ResourceImpl();

		resource.setScope(scope);

		return resource;
	}

	private void _testModelResourceActionsDefaults(
			List<String> expectedGuestDefaultActions,
			List<String> expectedOwnerDefaultActions,
			List<String> expectedSiteMemberDefaultActions, String modelName,
			String... sources)
		throws Exception {

		_resourceActions.populateModelResources(
			ResourcePermissionLocalServiceTest.class.getClassLoader(), sources);

		String primKey = "0";

		_resourceLocalService.addResources(
			TestPropsValues.getCompanyId(), TestPropsValues.getGroupId(),
			TestPropsValues.getUserId(), modelName, primKey, false, true, true);

		_assertResourceActionsDefaults(
			expectedGuestDefaultActions, expectedOwnerDefaultActions,
			expectedSiteMemberDefaultActions, primKey, modelName,
			_resourceActions.getModelResourceActions(modelName));
	}

	private void _testPortletResourceActionsDefaults(
			List<String> expectedGuestDefaultActions,
			List<String> expectedOwnerDefaultActions,
			List<String> expectedSiteMemberDefaultActions, String portletName,
			String... sources)
		throws Exception {

		Portlet portlet = new PortletImpl(
			TestPropsValues.getCompanyId(), portletName);

		_resourceActions.populatePortletResource(
			portlet, ResourcePermissionLocalServiceTest.class.getClassLoader(),
			sources);

		_resourcePermissionLocalService.initPortletDefaultPermissions(portlet);

		_assertResourceActionsDefaults(
			expectedGuestDefaultActions, expectedOwnerDefaultActions,
			expectedSiteMemberDefaultActions, portletName, portletName,
			_resourceActions.getPortletResourceActions(portletName));
	}

	private void _testResources(
			String expectedMessage, List<Resource> resources)
		throws Exception {

		_group = GroupTestUtil.addGroup();

		Role guestRole = _roleLocalService.getRole(
			_group.getCompanyId(), RoleConstants.GUEST);

		try {
			_resourcePermissionLocalService.hasResourcePermission(
				resources, new long[] {guestRole.getRoleId()}, ActionKeys.VIEW);
		}
		catch (IllegalArgumentException illegalArgumentException) {
			Assert.assertEquals(
				expectedMessage, illegalArgumentException.getMessage());
		}
	}

	private static final String _RESOURCE_ACTIONS_DEFAULTS_MODEL_NAME =
		"com.liferay.portal.security.permission.ResourceActionsDefaults";

	private static final String _RESOURCE_ACTIONS_DEFAULTS_PORTLET_NAME =
		"com_liferay_portal_security_ResourcePermissionLocalServiceTest_" +
			"ResourceActionsDefaultsPortlet";

	private static final String _RESOURCE_ACTIONS_EMPTY_DEFAULTS_MODEL_NAME =
		"com.liferay.portal.security.permission.ResourceActionsEmptyDefaults";

	private static final String _RESOURCE_ACTIONS_EMPTY_DEFAULTS_PORTLET_NAME =
		"com_liferay_portal_security_ResourcePermissionLocalServiceTest_" +
			"ResourceActionsEmptyDefaultsPortlet";

	private static final String _RESOURCE_ACTIONS_OVERRIDE_XML_PATH =
		"com/liferay/portal/security/permission/test/dependencies" +
			"/resource-actions-override.xml";

	private static final String
		_RESOURCE_ACTIONS_UNDECLARED_DEFAULTS_MODEL_NAME =
			"com.liferay.portal.security.permission." +
				"ResourceActionsUndeclaredDefaults";

	private static final String
		_RESOURCE_ACTIONS_UNDECLARED_DEFAULTS_PORTLET_NAME =
			"com_liferay_portal_security_ResourcePermissionLocalServiceTest_" +
				"ResourceActionsUndeclaredDefaultsPortlet";

	private static final String _RESOURCE_ACTIONS_XML_PATH =
		"com/liferay/portal/security/permission/test/dependencies" +
			"/resource-actions.xml";

	@DeleteAfterTestRun
	private Group _group;

	@Inject
	private ResourceActions _resourceActions;

	@Inject
	private ResourceLocalService _resourceLocalService;

	@Inject
	private ResourcePermissionLocalService _resourcePermissionLocalService;

	@Inject
	private RoleLocalService _roleLocalService;

}
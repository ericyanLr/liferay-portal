/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.resource.actions.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.ResourceActions;
import com.liferay.portal.kernel.service.ResourceLocalService;
import com.liferay.portal.kernel.service.ResourcePermissionLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.model.impl.PortletImpl;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Eric Yan
 */
@RunWith(Arquillian.class)
public class ResourceActionsTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testModelResourceActionsDefaults() throws Exception {
		String modelName = RandomTestUtil.randomString();
		String portletName = RandomTestUtil.randomString();

		_testModelResourceActionsDefaults(
			modelName,
			_generateResourceActionsXml(
				modelName, portletName, Arrays.asList(ActionKeys.VIEW),
				Arrays.asList(ActionKeys.VIEW), Arrays.asList(ActionKeys.VIEW)),
			null, Arrays.asList(ActionKeys.VIEW),
			Arrays.asList(ActionKeys.VIEW), Arrays.asList(ActionKeys.VIEW));
	}

	@Test
	public void testModelResourceActionsDefaultsWithEmptyDefaultActions()
		throws Exception {

		String modelName = RandomTestUtil.randomString();
		String portletName = RandomTestUtil.randomString();

		_testModelResourceActionsDefaults(
			modelName,
			_generateResourceActionsXml(
				modelName, portletName, Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList()),
			null, Collections.emptyList(), Collections.emptyList(),
			Collections.emptyList());
	}

	@Test
	public void testModelResourceActionsDefaultsWithOverride()
		throws Exception {

		String modelName = RandomTestUtil.randomString();
		String portletName = RandomTestUtil.randomString();

		_testModelResourceActionsDefaults(
			modelName,
			_generateResourceActionsXml(
				modelName, portletName, Arrays.asList(ActionKeys.VIEW),
				Arrays.asList(ActionKeys.VIEW), Arrays.asList(ActionKeys.VIEW)),
			_generateResourceActionsXml(
				modelName, portletName, Arrays.asList(ActionKeys.ADD_TO_PAGE),
				Arrays.asList(ActionKeys.ADD_TO_PAGE),
				Arrays.asList(ActionKeys.ADD_TO_PAGE)),
			Arrays.asList(ActionKeys.ADD_TO_PAGE),
			Arrays.asList(ActionKeys.ADD_TO_PAGE),
			Arrays.asList(ActionKeys.ADD_TO_PAGE));
	}

	@Test
	public void testModelResourceActionsDefaultsWithUndeclaredDefaultActions()
		throws Exception {

		String modelName = RandomTestUtil.randomString();
		String portletName = RandomTestUtil.randomString();

		_testModelResourceActionsDefaults(
			modelName,
			_generateResourceActionsXml(
				modelName, portletName, null, null, null),
			null, Collections.emptyList(),
			Arrays.asList(
				ActionKeys.ADD_TO_PAGE, ActionKeys.CONFIGURATION,
				ActionKeys.PERMISSIONS, ActionKeys.VIEW),
			Collections.emptyList());
	}

	@Test
	public void testPortletResourceActionsDefaults() throws Exception {
		String portletName = RandomTestUtil.randomString();

		_testPortletResourceActionsDefaults(
			portletName,
			_generateResourceActionsXml(
				null, portletName, Arrays.asList(ActionKeys.VIEW),
				Arrays.asList(ActionKeys.VIEW), Arrays.asList(ActionKeys.VIEW)),
			null, Arrays.asList(ActionKeys.VIEW),
			Arrays.asList(ActionKeys.VIEW), Arrays.asList(ActionKeys.VIEW));
	}

	@Test
	public void testPortletResourceActionsDefaultsWithEmptyDefaultActions()
		throws Exception {

		String portletName = RandomTestUtil.randomString();

		_testPortletResourceActionsDefaults(
			portletName,
			_generateResourceActionsXml(
				null, portletName, Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList()),
			null, Collections.emptyList(), Collections.emptyList(),
			Collections.emptyList());
	}

	@Test
	public void testPortletResourceActionsDefaultsWithOverride()
		throws Exception {

		String portletName = RandomTestUtil.randomString();

		_testPortletResourceActionsDefaults(
			portletName,
			_generateResourceActionsXml(
				null, portletName, Arrays.asList(ActionKeys.VIEW),
				Arrays.asList(ActionKeys.VIEW), Arrays.asList(ActionKeys.VIEW)),
			_generateResourceActionsXml(
				null, portletName, Arrays.asList(ActionKeys.ADD_TO_PAGE),
				Arrays.asList(ActionKeys.ADD_TO_PAGE),
				Arrays.asList(ActionKeys.ADD_TO_PAGE)),
			Arrays.asList(ActionKeys.ADD_TO_PAGE),
			Arrays.asList(ActionKeys.ADD_TO_PAGE),
			Arrays.asList(ActionKeys.ADD_TO_PAGE));
	}

	@Test
	public void testPortletResourceActionsDefaultsWithUndeclaredDefaultActions()
		throws Exception {

		String portletName = RandomTestUtil.randomString();

		_testPortletResourceActionsDefaults(
			portletName,
			_generateResourceActionsXml(null, portletName, null, null, null),
			null, Collections.emptyList(),
			Arrays.asList(
				ActionKeys.ADD_TO_PAGE, ActionKeys.CONFIGURATION,
				ActionKeys.PERMISSIONS, ActionKeys.PREFERENCES,
				ActionKeys.VIEW),
			Collections.emptyList());
	}

	private void _assertResourceActionsDefaults(
			String resourceName, String primKey, List<String> supportActionIds,
			List<String> expectedGuestDefaultActions,
			List<String> expectedOwnerDefaultActions,
			List<String> expectedSiteMemberDefaultActions)
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

	private String _generateResourceActionsXml(
		String modelName, String portletName, List<String> guestDefaultActions,
		List<String> ownerDefaultActions,
		List<String> siteMemberDefaultActions) {

		StringBundler sb = new StringBundler();

		sb.append("<?xml version=\"1.0\"?>");

		String docType =
			"\n<!DOCTYPE resource-action-mapping PUBLIC \"-//Liferay//DTD " +
				"Resource Action Mapping 7.4.0//EN\" \"http://www.liferay.com" +
					"/dtd/liferay-resource-action-mapping_7_4_0.dtd\">";

		sb.append(docType);

		sb.append("\n<resource-action-mapping>");

		if (modelName != null) {
			sb.append("\n\t<model-resource>");

			String modelNameEntry = StringBundler.concat(
				"\n\t\t<model-name>", modelName, "</model-name>");

			sb.append(modelNameEntry);

			sb.append("\n\t\t<portlet-ref>");

			String portletNameEntry = StringBundler.concat(
				"\n\t\t\t<portlet-name>", portletName, "</portlet-name>");

			sb.append(portletNameEntry);

			sb.append("\n\t\t</portlet-ref>");
		}
		else {
			sb.append("\n\t<portlet-resource>");

			String portletNameEntry = StringBundler.concat(
				"\n\t\t<portlet-name>", portletName, "</portlet-name>");

			sb.append(portletNameEntry);
		}

		sb.append("\n\t\t<permissions>");
		sb.append("\n\t\t\t<supports>");
		sb.append("\n\t\t\t\t<action-key>ADD_TO_PAGE</action-key>");
		sb.append("\n\t\t\t\t<action-key>CONFIGURATION</action-key>");
		sb.append("\n\t\t\t\t<action-key>VIEW</action-key>");
		sb.append("\n\t\t\t</supports>");

		if (siteMemberDefaultActions != null) {
			if (siteMemberDefaultActions.isEmpty()) {
				sb.append("\n\t\t\t<site-member-defaults/>");
			}
			else {
				sb.append("\n\t\t\t<site-member-defaults>");

				for (String defaultAction : siteMemberDefaultActions) {
					String actionKeyEntry = StringBundler.concat(
						"\n\t\t\t\t<action-key>", defaultAction,
						"</action-key>");

					sb.append(actionKeyEntry);
				}

				sb.append("\n\t\t\t</site-member-defaults>");
			}
		}

		if (guestDefaultActions != null) {
			if (guestDefaultActions.isEmpty()) {
				sb.append("\n\t\t\t<guest-defaults/>");
			}
			else {
				sb.append("\n\t\t\t<guest-defaults>");

				for (String defaultAction : guestDefaultActions) {
					String actionKeyEntry = StringBundler.concat(
						"\n\t\t\t\t<action-key>", defaultAction,
						"</action-key>");

					sb.append(actionKeyEntry);
				}

				sb.append("\n\t\t\t</guest-defaults>");
			}
		}

		if (ownerDefaultActions != null) {
			if (ownerDefaultActions.isEmpty()) {
				sb.append("\n\t\t\t<owner-defaults/>");
			}
			else {
				sb.append("\n\t\t\t<owner-defaults>");

				for (String defaultAction : ownerDefaultActions) {
					String actionKeyEntry = StringBundler.concat(
						"\n\t\t\t\t<action-key>", defaultAction,
						"</action-key>");

					sb.append(actionKeyEntry);
				}

				sb.append("\n\t\t\t</owner-defaults>");
			}
		}

		sb.append("\n\t\t</permissions>");

		if (modelName != null) {
			sb.append("\n\t</model-resource>");
		}
		else {
			sb.append("\n\t</portlet-resource>");
		}

		sb.append("\n</resource-action-mapping>");

		return sb.toString();
	}

	private void _testModelResourceActionsDefaults(
			String modelName, String defaultXml, String overrideXml,
			List<String> expectedGuestDefaultActions,
			List<String> expectedOwnerDefaultActions,
			List<String> expectedSiteMemberDefaultActions)
		throws Exception {

		_resourceActions.populateModelResources(
			new ClassLoader() {

				@Override
				public InputStream getResourceAsStream(String name) {
					if (name.equals("default.xml") && (defaultXml != null)) {
						return new ByteArrayInputStream(defaultXml.getBytes());
					}
					else if (name.equals("override.xml") &&
							 (overrideXml != null)) {

						return new ByteArrayInputStream(overrideXml.getBytes());
					}

					return null;
				}

			},
			"default.xml", "override.xml");

		String primKey = "0";

		_resourceLocalService.addResources(
			TestPropsValues.getCompanyId(), TestPropsValues.getGroupId(),
			TestPropsValues.getUserId(), modelName, primKey, false, true, true);

		_assertResourceActionsDefaults(
			modelName, primKey,
			_resourceActions.getModelResourceActions(modelName),
			expectedGuestDefaultActions, expectedOwnerDefaultActions,
			expectedSiteMemberDefaultActions);
	}

	private void _testPortletResourceActionsDefaults(
			String portletName, String defaultXml, String overrideXml,
			List<String> expectedGuestDefaultActions,
			List<String> expectedOwnerDefaultActions,
			List<String> expectedSiteMemberDefaultActions)
		throws Exception {

		Portlet portlet = new PortletImpl(
			TestPropsValues.getCompanyId(), portletName);

		_resourceActions.populatePortletResource(
			portlet,
			new ClassLoader() {

				@Override
				public InputStream getResourceAsStream(String name) {
					if (name.equals("default.xml") && (defaultXml != null)) {
						return new ByteArrayInputStream(defaultXml.getBytes());
					}
					else if (name.equals("override.xml") &&
							 (overrideXml != null)) {

						return new ByteArrayInputStream(overrideXml.getBytes());
					}

					return null;
				}

			},
			"default.xml", "override.xml");

		_resourcePermissionLocalService.initPortletDefaultPermissions(portlet);

		_assertResourceActionsDefaults(
			portletName, portletName,
			_resourceActions.getPortletResourceActions(portletName),
			expectedGuestDefaultActions, expectedOwnerDefaultActions,
			expectedSiteMemberDefaultActions);
	}

	@Inject
	private ResourceActions _resourceActions;

	@Inject
	private ResourceLocalService _resourceLocalService;

	@Inject
	private ResourcePermissionLocalService _resourcePermissionLocalService;

	@Inject
	private RoleLocalService _roleLocalService;

}
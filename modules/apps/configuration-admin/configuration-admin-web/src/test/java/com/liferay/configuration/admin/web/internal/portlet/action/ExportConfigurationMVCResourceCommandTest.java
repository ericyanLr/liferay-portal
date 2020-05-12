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

package com.liferay.configuration.admin.web.internal.portlet.action;

import com.liferay.configuration.admin.web.internal.model.ConfigurationModel;
import com.liferay.configuration.admin.web.internal.util.ConfigurationModelRetriever;
import com.liferay.portal.configuration.metatype.definitions.ExtendedAttributeDefinition;
import com.liferay.portal.configuration.metatype.definitions.ExtendedObjectClassDefinition;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.osgi.service.cm.Configuration;

/**
 * @author Eric Yan
 */
public class ExportConfigurationMVCResourceCommandTest {

	@Test
	public void testString() {
		Dictionary<String, Object> confProps = new Hashtable<String, Object>() {
			{
				put("foo", "bar");
				put("blank", "");
			}
		};

		Properties expectedProperties = new Properties() {
			{
				put("foo", "bar");
				put("blank", "");
			}
		};

		assertExportedConfigurationProperties(confProps, expectedProperties);
	}

	@Test
	public void testStringArray() {
		Dictionary<String, Object> confProps = new Hashtable<String, Object>() {
			{
				put("foo", new String[] {"bar", "baz"});
				put("blank", new String[] {""});
				put("empty", new String[0]);
			}
		};

		Properties expectedProperties = new Properties() {
			{
				put("foo", new String[] {"bar", "baz"});
				put("blank", new String[] {""});
				put("empty", new String[0]);
			}
		};

		assertExportedConfigurationProperties(confProps, expectedProperties);
	}

	@Test
	public void testStringArrayWithLocationVariableServerProperty() {
		Dictionary<String, Object> confProps = new Hashtable<String, Object>() {
			{
				put(
					"match-control",
					new String[] {"${server-property://foo.bar/foo.bar}"});
				put(
					"match-variation1",
					new String[] {"${server-property:/foo.bar/foo.bar}"});
				put(
					"match-variation2",
					new String[] {"${server-property:foo.bar/foo.bar}"});
				put("match-variation3", new String[] {"${server-property:}"});
				put(
					"mismatch-start",
					new String[] {"x${server-property://foo.bar/foo.bar}"});
				put(
					"mismatch-end",
					new String[] {"${server-property://foo.bar/foo.bar}x"});
				put(
					"mismatch-protocol",
					new String[] {"${xserver-property://foo.bar/foo.bar}"});
				put(
					"mismatch-separator",
					new String[] {"${server-property//foo.bar/foo.bar}"});
			}
		};

		Properties expectedProperties = new Properties() {
			{
				put(
					"match-control",
					new String[] {"$\\{server-property://foo.bar/foo.bar\\}"});
				put(
					"match-variation1",
					new String[] {"$\\{server-property:/foo.bar/foo.bar\\}"});
				put(
					"match-variation2",
					new String[] {"$\\{server-property:foo.bar/foo.bar\\}"});
				put(
					"match-variation3",
					new String[] {"$\\{server-property:\\}"});
				put(
					"mismatch-start",
					new String[] {"x${server-property://foo.bar/foo.bar}"});
				put(
					"mismatch-end",
					new String[] {"${server-property://foo.bar/foo.bar}x"});
				put(
					"mismatch-protocol",
					new String[] {"${xserver-property://foo.bar/foo.bar}"});
				put(
					"mismatch-separator",
					new String[] {"${server-property//foo.bar/foo.bar}"});
			}
		};

		assertExportedConfigurationProperties(confProps, expectedProperties);
	}

	@Test
	public void testStringWithLocationVariableServerProperty() {
		Dictionary<String, Object> confProps = new Hashtable<String, Object>() {
			{
				put("match-control", "${server-property://foo.bar/foo.bar}");
				put("match-variation1", "${server-property:/foo.bar/foo.bar}");
				put("match-variation2", "${server-property:foo.bar/foo.bar}");
				put("match-variation3", "${server-property:}");
				put("mismatch-start", "x${server-property://foo.bar/foo.bar}");
				put("mismatch-end", "${server-property://foo.bar/foo.bar}x");
				put(
					"mismatch-protocol",
					"${xserver-property://foo.bar/foo.bar}");
				put(
					"mismatch-separator",
					"${server-property//foo.bar/foo.bar}");
			}
		};

		Properties expectedProperties = new Properties() {
			{
				put(
					"match-control",
					"$\\{server-property://foo.bar/foo.bar\\}");
				put(
					"match-variation1",
					"$\\{server-property:/foo.bar/foo.bar\\}");
				put(
					"match-variation2",
					"$\\{server-property:foo.bar/foo.bar\\}");
				put("match-variation3", "$\\{server-property:\\}");
				put("mismatch-start", "x${server-property://foo.bar/foo.bar}");
				put("mismatch-end", "${server-property://foo.bar/foo.bar}x");
				put(
					"mismatch-protocol",
					"${xserver-property://foo.bar/foo.bar}");
				put(
					"mismatch-separator",
					"${server-property//foo.bar/foo.bar}");
			}
		};

		assertExportedConfigurationProperties(confProps, expectedProperties);
	}

	protected void assertExportedConfigurationProperties(
		Dictionary<String, Object> configurationProperties,
		Properties expectedExportedProperties) {

		Properties actualExportedProperties = exportConfigurationProperties(
			configurationProperties);

		Assert.assertEquals(
			expectedExportedProperties.size(), actualExportedProperties.size());

		Enumeration<Object> keys = expectedExportedProperties.keys();

		while (keys.hasMoreElements()) {
			String key = (String)keys.nextElement();

			Object actualValue = actualExportedProperties.get(key);
			Object expectedValue = expectedExportedProperties.get(key);

			doAssertExportedConfigurationProperty(
				key, expectedValue, actualValue);
		}
	}

	protected void doAssertExportedConfigurationProperty(
		String key, Object expectedValue, Object actualValue) {

		String msg = "Configuration property: " + key;

		if ((expectedValue instanceof Object[]) &&
			(actualValue instanceof Object[])) {

			List<Object> actualValues = Arrays.asList((Object[])actualValue);
			List<Object> expectedValues = Arrays.asList(
				(Object[])expectedValue);

			Assert.assertEquals(
				msg, expectedValues.toString(), actualValues.toString());
		}
		else {
			Assert.assertEquals(msg, expectedValue, actualValue);
		}
	}

	protected Properties exportConfigurationProperties(
		Dictionary<String, Object> configurationProperties) {

		ExportConfigurationMVCResourceCommand
			exportConfigurationMVCResourceCommand =
				_createExportConfigurationMVCResourceCommand(
					configurationProperties);

		return ReflectionTestUtil.invoke(
			exportConfigurationMVCResourceCommand, "getProperties",
			new Class<?>[] {
				String.class, String.class, String.class,
				com.liferay.portal.configuration.metatype.annotations.
					ExtendedObjectClassDefinition.Scope.class,
				Serializable.class
			},
			null, null, _PID,
			com.liferay.portal.configuration.metatype.annotations.
				ExtendedObjectClassDefinition.Scope.SYSTEM,
			null);
	}

	private ExportConfigurationMVCResourceCommand
		_createExportConfigurationMVCResourceCommand(
			Dictionary<String, Object> configurationProperties) {

		ConfigurationModelRetriever configurationModelRetriever = Mockito.mock(
			ConfigurationModelRetriever.class);

		_setUpConfigurationModelRetrieverMocks(
			configurationModelRetriever, configurationProperties);

		ExportConfigurationMVCResourceCommand
			exportConfigurationMVCResourceCommand =
				new ExportConfigurationMVCResourceCommand();

		ReflectionTestUtil.setFieldValue(
			exportConfigurationMVCResourceCommand,
			"_configurationModelRetriever", configurationModelRetriever);

		return exportConfigurationMVCResourceCommand;
	}

	private ExtendedAttributeDefinition[]
		_createSimpleExtendedAttributeDefinitions(
			Dictionary<String, Object> configurationProperties) {

		List<ExtendedAttributeDefinition> extendedAttributeDefinitions =
			new ArrayList<>();

		Enumeration<String> enumeration = configurationProperties.keys();

		while (enumeration.hasMoreElements()) {
			String key = enumeration.nextElement();

			ExtendedAttributeDefinition extendedAttributeDefinition =
				Mockito.mock(ExtendedAttributeDefinition.class);

			Mockito.doReturn(
				key
			).when(
				extendedAttributeDefinition
			).getID();

			int cardinality = 0;

			if (configurationProperties.get(key) instanceof Object[]) {
				cardinality = 1;
			}

			Mockito.doReturn(
				cardinality
			).when(
				extendedAttributeDefinition
			).getCardinality();

			extendedAttributeDefinitions.add(extendedAttributeDefinition);
		}

		return extendedAttributeDefinitions.toArray(
			new ExtendedAttributeDefinition[0]);
	}

	private void _setUpConfigurationModelRetrieverMocks(
		ConfigurationModelRetriever configurationModelRetriever,
		Dictionary<String, Object> configurationProperties) {

		Configuration configuration = Mockito.mock(Configuration.class);

		Mockito.doReturn(
			configurationProperties
		).when(
			configuration
		).getProperties();

		Mockito.doReturn(
			_PID
		).when(
			configuration
		).getPid();

		Mockito.doReturn(
			configuration
		).when(
			configurationModelRetriever
		).getConfiguration(
			Mockito.anyString(), Mockito.any(), Mockito.any()
		);

		ExtendedObjectClassDefinition extendedObjectClassDefinition =
			Mockito.mock(ExtendedObjectClassDefinition.class);

		Mockito.when(
			extendedObjectClassDefinition.getAttributeDefinitions(
				Mockito.anyInt())
		).thenAnswer(
			(Answer<ExtendedAttributeDefinition[]>)
				invocationOnMock -> _createSimpleExtendedAttributeDefinitions(
					configurationProperties)
		);

		ConfigurationModel configurationModel = Mockito.mock(
			ConfigurationModel.class);

		Mockito.doReturn(
			extendedObjectClassDefinition
		).when(
			configurationModel
		).getExtendedObjectClassDefinition();

		Mockito.doReturn(
			Collections.singletonMap(configuration.getPid(), configurationModel)
		).when(
			configurationModelRetriever
		).getConfigurationModels(
			Mockito.anyString(), Mockito.any(), Mockito.any()
		);
	}

	private static final String _PID = RandomTestUtil.randomString(50);

}
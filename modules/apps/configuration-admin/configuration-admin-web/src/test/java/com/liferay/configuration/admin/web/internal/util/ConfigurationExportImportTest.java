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

package com.liferay.configuration.admin.web.internal.util;

import com.liferay.configuration.admin.web.internal.exporter.ConfigurationExporter;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayInputStream;
import com.liferay.portal.kernel.util.ArrayUtil;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.felix.utils.properties.TypedProperties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Pei-Jung Lan
 */
public class ConfigurationExportImportTest {

	@Before
	public void setUp() {
		_dictionary = new Hashtable<>();
	}

	@Test
	public void testExportImportBlankString() throws Exception {
		String blankStringKey = "blankStringKey";
		String blankStringValue = StringPool.BLANK;

		_dictionary.put(blankStringKey, blankStringValue);

		Dictionary<String, Object> dictionary = _exportImportProperties(
			_dictionary);

		Assert.assertEquals(_dictionary, dictionary);
	}

	@Test
	public void testExportImportBoolean() throws Exception {
		String booleanKey = "booleanKey";

		_dictionary.put(booleanKey, true);

		Dictionary<String, Object> dictionary = _exportImportProperties(
			_dictionary);

		Assert.assertEquals(_dictionary, dictionary);
	}

	@Test
	public void testExportImportString() throws Exception {
		String stringKey = "stringKey";
		String stringValue = "stringValue";

		_dictionary.put(stringKey, stringValue);

		Dictionary<String, Object> dictionary = _exportImportProperties(
			_dictionary);

		Assert.assertEquals(_dictionary, dictionary);
	}

	@Test
	public void testExportImportStringArray() throws Exception {
		String arrayKey = "arrayKey";
		String[] arrayValues = {"value1", "value2", "value3"};

		_dictionary.put(arrayKey, arrayValues);

		Dictionary<String, Object> dictionary = _exportImportProperties(
			_dictionary);

		Assert.assertEquals(dictionary.toString(), 1, dictionary.size());
		Assert.assertArrayEquals(
			arrayValues, (String[])dictionary.get(arrayKey));
	}

	@Test
	public void testExportImportStringArrayTypedProperty() throws Exception {
		String arrayKey = "arrayKey";
		String[] arrayValues = {"value1", "value2", "value3"};

		_dictionary.put(arrayKey, arrayValues);

		Dictionary<String, Object> dictionary = _exportImportTypedProperties(
			_dictionary);

		Assert.assertArrayEquals(
			arrayValues,
			ArrayUtil.toStringArray((Object[])dictionary.get(arrayKey)));
	}

	@Test
	public void testExportImportStringArrayTypedPropertyWithLocationVariable()
		throws Exception {

		String arrayKey = "location-variables";
		String[] arrayValues = {
			"${server-property://foo.bar/foo.bar}",
			"${resource://foo.bar/foo.bar}", "${file://foo.bar/foo.bar}"
		};

		_dictionary.put(arrayKey, arrayValues);

		Dictionary<String, Object> dictionary = _exportImportTypedProperties(
			_dictionary);

		String[] expectedArrayValues = {"", "", ""};

		Assert.assertArrayEquals(
			expectedArrayValues,
			ArrayUtil.toStringArray((Object[])dictionary.get(arrayKey)));
	}

	@Test
	public void testExportImportStringTypedProperty() throws Exception {
		String stringKey = "stringKey";
		String stringValue = "stringValue";

		_dictionary.put(stringKey, stringValue);

		Dictionary<String, Object> dictionary = _exportImportTypedProperties(
			_dictionary);

		Assert.assertEquals(_dictionary, dictionary);
	}

	@Test
	public void testExportImportStringTypedPropertyWithLocationVariable()
		throws Exception {

		String stringKey = "server-property";
		String stringValue = "${server-property://foo.bar/foo.bar}";

		_dictionary.put(stringKey, stringValue);

		stringKey = "resource";
		stringValue = "${resource://foo.bar/foo.bar}";

		_dictionary.put(stringKey, stringValue);

		Dictionary<String, Object> dictionary = _exportImportTypedProperties(
			_dictionary);

		Dictionary<String, Object> expectedDictionary = new Hashtable<>();

		expectedDictionary.put("server-property", "");
		expectedDictionary.put("resource", "");

		Assert.assertEquals(expectedDictionary, dictionary);
	}

	@Test
	public void testExportImportStringTypedPropertyWithLocationVariableEscaped()
		throws Exception {

		String stringKey = "server-property";
		String stringValue = "$\\{server-property://foo.bar/foo.bar\\}";

		_dictionary.put(stringKey, stringValue);

		stringKey = "resource";
		stringValue = "$\\{resource://foo.bar/foo.bar\\}";

		_dictionary.put(stringKey, stringValue);

		Dictionary<String, Object> dictionary = _exportImportTypedProperties(
			_dictionary);

		Dictionary<String, Object> expectedDictionary = new Hashtable<>();

		expectedDictionary.put(
			"server-property", "${server-property://foo.bar/foo.bar}");
		expectedDictionary.put("resource", "${resource://foo.bar/foo.bar}");

		Assert.assertEquals(expectedDictionary, dictionary);
	}

	@SuppressWarnings("unchecked")
	private Dictionary<String, Object> _exportImportProperties(
			Dictionary<String, Object> dictionary)
		throws Exception {

		byte[] bytes = ConfigurationExporter.getPropertiesAsBytes(dictionary);

		return ConfigurationHandler.read(new UnsyncByteArrayInputStream(bytes));
	}

	private Dictionary<String, Object> _exportImportTypedProperties(
			Dictionary<String, Object> dictionary)
		throws Exception {

		byte[] bytes = ConfigurationExporter.getPropertiesAsBytes(dictionary);

		TypedProperties typedProperties = new TypedProperties();

		typedProperties.load(new UnsyncByteArrayInputStream(bytes));

		Hashtable<String, Object> importedDictionary = new Hashtable<>();

		for (String key : typedProperties.keySet()) {
			importedDictionary.put(key, typedProperties.get(key));
		}

		return importedDictionary;
	}

	private Dictionary<String, Object> _dictionary;

}
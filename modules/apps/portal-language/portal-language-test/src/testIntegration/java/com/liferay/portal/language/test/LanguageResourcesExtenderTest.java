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

package com.liferay.portal.language.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Lianne Louie
 */
@RunWith(Arquillian.class)
public class LanguageResourcesExtenderTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() throws Exception {
		_defaultLocale = LocaleUtil.getDefault();

		LocaleUtil.setDefault(
			LocaleUtil.US.getLanguage(), LocaleUtil.US.getCountry(),
			LocaleUtil.US.getVariant());
	}

	@AfterClass
	public static void tearDownClass() {
		LocaleUtil.setDefault(
			_defaultLocale.getLanguage(), _defaultLocale.getCountry(),
			_defaultLocale.getVariant());
	}

	@Test
	public void testResourceBundleRegistration() {
		String key1 = "test-key-1";
		String key2 = "test-key-2";

		String blankValue1 = "default value 1";
		String blankValue2 = "default value 2";
		String usEnglishValue2 = "US English value 2";
		String unsupportedValue = "French Canadian value";

		ResourceBundle blankResourceBundle = _getResourceBundle("", "");
		ResourceBundle defaultResourceBundle = _getResourceBundle("en", "");
		ResourceBundle unsupportedResourceBundle = _getResourceBundle(
			"fr", "CA");

		Assert.assertEquals(
			usEnglishValue2, defaultResourceBundle.getString(key2));
		Assert.assertEquals(
			unsupportedValue, unsupportedResourceBundle.getString(key1));
		Assert.assertEquals(blankValue1, blankResourceBundle.getString(key1));

		Assert.assertEquals(blankValue2, blankResourceBundle.getString(key2));
		Assert.assertEquals(
			blankValue2, unsupportedResourceBundle.getString(key2));
		Assert.assertEquals(
			usEnglishValue2, defaultResourceBundle.getString(key2));
	}

	private ResourceBundle _getResourceBundle(String language, String country) {
		Class<?> clazz = getClass();

		Package pkg = clazz.getPackage();

		return ResourceBundleUtil.getBundle(
			pkg.getName() + ".content.Language", new Locale(language, country),
			clazz);
	}

	private static Locale _defaultLocale;

}
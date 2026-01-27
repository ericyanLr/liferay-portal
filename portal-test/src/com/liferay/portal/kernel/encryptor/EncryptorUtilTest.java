/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.encryptor;

import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.security.Key;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Mika Koivisto
 */
public class EncryptorUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testKeySerialization() throws Exception {
		Key key = EncryptorUtil.generateKey();

		String encryptedString = EncryptorUtil.encrypt(key, "Hello World!");

		String serializedKey = EncryptorUtil.serializeKey(key);

		key = EncryptorUtil.deserializeKey(serializedKey);

		Assert.assertEquals(
			"Hello World!", EncryptorUtil.decrypt(key, encryptedString));
	}

}
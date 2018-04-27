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

package com.liferay.journal.search.test;

import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.search.IndexerRegistry;
import com.liferay.portal.kernel.search.SearchEngineHelper;
import com.liferay.portal.kernel.service.ResourcePermissionLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.search.test.util.IndexedFieldsFixture;
import com.liferay.portal.search.test.util.RoleFixture;
import com.liferay.portal.test.rule.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eric Yan
 */
public abstract class BaseJournalIndexerTestCase {

	public void setUp() throws Exception {
		indexedFieldsFixture = createIndexedFieldsFixture();
		journalFixture = createJournalFixture();

		journalFixture.setUp();

		journalSearchFixture = createJournalSearchFixture();
		roleFixture = createRoleFixture();
	}

	protected IndexedFieldsFixture createIndexedFieldsFixture() {
		return new IndexedFieldsFixture(
			resourcePermissionLocalService, searchEngineHelper);
	}

	protected JournalFixture createJournalFixture() {
		return new JournalFixture(journalArticleLocalService, _groups, _users);
	}

	protected JournalSearchFixture createJournalSearchFixture() {
		return new JournalSearchFixture(indexerRegistry);
	}

	protected RoleFixture createRoleFixture() {
		return new RoleFixture(
			resourcePermissionLocalService, roleLocalService);
	}

	protected void setGroup(Group group) {
		journalFixture.setGroup(group);
		journalSearchFixture.setGroup(group);
	}

	protected void setIndexerClass(Class<?> clazz) {
		journalSearchFixture.setIndexerClass(clazz);
	}

	protected void setUser(User user) {
		journalFixture.setUser(user);
		journalSearchFixture.setUser(user);
	}

	protected IndexedFieldsFixture indexedFieldsFixture;

	@Inject
	protected IndexerRegistry indexerRegistry;

	@Inject
	protected JournalArticleLocalService journalArticleLocalService;

	protected JournalFixture journalFixture;
	protected JournalSearchFixture journalSearchFixture;

	@Inject
	protected ResourcePermissionLocalService resourcePermissionLocalService;

	protected RoleFixture roleFixture;

	@Inject
	protected RoleLocalService roleLocalService;

	@Inject
	protected SearchEngineHelper searchEngineHelper;

	@DeleteAfterTestRun
	private final List<Group> _groups = new ArrayList<>(1);

	@DeleteAfterTestRun
	private final List<User> _users = new ArrayList<>(1);

}
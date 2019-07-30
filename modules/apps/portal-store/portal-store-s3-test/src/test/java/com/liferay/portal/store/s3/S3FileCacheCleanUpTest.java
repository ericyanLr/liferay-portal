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

package com.liferay.portal.store.s3;

import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.util.FileImpl;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Eric Yan
 */
public class S3FileCacheCleanUpTest {

	@Before
	public void setUp() throws IOException {
		_cacheDir = temporaryFolder.newFolder("liferay", "s3");
	}

	@Test
	public void testExpiredCacheDirWithSingleFile() throws IOException {
		File cacheFile = _generateCacheFile();

		long expirationTime = _cacheDir.lastModified() + 1;

		_cleanUpCacheFiles(expirationTime);

		Assert.assertFalse(_cacheDir.exists());
		Assert.assertFalse(cacheFile.exists());
	}

	@Test
	public void testExpiredCacheDirWithSingleFileAndExpiredEmptySubdirectory()
		throws IOException {

		File cacheFile = _generateCacheFile();

		File subdirectory = _generateSubdirectory();

		long expirationTime = subdirectory.lastModified() + 1;

		_cleanUpCacheFiles(expirationTime);

		Assert.assertTrue(_cacheDir.exists());
		Assert.assertTrue(cacheFile.exists());
		Assert.assertFalse(subdirectory.exists());
	}

	@Test
	public void testExpiredCacheDirWithSingleFileAndExpiredEmptySubdirectoryAndExpireCacheDirAgain()
		throws Exception {

		File cacheFile = _generateCacheFile();

		File subdirectory = _generateSubdirectory();

		long expirationTime = subdirectory.lastModified() + 1;

		// Delay for at least 1 second, to allow for a change to modified date
		// within the filesystem

		Thread.sleep(1000);

		_cleanUpCacheFiles(expirationTime);

		Assert.assertTrue(_cacheDir.exists());
		Assert.assertTrue(cacheFile.exists());
		Assert.assertFalse(subdirectory.exists());

		expirationTime = _cacheDir.lastModified() + 1;

		_cleanUpCacheFiles(expirationTime);

		Assert.assertFalse(_cacheDir.exists());
		Assert.assertFalse(cacheFile.exists());
		Assert.assertFalse(subdirectory.exists());
	}

	@Test
	public void testExpiredCacheDirWithSingleFileAndExpiredSubdirectoryWithSingleFile()
		throws IOException {

		File cacheFile = _generateCacheFile();

		File subdirectory = _generateSubdirectory();

		File subdirectoryCacheFile = _generateCacheFile(subdirectory);

		long expirationTime = subdirectory.lastModified() + 1;

		_cleanUpCacheFiles(expirationTime);

		Assert.assertTrue(_cacheDir.exists());
		Assert.assertTrue(cacheFile.exists());
		Assert.assertFalse(subdirectory.exists());
		Assert.assertFalse(subdirectoryCacheFile.exists());
	}

	@Test
	public void testExpiredCacheDirWithSingleFileAndExpiredSubdirectoryWithSingleFileAndExpireCacheDirAgain()
		throws Exception {

		File cacheFile = _generateCacheFile();

		File subdirectory = _generateSubdirectory();

		File subdirectoryCacheFile = _generateCacheFile(subdirectory);

		long expirationTime = subdirectory.lastModified() + 1;

		// Delay for at least 1 second, to allow for a change to modified date
		// within the filesystem

		Thread.sleep(1000);

		_cleanUpCacheFiles(expirationTime);

		Assert.assertTrue(_cacheDir.exists());
		Assert.assertTrue(cacheFile.exists());
		Assert.assertFalse(subdirectory.exists());
		Assert.assertFalse(subdirectoryCacheFile.exists());

		expirationTime = _cacheDir.lastModified() + 1;

		_cleanUpCacheFiles(expirationTime);

		Assert.assertFalse(_cacheDir.exists());
		Assert.assertFalse(cacheFile.exists());
		Assert.assertFalse(subdirectory.exists());
		Assert.assertFalse(subdirectoryCacheFile.exists());
	}

	@Test
	public void testExpiredCacheDirWithSingleFileAndValidEmptySubdirectory()
		throws IOException {

		File cacheFile = _generateCacheFile();

		File subdirectory = _generateSubdirectory();

		long expirationTime = subdirectory.lastModified();

		_cleanUpCacheFiles(expirationTime);

		Assert.assertTrue(_cacheDir.exists());
		Assert.assertTrue(cacheFile.exists());
		Assert.assertTrue(subdirectory.exists());
	}

	@Test
	public void testExpiredCacheDirWithSingleFileAndValidSubdirectoryWithSingleFile()
		throws IOException {

		File cacheFile = _generateCacheFile();

		File subdirectory = _generateSubdirectory();

		File subdirectoryCacheFile = _generateCacheFile(subdirectory);

		long expirationTime = subdirectory.lastModified();

		_cleanUpCacheFiles(expirationTime);

		Assert.assertTrue(_cacheDir.exists());
		Assert.assertTrue(cacheFile.exists());
		Assert.assertTrue(subdirectory.exists());
		Assert.assertTrue(subdirectoryCacheFile.exists());
	}

	@Test
	public void testExpiredEmptyCacheDir() {
		long expirationTime = _cacheDir.lastModified() + 1;

		_cleanUpCacheFiles(expirationTime);

		Assert.assertFalse(_cacheDir.exists());
	}

	@Test
	public void testValidCacheDirWithSingleFile() throws IOException {
		File cacheFile = _generateCacheFile();

		long expirationTime = _cacheDir.lastModified();

		_cleanUpCacheFiles(expirationTime);

		Assert.assertTrue(_cacheDir.exists());
		Assert.assertTrue(cacheFile.exists());
	}

	@Test
	public void testValidEmptyCacheDir() {
		long expirationTime = _cacheDir.lastModified();

		_cleanUpCacheFiles(expirationTime);

		Assert.assertTrue(_cacheDir.exists());
	}

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private void _cleanUpCacheFiles(long expirationTime) {
		ReflectionTestUtil.invoke(
			_s3FileCache, "cleanUpCacheFiles",
			new Class<?>[] {File.class, long.class}, _cacheDir, expirationTime);
	}

	private File _generateCacheFile() throws IOException {
		return _generateCacheFile(_cacheDir);
	}

	private File _generateCacheFile(File dir) throws IOException {
		File file = new File(dir, RandomTestUtil.randomString());

		file.createNewFile();

		return file;
	}

	private File _generateSubdirectory() {
		File file = new File(_cacheDir, RandomTestUtil.randomString());

		file.mkdir();

		return file;
	}

	private static final S3FileCache _s3FileCache = new S3FileCacheImpl();

	static {
		ReflectionTestUtil.setFieldValue(
			FileUtil.class, "_file", FileImpl.getInstance());
	}

	private File _cacheDir;

}
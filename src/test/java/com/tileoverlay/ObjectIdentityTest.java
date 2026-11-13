/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import org.junit.Assert;
import org.junit.Test;

public class ObjectIdentityTest
{
	@Test
	public void baseAndTransformedIdsMatchInEitherDirection()
	{
		int[] transforms = {33411, 33412, 33419};

		Assert.assertTrue(ObjectIdentity.same(29150, 29150, transforms, null));
		Assert.assertTrue(ObjectIdentity.same(29150, 33411, transforms, null));
		Assert.assertTrue(ObjectIdentity.same(33411, 29150, null, transforms));
		Assert.assertFalse(ObjectIdentity.same(33411, 33412, null, null));
		Assert.assertFalse(ObjectIdentity.same(-1, 33411, transforms, null));
	}
}

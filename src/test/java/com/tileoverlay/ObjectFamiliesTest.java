/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import org.junit.Assert;
import org.junit.Test;

public class ObjectFamiliesTest
{
	@Test
	public void transformsOfOneDecorationShareTheBase()
	{
		ObjectFamilies families = new ObjectFamilies();
		families.index(29150, new int[]{33411, 33412, 33419, -1});

		Assert.assertEquals(29150, families.canonical(33411));
		Assert.assertEquals(29150, families.canonical(33412));
		Assert.assertEquals(29150, families.canonical(29150));
		Assert.assertEquals(999, families.canonical(999));
		Assert.assertTrue(families.same(33411, 33412));
		Assert.assertTrue(families.same(29150, 33419));
		Assert.assertFalse(families.same(33411, 999));
		Assert.assertFalse(families.same(-1, 33411));
	}
}

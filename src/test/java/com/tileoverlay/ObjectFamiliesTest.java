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
		Assert.assertEquals(29150, families.canonical(33413));
		Assert.assertEquals(29150, families.canonical(33414));
		Assert.assertEquals(29150, families.canonical(33415));
		Assert.assertTrue(families.same(33415, 33419));
		Assert.assertFalse(families.same(33415, 33416));
		Assert.assertEquals(33416, families.canonical(33420));
	}

	@Test
	public void mountedTeleportsShareAFamilyBeforeTheCacheScan()
	{
		ObjectFamilies families = new ObjectFamilies();

		Assert.assertEquals(33411, families.canonical(33411));
		Assert.assertEquals(33411, families.canonical(33415));
		Assert.assertEquals(33411, families.canonical(33419));
		Assert.assertTrue(families.same(33411, 33412));
		Assert.assertTrue(families.same(33412, 33413));
		Assert.assertTrue(families.same(33414, 33419));
		Assert.assertEquals(33416, families.canonical(33418));
		Assert.assertEquals(33416, families.canonical(33420));
		Assert.assertTrue(families.same(33416, 33417));
		Assert.assertTrue(families.same(33417, 33420));
		Assert.assertFalse(families.same(33411, 33416));
		Assert.assertFalse(families.same(33419, 33420));
		Assert.assertFalse(families.same(33415, 33418));
	}

	@Test
	public void aSharedImpostorListDoesNotJoinTheTwoMounts()
	{
		ObjectFamilies families = new ObjectFamilies();
		families.index(40000, new int[]{33411, 33416, 33419, 33420});

		Assert.assertTrue(families.same(33411, 33419));
		Assert.assertTrue(families.same(33416, 33420));
		Assert.assertFalse(families.same(33411, 33416));
		Assert.assertFalse(families.same(33415, 33418));
		Assert.assertFalse(families.same(40000, 33411));
	}
}

/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.util.Arrays;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ObjectLeftClickTest
{
	@Test
	public void customIndexPicksThatAction()
	{
		String[] ops = {"Drink", null, "Remove", null, null};
		Assert.assertEquals("Remove", ObjectLeftClick.nameForIndex(ops, null, 2));
		Assert.assertEquals("Drink", ObjectLeftClick.nameForIndex(ops, null, 0));
		Assert.assertEquals("Walk here", ObjectLeftClick.nameForIndex(ops, null, -1));
	}

	@Test
	public void packedSubOptionIsTheDestinationName()
	{
		String[] ops = {"Teleport", "Teleport menu", "Configuration", "Remove", null};
		String[][] subs = {new String[]{"Great Kourend", "Varrock"}, null, null, null, null};
		int greatKourend = (0 + 1) << 8;
		int varrock = (1 + 1) << 8;
		Assert.assertEquals("Great Kourend", ObjectLeftClick.nameForIndex(ops, subs, greatKourend));
		Assert.assertEquals("Varrock", ObjectLeftClick.nameForIndex(ops, subs, varrock));
		Assert.assertEquals("Configuration", ObjectLeftClick.nameForIndex(ops, subs, 2));
		Assert.assertEquals("Remove", ObjectLeftClick.nameForIndex(ops, subs, 3));
	}

	@Test
	public void firstOptionSkipsEmptyActions()
	{
		Assert.assertEquals("Drink", ObjectLeftClick.firstOption(new String[]{null, "null", " Drink "}));
		Assert.assertNull(ObjectLeftClick.firstOption(new String[]{null, ""}));
		Assert.assertNull(ObjectLeftClick.nameForIndex(new String[]{"Drink"}, null, 9));
	}

	@Test
	public void highestOptionForTheObjectIsTheLeftClick()
	{
		Map<Integer, String> chosen = ObjectLeftClick.choose(Arrays.asList(
			ObjectLeftClick.Hit.walk(3, 4),
			ObjectLeftClick.Hit.object(3, 4, 10, -1, "Teleport"),
			ObjectLeftClick.Hit.skip(),
			ObjectLeftClick.Hit.object(3, 4, 10, 99, "Configuration")));
		Assert.assertEquals("Configuration", chosen.get(10));
		Assert.assertEquals("Configuration", chosen.get(99));
	}

	@Test
	public void rewrittenTopOptionKeepsTheObjectsId()
	{
		Map<Integer, String> chosen = ObjectLeftClick.choose(Arrays.asList(
			ObjectLeftClick.Hit.walk(8, 2),
			ObjectLeftClick.Hit.object(8, 2, 33410, -1, "Teleport"),
			ObjectLeftClick.Hit.object(8, 2, -1, -1, "Great Kourend")));
		Assert.assertEquals("Great Kourend", chosen.get(33410));
	}

	@Test
	public void walkHereWinsOnlyWhenItIsAboveTheObject()
	{
		Map<Integer, String> swapped = ObjectLeftClick.choose(Arrays.asList(
			ObjectLeftClick.Hit.object(1, 1, 5, -1, "Drink"),
			ObjectLeftClick.Hit.walk(1, 1)));
		Assert.assertEquals("Walk here", swapped.get(5));

		Map<Integer, String> covered = ObjectLeftClick.choose(Arrays.asList(
			ObjectLeftClick.Hit.walk(1, 1),
			ObjectLeftClick.Hit.object(1, 1, 5, -1, "Drink"),
			ObjectLeftClick.Hit.skip()));
		Assert.assertEquals("Drink", covered.get(5));
	}

	@Test
	public void twoObjectsKeepTheirOwnOptions()
	{
		Map<Integer, String> chosen = ObjectLeftClick.choose(Arrays.asList(
			ObjectLeftClick.Hit.walk(1, 1),
			ObjectLeftClick.Hit.object(1, 1, 5, -1, "Drink"),
			ObjectLeftClick.Hit.object(2, 2, 8, -1, "Open")));
		Assert.assertEquals("Drink", chosen.get(5));
		Assert.assertEquals("Open", chosen.get(8));
	}
}

/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.awt.Color;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.runelite.client.config.ConfigItem;
import org.junit.Assert;
import org.junit.Test;

public class MarkerLooksTest
{
	@Test
	public void everyConfigMethodIsARealConfigItem()
	{
		for (Method method : TileOverlayConfig.class.getDeclaredMethods())
		{
			if (method.isSynthetic())
			{
				continue;
			}
			Assert.assertNotNull(
				method.getName() + " would be called through the config proxy and return null",
				method.getAnnotation(ConfigItem.class));
		}
	}

	@Test
	public void eachMarkerStyleUsesItsOwnColours()
	{
		TileOverlayConfig config = config();
		Assert.assertEquals(new Color(1, 0, 0), MarkerLooks.border(config, MarkerStyle.TILE));
		Assert.assertEquals(new Color(0, 1, 0), MarkerLooks.border(config, MarkerStyle.REGION));
		Assert.assertEquals(new Color(0, 0, 1), MarkerLooks.border(config, MarkerStyle.BORDER));
		Assert.assertEquals(10, MarkerLooks.fillOpacity(config, MarkerStyle.TILE));
		Assert.assertEquals(20, MarkerLooks.fillOpacity(config, MarkerStyle.REGION));
		Assert.assertEquals(30, MarkerLooks.fillOpacity(config, MarkerStyle.BORDER));
		Assert.assertFalse(MarkerLooks.cornersOnly(config, MarkerStyle.TILE));
		Assert.assertTrue(MarkerLooks.cornersOnly(config, MarkerStyle.REGION));
		Assert.assertEquals(22, MarkerLooks.cornerSize(config, MarkerStyle.TILE));
		Assert.assertEquals(30, MarkerLooks.cornerSize(config, MarkerStyle.BORDER));
		Assert.assertTrue(MarkerLooks.merge(config, MarkerStyle.TILE));
		Assert.assertFalse(MarkerLooks.merge(config, MarkerStyle.REGION));
		Assert.assertTrue(MarkerLooks.merge(config, MarkerStyle.BORDER));
	}

	private static TileOverlayConfig config()
	{
		return (TileOverlayConfig) Proxy.newProxyInstance(
			TileOverlayConfig.class.getClassLoader(),
			new Class<?>[]{TileOverlayConfig.class},
			(proxy, method, args) ->
			{
				String name = method.getName();
				if ("tileBorderColour".equals(name))
				{
					return new Color(1, 0, 0);
				}
				if ("regionBorderColour".equals(name))
				{
					return new Color(0, 1, 0);
				}
				if ("borderStyleBorderColour".equals(name))
				{
					return new Color(0, 0, 1);
				}
				if ("tileFillOpacity".equals(name))
				{
					return 10;
				}
				if ("regionFillOpacity".equals(name))
				{
					return 20;
				}
				if ("borderStyleFillOpacity".equals(name))
				{
					return 30;
				}
				if ("regionCornersOnly".equals(name))
				{
					return true;
				}
				if ("borderStyleCornerSize".equals(name))
				{
					return 30;
				}
				if ("tileCornerSize".equals(name) || "regionCornerSize".equals(name))
				{
					return 22;
				}
				if ("tileMergeBorders".equals(name) || "borderMergeBorders".equals(name))
				{
					return true;
				}
				if ("regionMergeBorders".equals(name))
				{
					return false;
				}
				Class<?> type = method.getReturnType();
				if (type == boolean.class)
				{
					return false;
				}
				if (type == int.class)
				{
					return 0;
				}
				return null;
			});
	}
}

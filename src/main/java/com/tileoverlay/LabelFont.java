/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.awt.Font;
import net.runelite.client.ui.FontManager;

public enum LabelFont
{
	RUNESCAPE("RuneScape"),
	RUNESCAPE_SMALL("RuneScape small"),
	RUNESCAPE_BOLD("RuneScape bold"),
	BOLD("Bold"),
	PLAIN("Plain");

	private final String label;

	LabelFont(String label)
	{
		this.label = label;
	}

	public Font create(int size)
	{
		Font base;
		switch (this)
		{
			case RUNESCAPE_SMALL:
				base = FontManager.getRunescapeSmallFont();
				break;
			case RUNESCAPE_BOLD:
				base = FontManager.getRunescapeBoldFont();
				break;
			case BOLD:
				base = FontManager.getDefaultBoldFont();
				break;
			case PLAIN:
				base = FontManager.getDefaultFont();
				break;
			case RUNESCAPE:
			default:
				base = FontManager.getRunescapeFont();
				break;
		}
		if (base == null)
		{
			int style = this == BOLD || this == RUNESCAPE_BOLD ? Font.BOLD : Font.PLAIN;
			return new Font(Font.SANS_SERIF, style, size);
		}
		return base.deriveFont((float) size);
	}

	@Override
	public String toString()
	{
		return label;
	}
}

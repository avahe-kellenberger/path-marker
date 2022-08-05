package com.pathmarker;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.*;

@ConfigGroup("pathmarker")
public interface PathMarkerConfig extends Config
{
	@ConfigSection(
			name = "Active Path",
			description = "All options related to your active path",
			position = 0
	)
	String activePathSection = "activePath";

	@ConfigSection(
			name = "Hover-Path",
			description = "All options related to the path to the hovered location",
			position = 1
	)
	String hoverPathSection = "hoverPath";

	@ConfigItem(
		keyName = "markActivePath",
		name = "Mark active path",
		description = "Marks your active path",
		section = activePathSection
	)
	default boolean markActivePath()
	{
		return true;
	}

	@ConfigItem(
			keyName = "activePathColor1",
			name = "Active path color 1",
			description = "The main color of your active path",
			section = activePathSection
	)
	default Color activePathColor1()
	{
		return Color.RED;
	}

	@ConfigItem(
			keyName = "activePathColor2",
			name = "Active path color 2",
			description = "The secondary color of your active path, indicating the tiles you 'skip' while running.",
			section = activePathSection
	)
	default Color activePathColor2()
	{
		return Color.YELLOW;
	}

	@ConfigItem(
			keyName = "activePathFillOpacity",
			name = "Active path fill opacity",
			description = "Opacity of the active path's tile fill color",
			section = activePathSection
	)
	default int activePathFillOpacity()
	{
		return 50;
	}

	@ConfigItem(
			keyName = "markHoverPath",
			name = "Mark hover-path",
			description = "Marks the path to the hovered location",
			section = hoverPathSection
	)
	default boolean markHoverPath()
	{
		return false;
	}

	@ConfigItem(
			keyName = "hoverPathColor1",
			name = "Hover-path color 1",
			description = "The main color of the hover-path",
			section = hoverPathSection
	)
	default Color hoverPathColor1()
	{
		return Color.MAGENTA;
	}

	@ConfigItem(
			keyName = "hoverPathColor2",
			name = "Hover-path color 2",
			description = "The secondary color of the hover-path, indicating the tiles you 'skip' while running.",
			section = hoverPathSection
	)
	default Color hoverPathColor2()
	{
		return Color.GREEN;
	}

	@ConfigItem(
			keyName = "hoverPathFillOpacity",
			name = "Hover-path fill opacity",
			description = "Opacity of the hover-path's tile fill color",
			section = hoverPathSection
	)
	default int hoverPathFillOpacity()
	{
		return 50;
	}
}

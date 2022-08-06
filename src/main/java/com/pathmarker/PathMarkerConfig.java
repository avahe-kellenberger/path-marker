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

	enum pathMode
	{
		BOTH,
		GAME_WORLD,
		MINIMAP,
		NEITHER
	}
	@ConfigItem(
		keyName = "activePathDrawLocations",
		name = "Draw location(s)",
		description = "Marks your active path on the game world and/or the minimap",
		section = activePathSection
	)
	default pathMode activePathDrawLocations()
	{
		return pathMode.BOTH;
	}

	@ConfigItem(
			keyName = "activePathColor1",
			name = "Main tile color",
			description = "The main color of your active path",
			section = activePathSection
	)
	default Color activePathColor1()
	{
		return Color.RED;
	}

	@ConfigItem(
			keyName = "activePathColor2",
			name = "Secondary tile color",
			description = "The secondary color of your active path, indicating the tiles you 'skip' while running.",
			section = activePathSection
	)
	default Color activePathColor2()
	{
		return Color.YELLOW;
	}

	@ConfigItem(
			keyName = "activePathFillOpacity",
			name = "Tile fill opacity",
			description = "Opacity of the active path's tile fill color",
			section = activePathSection
	)
	default int activePathFillOpacity()
	{
		return 50;
	}

	@ConfigItem(
			keyName = "hoverPathMode",
			name = "Draw location(s)",
			description = "Marks your hover-path on the game world and/or the minimap",
			section = hoverPathSection
	)
	default pathMode hoverPathMode()
	{
		return pathMode.NEITHER;
	}

	@ConfigItem(
			keyName = "hoverPathColor1",
			name = "Main color",
			description = "The main color of the hover-path",
			section = hoverPathSection
	)
	default Color hoverPathColor1()
	{
		return Color.MAGENTA;
	}

	@ConfigItem(
			keyName = "hoverPathColor2",
			name = "Secondary color",
			description = "The secondary color of the hover-path, indicating the tiles you 'skip' while running.",
			section = hoverPathSection
	)
	default Color hoverPathColor2()
	{
		return Color.GREEN;
	}

	@ConfigItem(
			keyName = "hoverPathFillOpacity",
			name = "Tile fill opacity",
			description = "Opacity of the hover-path's tile fill color",
			section = hoverPathSection
	)
	default int hoverPathFillOpacity()
	{
		return 50;
	}
}

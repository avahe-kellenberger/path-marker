package com.pathmarker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

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

    enum drawLocations
    {
        BOTH,
        GAME_WORLD,
        MINIMAP
    }
    @ConfigItem(
            keyName = "activePathDrawLocations",
            name = "Draw location(s)",
            description = "Marks your active path on the game world and/or the minimap",
            section = activePathSection
    )
    default drawLocations activePathDrawLocations()
    {
        return drawLocations.BOTH;
    }

    enum DrawMode
    {
        FULL_PATH,
        TARGET_TILE
    }
    @ConfigItem(
            keyName = "activePathDrawMode",
            name = "Draw mode",
            description = "Determines which tiles are drawn",
            section = activePathSection
    )
    default DrawMode activePathDrawMode()
    {
        return DrawMode.FULL_PATH;
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

    @Range(
            max = 255
    )
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

    enum PathDisplaySetting
    {
        ALWAYS,
        WHILE_KEY_PRESSED,
        TOGGLE_ON_KEYPRESS,
        NEVER
    }
    @ConfigItem(
            keyName = "activePathDisplaySetting",
            name = "Display",
            description = "Configures when the active path should be displayed",
            section = activePathSection
    )
    default PathDisplaySetting activePathDisplaySetting()
    {
        return PathDisplaySetting.ALWAYS;
    }

    @ConfigItem(
            keyName = "displayKeybindActivePath",
            name = "Keybind",
            description = "Sets the keybind for the Display setting.",
            section = activePathSection
    )
    default Keybind displayKeybindActivePath()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            keyName = "hoverPathDrawLocations",
            name = "Draw location(s)",
            description = "Marks your hover-path on the game world and/or the minimap",
            section = hoverPathSection
    )
    default drawLocations hoverPathDrawLocations()
    {
        return drawLocations.BOTH;
    }

    @ConfigItem(
            keyName = "hoverPathDrawMode",
            name = "Draw mode",
            description = "Determines which tiles are drawn",
            section = hoverPathSection
    )
    default DrawMode hoverPathDrawMode()
    {
        return DrawMode.FULL_PATH;
    }

    @ConfigItem(
            keyName = "hoverPathColor1",
            name = "Main tile color",
            description = "The main color of the hover-path",
            section = hoverPathSection
    )
    default Color hoverPathColor1()
    {
        return Color.MAGENTA;
    }

    @ConfigItem(
            keyName = "hoverPathColor2",
            name = "Secondary tile color",
            description = "The secondary color of the hover-path, indicating the tiles you 'skip' while running.",
            section = hoverPathSection
    )
    default Color hoverPathColor2()
    {
        return Color.GREEN;
    }

    @Range(
            max = 255
    )
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

    @ConfigItem(
            keyName = "hoverPathDisplaySetting",
            name = "Display",
            description = "Configures when the hover-path should be displayed",
            section = hoverPathSection
    )
    default PathDisplaySetting hoverPathDisplaySetting()
    {
        return PathDisplaySetting.NEVER;
    }

    @ConfigItem(
            keyName = "displayKeybindHoverPath",
            name = "Keybind",
            description = "Sets the keybind for the Display setting.",
            section = hoverPathSection
    )
    default Keybind displayKeybindHoverPath()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            keyName = "drawOnlyIfNoActivePath",
            name = "Draw only if no active path",
            description = "Marks the path to your hovered location only if you don't have an active path",
            position = 0,
            section = hoverPathSection
    )
    default boolean drawOnlyIfNoActivePath()
    {
        return false;
    }
}

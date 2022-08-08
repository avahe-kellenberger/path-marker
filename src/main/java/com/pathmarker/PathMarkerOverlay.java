package com.pathmarker;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;

public class PathMarkerOverlay extends Overlay
{
    private final Client client;
    private final PathMarkerPlugin plugin;

    @Inject
    private PathMarkerConfig config;

    @Inject
    private PathMarkerOverlay(Client client, PathMarkerConfig config, PathMarkerPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if ((config.hoverPathDisplaySetting() != PathMarkerConfig.PathDisplaySetting.NEVER)
                && (config.activePathDisplaySetting() == PathMarkerConfig.PathDisplaySetting.NEVER || !plugin.isPathActive() || !config.drawOnlyIfNoActivePath())
                && (plugin.isKeyDisplayHoverPath() || config.hoverPathDisplaySetting() == PathMarkerConfig.PathDisplaySetting.ALWAYS))
        {
            for (WorldPoint worldPoint : plugin.getHoverPathTiles())
            {
                if (config.hoverPathDrawLocations() == PathMarkerConfig.drawLocations.BOTH || config.hoverPathDrawLocations() == PathMarkerConfig.drawLocations.GAME_WORLD)
                {
                    if (config.hoverPathDrawMode() == PathMarkerConfig.DrawMode.FULL_PATH || worldPoint == plugin.getHoverPathTiles().get(plugin.getHoverPathTiles().size() - 1))
                    {
                        renderTile(graphics, worldPoint, config.hoverPathColor1(), config.hoverPathColor1());
                    }
                }
            }
            for (WorldPoint worldPoint : plugin.getHoverMiddlePathTiles())
            {
                if (config.hoverPathDrawLocations() == PathMarkerConfig.drawLocations.BOTH || config.hoverPathDrawLocations() == PathMarkerConfig.drawLocations.GAME_WORLD)
                {
                    if (config.hoverPathDrawMode() == PathMarkerConfig.DrawMode.FULL_PATH || worldPoint == plugin.getHoverPathTiles().get(plugin.getHoverPathTiles().size() - 1))
                    {
                        renderTile(graphics, worldPoint, config.hoverPathColor2(), config.hoverPathColor2());
                    }
                }
            }
        }
        if (config.activePathDisplaySetting() != PathMarkerConfig.PathDisplaySetting.NEVER && plugin.isPathActive()
                && (plugin.isKeyDisplayActivePath() || config.activePathDisplaySetting() == PathMarkerConfig.PathDisplaySetting.ALWAYS))
        {
            for (WorldPoint worldPoint : plugin.getActivePathTiles())
            {
                if (config.activePathDrawLocations() == PathMarkerConfig.drawLocations.BOTH || config.activePathDrawLocations() == PathMarkerConfig.drawLocations.GAME_WORLD)
                {
                    if (config.activePathDrawMode() == PathMarkerConfig.DrawMode.FULL_PATH || worldPoint == plugin.getActivePathTiles().get(plugin.getActivePathTiles().size() - 1))
                    {
                        renderTile(graphics, worldPoint, config.activePathColor1(), config.activePathColor1());
                    }
                }
            }
            for (WorldPoint worldPoint : plugin.getActiveMiddlePathTiles())
            {
                if (config.activePathDrawLocations() == PathMarkerConfig.drawLocations.BOTH || config.activePathDrawLocations() == PathMarkerConfig.drawLocations.GAME_WORLD)
                {
                    if (config.activePathDrawMode() == PathMarkerConfig.DrawMode.FULL_PATH || worldPoint == plugin.getActivePathTiles().get(plugin.getActivePathTiles().size() - 1))
                    {
                        renderTile(graphics, worldPoint, config.activePathColor2(), config.activePathColor2());
                    }
                }
            }
        }
        return null;
    }

    private void renderTile(Graphics2D graphics, WorldPoint worldPoint, Color color, Color fillOpacity)
    {
        Stroke stroke = new BasicStroke(1);
        LocalPoint lp = LocalPoint.fromWorld(client, worldPoint);
        if (lp == null)
        {
            return;
        }
        final Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null)
        {
            return;
        }
        OverlayUtil.renderPolygon(graphics, poly, color, fillOpacity, stroke);
    }
}

package com.pathmarker;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
        Color fillColour1;
        Color fillColour2;
        if (config.hoverPathDrawLocations() != PathMarkerConfig.drawLocations.NEITHER)
        {
            fillColour1 = new Color(config.hoverPathColor1().getRed(),config.hoverPathColor1().getGreen(),config.hoverPathColor1().getBlue(),config.hoverPathFillOpacity());
            fillColour2 = new Color(config.hoverPathColor2().getRed(),config.hoverPathColor2().getGreen(),config.hoverPathColor2().getBlue(),config.hoverPathFillOpacity());
            for (WorldPoint worldPoint : plugin.getHoverPathTiles())
            {
                if (config.hoverPathDrawLocations() == PathMarkerConfig.drawLocations.BOTH || config.hoverPathDrawLocations() == PathMarkerConfig.drawLocations.GAME_WORLD)
                {
                    if (config.hoverPathDrawMode() == PathMarkerConfig.drawMode.FULL_PATH || worldPoint == plugin.getHoverPathTiles().get(plugin.getHoverPathTiles().size() - 1))
                    {
                        renderTile(graphics, worldPoint, config.hoverPathColor1(), fillColour1);
                    }
                }
            }
            for (WorldPoint worldPoint : plugin.getHoverMiddlePathTiles())
            {
                if (config.hoverPathDrawLocations() == PathMarkerConfig.drawLocations.BOTH || config.hoverPathDrawLocations() == PathMarkerConfig.drawLocations.GAME_WORLD)
                {
                    if (config.hoverPathDrawMode() == PathMarkerConfig.drawMode.FULL_PATH || worldPoint == plugin.getHoverPathTiles().get(plugin.getHoverPathTiles().size() - 1))
                    {
                        renderTile(graphics, worldPoint, config.hoverPathColor2(), fillColour2);
                    }
                }
            }
        }
        if (config.activePathDrawLocations() != PathMarkerConfig.drawLocations.NEITHER && plugin.isPathActive())
        {
            fillColour1 = new Color(config.activePathColor1().getRed(),config.activePathColor1().getGreen(),config.activePathColor1().getBlue(),config.activePathFillOpacity());
            fillColour2 = new Color(config.activePathColor2().getRed(),config.activePathColor2().getGreen(),config.activePathColor2().getBlue(),config.activePathFillOpacity());
            for (WorldPoint worldPoint : plugin.getActivePathTiles())
            {
                if (config.activePathDrawLocations() == PathMarkerConfig.drawLocations.BOTH || config.activePathDrawLocations() == PathMarkerConfig.drawLocations.GAME_WORLD)
                {
                    if (config.activePathDrawMode() == PathMarkerConfig.drawMode.FULL_PATH || worldPoint == plugin.getActivePathTiles().get(plugin.getActivePathTiles().size() - 1))
                    {
                        renderTile(graphics, worldPoint, config.activePathColor1(), fillColour1);
                    }
                }
            }
            for (WorldPoint worldPoint : plugin.getActiveMiddlePathTiles())
            {
                if (config.activePathDrawLocations() == PathMarkerConfig.drawLocations.BOTH || config.activePathDrawLocations() == PathMarkerConfig.drawLocations.GAME_WORLD)
                {
                    if (config.activePathDrawMode() == PathMarkerConfig.drawMode.FULL_PATH || worldPoint == plugin.getActivePathTiles().get(plugin.getActivePathTiles().size() - 1))
                    {
                        renderTile(graphics, worldPoint, config.activePathColor2(), fillColour2);
                    }
                }
            }
        }
        /*for (WorldPoint worldPoint : plugin.getActiveCheckpointWPs())
        {
            renderTile(graphics, worldPoint, Color.GREEN);
        }*/
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

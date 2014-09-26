/*
 * Copyright (c) IntellectualCrafters - 2014.
 * You are not allowed to distribute and/or monetize any of our intellectual property.
 * IntellectualCrafters is not affiliated with Mojang AB. Minecraft is a trademark of Mojang AB.
 *
 * >> File = Unlink.java
 * >> Generated by: Citymonstret at 2014-08-09 01:41
 */

package com.intellectualcrafters.plot.commands;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.intellectualcrafters.plot.C;
import com.intellectualcrafters.plot.PlayerFunctions;
import com.intellectualcrafters.plot.Plot;
import com.intellectualcrafters.plot.PlotHelper;
import com.intellectualcrafters.plot.PlotId;
import com.intellectualcrafters.plot.PlotMain;
import com.intellectualcrafters.plot.PlotSettings;
import com.intellectualcrafters.plot.PlotWorld;
import com.intellectualcrafters.plot.SetBlockFast;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.events.PlotMergeEvent;
import com.intellectualcrafters.plot.events.PlotUnlinkEvent;

/**
 * Created by Citymonstret on 2014-08-01.
 */
public class Unlink extends SubCommand {

    private short w_id;
    private byte w_v;
    private short wf_id;
    private byte wf_v;
    private short f1_id;
    private byte f1_v;
    private short f2_id;
    private byte f2_v;
    private int pathsize;
    private boolean stripes_enabled;
    private int wallheight;
    private int roadheight;
    public Unlink() {
        super(Command.UNLINK, "Unlink a mega-plot", "unlink", CommandCategory.ACTIONS);
    }

    @Override
    public boolean execute(Player plr, String... args) {
        if (!PlayerFunctions.isInPlot(plr)) {
            PlayerFunctions.sendMessage(plr, "You're not in a plot.");
            return true;
        }
        Plot plot = PlayerFunctions.getCurrentPlot(plr);
        if ((plot==null || !plot.hasOwner() || !plot.getOwner().equals(plr.getUniqueId())) && !plr.hasPermission("plots.admin")) {
            PlayerFunctions.sendMessage(plr, C.NO_PLOT_PERMS);
            return true;
        }
        if (PlayerFunctions.getTopPlot(plr.getWorld(), plot).equals(PlayerFunctions.getBottomPlot(plr.getWorld(), plot))) {
            PlayerFunctions.sendMessage(plr, C.UNLINK_IMPOSSIBLE);
            return true;
        }
        World world = plr.getWorld();
        PlotId pos1 = PlayerFunctions.getBottomPlot(world, plot).id;
        PlotId pos2 = PlayerFunctions.getTopPlot(world, plot).id;
        ArrayList<PlotId> ids = PlayerFunctions.getPlotSelectionIds(world, pos1, pos2);
        
        PlotUnlinkEvent event = new PlotUnlinkEvent(world, ids);
        
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            event.setCancelled(true);
            PlayerFunctions.sendMessage(plr, "&cUnlink has been cancelled");
            return false;
        }
        
        for (PlotId id:ids) {
            Plot myplot = PlotMain.getPlots(world).get(id);
            myplot.settings.setMerged(new boolean[] {false, false, false, false} );
            DBFunc.setMerged(world.getName(), myplot, myplot.settings.getMerged());
        }
        
        PlotWorld plotworld = PlotMain.getWorldSettings(world);
        this.pathsize = plotworld.ROAD_WIDTH;
        this.roadheight = plotworld.ROAD_HEIGHT;
        this.wallheight = plotworld.WALL_HEIGHT;
        this.stripes_enabled = plotworld.ROAD_STRIPES_ENABLED;
        
     // WALL
        short[] result_w = PlotHelper.getBlock(plotworld.WALL_BLOCK);
        this.w_id = result_w[0];
        this.w_v = (byte) result_w[1];

        // WALL FILLING
        short[] result_wf = PlotHelper.getBlock(plotworld.WALL_FILLING);
        this.wf_id = result_wf[0];
        this.wf_v = (byte) result_wf[1];

        // ROAD
        short[] result_f1 = PlotHelper.getBlock(plotworld.ROAD_BLOCK);
        this.f1_id = result_f1[0];
        this.f1_v = (byte) result_f1[1];
        //

        // Floor 2
        short[] result_f2 = PlotHelper.getBlock(plotworld.ROAD_STRIPES);
        this.f2_id = result_f2[0];
        this.f2_v = (byte) result_f2[1];
        
        for (int x = pos1.x; x <= pos2.x; x++) {
            for (int y = pos1.y; y <= pos2.y; y++) {
                
                boolean lx = x < pos2.x;
                boolean ly = y < pos2.y;
                
                PlotId id = new PlotId(x,y);
                
                if (lx) {
                    System.out.print("LX "+id);
                    setRoadX(world, id);
                    
                    if (ly) {
                        System.out.print("LXY "+id);
                        setRoadXY(world, id);
                    }
                    
                }
                
                if (ly) {
                    System.out.print("LY "+id);
                    setRoadY(world, id);
                }
                
            }
        }
        
        try {
            SetBlockFast.update(plr);
        }
        catch (Exception e) {
            
        }
        
        PlayerFunctions.sendMessage(plr, "&cPLOT UNLINKING IS NOT FINISHED (as you can see by the lack of roads appearing)");
        return true;
    }
    /**
     * Setting the road with the greatest X value
     * @param world
     * @param id
     */
    public void setRoadX(World w, PlotId id) {
        Location pos1 = PlotHelper.getPlotBottomLoc(w, id);
        Location pos2 = PlotHelper.getPlotTopLoc(w, id);
        
        int sx = pos2.getBlockX()+1;
        int ex = sx + pathsize - 1;
        int sz = pos1.getBlockZ()-1;
        int ez = pos2.getBlockZ()+2;
        
        PlotHelper.setSimpleCuboid(w, new Location(w,sx,Math.min(wallheight, roadheight)+1,sz+1), new Location(w,ex+1,257+1,ez), (short) 0);
        
        PlotHelper.setCuboid(w, new Location(w,sx,1,sz+1), new Location(w,sx+1,wallheight+1,ez), new short[] {wf_id}, new short[] {wf_v});
        PlotHelper.setCuboid(w, new Location(w,sx,wallheight+1,sz+1), new Location(w,sx+1,wallheight+2,ez), new short[] {w_id}, new short[] {w_v});
        
        PlotHelper.setCuboid(w, new Location(w,ex,1,sz+1), new Location(w,ex+1,wallheight+1,ez), new short[] {w_id}, new short[] {w_v});
        PlotHelper.setCuboid(w, new Location(w,ex,wallheight+1,sz+1), new Location(w,ex+1,wallheight+2,ez), new short[] {wf_id}, new short[] {wf_v});
        
        PlotHelper.setCuboid(w, new Location(w,sx+1,1,sz+1), new Location(w,ex,roadheight+1,ez), new short[] {f1_id}, new short[] {f1_v});
    }
    /**
     * Setting the road with the greatest Y value
     * @param world
     * @param id
     */
    public void setRoadY(World w, PlotId id) {
        Location pos1 = PlotHelper.getPlotBottomLoc(w, id);
        Location pos2 = PlotHelper.getPlotTopLoc(w, id);
        
        int sz = pos2.getBlockX()+1;
        int ez = sz + pathsize - 1;
        int sx = pos1.getBlockZ()-1;
        int ex = pos2.getBlockZ()+2;
        
        PlotHelper.setSimpleCuboid(w, new Location(w,sx,Math.min(wallheight, roadheight)+1,sz+1), new Location(w,ex+1,257+1,ez), (short) 0);
        
        PlotHelper.setCuboid(w, new Location(w,sx,1,sz+1), new Location(w,sx+1,wallheight+1,ez), new short[] {wf_id}, new short[] {wf_v});
        PlotHelper.setCuboid(w, new Location(w,sx,wallheight+1,sz+1), new Location(w,sx+1,wallheight+2,ez), new short[] {w_id}, new short[] {w_v});
        
        PlotHelper.setCuboid(w, new Location(w,ex,1,sz+1), new Location(w,ex+1,wallheight+1,ez), new short[] {wf_id}, new short[] {wf_v});
        PlotHelper.setCuboid(w, new Location(w,ex,wallheight+1,sz+1), new Location(w,ex+1,wallheight+2,ez), new short[] {w_id}, new short[] {w_v});
        
        PlotHelper.setCuboid(w, new Location(w,sx+1,1,sz+1), new Location(w,ex,roadheight+1,ez), new short[] {f1_id}, new short[] {f1_v});
    }
    /**
     * Setting the intersection with the greatest X and Y value
     * @param world
     * @param id
     */
    public void setRoadXY(World w, PlotId id) {
        Location pos2 = PlotHelper.getPlotTopLoc(w, id);
        
        int sx = pos2.getBlockX()+2;
        int ex = sx + pathsize - 1;
        int sz = pos2.getBlockZ()+2;
        int ez = sz + pathsize - 1;
        
        PlotHelper.setSimpleCuboid(w, new Location(w,sx,roadheight+1,sz+1), new Location(w,ex+1,257+1,ez), (short) 0);
        
        PlotHelper.setCuboid(w, new Location(w,sx+1,1,sz+1), new Location(w,ex,roadheight+1,ez), new short[] {f1_id}, new short[] {f1_v});
    }
}

package net.sushiclient.client.utils.combat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PistonPushTarget implements Comparable<PistonPushTarget> {

    private BlockPos pistonPos;
    private EnumFacing facing;
    private EntityPlayer target;
    private boolean pistonPlaced;
    private boolean redstonePlaced;
    private int placeCost;

    public PistonPushTarget(BlockPos pistonPos, EnumFacing facing, EntityPlayer target, int placeCost) {
        this.pistonPos = pistonPos;
        this.facing = facing;
        this.target = target;
        this.placeCost = placeCost;
        this.pistonPlaced = false;
        this.redstonePlaced = false;
    }

    public BlockPos getPistonPos() {
        return pistonPos;
    }

    public void setPistonPos(BlockPos pistonPos) {
        this.pistonPos = pistonPos;
    }

    public EnumFacing getFacing() {
        return facing;
    }

    public void setFacing(EnumFacing facing) {
        this.facing = facing;
    }

    public EntityPlayer getTarget() {
        return target;
    }

    public void setTarget(EntityPlayer target) {
        this.target = target;
    }

    public boolean isPistonPlaced() {
        return pistonPlaced;
    }

    public void setPistonPlaced(boolean pistonPlaced) {
        this.pistonPlaced = pistonPlaced;
    }

    public boolean isRedstonePlaced() {
        return redstonePlaced;
    }

    public void setRedstonePlaced(boolean redstonePlaced) {
        this.redstonePlaced = redstonePlaced;
    }

    public int getPlaceCost() {
        return placeCost;
    }

    public void setPlaceCost(int placeCost) {
        this.placeCost = placeCost;
    }
    public Vec3d getTargetPosition() {
        return target.getPositionVector();
    }
    @Override
    public int compareTo(PistonPushTarget o) {

        int temp = Boolean.compare(!pistonPlaced, !o.pistonPlaced);
        if (temp != 0) return temp;

        temp = Boolean.compare(!redstonePlaced, !o.redstonePlaced);
        if (temp != 0) return temp;

        temp = Integer.compare(placeCost, o.placeCost);
        if (temp != 0) return temp;

        Vec3d pistonCenter = new Vec3d(pistonPos).add(0.5, 0.5, 0.5);
        Vec3d otherPistonCenter = new Vec3d(o.pistonPos).add(0.5, 0.5, 0.5);
        double dist1 = pistonCenter.squareDistanceTo(target.getPositionVector());
        double dist2 = otherPistonCenter.squareDistanceTo(o.target.getPositionVector());
        return Double.compare(dist1, dist2);
    }
}

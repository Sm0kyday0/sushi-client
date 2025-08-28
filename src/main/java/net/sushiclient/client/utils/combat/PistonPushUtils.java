package net.sushiclient.client.utils.combat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.sushiclient.client.utils.EntityUtils;
import net.sushiclient.client.utils.world.BlockPlaceInfo;
import net.sushiclient.client.utils.world.BlockPlaceUtils;
import net.sushiclient.client.utils.world.BlockUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class PistonPushUtils {

    private static PlaceState getPlaceState(EntityPlayer player, BlockPos pos, EnumFacing facing) {
        if (player.world.getBlockState(pos).getBlock() == Blocks.PISTON) return PlaceState.PLACED;
        BlockPlaceInfo info = BlockUtils.findBlockPlaceInfo(player.world, pos);
        if (info == null) return PlaceState.UNREACHABLE;
        return PlaceState.AIR;
    }

    private static boolean checkPistonFacing(EntityPlayer player, BlockPos pos) {
        double dx = Math.abs(player.posX - pos.getX() - 0.5);
        double dz = Math.abs(player.posZ - pos.getZ() - 0.5);
        if (dx >= 2 || dz >= 2) return true;

        double eyeHeight = player.posY + player.getEyeHeight();
        if (eyeHeight - pos.getY() >= 2 || eyeHeight - pos.getY() <= 0) return false;

        return true;
    }

    public static List<PistonPushTarget> findTargets(EntityPlayer player, int maxTargets) {
        ArrayList<PistonPushTarget> targets = new ArrayList<>();
        int count = 0;

        for (EntityPlayer target : EntityUtils.getNearbyPlayers(6)) {
            if (target == player) continue;
            if (count++ >= maxTargets) break;

            BlockPos targetPos = BlockUtils.toBlockPos(target.getPositionVector());

            for (EnumFacing facing : EnumFacing.HORIZONTALS) {
                BlockPos pistonPos = targetPos.offset(facing.getOpposite());
                if (!checkPistonFacing(player, pistonPos)) continue;
                if (getPlaceState(player, pistonPos, facing) == PlaceState.UNREACHABLE) continue;

                List<BlockPlaceInfo> pistonPlace = BlockPlaceUtils.search(player.world, pistonPos, 5, new HashSet<>(), p -> true);
                int placeCost = pistonPlace == null ? 0 : pistonPlace.size();

                PistonPushTarget pushTarget = new PistonPushTarget(pistonPos, facing, target, placeCost);
                targets.add(pushTarget);
            }
        }

        return targets;
    }

    public static List<PistonPushTarget> findTargets(EntityPlayer player, int maxTargets, int maxDistance) {
        ArrayList<PistonPushTarget> result = new ArrayList<>();
        List<PistonPushTarget> allTargets = findTargets(player, maxTargets * 2); // 多めに取得

        allTargets.sort((o1, o2) -> {
            Vec3d pos1 = new Vec3d(o1.getPistonPos()).add(0.5, 0.5, 0.5);
            Vec3d pos2 = new Vec3d(o2.getPistonPos()).add(0.5, 0.5, 0.5);
            double d1 = pos1.squareDistanceTo(player.getPositionVector());
            double d2 = pos2.squareDistanceTo(player.getPositionVector());
            return Double.compare(d1, d2);
        });

        for (PistonPushTarget target : allTargets) {
            if (result.size() >= maxTargets) break;
            Vec3d pos = new Vec3d(target.getPistonPos()).add(0.5, 0.5, 0.5);
            if (pos.squareDistanceTo(player.getPositionVector()) > maxDistance * maxDistance) continue;
            result.add(target);
        }

        return result;
    }

    private enum PlaceState {
        PLACED,
        UNREACHABLE,
        AIR
    }
}

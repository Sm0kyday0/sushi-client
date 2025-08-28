package net.sushiclient.client.utils.combat;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.sushiclient.client.utils.EntityUtils;
import net.sushiclient.client.utils.world.BlockUtils;

import java.util.ArrayList;
import java.util.List;

public class CevBreakUtils {

    private static CevBreakAttack find(EntityPlayer player, EntityPlayer target, BlockPos pos, double enemy, double self, double ratio) {
        BlockPos obsidianPos = pos;
        BlockPos crystalPos = pos.up();

        IBlockState obsidianState = player.world.getBlockState(obsidianPos);
        Block obsidianBlock = obsidianState.getBlock();
        if (obsidianBlock != Blocks.OBSIDIAN && obsidianBlock != Blocks.AIR) return null;
        boolean obsidianPlaced = obsidianBlock == Blocks.OBSIDIAN;

        EntityEnderCrystal placed = null;
        Vec3d crystalVec = BlockUtils.toVec3d(crystalPos).add(0.5, 0, 0.5);

        player.world.setBlockState(obsidianPos, Blocks.AIR.getDefaultState());
        boolean canInteract = EntityUtils.canInteract(crystalVec.add(0, 1.7, 0), 6, 3);
        double damage = DamageUtils.getCrystalDamage(target, crystalVec);
        double selfDamage = DamageUtils.getCrystalDamage(player, crystalVec);
        player.world.setBlockState(obsidianPos, obsidianState);

        if (!canInteract) return null;
        if (damage < enemy) return null;
        if (selfDamage > self) return null;
        if (selfDamage / damage > ratio) return null;
        for (Entity crystal : player.world.loadedEntityList) {
            if (!(crystal instanceof EntityEnderCrystal)) continue;
            if (crystal.getPositionVector().squareDistanceTo(crystalVec) > 0.3) continue;
            placed = (EntityEnderCrystal) crystal;
            break;
        }

        if (placed == null) {
            AxisAlignedBB box = new AxisAlignedBB(crystalPos.getX(), crystalPos.getY(), crystalPos.getZ(),
                                               crystalPos.getX() + 1, crystalPos.getY() + 2, crystalPos.getZ() + 1);
            if (BlockUtils.isColliding(player.world, box)) return null;
        }

        return new CevBreakAttack(crystalPos, obsidianPos, player, target, placed, damage, placed != null, obsidianPlaced);
    }

    public static List<CevBreakAttack> find(EntityPlayer player, EntityPlayer target, double damage, double self, double ratio) {
        BlockPos headPos = new BlockPos(
            target.posX,
            Math.floor(target.posY + target.getEyeHeight() + 1.0),
            target.posZ
        );

        ArrayList<CevBreakAttack> result = new ArrayList<>();

        CevBreakAttack attack = find(player, target, headPos, damage, self, ratio);
        if (attack != null) {
            result.add(attack);
        }

        return result;
    }

    public static List<CevBreakAttack> find(EntityPlayer player, double damage, double self, double ratio) {
        ArrayList<CevBreakAttack> result = new ArrayList<>();
        for (EntityPlayer entity : EntityUtils.getNearbyPlayers(4)) {
            result.addAll(find(player, entity, damage, self, ratio));
        }
        return result;
    }
}
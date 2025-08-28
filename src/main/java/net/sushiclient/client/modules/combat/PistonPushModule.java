package net.sushiclient.client.modules.combat;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.player.EntityPlayer;
import net.sushiclient.client.config.Configuration;
import net.sushiclient.client.config.ConfigurationCategory;
import net.sushiclient.client.config.RootConfigurations;
import net.sushiclient.client.config.data.IntRange;
import net.sushiclient.client.events.EventHandler;
import net.sushiclient.client.events.EventHandlers;
import net.sushiclient.client.events.EventTiming;
import net.sushiclient.client.events.tick.ClientTickEvent;
import net.sushiclient.client.modules.*;
import net.sushiclient.client.task.forge.TaskExecutor;
import net.sushiclient.client.task.tasks.BlockPlaceTask;
import net.sushiclient.client.task.tasks.ItemSwitchMode;
import net.sushiclient.client.task.tasks.ItemSwitchTask;
import net.sushiclient.client.utils.UpdateTimer;
import net.sushiclient.client.utils.combat.PistonPushTarget;
import net.sushiclient.client.utils.combat.PistonPushUtils;
import net.sushiclient.client.utils.player.*;
import net.sushiclient.client.utils.world.BlockPlaceInfo;
import net.sushiclient.client.utils.world.BlockUtils;

import java.util.Collections;
import java.util.List;

public class PistonPushModule extends BaseModule {

    private final Configuration<IntRange> pistonDelay;
    private final Configuration<IntRange> redstoneDelay;
    private final Configuration<IntRange> recalculationDelay;
    private final Configuration<IntRange> maxTargets;
    private final Configuration<RotateMode> rotateMode;

    private final UpdateTimer recalculationTimer;

    private boolean running;
    private int repeatCounter;
    private int timeout;

    private CloseablePositionOperator operator;
    private PistonPushTarget currentTarget;

    public PistonPushModule(String id, Modules modules, Categories categories, RootConfigurations provider, ModuleFactory factory) {
        super(id, modules, categories, provider, factory);

        ConfigurationCategory delay = provider.getCategory("delay", "Delay Settings", null);
        pistonDelay = delay.get("piston_delay", "Piston Place Delay", null, IntRange.class, new IntRange(0, 20, 0, 1));
        redstoneDelay = delay.get("redstone_delay", "Redstone Place Delay", null, IntRange.class, new IntRange(0, 20, 0, 1));

        ConfigurationCategory other = provider.getCategory("other", "Other Settings", null);
        rotateMode = other.get("rotate_mode", "Rotate Mode", null, RotateMode.class, RotateMode.VANILLA);
        recalculationDelay = other.get("recalculation_delay", "Recalculation Delay", null, IntRange.class, new IntRange(1, 40, 0, 1));
        maxTargets = other.get("max_targets", "Max Targets", null, IntRange.class, new IntRange(1, 5, 1, 1));

        recalculationTimer = new UpdateTimer(false, recalculationDelay);
    }

    @Override
    public void onEnable() {
        EventHandlers.register(this);
        timeout = 5;
    }

    @Override
    public void onDisable() {
        EventHandlers.unregister(this);
        PositionUtils.close(operator);
        operator = null;
        stop();
        currentTarget = null;
    }

    private void stop() {
        running = false;
    }

    public void update() {
        if (running) return;
        if (repeatCounter >= 5) {
            repeatCounter = 0;
            return;
        }

        if (currentTarget == null || (repeatCounter == 0 && recalculationTimer.update())) {
            List<PistonPushTarget> targets = PistonPushUtils.findTargets(getPlayer(), maxTargets.getValue().getCurrent());
            if (targets.isEmpty()) return;
            Collections.sort(targets);
            currentTarget = targets.get(0);
        }

        if (currentTarget == null) return;

        running = true;
        Vec3d lookAt = currentTarget.getTargetPosition().add(0, 1, 0);

        if (operator == null) {
            PositionOperator fake = new PositionOperator();
            fake.desyncMode(PositionMask.LOOK).lookAt(lookAt);
            sendPacket(new CPacketPlayer.Rotation(fake.getYaw(), fake.getPitch(), getPlayer().onGround));
            operator = PositionUtils.desync().desyncMode(PositionMask.LOOK);
        }

        operator.lookAt(lookAt);

        final PistonPushTarget target = this.currentTarget;

        if (!target.isPistonPlaced()) {
            BlockPos pos = target.getPistonPos();
            IBlockState state = getWorld().getBlockState(pos);
            TaskExecutor.newTaskChain()
                    .delay(pistonDelay.getValue().getCurrent())
                    .supply(Item.getItemFromBlock(Blocks.PISTON))
                    .then(new ItemSwitchTask(null, ItemSwitchMode.INVENTORY))
                    .abortIfFalse()
                    .then(() -> {
                        BlockPlaceInfo info = BlockUtils.findBlockPlaceInfo(getWorld(), pos);
                        if (info == null) return;
                        BlockUtils.place(info, true);
                        getWorld().setBlockState(pos, Blocks.PISTON.getDefaultState());
                        target.setPistonPlaced(true);
                    })
                    .last(() -> getWorld().setBlockState(pos, state))
                    .last(this::stop)
                    .execute();
            return;
        }

        if (!target.isRedstonePlaced()) {
            boolean found = false;
            for (EnumFacing facing : EnumFacing.HORIZONTALS) {
                BlockPos pos = target.getPistonPos().offset(facing);
                IBlockState state = getWorld().getBlockState(pos);
                BlockPlaceInfo info = BlockUtils.findBlockPlaceInfo(getWorld(), pos);
                if (info == null) continue;
                TaskExecutor.newTaskChain()
                        .delay(redstoneDelay.getValue().getCurrent())
                        .supply(Item.getItemFromBlock(Blocks.REDSTONE_BLOCK))
                        .then(new ItemSwitchTask(null, ItemSwitchMode.INVENTORY))
                        .abortIfFalse()
                        .then(() -> {
                            BlockUtils.place(info, true);
                            target.setRedstonePlaced(true);
                            getWorld().setBlockState(pos, Blocks.REDSTONE_BLOCK.getDefaultState());
                        })
                        .last(() -> getWorld().setBlockState(pos, state))
                        .last(this::stop)
                        .execute();
                found = true;
                break;
            }
            if (found) return;
        }

        stop();
        currentTarget = null;
        repeatCounter = 0;
    }

    @EventHandler(timing = EventTiming.POST)
    public void onPostClientTick(ClientTickEvent e) {
        repeatCounter = 0;
        update();

        if (currentTarget == null && timeout-- < 0) {
            PositionUtils.close(operator);
            operator = null;
        }

        if (currentTarget != null) {
            timeout = 5;
        }
    }

    @Override
    public String getDefaultName() {
        return "PistonPush";
    }

    @Override
    public Category getDefaultCategory() {
        return Category.COMBAT;
    }
}

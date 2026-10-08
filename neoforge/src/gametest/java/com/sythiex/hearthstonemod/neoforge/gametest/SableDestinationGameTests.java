package com.sythiex.hearthstonemod.neoforge.gametest;

import java.util.List;
import java.util.UUID;

import com.sythiex.hearthstonemod.HearthstoneCommon;
import com.sythiex.hearthstonemod.config.HearthstoneConfigData;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationBridge;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationResolution;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationResolution.Result;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationResolution.Status;
import com.sythiex.hearthstonemod.content.HearthstoneDataComponents;
import com.sythiex.hearthstonemod.content.HearthstoneHomeLink;
import com.sythiex.hearthstonemod.content.HearthstoneItemData;
import com.sythiex.hearthstonemod.content.HearthstoneLinkType;
import com.sythiex.hearthstonemod.content.ItemHearthstone;
import com.sythiex.hearthstonemod.neoforge.compat.sable.SableDestinationBridge;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import dev.ryanhcode.sable.sublevel.tracking_points.TrackingPoint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;


/**
 * Actual Sable fixtures: no fake sublevel, transform, storage, or loading implementation.
 */
@PrefixGameTestTemplate(false)
public final class SableDestinationGameTests
{
    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty")
    public void movedBedAndBrokenBedUseCurrentTransform(GameTestHelper helper)
    {
        Fixture fixture = fixture(helper);
        FakePlayer player = HearthstoneDestinationGameTests.player(helper.getLevel());
        BlockPos respawn = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setRespawnPosition(helper.getLevel().dimension(), respawn, 0, true, false);
        ItemStack stack = HearthstoneDestinationGameTests.stone(player, fixture.link());
        try
        {
            fixture.subLevel().logicalPose().position().add(12, 6, 9);
            fixture.subLevel().logicalPose().orientation().rotationY(Math.PI / 2);
            try (HearthstoneDestinationResolution resolution = ItemHearthstone.prepareCast(player, stack))
            {
                Result result = resolution.poll();
                helper.assertTrue(result.status() == Status.READY, "Loaded destination must resolve");
                helper.assertTrue(ItemHearthstone.finishCast(player, stack, result.destination()), "Bed teleport must succeed");
                Vec3 localArrival = fixture.subLevel().logicalPose().transformPositionInverse(player.position());
                helper.assertTrue(localArrival.distanceToSqr(Vec3.atCenterOf(fixture.home())) < 16, "Arrival must be near the bed in sublevel space");
                helper.assertTrue(player.getRespawnPosition().equals(respawn), "Hearthstone must not alter respawn position");
            }
            helper.getLevel().setBlock(fixture.home(), Blocks.AIR.defaultBlockState(), 2);
            helper.getLevel().setBlock(fixture.home().east(), Blocks.AIR.defaultBlockState(), 2);
            stack.set(HearthstoneDataComponents.hearthstoneData(), HearthstoneItemData.DEFAULT.withHomeLink(fixture.link()));
            try (HearthstoneDestinationResolution resolution = ItemHearthstone.prepareCast(player, stack))
            {
                Result result = resolution.poll();
                helper.assertTrue(result.status() == Status.READY && ItemHearthstone.finishCast(player, stack, result.destination()), "Confirmed missing bed uses transformed fallback");
                Vec3 expected = fixture.subLevel().logicalPose().transformPosition(Vec3.atBottomCenterOf(fixture.home()).add(0, 1, 0));
                helper.assertTrue(player.position().distanceToSqr(expected) < 0.001, "Fallback must use world-space coordinates");
                helper.assertTrue(HearthstoneDestinationGameTests.data(stack).homeLink().isEmpty(), "Confirmed missing bed clears link");
            }
        } finally { remove(fixture); }
        helper.succeed();
    }

    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty")
    public void legacyAndDeletedTargetsFailWithoutMutation(GameTestHelper helper)
    {
        Fixture fixture = fixture(helper);
        FakePlayer player = HearthstoneDestinationGameTests.player(helper.getLevel());
        HearthstoneHomeLink legacy = new HearthstoneHomeLink(fixture.home(), fixture.link().dimensionId(), HearthstoneLinkType.BED);
        try (HearthstoneDestinationResolution resolution = HearthstoneCommon.destinationBridge().prepare(player, legacy))
        {
            helper.assertTrue(resolution.poll().status() == Status.UNAVAILABLE, "Legacy plot links must require explicit relinking even when loaded");
        }
        ItemStack stack = HearthstoneDestinationGameTests.stone(player, fixture.link());
        remove(fixture);
        Vec3 before = player.position();
        HearthstoneDestinationGameTests.cast(player);
        helper.assertTrue(player.position().equals(before), "Deleted target must not teleport");
        helper.assertTrue(HearthstoneDestinationGameTests.data(stack).homeLink().orElseThrow().equals(fixture.link()), "Deleted target preserves link");
        helper.assertTrue(HearthstoneDestinationGameTests.data(stack).lastUsedGameTime() == -1, "Deleted target must not consume cooldown");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty")
    public void copiedLinksRemainIndependentWhenRelinked(GameTestHelper helper)
    {
        Fixture fixture = fixture(helper);
        FakePlayer player = HearthstoneDestinationGameTests.player(helper.getLevel());
        ItemStack original = HearthstoneDestinationGameTests.stone(player, fixture.link());
        ItemStack copy = original.copy();
        try
        {
            HearthstoneHomeLink ordinary = new HearthstoneHomeLink(helper.absolutePos(new BlockPos(1, 1, 1)), fixture.link().dimensionId(), HearthstoneLinkType.BED);
            original.set(HearthstoneDataComponents.hearthstoneData(), HearthstoneItemData.DEFAULT.withHomeLink(ordinary));
            try (HearthstoneDestinationResolution resolution = ItemHearthstone.prepareCast(player, copy))
            {
                helper.assertTrue(resolution.poll().status() == Status.READY, "Relinking a copy must not consume the shared tracking point");
            }
            helper.assertTrue(HearthstoneDestinationGameTests.data(copy).homeLink().orElseThrow().equals(fixture.link()), "Copy retains its own link");
        } finally { remove(fixture); }
        helper.succeed();
    }

    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty", timeoutTicks = 150)
    public void heldUnloadedSublevelLoadsBeforeBedValidation(GameTestHelper helper)
    {
        Fixture fixture = fixture(helper);
        fixture.subLevel().logicalPose().position().set(12000, 100, 12000);
        fixture.subLevel().forceUpdateGlobalBounds();
        SubLevelHoldingChunkMap holding = fixture.container().getHoldingChunkMap();
        holding.moveToUnloaded(fixture.subLevel(), new ChunkPos(750, 750));
        helper.assertTrue(fixture.container().getSubLevel(fixture.id()) == null, "Fixture must start unloaded");
        FakePlayer player = HearthstoneDestinationGameTests.player(helper.getLevel());
        ItemStack stack = HearthstoneDestinationGameTests.stone(player, fixture.link());
        HearthstoneDestinationResolution resolution = ItemHearthstone.prepareCast(player, stack);
        Vec3 before = player.position();
        helper.succeedWhen(() ->
        {
            Result result = resolution.poll();
            helper.assertTrue(result.status() != Status.UNAVAILABLE, "Held sublevel must remain recoverable");
            helper.assertTrue(player.position().equals(before), "Preparation must leave the player at source");
            helper.assertTrue(result.status() == Status.READY, "Wait for sublevel activation");
            try
            {
                helper.assertTrue(ItemHearthstone.finishCast(player, stack, result.destination()), "Recovered bed teleport succeeds");
                helper.assertTrue(player.position().distanceToSqr(new Vec3(12000, 100, 12000)) < 100, "Must arrive at the sublevel, not its plot");
                helper.assertTrue(HearthstoneDestinationGameTests.data(stack).homeLink().isPresent(), "Unloaded bed must not be treated as missing");
            } finally { resolution.close(); remove(fixture); }
        });
    }

    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty")
    public void anchorChargeChangesOnlyAfterSuccessfulArrival(GameTestHelper helper) throws Exception
    {
        Fixture fixture = fixture(helper);
        ServerLevel level = helper.getLevel();
        FakePlayer player = HearthstoneDestinationGameTests.player(level);
        HearthstoneConfigData previous = HearthstoneCommon.config();
        try
        {
            HearthstoneCommon.configStore().save(new HearthstoneConfigData(previous.channelTime(), previous.cooldown(), true, true, true, List.of()));
            level.setBlock(fixture.home(), Blocks.RESPAWN_ANCHOR.defaultBlockState().setValue(RespawnAnchorBlock.CHARGE, 2), 2);
            HearthstoneHomeLink link = HearthstoneCommon.destinationBridge().capture(level, fixture.home(), HearthstoneLinkType.RESPAWN_ANCHOR).link();
            ItemStack stack = HearthstoneDestinationGameTests.stone(player, link);
            try (HearthstoneDestinationResolution resolution = ItemHearthstone.prepareCast(player, stack))
            {
                helper.assertTrue(ItemHearthstone.finishCast(player, stack, resolution.poll().destination()), "Anchor arrival succeeds");
                helper.assertTrue(level.getBlockState(fixture.home()).getValue(RespawnAnchorBlock.CHARGE) == 1, "Successful arrival consumes exactly one charge");
            }
            var points = SubLevelTrackingPointSavedData.getOrLoad(level);
            UUID reference = link.externalTarget().orElseThrow().trackingPointId();
            TrackingPoint point = points.getTrackingPoint(reference);
            points.setTrackingPoint(reference, new TrackingPoint(true, UUID.randomUUID(), point.lastSavedSubLevelPointer(), point.point(), null));
            stack.set(HearthstoneDataComponents.hearthstoneData(), HearthstoneItemData.DEFAULT.withHomeLink(link));
            HearthstoneDestinationGameTests.cast(player);
            helper.assertTrue(level.getBlockState(fixture.home()).getValue(RespawnAnchorBlock.CHARGE) == 1, "Unresolved destination must not consume charge");
            helper.assertTrue(HearthstoneDestinationGameTests.data(stack).lastUsedGameTime() == -1, "Failed anchor cast has no cooldown");
        } finally { HearthstoneCommon.configStore().save(previous); remove(fixture); }
        helper.succeed();
    }

    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty", timeoutTicks = 150)
    public void savedUnloadedSublevelResolvesFromDisk(GameTestHelper helper)
    {
        Fixture fixture = fixture(helper);
        fixture.subLevel().logicalPose().position().set(14000, 100, 14000);
        fixture.subLevel().forceUpdateGlobalBounds();
        SubLevelHoldingChunkMap holding = fixture.container().getHoldingChunkMap();
        ChunkPos storageChunk = new ChunkPos(875, 875);
        holding.moveToUnloaded(fixture.subLevel(), storageChunk);
        holding.updateChunkStatus(storageChunk, false);
        holding.processChanges();
        holding.saveAll();
        helper.assertTrue(fixture.container().getSubLevel(fixture.id()) == null && holding.getHoldingSubLevel(fixture.id()) == null,
                "Fixture must have neither a loaded nor an in-memory held sublevel");
        UUID trackingId = fixture.link().externalTarget().orElseThrow().trackingPointId();
        TrackingPoint point = SubLevelTrackingPointSavedData.getOrLoad(helper.getLevel()).getTrackingPoint(trackingId);
        helper.assertTrue(point.lastSavedSubLevelPointer() != null, "Sable must have updated the saved tracking pointer");
        ServerLevel source = helper.getLevel().getServer().getLevel(Level.NETHER);
        FakePlayer player = HearthstoneDestinationGameTests.player(source);
        ItemStack stack = HearthstoneDestinationGameTests.stone(player, fixture.link());
        try (HearthstoneDestinationResolution resolution = ItemHearthstone.prepareCast(player, stack))
        {
            Result result = resolution.poll();
            helper.assertTrue(result.status() == Status.READY, "Saved destination must load through its persistent pointer");
            helper.assertTrue(player.serverLevel() == source, "Cross-dimension preparation must leave the player at source");
            helper.assertTrue(ItemHearthstone.finishCast(player, stack, result.destination()), "Saved destination teleport succeeds");
            helper.assertTrue(player.serverLevel() == helper.getLevel(), "Arrival must enter the sublevel's dimension");
            helper.assertTrue(player.position().distanceToSqr(new Vec3(14000, 100, 14000)) < 100, "Disk recovery must preserve the transform");
        } finally { player.connection.getConnection().channel().close(); remove(fixture); }
        helper.succeed();
    }

    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty")
    public void obstructedBedUsesTransformedFallback(GameTestHelper helper)
    {
        Fixture fixture = fixture(helper);
        FakePlayer player = HearthstoneDestinationGameTests.player(helper.getLevel());
        ItemStack stack = HearthstoneDestinationGameTests.stone(player, fixture.link());
        try
        {
            for (BlockPos pos : BlockPos.betweenClosed(fixture.home().offset(-3, 0, -3), fixture.home().offset(3, 3, 3)))
            {
                if (!pos.equals(fixture.home()) && !pos.equals(fixture.home().east()))
                    helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
            helper.assertTrue(BedBlock.findStandUpPosition(EntityType.PLAYER, helper.getLevel(), fixture.home(), Direction.EAST, 0).isEmpty(),
                    "Fixture must obstruct every stand-up position");
            fixture.subLevel().logicalPose().position().add(10, 5, 10);
            fixture.subLevel().logicalPose().orientation().rotationY(Math.PI / 2);
            try (HearthstoneDestinationResolution resolution = ItemHearthstone.prepareCast(player, stack))
            {
                Result result = resolution.poll();
                helper.assertTrue(result.status() == Status.READY && ItemHearthstone.finishCast(player, stack, result.destination()), "Blocked bed keeps existing fallback behavior");
                Vec3 expected = fixture.subLevel().logicalPose().transformPosition(Vec3.atBottomCenterOf(fixture.home()).add(0, 1, 0));
                helper.assertTrue(player.position().distanceToSqr(expected) < 0.001, "Blocked arrival must transform the fallback");
                helper.assertTrue(HearthstoneDestinationGameTests.data(stack).homeLink().isPresent(), "An obstructed bed is not a missing bed");
            }
        } finally { remove(fixture); }
        helper.succeed();
    }

    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty")
    public void concurrentResolutionsReleaseOnlyTheirOwnTickets(GameTestHelper helper)
    {
        Fixture fixture = fixture(helper);
        int initialTickets = ticketCount(fixture);
        HearthstoneDestinationResolution first = HearthstoneCommon.destinationBridge().prepare(HearthstoneDestinationGameTests.player(helper.getLevel()), fixture.link());
        HearthstoneDestinationResolution second = HearthstoneCommon.destinationBridge().prepare(HearthstoneDestinationGameTests.player(helper.getLevel()), fixture.link());
        try
        {
            helper.assertTrue(first.poll().status() == Status.READY && second.poll().status() == Status.READY, "Both players must resolve the shared destination");
            helper.assertTrue(ticketCount(fixture) == initialTickets + 2, "Each resolver owns a separate Sable ticket");
            first.close();
            first.close();
            helper.assertTrue(ticketCount(fixture) == initialTickets + 1, "Idempotent cleanup must preserve the other player's ticket");
            second.close();
            helper.assertTrue(ticketCount(fixture) == initialTickets, "All temporary Sable tickets must be removed");
        } finally { first.close(); second.close(); remove(fixture); }
        helper.succeed();
    }

    private static int ticketCount(Fixture fixture)
    {
        return fixture.container().collectForceLoadTickets().values().stream().mapToInt(tickets -> tickets.size()).sum();
    }

    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty")
    public void plotIdentityMismatchIsRejected(GameTestHelper helper)
    {
        Fixture fixture = fixture(helper);
        UUID id = fixture.link().externalTarget().orElseThrow().trackingPointId();
        var data = SubLevelTrackingPointSavedData.getOrLoad(helper.getLevel());
        TrackingPoint point = data.getTrackingPoint(id);
        data.setTrackingPoint(id, new TrackingPoint(true, UUID.randomUUID(), point.lastSavedSubLevelPointer(), point.point(), point.globalPlaceholderPosition()));
        FakePlayer player = HearthstoneDestinationGameTests.player(helper.getLevel());
        try (HearthstoneDestinationResolution resolution = HearthstoneCommon.destinationBridge().prepare(player, fixture.link()))
        {
            helper.assertTrue(resolution.poll().status() == Status.UNAVAILABLE, "A different owner in the plot must never be accepted");
        } finally { remove(fixture); }
        helper.succeed();
    }

    static Fixture fixture(GameTestHelper helper)
    {
        helper.assertTrue(HearthstoneCommon.destinationBridge() instanceof SableDestinationBridge, "Sable bootstrap must install the adapter");
        ServerLevel level = helper.getLevel();
        BlockPos bed = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(bed.below(), Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(bed.east().below(), Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(bed, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.FOOT), 2);
        level.setBlock(bed.east(), Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.HEAD), 2);
        List<BlockPos> blocks = List.of(bed, bed.east(), bed.below(), bed.east().below());
        ServerSubLevel subLevel = (ServerSubLevel) SubLevelAssemblyHelper.assembleBlocks(level, bed, blocks, BoundingBox3i.from(blocks));
        BlockPos home = subLevel.getPlot().getCenterBlock();
        HearthstoneDestinationBridge.Capture capture = HearthstoneCommon.destinationBridge().capture(level, home, HearthstoneLinkType.BED);
        helper.assertTrue(capture.link() != null && capture.link().externalTarget().isPresent(), "Capture must create a persistent external link");
        return new Fixture(SubLevelContainer.getContainer(level), subLevel, subLevel.getUniqueId(), home, capture.link());
    }

    static void remove(Fixture fixture)
    {
        SubLevel current = fixture.container().getSubLevel(fixture.id());
        if (current != null) fixture.container().removeSubLevel(current, SubLevelRemovalReason.REMOVED);
    }

    record Fixture(ServerSubLevelContainer container, ServerSubLevel subLevel, UUID id, BlockPos home, HearthstoneHomeLink link) {}
}

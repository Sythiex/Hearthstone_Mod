package com.sythiex.hearthstonemod.neoforge.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import com.sythiex.hearthstonemod.HearthstoneCasting;
import com.sythiex.hearthstonemod.HearthstoneCommon;
import com.sythiex.hearthstonemod.HearthstoneConstants;
import com.sythiex.hearthstonemod.compat.HearthstoneDestination;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationBridge;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationResolution;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationResolution.Result;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationResolution.Status;
import com.sythiex.hearthstonemod.compat.HearthstoneExternalTarget;
import com.sythiex.hearthstonemod.content.HearthstoneDataComponents;
import com.sythiex.hearthstonemod.content.HearthstoneHomeLink;
import com.sythiex.hearthstonemod.content.HearthstoneItemData;
import com.sythiex.hearthstonemod.content.HearthstoneLinkType;
import com.sythiex.hearthstonemod.content.ItemHearthstone;
import com.sythiex.hearthstonemod.network.HearthstoneNetworkBridge;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Exercises shared persistence and cast lifecycle in a real registry and server level.
 */
@PrefixGameTestTemplate(false)
public final class HearthstoneDestinationGameTests
{
    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty")
    public void legacyAndExternalLinksRoundTrip(GameTestHelper helper)
    {
        HearthstoneHomeLink legacy = new HearthstoneHomeLink(BlockPos.ZERO, Level.OVERWORLD.location(), HearthstoneLinkType.BED);
        var encodedLegacy = HearthstoneHomeLink.CODEC.encodeStart(JsonOps.INSTANCE, legacy).getOrThrow();
        helper.assertTrue(!encodedLegacy.getAsJsonObject().has("external_target"), "Legacy encoding should omit optional metadata");
        helper.assertTrue(HearthstoneHomeLink.CODEC.parse(JsonOps.INSTANCE, encodedLegacy).getOrThrow().equals(legacy), "Old saves must decode");
        HearthstoneHomeLink external = new HearthstoneHomeLink(legacy.homePos(), legacy.dimensionId(), legacy.linkType(),
                Optional.of(new HearthstoneExternalTarget(HearthstoneConstants.id("test_provider"), UUID.randomUUID())));
        var json = HearthstoneHomeLink.CODEC.encodeStart(JsonOps.INSTANCE, external).getOrThrow();
        helper.assertTrue(HearthstoneHomeLink.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(external), "External save round trip");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try
        {
            HearthstoneHomeLink.STREAM_CODEC.encode(buffer, external);
            helper.assertTrue(HearthstoneHomeLink.STREAM_CODEC.decode(buffer).equals(external), "External network round trip");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty")
    public void missingProviderPreservesPlayerAndItem(GameTestHelper helper)
    {
        FakePlayer player = player(helper.getLevel());
        HearthstoneHomeLink link = new HearthstoneHomeLink(new BlockPos(25000000, 80, 25000000), helper.getLevel().dimension().location(),
                HearthstoneLinkType.BED, Optional.of(new HearthstoneExternalTarget(HearthstoneConstants.id("missing"), UUID.randomUUID())));
        ItemStack stack = stone(player, link);
        Vec3 before = player.position();
        HearthstoneDestinationBridge original = HearthstoneCommon.destinationBridge();
        try
        {
            HearthstoneCommon.setDestinationBridge(HearthstoneDestinationBridge.DEFAULT);
            cast(player);
            helper.assertTrue(player.position().equals(before), "Missing provider must not move player");
            helper.assertTrue(data(stack).homeLink().orElseThrow().equals(link) && data(stack).lastUsedGameTime() == -1, "Failure must preserve link and cooldown");
        } finally { HearthstoneCasting.clear(player); HearthstoneCommon.setDestinationBridge(original); }
        helper.succeed();
    }

    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty")
    public void pendingTimeoutAndCancellationReleaseLeases(GameTestHelper helper)
    {
        FakePlayer player = player(helper.getLevel());
        HearthstoneHomeLink link = new HearthstoneHomeLink(helper.absolutePos(new BlockPos(2, 2, 2)), helper.getLevel().dimension().location(), HearthstoneLinkType.BED);
        ItemStack stack = stone(player, link);
        HearthstoneDestinationBridge original = HearthstoneCommon.destinationBridge();
        try
        {
            ProbeResolution timeout = new ProbeResolution(null);
            HearthstoneCommon.setDestinationBridge(probeBridge(timeout));
            HearthstoneCasting.startCasting(player);
            for (int i = 0; i < HearthstoneCommon.config().channelTime() + 99; i++) HearthstoneCasting.tick(player);
            helper.assertTrue(!HearthstoneCasting.isCasting(player.getUUID()) && timeout.closes == 1, "Timeout must close exactly once");
            helper.assertTrue(data(stack).lastUsedGameTime() == -1 && data(stack).homeLink().orElseThrow().equals(link), "Timeout must preserve item");

            for (int cancellation = 0; cancellation < 7; cancellation++)
            {
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                ProbeResolution pending = new ProbeResolution(null);
                HearthstoneCommon.setDestinationBridge(probeBridge(pending));
                cast(player);
                helper.assertTrue(HearthstoneCasting.isCasting(player.getUUID()), "Must stay casting while pending");
                switch (cancellation)
                {
                    case 0 -> HearthstoneCasting.requestStop(player);
                    case 1 -> player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
                    case 2 -> player.setPos(player.getX() + 1, player.getY(), player.getZ());
                    case 3 -> HearthstoneCasting.clear(player);
                    case 4 -> HearthstoneCasting.clearAll();
                    case 5 -> player.setHealth(0);
                    case 6 -> stack.set(HearthstoneDataComponents.hearthstoneData(), HearthstoneItemData.DEFAULT.withHomeLink(
                            new HearthstoneHomeLink(link.homePos().above(), link.dimensionId(), link.linkType())));
                }
                HearthstoneCasting.tick(player);
                HearthstoneCasting.clear(player);
                helper.assertTrue(pending.closes == 1 && !HearthstoneCasting.isCasting(player.getUUID()), "Cancellation must release the pending lease once");
                player.setHealth(20);
            }
        } finally { HearthstoneCasting.clear(player); HearthstoneCommon.setDestinationBridge(original); }
        helper.succeed();
    }

    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty")
    public void simultaneousCastsOwnSeparateLeases(GameTestHelper helper)
    {
        FakePlayer first = player(helper.getLevel());
        FakePlayer second = player(helper.getLevel());
        HearthstoneHomeLink link = new HearthstoneHomeLink(helper.absolutePos(new BlockPos(2, 2, 2)), helper.getLevel().dimension().location(), HearthstoneLinkType.BED);
        stone(first, link);
        stone(second, link);
        ProbeResolution firstLease = new ProbeResolution(null);
        ProbeResolution secondLease = new ProbeResolution(null);
        HearthstoneDestinationBridge original = HearthstoneCommon.destinationBridge();
        try
        {
            HearthstoneCommon.setDestinationBridge(new HearthstoneDestinationBridge()
            {
                public Capture capture(ServerLevel level, BlockPos pos, HearthstoneLinkType type) { return DEFAULT.capture(level, pos, type); }
                public HearthstoneDestinationResolution prepare(ServerPlayer player, HearthstoneHomeLink home) { return player == first ? firstLease : secondLease; }
            });
            cast(first);
            cast(second);
            HearthstoneCasting.clear(first);
            helper.assertTrue(firstLease.closes == 1 && secondLease.closes == 0 && HearthstoneCasting.isCasting(second.getUUID()),
                    "Disconnecting one player must leave the other cast intact");
        } finally { HearthstoneCasting.clear(first); HearthstoneCasting.clear(second); HearthstoneCommon.setDestinationBridge(original); }
        helper.assertTrue(secondLease.closes == 1, "Second player must release its own lease");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty")
    public void castingStopsInSourceDimensionAndRetainsArrivalLease(GameTestHelper helper)
    {
        FakePlayer player = player(helper.getLevel());
        ServerLevel destination = helper.getLevel().getServer().getLevel(Level.NETHER);
        BlockPos pos = new BlockPos(0, 100, 0);
        stone(player, new HearthstoneHomeLink(pos, destination.dimension().location(), HearthstoneLinkType.BED));
        ProbeResolution ready = new ProbeResolution(HearthstoneDestination.ordinary(destination, pos));
        HearthstoneDestinationBridge original = HearthstoneCommon.destinationBridge();
        HearthstoneNetworkBridge originalNetwork = HearthstoneCommon.networkBridge();
        List<ServerLevel> stopDimensions = new ArrayList<>();
        List<Integer> closesAtStop = new ArrayList<>();
        try
        {
            HearthstoneCommon.bootstrap(FMLPaths.CONFIGDIR.get(), (caster, casting) ->
            {
                if (caster == player && !casting)
                {
                    stopDimensions.add(caster.serverLevel());
                    closesAtStop.add(ready.closes);
                }
            });
            HearthstoneCommon.setDestinationBridge(probeBridge(ready));
            cast(player);
            helper.assertTrue(stopDimensions.equals(List.of(helper.getLevel())), "Exactly one stop update must reach the source trackers before dimension travel");
            helper.assertTrue(closesAtStop.equals(List.of(0)), "Stopping the animation must not close the destination lease");
            helper.assertTrue(player.serverLevel() == destination, "The cast must complete dimension travel");
            helper.assertTrue(!HearthstoneCasting.isCasting(player.getUUID()) && ready.closes == 0, "Arrival lease must survive completed casting");
            for (int i = 0; i < 19; i++) HearthstoneCasting.tick(player);
            helper.assertTrue(ready.closes == 0, "Lease must survive 19 ticks");
            HearthstoneCasting.tick(player);
            helper.assertTrue(ready.closes == 1, "Lease must close on tick 20");
        } finally
        {
            HearthstoneCasting.clear(player);
            HearthstoneCommon.setDestinationBridge(original);
            HearthstoneCommon.bootstrap(FMLPaths.CONFIGDIR.get(), originalNetwork);
            player.connection.getConnection().channel().close();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty")
    public void crossDimensionDestinationIsPreparedBeforeMoving(GameTestHelper helper)
    {
        FakePlayer player = player(helper.getLevel());
        ServerLevel destination = helper.getLevel().getServer().getLevel(Level.NETHER);
        BlockPos pos = new BlockPos(0, 100, 0);
        ItemStack stack = stone(player, new HearthstoneHomeLink(pos, Level.NETHER.location(), HearthstoneLinkType.BED));
        try (HearthstoneDestinationResolution resolution = ItemHearthstone.prepareCast(player, stack))
        {
            Result result = resolution.poll();
            helper.assertTrue(result.status() == Status.READY && player.serverLevel() == helper.getLevel(), "Preparation must not change dimensions");
            helper.assertTrue(ItemHearthstone.finishCast(player, stack, result.destination()), "Commit must succeed");
            helper.assertTrue(player.serverLevel() == destination && player.position().distanceToSqr(Vec3.atBottomCenterOf(pos).add(0, 1, 0)) < 0.001,
                    "One commit must arrive at the final destination");
        } finally { player.connection.getConnection().channel().close(); }
        helper.succeed();
    }

    static FakePlayer player(ServerLevel level)
    {
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "HearthstoneTest"));
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        // FakePlayer's default listener ignores teleports. Keep vanilla teleport bookkeeping,
        // replacing only outbound transport because this is a headless server test.
        player.connection = new ServerGamePacketListenerImpl(level.getServer(), connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false))
        {
            @Override
            public void send(Packet<?> packet) {}
            @Override
            public void send(Packet<?> packet, PacketSendListener listener) {}
        };
        return player;
    }

    static ItemStack stone(ServerPlayer player, HearthstoneHomeLink link)
    {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(HearthstoneConstants.id("hearthstone")));
        stack.set(HearthstoneDataComponents.hearthstoneData(), HearthstoneItemData.DEFAULT.withHomeLink(link));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack;
    }

    static HearthstoneItemData data(ItemStack stack)
    {
        return stack.get(HearthstoneDataComponents.hearthstoneData());
    }

    static void cast(ServerPlayer player)
    {
        HearthstoneCasting.startCasting(player);
        for (int i = 0; i < HearthstoneCommon.config().channelTime(); i++) HearthstoneCasting.tick(player);
    }

    private static HearthstoneDestinationBridge probeBridge(ProbeResolution resolution)
    {
        return new HearthstoneDestinationBridge()
        {
            public Capture capture(ServerLevel level, BlockPos pos, HearthstoneLinkType type) { return DEFAULT.capture(level, pos, type); }
            public HearthstoneDestinationResolution prepare(ServerPlayer player, HearthstoneHomeLink link) { return resolution; }
        };
    }

    private static final class ProbeResolution implements HearthstoneDestinationResolution
    {
        private final HearthstoneDestination destination;
        private int closes;

        private ProbeResolution(HearthstoneDestination destination) { this.destination = destination; }
        public Result poll() { return destination == null ? Result.pending() : Result.ready(destination); }
        public void close() { closes++; }
    }
}

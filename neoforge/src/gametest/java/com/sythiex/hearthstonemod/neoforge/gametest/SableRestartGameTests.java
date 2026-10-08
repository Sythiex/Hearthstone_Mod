package com.sythiex.hearthstonemod.neoforge.gametest;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationResolution;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationResolution.Status;
import com.sythiex.hearthstonemod.content.HearthstoneHomeLink;
import com.sythiex.hearthstonemod.content.ItemHearthstone;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Two-process test: seed shuts down normally; verify resolves the same saved reference after restart.
 */
@PrefixGameTestTemplate(false)
public final class SableRestartGameTests
{
    @GameTest(templateNamespace = "hearthstonemod_tests", template = "empty")
    public void persistentDestinationAcrossServerRestart(GameTestHelper helper) throws Exception
    {
        Path referenceFile = Path.of("hearthstone-restart.json");
        String stage = System.getProperty("hearthstonemod.restartTest");
        if (stage.equals("seed"))
        {
            var fixture = SableDestinationGameTests.fixture(helper);
            fixture.subLevel().logicalPose().position().set(16000, 100, 16000);
            fixture.subLevel().forceUpdateGlobalBounds();
            var holdingMap = fixture.container().getHoldingChunkMap();
            holdingMap.moveToUnloaded(fixture.subLevel(), new ChunkPos(1000, 1000));
            holdingMap.saveAll();
            Files.writeString(referenceFile, HearthstoneHomeLink.CODEC.encodeStart(JsonOps.INSTANCE, fixture.link()).getOrThrow().toString());
            // The test server's normal shutdown saves tracking-point SavedData and occupancy.
            helper.succeed();
            return;
        }
        helper.assertTrue(stage.equals("verify"), "restartTest must be seed or verify");
        HearthstoneHomeLink link = HearthstoneHomeLink.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(referenceFile))).getOrThrow();
        var level = helper.getLevel();
        var container = SubLevelContainer.getContainer(level);
        var point = SubLevelTrackingPointSavedData.getOrLoad(level).getTrackingPoint(link.externalTarget().orElseThrow().trackingPointId());
        helper.assertTrue(point != null && point.subLevelID() != null && point.lastSavedSubLevelPointer() != null, "Tracking point must survive process restart");
        helper.assertTrue(container.getSubLevel(point.subLevelID()) == null, "Saved target must initially be unloaded in the restarted server");
        var player = HearthstoneDestinationGameTests.player(level);
        var stack = HearthstoneDestinationGameTests.stone(player, link);
        try (HearthstoneDestinationResolution resolution = ItemHearthstone.prepareCast(player, stack))
        {
            var result = resolution.poll();
            helper.assertTrue(result.status() == Status.READY, "Persisted target must load after restart");
            helper.assertTrue(ItemHearthstone.finishCast(player, stack, result.destination()), "Teleport to restarted destination must succeed");
            helper.assertTrue(player.position().distanceToSqr(new Vec3(16000, 100, 16000)) < 100, "Arrival must use the saved world transform");
            helper.assertTrue(HearthstoneDestinationGameTests.data(stack).homeLink().orElseThrow().equals(link), "Restarted bed link must remain intact");
        } finally
        {
            var loaded = container.getSubLevel(point.subLevelID());
            if (loaded != null) container.removeSubLevel(loaded, SubLevelRemovalReason.REMOVED);
        }
        helper.succeed();
    }
}

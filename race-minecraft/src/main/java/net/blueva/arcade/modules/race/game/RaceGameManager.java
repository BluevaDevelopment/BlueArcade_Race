package net.blueva.arcade.modules.race.game;

import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.ModuleAPI;
import net.blueva.arcade.api.visuals.VisualEffectsAPI;
import net.blueva.arcade.modules.race.state.RaceStateRegistry;
import net.blueva.arcade.modules.race.support.RaceLoadoutService;
import net.blueva.arcade.modules.race.support.RaceMessagingService;
import net.blueva.arcade.modules.race.support.RaceProgressService;
import net.blueva.arcade.modules.race.support.RaceStatsService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaceGameManager {

    private final ModuleInfo moduleInfo;
    private final ModuleConfigAPI moduleConfig;
    private final CoreConfigAPI coreConfig;
    private final RaceStatsService statsService;
    private final RaceLoadoutService loadoutService;
    private final RaceMessagingService messagingService;
    private final RaceProgressService progressService;
    private final RaceStateRegistry stateRegistry;

    public RaceGameManager(ModuleInfo moduleInfo,
                           ModuleConfigAPI moduleConfig,
                           CoreConfigAPI coreConfig,
                           RaceStatsService statsService) {
        this.moduleInfo = moduleInfo;
        this.moduleConfig = moduleConfig;
        this.coreConfig = coreConfig;
        this.statsService = statsService;
        this.stateRegistry = new RaceStateRegistry();
        this.loadoutService = new RaceLoadoutService(moduleConfig);
        this.progressService = new RaceProgressService();
        this.messagingService = new RaceMessagingService(moduleInfo, moduleConfig, coreConfig, progressService);
    }

    public void handleStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();

        context.getSchedulerAPI().cancelArenaTasks(arenaId);

        stateRegistry.registerArena(context);
        messagingService.sendDescription(context);
    }

    public void handleCountdownTick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    int secondsLeft) {
        messagingService.sendCountdownTick(context, secondsLeft);
    }

    public void handleCountdownFinish(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        messagingService.sendCountdownFinished(context);
    }

    public void handleGameStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        startGameTimer(context);

        for (Player player : context.getPlayers()) {
            loadoutService.giveStartingItems(player);
            loadoutService.applyStartingEffects(player);
            context.getScoreboardAPI().showModuleScoreboard(player);
        }
    }

    private void startGameTimer(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();

        Integer gameTime = context.getDataAccess().getGameData("basic.time", Integer.class);
        if (gameTime == null || gameTime == 0) {
            gameTime = 60;
        }

        final int[] timeLeft = {gameTime};
        final int[] tickCount = {0};

        String taskId = "arena_" + arenaId + "_race_timer";

        context.getSchedulerAPI().runTimer(taskId, () -> {
            if (stateRegistry.isEnded(arenaId)) {
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }

            tickCount[0]++;

            if (tickCount[0] % 2 == 0) {
                timeLeft[0]--;
            }

            List<Player> alivePlayers = context.getAlivePlayers();
            List<Player> allPlayers = context.getPlayers();
            List<Player> spectators = context.getSpectators();

            if (allPlayers.size() < 2 || alivePlayers.isEmpty() || spectators.size() >= 3 || timeLeft[0] <= 0) {
                endGameOnce(context);
                return;
            }

            for (Player player : allPlayers) {
                if (!player.isOnline()) {
                    continue;
                }

                messagingService.sendActionBar(context, player, timeLeft[0]);

                Map<String, String> customPlaceholders = getCustomPlaceholders(player);
                customPlaceholders.put("time", String.valueOf(timeLeft[0]));
                customPlaceholders.put("round", String.valueOf(context.getCurrentRound()));
                customPlaceholders.put("round_max", String.valueOf(context.getMaxRounds()));

                List<Player> topPlayers = progressService.getTopPlayersByDistance(context);
                customPlaceholders.put("distance_1", topPlayers.size() >= 1 ? progressService.formatDistance(context, topPlayers.get(0)) : "-");
                customPlaceholders.put("distance_2", topPlayers.size() >= 2 ? progressService.formatDistance(context, topPlayers.get(1)) : "-");
                customPlaceholders.put("distance_3", topPlayers.size() >= 3 ? progressService.formatDistance(context, topPlayers.get(2)) : "-");
                customPlaceholders.put("distance_4", topPlayers.size() >= 4 ? progressService.formatDistance(context, topPlayers.get(3)) : "-");
                customPlaceholders.put("distance_5", topPlayers.size() >= 5 ? progressService.formatDistance(context, topPlayers.get(4)) : "-");

                context.getScoreboardAPI().update(player, customPlaceholders);
            }
        }, 0L, 10L);
    }

    public void handlePlayerMove(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                 Player player,
                                 Location from,
                                 Location to) {
        if (to == null || (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ())) {
            return;
        }

        switch (context.getPhase()) {
            case COUNTDOWN -> player.teleport(from);
            case PLAYING -> processActiveMovement(context, player, to);
            default -> {
                if (!context.isInsideBounds(to)) {
                    context.respawnPlayer(player);
                    messagingService.playRespawnSound(context, player);
                    handlePlayerDeath(context, player, false);
                    handlePlayerRespawn(player);
                }
            }
        }
    }

    private void processActiveMovement(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                       Player player,
                                       Location to) {
        // Skip death handling for spectators
        if (context.getSpectators().contains(player)) {
            return;
        }

        if (!context.isInsideBounds(to)) {
            playDeathEffect(player);
            context.respawnPlayer(player);
            messagingService.playRespawnSound(context, player);
            handlePlayerDeath(context, player, false);
            handlePlayerRespawn(player);
            return;
        }

        org.bukkit.Location blockBelow = to.clone().subtract(0, 1, 0);
        org.bukkit.Material blockBelowType = blockBelow.getBlock().getType();

        org.bukkit.Material deathBlock = messagingService.getDeathBlock(context);
        if (blockBelowType == deathBlock) {
            playDeathEffect(player);
            context.respawnPlayer(player);
            messagingService.playRespawnSound(context, player);
            handlePlayerDeath(context, player, true);
            handlePlayerRespawn(player);
            return;
        }

        if (messagingService.isInsideFinishLine(context, to)) {
            context.finishPlayer(player);

            int position = context.getSpectators().indexOf(player) + 1;

            handlePlayerFinish(player);
            messagingService.broadcastFinish(context, player, position);
            messagingService.sendFinishTitles(context, player, position);
        }
    }

    public void handlePlayerFinish(Player player) {
        Integer arenaId = stateRegistry.getArenaId(player);
        if (arenaId == null) {
            return;
        }

        statsService.recordFinishLineCross(player);

        if (stateRegistry.markWinner(arenaId, player.getUniqueId())) {
            statsService.recordWin(player);
        }
    }

    public void handlePlayerRespawn(Player player) {
        loadoutService.applyRespawnEffects(player);
    }

    public void handlePlayerDeath(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                  Player player,
                                  boolean deathBlock) {
        messagingService.broadcastDeath(context, player, deathBlock);
    }

    private void endGameOnce(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();

        if (stateRegistry.markEnded(arenaId)) {
            context.getSchedulerAPI().cancelArenaTasks(arenaId);
            context.endGame();
        }
    }

    private void playDeathEffect(Player player) {
        VisualEffectsAPI visualEffectsAPI = ModuleAPI.getVisualEffectsAPI();
        if (visualEffectsAPI == null || player == null) {
            return;
        }
        visualEffectsAPI.playDeathEffect(player, player.getLocation());
    }

    public void handleEnd(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();

        context.getSchedulerAPI().cancelArenaTasks(arenaId);
        statsService.recordGamePlayed(context.getPlayers());
        stateRegistry.clearArena(arenaId);
    }

    public void handleDisable() {
        stateRegistry.cancelAllSchedulers(moduleInfo.getId());
        stateRegistry.clearAll();
    }

    public GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getGameContext(Player player) {
        Integer arenaId = stateRegistry.getArenaId(player);
        if (arenaId == null) {
            return null;
        }
        return stateRegistry.getContext(arenaId);
    }

    public Map<String, String> getCustomPlaceholders(Player player) {
        Map<String, String> placeholders = new HashMap<>();

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getGameContext(player);
        if (context != null) {
            int position = progressService.calculateLivePosition(context, player);
            placeholders.put("race_position", String.valueOf(position));

            List<Player> topPlayers = progressService.getTopPlayersByDistance(context);
            placeholders.put("place_1", topPlayers.size() >= 1 ? topPlayers.get(0).getName() : "-");
            placeholders.put("place_2", topPlayers.size() >= 2 ? topPlayers.get(1).getName() : "-");
            placeholders.put("place_3", topPlayers.size() >= 3 ? topPlayers.get(2).getName() : "-");
            placeholders.put("place_4", topPlayers.size() >= 4 ? topPlayers.get(3).getName() : "-");
            placeholders.put("place_5", topPlayers.size() >= 5 ? topPlayers.get(4).getName() : "-");
        }

        return placeholders;
    }
}

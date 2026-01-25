package net.blueva.arcade.modules.race.support;

import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GamePhase;
import net.blueva.arcade.api.module.ModuleInfo;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class RaceMessagingService {

    private final ModuleInfo moduleInfo;
    private final ModuleConfigAPI moduleConfig;
    private final CoreConfigAPI coreConfig;
    private final RaceProgressService progressService;

    public RaceMessagingService(ModuleInfo moduleInfo,
                                ModuleConfigAPI moduleConfig,
                                CoreConfigAPI coreConfig,
                                RaceProgressService progressService) {
        this.moduleInfo = moduleInfo;
        this.moduleConfig = moduleConfig;
        this.coreConfig = coreConfig;
        this.progressService = progressService;
    }

    public void sendDescription(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        List<String> description = moduleConfig.getStringListFrom("language.yml", "description");

        for (Player player : context.getPlayers()) {
            for (String line : description) {
                context.getMessagesAPI().sendRaw(player, line);
            }
        }
    }

    public void sendCountdownTick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                  int secondsLeft) {
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) continue;

            context.getSoundsAPI().play(player, coreConfig.getSound("sounds.starting_game.countdown"));

            String title = coreConfig.getLanguage("titles.starting_game.title")
                    .replace("{game_display_name}", moduleInfo.getName())
                    .replace("{time}", String.valueOf(secondsLeft));

            String subtitle = coreConfig.getLanguage("titles.starting_game.subtitle")
                    .replace("{game_display_name}", moduleInfo.getName())
                    .replace("{time}", String.valueOf(secondsLeft));

            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 5);
        }
    }

    public void sendCountdownFinished(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) continue;

            String title = coreConfig.getLanguage("titles.game_started.title")
                    .replace("{game_display_name}", moduleInfo.getName());

            String subtitle = coreConfig.getLanguage("titles.game_started.subtitle")
                    .replace("{game_display_name}", moduleInfo.getName());

            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 20);

            context.getSoundsAPI().play(player, coreConfig.getSound("sounds.starting_game.start"));
        }
    }

    public void sendActionBar(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                              Player player,
                              int timeLeft) {
        String actionBarTemplate = coreConfig.getLanguage("action_bar.in_game.global");
        if (actionBarTemplate == null) {
            return;
        }

        String actionBarMessage = actionBarTemplate
                .replace("{time}", String.valueOf(timeLeft))
                .replace("{round}", String.valueOf(context.getCurrentRound()))
                .replace("{round_max}", String.valueOf(context.getMaxRounds()));
        context.getMessagesAPI().sendActionBar(player, actionBarMessage);
    }

    public void broadcastDeath(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                               Player player,
                               boolean deathBlock) {
        String path = deathBlock ? "messages.deaths.death_block" : "messages.deaths.void";
        String message = getRandomMessage(path);
        if (message == null) {
            return;
        }

        message = message.replace("{player}", player.getName());
        for (Player target : context.getPlayers()) {
            context.getMessagesAPI().sendRaw(target, message);
        }
    }

    public void broadcastFinish(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                Player player,
                                int position) {
        String message = getRandomMessage("messages.finish.crossed");
        if (message == null) {
            return;
        }

        message = message
                .replace("{player}", player.getName())
                .replace("{position}", String.valueOf(position));

        for (Player target : context.getPlayers()) {
            context.getMessagesAPI().sendRaw(target, message);
        }
    }

    public void sendFinishTitles(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                 Player player,
                                 int position) {
        String title = moduleConfig.getStringFrom("language.yml", "titles.finished.title");
        String subtitle = moduleConfig.getStringFrom("language.yml", "titles.finished.subtitle")
                .replace("{position}", String.valueOf(position));

        context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 80, 20);
        context.getSoundsAPI().play(player, coreConfig.getSound("sounds.in_game.classified"));
    }

    public void playRespawnSound(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                 Player player) {
        context.getSoundsAPI().play(player, coreConfig.getSound("sounds.in_game.respawn"));
    }

    public Material getDeathBlock(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        try {
            String deathBlockName = context.getDataAccess().getGameData("basic.death_block", String.class);
            if (deathBlockName != null) {
                return Material.valueOf(deathBlockName.toUpperCase());
            }
        } catch (Exception ignored) {
            // Fallback to default
        }
        return Material.BARRIER;
    }

    public boolean isInsideFinishLine(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                      Location location) {
        try {
            Location finishMin = context.getDataAccess().getGameLocation("game.finish_line.bounds.min");
            Location finishMax = context.getDataAccess().getGameLocation("game.finish_line.bounds.max");

            if (finishMin == null || finishMax == null) {
                return false;
            }

            return location.getX() >= Math.min(finishMin.getX(), finishMax.getX()) &&
                    location.getX() <= Math.max(finishMin.getX(), finishMax.getX()) &&
                    location.getY() >= Math.min(finishMin.getY(), finishMax.getY()) &&
                    location.getY() <= Math.max(finishMin.getY(), finishMax.getY()) &&
                    location.getZ() >= Math.min(finishMin.getZ(), finishMax.getZ()) &&
                    location.getZ() <= Math.max(finishMin.getZ(), finishMax.getZ());

        } catch (Exception e) {
            return false;
        }
    }

    public void sendSpectatorDescriptions(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        if (context.getPhase() != GamePhase.PLAYING) {
            return;
        }

        List<Player> spectators = context.getSpectators();
        for (int i = 0; i < spectators.size(); i++) {
            Player spectator = spectators.get(i);
            Map<String, String> placeholders = Map.of("race_position", String.valueOf(i + 1));
            context.getScoreboardAPI().update(spectator, placeholders);
        }
    }

    private String getRandomMessage(String path) {
        List<String> messages = moduleConfig.getStringListFrom("language.yml", path);
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        int index = ThreadLocalRandom.current().nextInt(messages.size());
        return messages.get(index);
    }
}

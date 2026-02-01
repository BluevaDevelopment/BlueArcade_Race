package net.blueva.arcade.modules.race.support;

import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.stats.StatDefinition;
import net.blueva.arcade.api.stats.StatScope;
import net.blueva.arcade.api.stats.StatsAPI;
import com.hypixel.hytale.server.core.entity.entities.Player;

import java.util.Collection;

public class RaceStatsService {

    private final StatsAPI<Player> statsAPI;
    private final ModuleInfo moduleInfo;

    public RaceStatsService(StatsAPI<Player> statsAPI, ModuleInfo moduleInfo) {
        this.statsAPI = statsAPI;
        this.moduleInfo = moduleInfo;
    }

    public void registerStats() {
        if (statsAPI == null) {
            return;
        }

        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("wins", "Wins", "Race wins", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("games_played", "Games Played", "Races played", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("finish_line_crosses", "Finish line crosses", "Times you reached the finish line", StatScope.MODULE));
    }

    public void recordFinishLineCross(Player player) {
        if (statsAPI == null) {
            return;
        }

        statsAPI.addModuleStat(player, moduleInfo.getId(), "finish_line_crosses", 1);
    }

    public void recordWin(Player player) {
        if (statsAPI == null) {
            return;
        }

        statsAPI.addModuleStat(player, moduleInfo.getId(), "wins", 1);
        statsAPI.addGlobalStat(player, "wins", 1);
    }

    public void recordGamePlayed(Collection<Player> players) {
        if (statsAPI == null) {
            return;
        }

        for (Player player : players) {
            statsAPI.addModuleStat(player, moduleInfo.getId(), "games_played", 1);
        }
    }
}

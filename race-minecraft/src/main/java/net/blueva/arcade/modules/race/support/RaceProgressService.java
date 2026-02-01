package net.blueva.arcade.modules.race.support;

import net.blueva.arcade.api.game.GameContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaceProgressService {

    public int calculateLivePosition(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                     Player player) {
        List<Player> alivePlayers = context.getAlivePlayers();
        List<Player> spectators = context.getSpectators();

        if (spectators.contains(player)) {
            return spectators.indexOf(player) + 1;
        }

        Map<Player, Double> distances = new HashMap<>();
        for (Player p : alivePlayers) {
            if (!p.isOnline()) continue;
            double distance = getDistanceToFinish(context, p);
            distances.put(p, distance);
        }

        List<Map.Entry<Player, Double>> sorted = new ArrayList<>(distances.entrySet());
        sorted.sort(Map.Entry.comparingByValue());

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getKey().equals(player)) {
                return spectators.size() + i + 1;
            }
        }

        return spectators.size() + alivePlayers.size();
    }

    public List<Player> getTopPlayersByDistance(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        List<Player> alivePlayers = context.getAlivePlayers();
        List<Player> spectators = context.getSpectators();

        List<Player> topPlayers = new ArrayList<>(spectators);

        Map<Player, Double> distances = new HashMap<>();
        for (Player p : alivePlayers) {
            if (!p.isOnline()) continue;
            double distance = getDistanceToFinish(context, p);
            distances.put(p, distance);
        }

        List<Map.Entry<Player, Double>> sorted = new ArrayList<>(distances.entrySet());
        sorted.sort(Map.Entry.comparingByValue());

        for (Map.Entry<Player, Double> entry : sorted) {
            topPlayers.add(entry.getKey());
        }

        return topPlayers;
    }

    public String formatDistance(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                 Player player) {
        if (context.getSpectators().contains(player)) {
            return "0";
        }

        double distance = getDistanceToFinish(context, player);
        if (distance == Double.MAX_VALUE) {
            return "?";
        }

        return String.format("%.0f", distance);
    }

    private double getDistanceToFinish(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                       Player player) {
        try {
            Location finishMin = context.getDataAccess().getGameLocation("game.finish_line.bounds.min");
            Location finishMax = context.getDataAccess().getGameLocation("game.finish_line.bounds.max");

            if (finishMin == null || finishMax == null) {
                return Double.MAX_VALUE;
            }

            double centerX = (finishMin.getX() + finishMax.getX()) / 2;
            double centerY = (finishMin.getY() + finishMax.getY()) / 2;
            double centerZ = (finishMin.getZ() + finishMax.getZ()) / 2;

            Location playerLoc = player.getLocation();

            double dx = playerLoc.getX() - centerX;
            double dy = playerLoc.getY() - centerY;
            double dz = playerLoc.getZ() - centerZ;

            return Math.sqrt(dx * dx + dy * dy + dz * dz);

        } catch (Exception e) {
            return Double.MAX_VALUE;
        }
    }
}

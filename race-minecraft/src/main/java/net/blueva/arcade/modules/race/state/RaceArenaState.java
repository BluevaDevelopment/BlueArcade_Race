package net.blueva.arcade.modules.race.state;

import net.blueva.arcade.api.game.GameContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class RaceArenaState {

    private final GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context;
    private boolean ended;
    private UUID winner;

    public RaceArenaState(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        this.context = context;
    }

    public GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getContext() {
        return context;
    }

    public boolean isEnded() {
        return ended;
    }

    public void markEnded() {
        this.ended = true;
    }

    public UUID getWinner() {
        return winner;
    }

    public void setWinner(UUID winner) {
        this.winner = winner;
    }
}

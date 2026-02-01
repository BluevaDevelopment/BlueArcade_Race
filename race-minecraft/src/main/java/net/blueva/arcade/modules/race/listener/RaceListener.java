package net.blueva.arcade.modules.race.listener;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.race.game.RaceGameManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class RaceListener implements Listener {

    private final RaceGameManager gameManager;

    public RaceListener(RaceGameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                gameManager.getGameContext(player);
        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        gameManager.handlePlayerMove(context, player, event.getFrom(), event.getTo());
    }
}

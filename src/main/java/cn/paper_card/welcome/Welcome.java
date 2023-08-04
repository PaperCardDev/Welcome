package cn.paper_card.welcome;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class Welcome extends JavaPlugin implements Listener {

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (player.hasPlayedBefore()) {
            event.joinMessage(Component.text()
                    .append(player.displayName())
                    .append(Component.space())
                    .append(Component.text("欢迎回来吖~").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                    .build());
        } else {
            event.joinMessage(Component.text()
                    .append(player.displayName())
                    .append(Component.space())
                    .append(Component.text("欢迎萌新~").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD))
                    .build());
        }
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        event.quitMessage(Component.text()
                .append(event.getPlayer().displayName())
                .append(Component.space())
                .append(Component.text("等你回来哟~").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                .build());
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }
}

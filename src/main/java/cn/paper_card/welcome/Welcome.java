package cn.paper_card.welcome;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class Welcome extends JavaPlugin implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {

        final Player player = event.getPlayer();
        final long firstPlayed = player.getFirstPlayed();
        final long delta = System.currentTimeMillis() - firstPlayed;

        final NamedTextColor nameColor = player.isOp() ? NamedTextColor.DARK_RED : NamedTextColor.WHITE;

        final Component prefix;

        // 萌新头衔
        if (firstPlayed == 0 || delta < 7 * 24 * 60 * 60 * 1000L) {
            prefix = Component.text("[萌新]").color(NamedTextColor.LIGHT_PURPLE);
        } else prefix = Component.text("");

        player.displayName(Component.text()
                .append(prefix)
                .append(Component.text(" "))
                .append(Component.text(player.getName()).color(nameColor))
                .build());

        player.customName(player.displayName());

        // 萌新欢迎语
        if (firstPlayed == 0 || delta < 24 * 60 * 60 * 1000L) {

            event.joinMessage(Component.text()
                    .append(player.displayName())
                    .append(Component.space())
                    .append(Component.text("欢迎萌新~").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD))
                    .build());

            if (firstPlayed == 0) {
                getServer().broadcast(Component.text()
                        .append(player.displayName())
                        .append(Component.text(" 这是你第一次进入服务器吖~").color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD))
                        .build());
            }

        } else {

            event.joinMessage(Component.text()
                    .append(player.displayName())
                    .append(Component.space())
                    .append(Component.text("欢迎回来吖~").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
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

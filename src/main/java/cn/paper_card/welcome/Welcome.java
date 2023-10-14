package cn.paper_card.welcome;

import cn.paper_card.player_online_time.PlayerOnlineTimeApi;
import cn.paper_card.sponsorship.SponsorshipApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Welcome extends JavaPlugin implements Listener, WelcomeApi {

    private SponsorshipApi sponsorshipApi = null;

    private PlayerOnlineTimeApi playerOnlineTimeApi = null;

    private final @NotNull TaskScheduler taskScheduler;

    public Welcome() {
        this.taskScheduler = UniversalScheduler.getScheduler(this);
    }

    private @Nullable PlayerOnlineTimeApi getPlayerOnlineTimeApi() {
        final Plugin p = this.getServer().getPluginManager().getPlugin("PlayerOnlineTime");
        if (p instanceof final PlayerOnlineTimeApi api) {
            return api;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {

        final Player player = event.getPlayer();

        this.configDisplayName(player);

        final long firstPlayed = player.getFirstPlayed();
        final long cur = System.currentTimeMillis();
        final long delta = cur - firstPlayed;

        // 萌新欢迎语
        if (delta < 24 * 60 * 60 * 1000L) {

            event.joinMessage(Component.text()
                    .append(player.displayName())
                    .append(Component.space())
                    .append(Component.text("欢迎萌新~").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD))
                    .build());

            if (!player.hasPlayedBefore()) {
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

        // 增加进服次数
        this.onJoinAddCount(player, cur);
    }

    private void onJoinAddCount(@NotNull Player player, long cur) {
        if (this.playerOnlineTimeApi == null) return;

        this.taskScheduler.runTaskAsynchronously(() -> {
            // 查询进入次数和总计在线时长
            final PlayerOnlineTimeApi.OnlineTimeAndJoinCount onlineTimeAndJoinCount;

            try {
                onlineTimeAndJoinCount = this.playerOnlineTimeApi.queryTotalOnlineAndJoinCount(player.getUniqueId());

                this.getLogger().info("玩家%s的总进入次数：%d，在线时长：%d".formatted(player.getName(),
                        onlineTimeAndJoinCount.jointCount(), onlineTimeAndJoinCount.onlineTime()));

                final long joinNo = onlineTimeAndJoinCount.jointCount() + 1;

                if (joinNo % 30 == 0) {
                    // 提示赞助
                    if (this.sponsorshipApi != null) {
                        final TextComponent message = this.sponsorshipApi.buildPromptMessage(player, joinNo, onlineTimeAndJoinCount.onlineTime());
                        player.sendMessage(message);
                    }
                }

            } catch (Exception e) {
                this.getLogger().warning(e.toString());
                e.printStackTrace();
            }

            try {
                final boolean added = this.playerOnlineTimeApi.addJoinCountToday(player.getUniqueId(), cur);
                this.getLogger().info("%s成功，将玩家%s今天的进服次数加一".formatted(added ? "添加" : "更新", player.getName()));
            } catch (Exception e) {
                this.getLogger().warning(e.toString());
                e.printStackTrace();
            }
        });
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


        final Plugin plugin = this.getServer().getPluginManager().getPlugin("Sponsorship");
        if (plugin instanceof final SponsorshipApi api) {
            this.sponsorshipApi = api;
        } else {
            getLogger().warning("Sponsorship插件未安装！");
        }

        this.playerOnlineTimeApi = this.getPlayerOnlineTimeApi();
    }

    @Override
    public void configDisplayName(@NotNull Player player) {

        final long firstPlayed = player.getFirstPlayed();
        final long delta = System.currentTimeMillis() - firstPlayed;

        final NamedTextColor nameColor = player.isOp() ? NamedTextColor.DARK_RED : NamedTextColor.WHITE;

        final TextComponent.Builder text = Component.text();

        boolean prefixSet = false;

        // 赞助头衔
        if (this.sponsorshipApi != null) {
            final SponsorshipApi.Info3 info3;
            try {
                info3 = this.sponsorshipApi.queryTimeAndTotal(player.getUniqueId());

                assert info3 != null;
                if (info3.total() >= 10000) {
                    text.append(Component.text("[").color(TextColor.fromHexString("#33CCFF")));
                    text.append(Component.text("赞助").color(TextColor.fromHexString("#C71585")));
                    text.append(Component.text("]").color(TextColor.fromHexString("#33CCFF")));
                    text.appendSpace();
                    prefixSet = true;
                } else if (info3.total() >= 1000) {
                    text.append(Component.text("[").color(TextColor.fromHexString("#33CCFF")));
                    text.append(Component.text("赞助").color(NamedTextColor.AQUA));
                    text.append(Component.text("]").color(TextColor.fromHexString("#33CCFF")));
                    text.appendSpace();
                    prefixSet = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 萌新头衔
        if (!prefixSet && (firstPlayed <= 0 || delta < 7 * 24 * 60 * 60 * 1000L)) {
            text.append(Component.text("[萌新]").color(NamedTextColor.LIGHT_PURPLE));
            text.appendSpace();
        }

        text.append(Component.text(player.getName()).color(nameColor));

        player.displayName(text.build());
        player.customName(player.displayName());
        player.setCustomNameVisible(true);
    }
}

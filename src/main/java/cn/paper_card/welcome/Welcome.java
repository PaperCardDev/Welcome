package cn.paper_card.welcome;

import cn.paper_card.client.api.PaperClientApi;
import cn.paper_card.welcome.api.WelcomeApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.themoep.minedown.adventure.MineDown;
import io.papermc.paper.advancement.AdvancementDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

public final class Welcome extends JavaPlugin implements Listener, WelcomeApi {

    private PaperClientApi paperClientApi = null;

    private final @NotNull TaskScheduler taskScheduler;

    private final @NotNull HashMap<UUID, String> quitMsg = new HashMap<>();

    public Welcome() {
        this.taskScheduler = UniversalScheduler.getScheduler(this);
        this.prefix = Component.text()
                .append(Component.text("[").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(this.getName()).color(NamedTextColor.DARK_AQUA))
                .append(Component.text("]").color(NamedTextColor.DARK_GRAY))
                .build();

    }

    private final @NotNull TextComponent prefix;


    void sendException(@NotNull CommandSender sender, @NotNull Throwable e) {
        final TextComponent.Builder text = Component.text();

        text.append(this.prefix);
        text.appendSpace();
        text.append(Component.text("==== 异常信息 ====").color(NamedTextColor.DARK_RED));

        for (Throwable t = e; t != null; t = t.getCause()) {
            text.appendNewline();
            text.append(Component.text(t.toString()).color(NamedTextColor.RED));
        }
        sender.sendMessage(text.build());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {

        final Player player = event.getPlayer();

        event.joinMessage(null);

        final PaperClientApi api = this.paperClientApi;
        if (api != null) {

            this.taskScheduler.runTaskAsynchronously(() -> {
                // IP
                String ip = null;
                {
                    final InetSocketAddress address = player.getAddress();
                    if (address != null) {
                        final InetAddress address1 = address.getAddress();
                        if (address1 != null) {
                            ip = address1.getHostAddress();
                        }
                    }
                }

                final String url = "/mc/welcome/%s?name=%s&ip=%s&isOp=%s&notPlayedBefore=%s&firstPlayed=%s".formatted(player.getUniqueId(),
                        player.getName(), ip, player.isOp(), !player.hasPlayedBefore(), player.getFirstPlayed());

                final JsonElement dataEle;
                try {
                    dataEle = api.getWithAuth(url);
                } catch (Exception e) {
                    this.getSLF4JLogger().error("", e);
                    this.sendException(player, e);
                    return;
                }

                if (dataEle == null) return;

                final String titleMinedown;
                final String namePrefixMinedown;
                final String msgToPlayerMinedown;
                final String msgBroadcastMinedown;
                final String msgQuitMinedown;
                try {
                    final JsonObject obj = dataEle.getAsJsonObject();
                    titleMinedown = obj.get("title_minedown").getAsString();
                    namePrefixMinedown = obj.get("name_prefix_minedown").getAsString();
                    msgToPlayerMinedown = obj.get("message_to_player_minedown").getAsString();
                    msgBroadcastMinedown = obj.get("message_broadcast_minedown").getAsString();
                    msgQuitMinedown = obj.get("message_quit_minedown").getAsString();
                } catch (Exception e) {
                    this.getSLF4JLogger().error("", e);
                    this.sendException(player, e);
                    return;
                }

                this.quitMsg.put(player.getUniqueId(), msgQuitMinedown);

                this.taskScheduler.runTask(() -> {
                    // 配置名字
                    final TextComponent.Builder text = Component.text();

                    if (titleMinedown != null && !titleMinedown.isEmpty()) {
                        text.append(new MineDown(titleMinedown).toComponent());
                    }

                    String prefix = "";
                    if (namePrefixMinedown != null && !namePrefixMinedown.isEmpty()) {
                        prefix = namePrefixMinedown;
                    }
                    text.append(new MineDown(prefix + player.getName()).toComponent());

                    player.displayName(text.build());

                    // 广播
                    if (msgBroadcastMinedown != null && !msgBroadcastMinedown.isEmpty()) {
                        this.getServer().broadcast(new MineDown(msgBroadcastMinedown).toComponent());
                    }

                    // 消息
                    if (msgToPlayerMinedown != null && !msgToPlayerMinedown.isEmpty()) {
                        player.sendMessage(new MineDown(msgToPlayerMinedown).toComponent());
                    }
                });
            });


        } else {
            this.taskScheduler.runTaskAsynchronously(() -> {

                // 计算displayName
                this.configDisplayName(player);

                final long firstPlayed = player.getFirstPlayed();
                final long cur = System.currentTimeMillis();
                final long delta = cur - firstPlayed;

                final Component joinMessage;

                // 萌新欢迎语
                if (delta < 24 * 60 * 60 * 1000L) {

                    joinMessage = Component.text()
                            .append(player.displayName())
                            .append(Component.space())
                            .append(Component.text("欢迎萌新~").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD))
                            .build();


                } else {

                    joinMessage = Component.text()
                            .append(player.displayName())
                            .append(Component.space())
                            .append(Component.text("欢迎回来吖~").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                            .build();
                }

                // 关播join消息
                getServer().broadcast(joinMessage);

                // 第一次进入
                if (!player.hasPlayedBefore()) {
                    getServer().broadcast(Component.text()
                            .append(player.displayName())
                            .append(Component.text(" 这是你第一次进入服务器吖~").color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD))
                            .build());
                }
            });
        }
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        final UUID uniqueId = event.getPlayer().getUniqueId();

        final String quitMsgMinedown = this.quitMsg.remove(uniqueId);

        if (quitMsgMinedown != null && !quitMsgMinedown.isEmpty()) {
            event.quitMessage(new MineDown(quitMsgMinedown).toComponent());
        }
    }

    @Override
    public void onLoad() {
        this.getServer().getServicesManager().register(WelcomeApi.class, this, this, ServicePriority.High);
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        final PluginCommand command2 = this.getCommand("check-progress");
        assert command2 != null;
        command2.setExecutor((commandSender, command, s, strings) -> {
            if (!(commandSender instanceof final Player player)) {
                return true;
            }

            final Iterator<Advancement> advancementIterator = this.getServer().advancementIterator();
            player.sendMessage(Component.text("未完成的成就（只显示前4条）:"));

            int notDone = 0;
            while (advancementIterator.hasNext()) {
                final Advancement next = advancementIterator.next();


                final AdvancementProgress progress = player.getAdvancementProgress(next);
                if (!progress.isDone()) {
                    if (notDone < 4) {
                        final TextComponent.Builder text = Component.text();

                        text.append(next.displayName());
                        text.appendNewline();

                        final AdvancementDisplay display = next.getDisplay();
                        if (display != null) {
                            text.append(display.displayName());
                            text.hoverEvent(HoverEvent.showText(display.description()));
                            text.appendNewline();
                        }

                        player.sendMessage(text.build());
                    }
                    ++notDone;
                }
            }
            player.sendMessage(Component.text("一共%d个成就未完成".formatted(notDone)));
            return true;
        });

        final ServicesManager servicesManager = this.getServer().getServicesManager();

        this.paperClientApi = servicesManager.load(PaperClientApi.class);

        if (this.paperClientApi == null) {
            this.getSLF4JLogger().warn("Fail to link PlayerTitleApi");
        }
    }

    @Override
    public void onDisable() {
        this.quitMsg.clear();

        this.taskScheduler.cancelTasks(this);

        this.getServer().getServicesManager().unregisterAll(this);
    }

    @Override
    public void configDisplayName(Object o) {
        throw new UnsupportedOperationException("TODO: 不支持该API");
    }
}
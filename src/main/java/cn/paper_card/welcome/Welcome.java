package cn.paper_card.welcome;

import cn.paper_card.paper_card_tip.PaperCardTipApi;
import cn.paper_card.player_gender.PlayerGenderApi;
import cn.paper_card.player_online_time.PlayerOnlineTimeApi;
import cn.paper_card.player_title.api.PlayerTitleApi;
import cn.paper_card.player_title.api.PlayerTitleInfoInUse;
import cn.paper_card.player_title.api.PlayerTitleService;
import cn.paper_card.qq_bind.api.BindInfo;
import cn.paper_card.qq_bind.api.QqBindApi;
import cn.paper_card.qq_group_access.api.GroupAccess;
import cn.paper_card.qq_group_access.api.QqGroupAccessApi;
import cn.paper_card.smurf.api.SmurfApi;
import cn.paper_card.smurf.api.SmurfInfo;
import cn.paper_card.sponsorship.SponsorshipApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import de.themoep.minedown.adventure.MineDown;
import io.papermc.paper.advancement.AdvancementDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public final class Welcome extends JavaPlugin implements Listener, WelcomeApi {

    private SponsorshipApi sponsorshipApi = null;

    private PlayerOnlineTimeApi playerOnlineTimeApi = null;

    private PaperCardTipApi paperCardTipApi = null;

    private PlayerGenderApi playerGenderApi = null;

    private PlayerTitleApi playerTitleApi = null;

    private QqGroupAccessApi qqGroupAccessApi = null;

    private QqBindApi qqBindApi = null;

    private SmurfApi smurfApi = null;

    private final @NotNull TaskScheduler taskScheduler;

    public Welcome() {
        this.taskScheduler = UniversalScheduler.getScheduler(this);
        this.prefix = Component.text()
                .append(Component.text("[").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(this.getName()).color(NamedTextColor.YELLOW))
                .append(Component.text("]").color(NamedTextColor.DARK_GRAY))
                .build();
    }

    private final @NotNull TextComponent prefix;

    private @Nullable PlayerOnlineTimeApi getPlayerOnlineTimeApi() {
        final Plugin p = this.getServer().getPluginManager().getPlugin("PlayerOnlineTime");
        if (p instanceof final PlayerOnlineTimeApi api) {
            return api;
        }
        return null;
    }

    private @Nullable PaperCardTipApi getPaperCardTipApi() {
        final Plugin p = this.getServer().getPluginManager().getPlugin("PaperCardTip");
        if (p instanceof final PaperCardTipApi api) {
            return api;
        }
        return null;
    }

    private @Nullable SponsorshipApi getSponsorshipApi() {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("Sponsorship");
        if (plugin instanceof final SponsorshipApi api) {
            return api;
        }
        return null;
    }

    private @Nullable PlayerGenderApi getPlayerGenderApi() {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("PlayerGender");
        if (plugin instanceof final PlayerGenderApi api) {
            return api;
        }
        return null;
    }

    void sendError(@NotNull CommandSender sender, @NotNull String error) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(Component.text(error).color(NamedTextColor.RED))
        );
    }

    void sendException(@NotNull CommandSender sender, @NotNull Throwable e) {
        final TextComponent.Builder text = Component.text();

        text.append(this.prefix);
        text.append(Component.text("==== 异常信息 ====").color(NamedTextColor.DARK_RED));

        for (Throwable t = e; t != null; t = t.getCause()) {
            text.appendNewline();
            text.append(Component.text(t.toString()).color(NamedTextColor.RED));
        }
        sender.sendMessage(text.build());
    }

    private @Nullable GroupAccess createMainGroupAccess() {
        final QqGroupAccessApi api = this.qqGroupAccessApi;
        if (api == null) return null;

        try {
            return api.createMainGroupAccess();
        } catch (Exception e) {
            getSLF4JLogger().error("createMainGroupAccess", e);
        }
        return null;
    }

    private void sendFirstJoinToQqGroup(@NotNull Player player) {


        final String msg = """
                [首次进入]
                欢迎玩家 %s 加入PaperCard服务器(σ≧∀≦)σ
                这是本周目第 %d 位玩家~""".formatted(player.getName(),
                this.getServer().getOfflinePlayers().length);


        final GroupAccess access = this.createMainGroupAccess();

        if (access == null) return;

        try {
            access.sendNormalMessage(msg);
        } catch (Exception e) {
            getSLF4JLogger().error("sendNormalMessage", e);
        }
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

                // 发送信息到QQ群里
                this.taskScheduler.runTaskAsynchronously(() -> this.sendFirstJoinToQqGroup(player));
            }

        } else {

            event.joinMessage(Component.text()
                    .append(player.displayName())
                    .append(Component.space())
                    .append(Component.text("欢迎回来吖~").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                    .build());

        }

        // 增加进服次数
        this.onJoinAddCount(player);
    }

    private @NotNull TextComponent buildTip(@NotNull PaperCardTipApi.Tip tip) {
        return Component.text()
                .append(Component.text("[你知道吗？#%d]".formatted(tip.id())).color(NamedTextColor.GREEN))
                .appendSpace()
                .append(Component.text(tip.content()).color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .append(Component.text("  --"))
                .append(Component.text(tip.category()).color(NamedTextColor.GOLD))
                .build();
    }

    private boolean showQqBindTip(@NotNull Player player) throws Exception {
        final QqBindApi qqBindApi1 = this.qqBindApi;
        final SmurfApi smurfApi1 = this.smurfApi;
        final QqGroupAccessApi qqGroupAccessApi1 = this.qqGroupAccessApi;

        if (qqBindApi1 == null) return false;
        if (qqGroupAccessApi1 == null) return false;

        final GroupAccess access = this.createMainGroupAccess();
        if (access == null) return false;

        final BindInfo bindInfo = qqBindApi1.getBindService().queryByUuid(player.getUniqueId());

        // 已经绑定了
        if (bindInfo != null) return false;

        // 未绑定

        // 查询是不是小号，不要求小号进行绑定
        if (smurfApi1 != null) {
            final SmurfInfo smurfInfo = smurfApi1.getSmurfService().queryBySmurfUuid(player.getUniqueId());
            // 不要求小号进行QQ绑定
            if (smurfInfo != null) return false;
        }

        // 非小号、且未绑定

        final TextComponent.Builder text = Component.text();
        text.append(this.prefix);
        text.appendSpace();
        text.append(Component.text("你还没有绑定QQ，请先绑定一下QQ哦").color(NamedTextColor.GREEN));
        text.appendSpace();
        text.append(Component.text("[点击绑定QQ]").color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand("/qq-bind code"))
                .hoverEvent(HoverEvent.showText(Component.text("点击生成绑定验证码"))));

        player.sendMessage(text.build());

        return true;
    }

    private void onJoinAddCount(@NotNull Player player) {
        if (this.playerOnlineTimeApi == null) return;

        this.taskScheduler.runTaskAsynchronously(() -> {
            // 没有QQ绑定的话，提示一下绑定QQ
            try {
                if (showQqBindTip(player)) return;
            } catch (Exception e) {
                getSLF4JLogger().error("", e);
                sendException(player, e);
                return;
            }

            // 查询进入次数和总计在线时长
            final PlayerOnlineTimeApi.OnlineTimeAndJoinCount onlineTimeAndJoinCount;

            try {
                onlineTimeAndJoinCount = this.playerOnlineTimeApi.queryTotalOnlineAndJoinCount(player.getUniqueId());
            } catch (Exception e) {
                getSLF4JLogger().error("", e);
                sendException(player, e);
                return;
            }

            getSLF4JLogger().info("玩家%s的总进入次数：%d，在线时长：%d".formatted(player.getName(),
                    onlineTimeAndJoinCount.jointCount(), onlineTimeAndJoinCount.onlineTime()));

            final long joinNo = onlineTimeAndJoinCount.jointCount() + 1;

            if (joinNo % 20 == 0) {
                // 提示赞助
                if (this.sponsorshipApi != null) {
                    final TextComponent message = this.sponsorshipApi.buildPromptMessage(player, joinNo, onlineTimeAndJoinCount.onlineTime());
                    player.sendMessage(message);
                }
            } else {
                // 提示TIP
                if (this.paperCardTipApi != null) {
                    try {
                        this.showTipForPlayer(player, this.paperCardTipApi);
                    } catch (Exception e) {
                        getSLF4JLogger().error("", e);
                        sendException(player, e);
                    }
                }
            }
        });
    }

    private void showTipForPlayer(@NotNull Player player, @NotNull PaperCardTipApi api) throws Exception {
        final int count = api.queryCount();

        if (count <= 0) {
            player.sendMessage(this.buildTip(new PaperCardTipApi.Tip(0, "管理员还没有添加一条Tip", "警告")));
        } else {
            int index = api.queryPlayerIndex(player.getUniqueId());
            index %= count;
            final List<PaperCardTipApi.Tip> tips = api.queryByPage(1, index);
            final int size = tips.size();
            if (size == 1) {
                final PaperCardTipApi.Tip tip = tips.get(0);
                player.sendMessage(this.buildTip(tip));
            }
            index += 1;
            index %= count;
            api.setPlayerIndex(player.getUniqueId(), index);
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

        this.sponsorshipApi = this.getSponsorshipApi();
        this.playerOnlineTimeApi = this.getPlayerOnlineTimeApi();
        this.paperCardTipApi = this.getPaperCardTipApi();
        this.playerGenderApi = this.getPlayerGenderApi();

        this.qqBindApi = this.getServer().getServicesManager().load(QqBindApi.class);
        this.smurfApi = this.getServer().getServicesManager().load(SmurfApi.class);

        this.playerTitleApi = this.getServer().getServicesManager().load(PlayerTitleApi.class);

        this.qqGroupAccessApi = this.getServer().getServicesManager().load(QqGroupAccessApi.class);
        if (this.qqGroupAccessApi == null) {
            getSLF4JLogger().warn("未连接到QqGroupAccessApi");
        } else {
            getSLF4JLogger().info("已连接到QqGroupAccessApi");
        }

        final PluginCommand command = this.getCommand("zanzhu");
        assert command != null;
        command.setExecutor((commandSender, command1, s, strings) -> {
            if (this.playerOnlineTimeApi == null) return true;
            if (this.sponsorshipApi == null) return true;

            if (!(commandSender instanceof final Player player)) {
                this.sendError(commandSender, "该命令只能由玩家来执行");
                return true;
            }

            this.taskScheduler.runTaskAsynchronously(() -> {
                final PlayerOnlineTimeApi.OnlineTimeAndJoinCount info;

                try {
                    info = this.playerOnlineTimeApi.queryTotalOnlineAndJoinCount(player.getUniqueId());
                } catch (Exception e) {
                    getSLF4JLogger().error("", e);
                    this.sendError(commandSender, e.toString());
                    return;
                }

                final TextComponent message = this.sponsorshipApi.buildPromptMessage(player,
                        info.jointCount() + 1, info.onlineTime());

                player.sendMessage(message);

            });

            return true;
        });
    }

    @Override
    public void configDisplayName(@NotNull Player player) {

        final long firstPlayed = player.getFirstPlayed();
        final long delta = System.currentTimeMillis() - firstPlayed;

        final NamedTextColor nameColor = player.isOp() ? NamedTextColor.DARK_RED : NamedTextColor.WHITE;

        final TextComponent.Builder text = Component.text();

        boolean prefixSet = false;

        // 自定义称号
        if (this.playerTitleApi != null) {
            final PlayerTitleService service = this.playerTitleApi.getPlayerTitleService();
            final PlayerTitleInfoInUse info;
            try {
                info = service.queryOnePlayerTitleInUse(player.getUniqueId());
                if (info != null) {
                    final Component titleContent = (Component) this.playerTitleApi.parseTitleContent(info.content());
                    text.append(titleContent);
                    text.appendSpace();
                    prefixSet = true;
                }

            } catch (Exception e) {
                getSLF4JLogger().error("", e);
            }
        }


        // 妹纸头衔
        // &#fff0f5-#ff69b4&[妹纸]
        if (!prefixSet && this.playerGenderApi != null) {
            final PlayerGenderApi.GenderType genderType;

            try {
                genderType = this.playerGenderApi.queryGender(player.getUniqueId());
                if (genderType == PlayerGenderApi.GenderType.FEMALE) {
                    text.append(new MineDown("&#fff0f5-#ff69b4&[妹纸]").toComponent());
                    text.appendSpace();
                    prefixSet = true;
                }

            } catch (Exception e) {
                getSLF4JLogger().error("", e);
            }
        }

        // 全成就头衔
        if (!prefixSet) {
            boolean allDone = true;
            final Iterator<Advancement> advancementIterator = this.getServer().advancementIterator();
            while (advancementIterator.hasNext()) {
                final Advancement next = advancementIterator.next();
                final AdvancementProgress advancementProgress = player.getAdvancementProgress(next);

                final AdvancementDisplay display = next.getDisplay();
                if (display == null) continue;

                if (!advancementProgress.isDone()) {
                    final TextComponent.Builder builder = Component.text();
                    builder.append(Component.text("玩家%s未完成成就：".formatted(player.getName())));
                    builder.append(next.displayName());

                    this.getServer().getConsoleSender().sendMessage(builder.build());

                    allDone = false;
                    break;
                }
            }

            if (allDone) {
                text.append(Component.text("[").color(TextColor.fromHexString("#EE6363")));
                text.append(Component.text("登峰造极").color(NamedTextColor.DARK_PURPLE));
                text.append(Component.text("]").color(TextColor.fromHexString("#EE6363")));
                text.appendSpace();
                prefixSet = true;
            }
        }


        // 赞助头衔
        if (!prefixSet && this.sponsorshipApi != null) {
            final SponsorshipApi.Info3 info3;
            try {
                info3 = this.sponsorshipApi.queryTimeAndTotal(player.getUniqueId());
                if (info3 != null) {
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
                }
            } catch (Exception e) {
                getSLF4JLogger().error("", e);
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
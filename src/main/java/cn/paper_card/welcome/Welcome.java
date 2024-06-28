package cn.paper_card.welcome;

import cn.paper_card.paper_card_tip.PaperCardTipApi;
import cn.paper_card.paper_online_time.api.OnlineTimeAndJoinCount;
import cn.paper_card.paper_online_time.api.PlayerOnlineTimeApi;
import cn.paper_card.player_gender.PlayerGenderApi;
import cn.paper_card.player_title.api.PlayerTitleApi;
import cn.paper_card.player_title.api.PlayerTitleInfoInUse;
import cn.paper_card.player_title.api.PlayerTitleService;
import cn.paper_card.qq_group_member_info.api.QqGroupMemberInfoApi;
import cn.paper_card.smurf.api.SmurfApi;
import cn.paper_card.sponsorship.api.SponsorshipApi2;
import cn.paper_card.sponsorship.api.SponsorshipPlayerInfo;
import cn.paper_card.welcome.api.WelcomeApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import de.themoep.minedown.adventure.MineDown;
import io.papermc.paper.advancement.AdvancementDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public final class Welcome extends JavaPlugin implements Listener, WelcomeApi {

    private SponsorshipApi2 sponsorshipApi = null;

    private PlayerOnlineTimeApi playerOnlineTimeApi = null;

    private PaperCardTipApi paperCardTipApi = null;
    private PlayerTitleApi playerTitleApi = null;

//    private QqGroupAccessApi qqGroupAccessApi = null;

    private QqGroupMemberInfoApi qqGroupMemberInfoApi = null;

//    private QqBindApi qqBindApi = null;

    private SmurfApi smurfApi = null;

    private final @NotNull TaskScheduler taskScheduler;

    private final @NotNull AdvancementTitle advancementTitle;

    private final @NotNull GirlTitle girlTitle;

    public Welcome() {
        this.taskScheduler = UniversalScheduler.getScheduler(this);
        this.prefix = Component.text()
                .append(Component.text("[").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(this.getName()).color(NamedTextColor.DARK_AQUA))
                .append(Component.text("]").color(NamedTextColor.DARK_GRAY))
                .build();

        this.advancementTitle = new AdvancementTitle(this);
        this.girlTitle = new GirlTitle(this);
    }

    private final @NotNull TextComponent prefix;

    private @Nullable PaperCardTipApi getPaperCardTipApi() {
        return this.getServer().getServicesManager().load(PaperCardTipApi.class);
    }

    @Nullable PlayerGenderApi getPlayerGenderApi() {
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

//    private @Nullable GroupAccess createMainGroupAccess() {
//        final QqGroupAccessApi api = this.qqGroupAccessApi;
//        if (api == null) return null;
//
//        try {
//            return api.createMainGroupAccess();
//        } catch (Exception e) {
//            getSLF4JLogger().warn(e.toString());
//        }
//        return null;
//    }

    private void sendFirstJoinToQqGroup(@NotNull Player player) {

//
//        final String msg = """
//                [首次进入]
//                欢迎玩家 %s 加入PaperCard服务器(σ≧∀≦)σ
//                这是本周目第 %d 位玩家~""".formatted(player.getName(),
//                this.getServer().getOfflinePlayers().length);
//
//
//        final GroupAccess access = this.createMainGroupAccess();
//
//        if (access == null) return;
//
//        try {
//            access.sendNormalMessage(msg);
//        } catch (Exception e) {
//            getSLF4JLogger().error("sendNormalMessage", e);
//        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {

        final Player player = event.getPlayer();

        event.joinMessage(null);

        this.taskScheduler.runTaskAsynchronously(() -> {

            try {
                this.advancementTitle.check(player);
            } catch (Exception e) {
                getSLF4JLogger().error("", e);
            }

            try {
                this.girlTitle.check(player);
            } catch (Exception e) {
                getSLF4JLogger().error("", e);
            }

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

                // 发送信息到QQ群里
                this.taskScheduler.runTaskAsynchronously(() -> this.sendFirstJoinToQqGroup(player));
            }

            // 增加进服次数
            this.onJoinAddCount(player);
        });
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

//    void tipJoinGroup(@NotNull Player player, @NotNull GroupAccess access) {
//        final TextComponent.Builder text = Component.text();
//        text.append(this.prefix);
//        text.appendSpace();
//        text.append(Component.text("您还未加入任何一个QQ交流群，"));
//
//        text.appendNewline();
//        text.append(Component.text("请加入任意一个QQ群（点击可复制群号）："));
//
//        final String str = "%d".formatted(access.getId());
//        text.appendNewline();
//        text.append(Component.text("[").color(NamedTextColor.GRAY));
//        text.append(Component.text(str)
//                .color(NamedTextColor.RED).decorate(TextDecoration.UNDERLINED).decorate(TextDecoration.BOLD)
//                .clickEvent(ClickEvent.copyToClipboard(str))
//                .hoverEvent(HoverEvent.showText(Component.text("点击复制QQ群号")))
//        );
//        text.append(Component.text("]").color(NamedTextColor.GRAY));
//
//        text.appendNewline();
//        text.append(Component.text("加入任意一个QQ群后此消息不再显示").color(NamedTextColor.GRAY));
//
//        text.appendNewline();
//        text.append(Component.text("若您已经加群，请在群内发送任意消息以更新信息").color(NamedTextColor.GRAY));
//
//        player.sendMessage(text.build().color(NamedTextColor.YELLOW));
//    }

    // 提示玩家加入交流群，自动修改群昵称为游戏名
//    void checkGroupNameCard(@NotNull BindInfo bindInfo, @NotNull Player player, @NotNull GroupAccess access) throws Exception {
//        final QqGroupMemberInfoApi api = this.qqGroupMemberInfoApi;
//        if (api == null) return;
//
//        final QqGroupMemberInfoService service = api.getQqGroupMemberInfoService();
//
//        final QqGroupMemberInfo qqInfo = service.queryByQq(bindInfo.qq());
//
//        // 未记录
//        if (qqInfo == null) return;
//
//        // 不在群，提示加群
//        if (!qqInfo.inGroup()) {
//            this.tipJoinGroup(player, access);
//            return;
//        }
//
//        // 符合规范
//        if (qqInfo.nameCard().startsWith(player.getName())) {
//            getSLF4JLogger().info("玩家 %s 的群昵称 %s 检查通过".formatted(player.getName(), qqInfo.nameCard()));
//            return;
//        }
//
//        // 修改群昵称
//        access.setGroupMemberRemark(qqInfo.qq(), player.getName());
//
//        // 记录
//        service.updateNameCard(qqInfo.qq(), player.getName());
//
//        // 发送消息
//        access.sendAtMessage(qqInfo.qq(), "\n已自动修改你的群昵称为游戏名：" + player.getName());
//    }

//    private void showQqBindTip(@NotNull Player player) throws Exception {
//        final QqBindApi qqBindApi1 = this.qqBindApi;
//        final SmurfApi smurfApi1 = this.smurfApi;
//        final QqGroupAccessApi qqGroupAccessApi1 = this.qqGroupAccessApi;
//
//        if (qqBindApi1 == null) return;
//        if (qqGroupAccessApi1 == null) return;
//
//        final GroupAccess access = this.createMainGroupAccess();
//        if (access == null) return;
//
//        final BindInfo bindInfo = qqBindApi1.getBindService().queryByUuid(player.getUniqueId());
//
//        // 已经绑定了
//        if (bindInfo != null) {
//            // 检查群昵称，是否在群
//            this.checkGroupNameCard(bindInfo, player, access);
//            return;
//        }
//
//        // 未绑定
//
//        // 查询是不是小号，不要求小号进行绑定
//        if (smurfApi1 != null) {
//            final SmurfInfo smurfInfo = smurfApi1.getSmurfService().queryBySmurfUuid(player.getUniqueId());
//            // 不要求小号进行QQ绑定
//            if (smurfInfo != null) return;
//        }
//
//        // 非小号、且未绑定
//
//        final TextComponent.Builder text = Component.text();
//        text.append(this.prefix);
//        text.appendSpace();
//        text.append(Component.text("您还没有绑定QQ，请先绑定一下QQ哦"));
//        text.appendSpace();
//        text.append(Component.text("[点击绑定QQ]").color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
//                .clickEvent(ClickEvent.runCommand("/qq-bind code"))
//                .hoverEvent(HoverEvent.showText(Component.text("点击生成绑定验证码"))));
//
//        text.appendNewline();
//        text.append(Component.text("完成QQ绑定后不再显示此消息").color(NamedTextColor.GRAY));
//
//        player.sendMessage(text.build().color(NamedTextColor.YELLOW));
//
//    }

    private void onJoinAddCount(@NotNull Player player) {

        this.taskScheduler.runTaskAsynchronously(() -> {

            final PlayerOnlineTimeApi playerOnlineTimeApi1 = this.playerOnlineTimeApi;

            if (playerOnlineTimeApi1 == null) return;

            // 查询进入次数和总计在线时长
            final OnlineTimeAndJoinCount onlineTimeAndJoinCount;

            try {
                onlineTimeAndJoinCount = playerOnlineTimeApi1.queryTotal(player.getUniqueId());
            } catch (Exception e) {
                this.getSLF4JLogger().error("", e);
                this.sendException(player, e);
                return;
            }

            this.getSLF4JLogger().info("玩家%s的总进入次数：%d，在线时长：%d".formatted(player.getName(),
                    onlineTimeAndJoinCount.jointCount(), onlineTimeAndJoinCount.onlineTime()));

            final long joinNo = onlineTimeAndJoinCount.jointCount() + 1;

            if (joinNo % 20 == 0) {
                final SponsorshipApi2 sponsorshipApi1 = this.sponsorshipApi;
                // 提示赞助
                if (sponsorshipApi1 != null) {
                    final TextComponent.Builder text = Component.text();
                    sponsorshipApi1.appendPromptMessage(text, player.getUniqueId(), joinNo, onlineTimeAndJoinCount.onlineTime());
                    player.sendMessage(text.build());
                }
            } else {
                // 提示TIP
                final PaperCardTipApi paperCardTipApi1 = this.paperCardTipApi;
                if (paperCardTipApi1 != null) {
                    try {
                        this.showTipForPlayer(player, paperCardTipApi1);
                    } catch (Exception e) {
                        this.getSLF4JLogger().error("", e);
                        this.sendException(player, e);
                    }
                }
            }

            // 添加进服次数
            final boolean added;
            try {
                added = playerOnlineTimeApi1.addJoinCountToday(player.getUniqueId(), System.currentTimeMillis());
            } catch (Exception e) {
                getSLF4JLogger().error("", e);
                return;
            }
            this.getSLF4JLogger().info("%s成功，将玩家%s的进服次数加一".formatted(added ? "添加" : "更新", player.getName()));

            // 没有QQ绑定的话，提示一下绑定QQ
//            try {
////                showQqBindTip(player);
//            } catch (Exception e) {
//                this.getSLF4JLogger().error("", e);
//                this.sendException(player, e);
//            }
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

        this.sponsorshipApi = servicesManager.load(SponsorshipApi2.class);
        this.playerOnlineTimeApi = servicesManager.load(PlayerOnlineTimeApi.class);
        this.paperCardTipApi = this.getPaperCardTipApi();

//        this.qqBindApi = servicesManager.load(QqBindApi.class);
        this.smurfApi = servicesManager.load(SmurfApi.class);

        this.playerTitleApi = servicesManager.load(PlayerTitleApi.class);
        if (this.playerTitleApi == null) {
            this.getSLF4JLogger().warn("Fail to link PlayerTitleApi");
        }

        this.qqGroupMemberInfoApi = servicesManager.load(QqGroupMemberInfoApi.class);

//        this.qqGroupAccessApi = servicesManager.load(QqGroupAccessApi.class);
//        if (this.qqGroupAccessApi == null) {
//            getSLF4JLogger().warn("未连接到QqGroupAccessApi");
//        } else {
//            getSLF4JLogger().info("已连接到QqGroupAccessApi");
//        }

        final PluginCommand command = this.getCommand("zanzhu");
        assert command != null;
        command.setExecutor((commandSender, command1, s, strings) -> {

            if (!(commandSender instanceof final Player player)) {
                this.sendError(commandSender, "该命令只能由玩家来执行");
                return true;
            }

            final SponsorshipApi2 api = this.sponsorshipApi;
            final PlayerOnlineTimeApi api2 = this.playerOnlineTimeApi;

            if (api == null || api2 == null) {
                this.sendError(commandSender, "API不可用！");
                return true;
            }


            this.taskScheduler.runTaskAsynchronously(() -> {

                final OnlineTimeAndJoinCount info;

                try {
                    info = api2.queryTotal(player.getUniqueId());
                } catch (Exception e) {
                    this.getSLF4JLogger().error("", e);
                    this.sendException(player, e);
                    return;
                }

                final TextComponent.Builder text = Component.text();

                api.appendPromptMessage(text, player.getUniqueId(), info.jointCount() + 1, info.onlineTime());

                player.sendMessage(text.build());
            });

            return true;
        });
    }

    @Override
    public void onDisable() {
        this.taskScheduler.cancelTasks(this);
        this.getServer().getServicesManager().unregisterAll(this);
    }

    private boolean handleSponsorshipTitle(@NotNull TextComponent.Builder text, @NotNull Player player) {
        // 赞助头衔
        final SponsorshipApi2 api = this.sponsorshipApi;

        if (api == null) return false;

        final SponsorshipPlayerInfo info;
        try {
            info = api.getSponsorshipService().queryPlayerInfo(player.getUniqueId());
        } catch (Exception e) {
            getSLF4JLogger().error("", e);
            return false;
        }

        if (info == null) return false;


        if (info.totalMoney() >= 10000) {
            final Component t = new MineDown("&#FF1493-#FF0000&『赞助』").toComponent();
            text.append(t);
            text.appendSpace();
        } else if (info.totalMoney() >= 1000) {
            text.append(Component.text("『").color(TextColor.fromHexString("#33CCFF")));
            text.append(Component.text("赞助").color(NamedTextColor.AQUA));
            text.append(Component.text("』").color(TextColor.fromHexString("#33CCFF")));
            text.appendSpace();
        } else {
            return false;
        }

        return true;
    }

    @Nullable PlayerTitleApi getPlayerTitleApi() {
        return this.playerTitleApi;
    }


    void configDisplayName0(@NotNull Player player) {

        final long firstPlayed = player.getFirstPlayed();
        final long delta = System.currentTimeMillis() - firstPlayed;

        final NamedTextColor nameColor = player.isOp() ? NamedTextColor.DARK_RED : NamedTextColor.WHITE;

//        final NamedTextColor nameColor = NamedTextColor.GREEN;

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

        // 全成就头衔

        // 赞助头衔
        if (!prefixSet)
            prefixSet = this.handleSponsorshipTitle(text, player);


        // 萌新头衔
        if (!prefixSet && (firstPlayed <= 0 || delta < 7 * 24 * 60 * 60 * 1000L)) {
            text.append(Component.text("『萌新』").color(NamedTextColor.LIGHT_PURPLE));
            text.appendSpace();
        }

        text.append(Component.text(player.getName()).color(nameColor));

        player.displayName(text.build());
        player.customName(player.displayName());
        player.setCustomNameVisible(true);
    }

    @Override
    public void configDisplayName(Object o) {
        this.configDisplayName0((Player) o);
    }
}
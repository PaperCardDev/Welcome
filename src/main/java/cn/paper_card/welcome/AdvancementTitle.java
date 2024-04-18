package cn.paper_card.welcome;

import cn.paper_card.player_title.api.PlayerTitleApi;
import cn.paper_card.player_title.api.PlayerTitleInfo;
import cn.paper_card.player_title.api.PlayerTitleService;
import io.papermc.paper.advancement.AdvancementDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Server;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

class AdvancementTitle {

    private final @NotNull Welcome plugin;

    AdvancementTitle(@NotNull Welcome plugin) {
        this.plugin = plugin;
    }

    boolean isAllDone(@NotNull Player player) {

        final Server server = plugin.getServer();

        boolean allDone = true;
        final Iterator<Advancement> advancementIterator = server.advancementIterator();
        while (advancementIterator.hasNext()) {
            final Advancement next = advancementIterator.next();
            final AdvancementProgress advancementProgress = player.getAdvancementProgress(next);

            final AdvancementDisplay display = next.getDisplay();
            if (display == null) continue;

            if (!advancementProgress.isDone()) {
                final TextComponent.Builder builder = Component.text();
                builder.append(Component.text("玩家%s未完成成就：".formatted(player.getName())));
                builder.append(next.displayName());

                server.getConsoleSender().sendMessage(builder.build());

                allDone = false;
                break;
            }
        }
        return allDone;
    }

    void check(@NotNull Player player) throws Exception {

        final PlayerTitleApi api = this.plugin.getPlayerTitleApi();
        if (api == null) return;

        final PlayerTitleService service = api.getPlayerTitleService();

        @NotNull String key = "done-all-advancements";

        if (this.isAllDone(player)) {
            @NotNull String titleMinedown = "&#ff9900-#fcc515&『登峰造极』";
            service.addOrUpdate(key, titleMinedown);

            service.addOrUpdatePlayerTitle(new PlayerTitleInfo(
                    player.getUniqueId(),
                    key,
                    true,
                    System.currentTimeMillis(),
                    24 * 60 * 60 * 1000
            ));
        } else {
            // 移除
            service.removePlayerTitle(key, player.getUniqueId());
        }
    }
}

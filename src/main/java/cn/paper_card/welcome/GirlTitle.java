package cn.paper_card.welcome;

import cn.paper_card.player_gender.PlayerGenderApi;
import cn.paper_card.player_title.api.PlayerTitleApi;
import cn.paper_card.player_title.api.PlayerTitleInfo;
import cn.paper_card.player_title.api.PlayerTitleService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

class GirlTitle {

    private final @NotNull Welcome plugin;

    GirlTitle(@NotNull Welcome plugin) {
        this.plugin = plugin;
    }

    boolean isGirl(@NotNull Player player) {
        final PlayerGenderApi api = plugin.getPlayerGenderApi();
        if (api == null) return false;

        final PlayerGenderApi.GenderType genderType;

        try {
            genderType = api.queryGender(player.getUniqueId());
            if (genderType == PlayerGenderApi.GenderType.FEMALE) {
                return true;
            }

        } catch (Exception e) {
            plugin.getSLF4JLogger().error("", e);
        }
        return false;
    }

    void check(@NotNull Player player) throws Exception {

        final PlayerTitleApi api = this.plugin.getPlayerTitleApi();
        if (api == null) return;

        final PlayerTitleService service = api.getPlayerTitleService();

        @NotNull String key = "girl";

        // 妹纸头衔
        // &#fff0f5-#ff69b4&[妹纸]
        if (this.isGirl(player)) {
            @NotNull String titleMinedown = "&#fff0f5-#ff69b4&『妹纸』";
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

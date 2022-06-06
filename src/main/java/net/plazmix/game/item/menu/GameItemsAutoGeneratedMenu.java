package net.plazmix.game.item.menu;

import lombok.Getter;
import lombok.NonNull;
import net.plazmix.game.item.GameItemsCategory;
import net.plazmix.game.user.GameUser;
import net.plazmix.inventory.impl.BasePaginatedInventory;
import net.plazmix.utility.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class GameItemsAutoGeneratedMenu extends BasePaginatedInventory {

    private final GameCategoriesAutoGeneratedMenu previousGeneratedMenu;

    @Getter
    protected final GameItemsCategory gameItemsCategory;

    public GameItemsAutoGeneratedMenu(int inventoryRows,

                                      @NonNull String inventoryTitle,
                                      @NonNull GameItemsCategory gameItemsCategory,
                                      @NonNull GameCategoriesAutoGeneratedMenu previousGeneratedMenu) {

        super(inventoryTitle, inventoryRows);

        this.gameItemsCategory = gameItemsCategory;
        this.previousGeneratedMenu = previousGeneratedMenu;
    }

    @Override
    public void drawInventory(Player player) {
        GameUser gameUser = GameUser.from(player);

        for (int row = 2; row <= inventoryRows - 1; row++) {
            addRowToMarkup(row, 1);
        }

        gameItemsCategory.getMappedItems().forEach(gameItem ->
                addClickItemToMarkup(gameItem.getParameter().toBukkitItem(gameUser, gameItemsCategory),
                        (player1, event) -> gameItem.getParameter().onGeneratedInventoryAction(gameUser, this, event, gameItemsCategory)));

        setClickItem(inventorySize - 4, ItemUtil.newBuilder(Material.SKULL_ITEM)
                        .setDurability(3)
                        .setTextureValue("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzYyNTkwMmIzODllZDZjMTQ3NTc0ZTQyMmRhOGY4ZjM2MWM4ZWI1N2U3NjMxNjc2YTcyNzc3ZTdiMWQifX19")

                        .setName("§eВернуться назад")
                        .addLore("§7▸ Нажмите, чтобы вернуться на страницу назад")
                        .build(),

                (baseInventory, inventoryClickEvent) -> previousGeneratedMenu.openInventory(player));
    }

}
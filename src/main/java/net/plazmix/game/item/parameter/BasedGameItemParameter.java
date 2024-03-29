package net.plazmix.game.item.parameter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.plazmix.game.GamePlugin;
import net.plazmix.game.item.GameItem;
import net.plazmix.game.item.GameItemParameter;
import net.plazmix.game.item.GameItemsCategory;
import net.plazmix.game.item.menu.GameItemsAutoGeneratedMenu;
import net.plazmix.game.mysql.GameMysqlDatabase;
import net.plazmix.game.mysql.WhereQuery;
import net.plazmix.game.mysql.type.BasedGameItemsMysqlDatabase;
import net.plazmix.game.user.GameUser;
import net.plazmix.utility.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class BasedGameItemParameter implements GameItemParameter {
    
    @NonNull
    GameItem item;

    @Override
    public GameMysqlDatabase getDatabase() {
        GameMysqlDatabase database = GameUser.getService().getGameDatabase(BasedGameItemsMysqlDatabase.class);

        if (database == null) {
            GameUser.getService().addGameDatabase(database = new BasedGameItemsMysqlDatabase(
                    GamePlugin.getInstance().getService().getGameName()
            ));
        }

        return database;
    }

    @Override
    public ItemStack toBukkitItem(@NonNull GameUser gameUser, @NonNull GameItemsCategory categoryFor) {
        ItemUtil.ItemBuilder itemBuilder = ItemUtil.newBuilder(item.getIconItem());
        itemBuilder.addLore("");

        if (item.getDescription() != null) {
            itemBuilder.addLoreArray(item.getDescription().toArray(new String[0]));

            itemBuilder.addLore("");
        }

        ChatColor displayColor;

        if (!item.equals(gameUser.getSelectedItem(categoryFor)) && gameUser.hasItem(item)) {
            displayColor = ChatColor.GREEN;

            itemBuilder.addLore("§e▸ Нажмите, чтобы выбрать!");

        } else if (item.equals(gameUser.getSelectedItem(categoryFor))) {
            displayColor = ChatColor.GOLD;

            itemBuilder.setGlowing(true);
            itemBuilder.addLore("§6▸ Предмет выбран");

        } else if (item.getPrice().getCurrency().has(item.getPrice().getCount(), gameUser)) {
            displayColor = ChatColor.YELLOW;

            itemBuilder.setMaterial(Material.STAINED_GLASS_PANE);
            itemBuilder.setDurability(4);

            itemBuilder.addLore("§7Стоимость: §f" + item.getPrice().formattingDisplay());
            itemBuilder.addLore("");
            itemBuilder.addLore("§a▸ Нажмите, чтобы приобрести!");

        } else {
            displayColor = ChatColor.RED;

            itemBuilder.setMaterial(Material.STAINED_GLASS_PANE);
            itemBuilder.setDurability(14);

            itemBuilder.addLore("§7Стоимость: §с" + item.getPrice().formattingDisplay(true));
            itemBuilder.addLore("");
            itemBuilder.addLore("§c▸ Недостаточно средств для покупки!");
        }

        itemBuilder.setName(displayColor + item.getItemName());
        return itemBuilder.build();
    }

    @Override
    public void onGeneratedInventoryAction(@NonNull GameUser gameUser, @NonNull GameItemsAutoGeneratedMenu menu, @NonNull InventoryClickEvent event,  @NonNull GameItemsCategory categoryFor) {
        String prefix = GameUser.getService().getGameName();
        Player player = gameUser.getBukkitHandle();

        if (!item.getPrice().getCurrency().has(item.getPrice().getCount(), gameUser) && !gameUser.hasItem(item)) {
            player.sendMessage("§cОшибка, у Вас недостаточно средств!");
            return;
        }

        if (!gameUser.hasItem(item)) {
            player.sendMessage("§d§l" + prefix + " §8:: §fВы приобрели §a" + item.getItemName() + " §fза " + item.getPrice().formattingDisplay());

            item.getPrice().getCurrency().take(item.getPrice().getCount(), gameUser);
            gameUser.addItem(item);

            menu.updateInventory(player);
            return;
        }

        if (gameUser.hasItem(item) && !item.equals(gameUser.getSelectedItem(categoryFor))) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);

            gameUser.setSelectedItem(item);

            onDefaultSelect(gameUser);
            item.onSelect(gameUser);
        }

        menu.updateInventory(player);
    }

    @Override
    public void onDefaultApply(@NonNull GameUser gameUser) {
    }

    @Override
    public void onDefaultCancel(@NonNull GameUser gameUser) {
    }

    @Override
    public void onDefaultSelect(@NonNull GameUser gameUser) {
        // Current item.
        getDatabase().update(true, gameUser, "State", true,

                new WhereQuery("CategoryID", item.getItemCategory().getId()),
                new WhereQuery("ItemID", item.getId()));

        // Previous item.
        GameItem previousSelectedItem = gameUser.getSelectedItem(item.getItemCategory());

        if (previousSelectedItem != null) {
            gameUser.unselectItem(previousSelectedItem);
        }

        // Player cache
        gameUser.getCache().set("SItems" + item.getItemCategory().getId(), Collections.singletonList(item));
    }

    @Override
    public void onDefaultUnselect(@NonNull GameUser gameUser) {
        getDatabase().update(true, gameUser, "State", false,

                new WhereQuery("CategoryID", item.getItemCategory().getId()),
                new WhereQuery("ItemID", item.getId()));

        // Player cache.
        gameUser.getCache().set("SItems" + item.getItemCategory().getId(), null);
    }

    @Override
    public void onDefaultPurchased(@NonNull GameUser gameUser) {
        getDatabase().insert(true, gameUser, item.getItemCategory().getId(), item.getId(), false);
    }

}

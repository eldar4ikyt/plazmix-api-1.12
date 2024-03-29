package net.plazmix.game;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.plazmix.coreconnector.CoreConnector;
import net.plazmix.coreconnector.module.type.NetworkModule;
import net.plazmix.coreconnector.utility.server.ServerMode;
import net.plazmix.coreconnector.utility.server.game.GameServerInfo;
import net.plazmix.game.event.GameSettingChangeEvent;
import net.plazmix.game.item.GameItemsCategory;
import net.plazmix.game.item.menu.GameCategoriesAutoGeneratedMenu;
import net.plazmix.game.mysql.GameMysqlDatabase;
import net.plazmix.game.setting.GameSetting;
import net.plazmix.game.state.GameState;
import net.plazmix.game.state.GameStateManager;
import net.plazmix.game.team.GameTeam;
import net.plazmix.game.team.GameTeamManager;
import net.plazmix.game.user.GameUser;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public final class GamePluginService {

    // Game arena info.
    private String gameName;
    private String mapName;
    private String serverMode;

    private int alivePlayers;
    private int maxPlayers;


    // Game arena cache & providers.
    private final Map<String, GameUser> gameUsers
            = new HashMap<>();

    private final Map<Class<? extends GameMysqlDatabase>, GameMysqlDatabase> gameDatabasesMap
            = new HashMap<>();

    private final TIntObjectMap<GameItemsCategory> gameItemCategoriesMap
            = new TIntObjectHashMap<>();

    private boolean settingsChangeAllow = true;
    private final EnumMap<GameSetting, Object> gameSettingsMap
            = new EnumMap<>(GameSetting.class);


    // Game managers.
    private final GameTeamManager teamManager       = new GameTeamManager();
    private final GameStateManager stateManager     = new GameStateManager();


    public World getMapWorld() {
        return Bukkit.getWorld(getMapName());
    }

    /**
     * Получить игрового пользователя по
     * уникальному идентификатору.
     *
     * @param playerId - уникальный ID игрока
     */
    public GameUser getGameUser(int playerId) {
        return gameUsers.computeIfAbsent(NetworkModule.getInstance().getPlayerName(playerId).toLowerCase(), f -> new GameUser(playerId));
    }

    /**
     * Получить игрового пользователя по
     * нику игрока.
     *
     * @param playerName - ник игрока
     */
    public GameUser getGameUser(@NonNull String playerName) {
        return getGameUser(NetworkModule.getInstance().getPlayerId(playerName));
    }

    /**
     * Получить игрового пользователя по
     * подключенному Bukkit игроку.
     *
     * @param bukkitPlayer - подключенный Bukkit игрок
     */
    public GameUser getGameUser(@NonNull Player bukkitPlayer) {
        return getGameUser(bukkitPlayer.getName());
    }


    /**
     * Добавить новую игровую базу данных
     * для обработки и хранения игровых
     * данных.
     *
     * @param databaseClass - класс базы данных, по которому она будет кешироваться
     * @param gameMysqlDatabase - игровая база данных
     */
    public void addGameDatabase(@NonNull Class<? extends GameMysqlDatabase> databaseClass,
                                @NonNull GameMysqlDatabase gameMysqlDatabase) {

        gameMysqlDatabase.initTableConnection();
        gameDatabasesMap.put(databaseClass, gameMysqlDatabase);
    }

    /**
     * Добавить новую игровую базу данных
     * для обработки и хранения игровых
     * данных.
     *
     * @param gameMysqlDatabase - игровая база данных
     */
    public void addGameDatabase(@NonNull GameMysqlDatabase gameMysqlDatabase) {
        addGameDatabase(gameMysqlDatabase.getClass(), gameMysqlDatabase);
    }

    /**
     * Получить кешированную игровую базу
     * данных по ее классу.
     *
     * @param databaseClass - класс кешированной игровой базы данных
     */
    public GameMysqlDatabase getGameDatabase(@NonNull Class<? extends GameMysqlDatabase> databaseClass) {
        return gameDatabasesMap.get(databaseClass);
    }


    /**
     * Зарегистрировать одно из возможных
     * игровых состояний арены.
     *
     * @param gameState - состояние арены
     */
    public void registerState(@NonNull GameState gameState) {
        stateManager.registerState(gameState);
    }

    /**
     * Узнать и получить текуще активное
     * состояние арены.
     */
    public GameState getCurrentState() {
        return stateManager.getCurrentState();
    }


    /**
     * Зарегистрировать новую категорию
     * со списком игровых предметов.
     *
     * @param gameItemsCategory - категория игровых предметов
     */
    public void registerItemsCategory(@NonNull GameItemsCategory gameItemsCategory) {
        gameItemCategoriesMap.put(gameItemsCategory.getId(), gameItemsCategory);
    }

    /**
     * Получить зарегистрированную категорию
     * игровых предметов по ее уникальному
     * идентификатору
     *
     * @param categoryID - идентификатор игровой категории
     */
    public GameItemsCategory getItemsCategory(int categoryID) {
        return gameItemCategoriesMap.get(categoryID);
    }


    /**
     * Преобразование сервиса игровой арены
     * в информационный класс, который хранит
     * в себе необходимые данные для их
     * дальнейшей передачи на Core.
     */
    public GameServerInfo getGameServerInfo() {
        boolean availableServer = getCurrentState() != null && getCurrentState().isAvailableJoinPlayers();
        return new GameServerInfo(mapName, serverMode, availableServer, alivePlayers, maxPlayers);
    }

    /**
     * Получить значение игровой настройки.
     *
     * @param gameSetting - настройка
     */
    @SuppressWarnings("unchecked")
    public <V> V getSetting(@NonNull GameSetting gameSetting) {
        return (V) gameSettingsMap.get(gameSetting);
    }

    /**
     * Получить значение игровой настройки.
     *
     * @param gameSetting - настройка
     * @param valueType - тип получаемого значения
     */
    @SuppressWarnings("unchecked")
    public <V> V getSetting(@NonNull GameSetting gameSetting, @NonNull Class<V> valueType) {
        return (V) gameSettingsMap.get(gameSetting);
    }

    /**
     * Установить новое значение игровой
     * настроке.
     *
     * @param gameSetting - настройка
     * @param value - новое значение
     */
    public void setSetting(@NonNull GameSetting gameSetting, Object value) {
        GameSettingChangeEvent settingChangeEvent = new GameSettingChangeEvent(gameSetting, getSetting(gameSetting), value);
        Bukkit.getPluginManager().callEvent(settingChangeEvent);

        if (settingChangeEvent.isCancelled()) {
            return;
        }

        gameSettingsMap.put(gameSetting, value);
    }

    /**
     * Проверить наличие значения у
     * игровой настрйоки.
     *
     * @param gameSetting - настройка
     */
    public boolean hasSettingValue(@NonNull GameSetting gameSetting) {
        return gameSettingsMap.containsKey(gameSetting);
    }


    /**
     * Проверить возможность запуска
     * игрового процесса на сервере.
     */
    public boolean canPlaying() {
        return !stateManager.getGameStates().isEmpty() && (mapName != null && serverMode != null) && maxPlayers > 0;
    }

    public boolean throwPlayersToTeams(int playersInTeam, Collection<GameTeam> gameTeams) {
        Iterator<GameTeam> teamIterator = gameTeams.stream()
                .filter(gameTeam -> gameTeam.getPlayersCount() < playersInTeam)
                .iterator();

        if (!teamIterator.hasNext()) {
            return false;
        }

        GameTeam gameTeam = teamIterator.next();

        for (Player player : Bukkit.getOnlinePlayers()) {
            GameUser gameUser = GameUser.from(player);

            if (gameUser.getCurrentTeam() != null) {
                continue;
            }

            gameTeam.addPlayer(player);

            if (gameTeam.getPlayersCount() >= playersInTeam) {
                if (!teamIterator.hasNext())
                    return true;

                gameTeam = teamIterator.next();
            }
        }

        return true;
    }

    public Collection<GameUser> getAlivePlayers() {
        return gameUsers.values().stream().filter(GameUser::isAlive).collect(Collectors.toSet());
    }

    public Collection<GameUser> getGhostPlayers() {
        return gameUsers.values().stream().filter(GameUser::isGhost).collect(Collectors.toSet());
    }

    public boolean isAlive(@NonNull Player player) {
        return GameUser.from(player).isAlive();
    }

    public boolean isGhost(@NonNull Player player) {
        return GameUser.from(player).isGhost();
    }

    public void registerTeam(int teamId, @NonNull GameTeam gameTeam) {
        teamManager.registerTeam(teamId, gameTeam);
    }

    public void registerTeam(@NonNull GameTeam gameTeam) {
        teamManager.registerTeam(gameTeam.getTeamIndex(), gameTeam);
    }

    public GameTeam getTeam(int teamId) {
        return teamManager.getTeam(teamId);
    }

    public Collection<GameTeam> getLoadedTeams() {
        return teamManager.getLoadedTeamMap().valueCollection();
    }


    public GameCategoriesAutoGeneratedMenu createCategoriesAutoGeneratedMenu(int inventoryRows, String inventoryTitle) {
        return new GameCategoriesAutoGeneratedMenu(inventoryRows, inventoryTitle);
    }

    public void playAgain(@NonNull Player player) {
        String serverName = CoreConnector.getInstance().getServerName();

        Collection<String> connectedServers = NetworkModule.getInstance()
                .getConnectedServers(ServerMode.getSubMode(serverName).getSubPrefix())
                .stream()
                .filter(server -> !server.equalsIgnoreCase(serverName))
                .collect(Collectors.toList());

        if (connectedServers.isEmpty()) {
            player.sendMessage("§6§lPLAZMIX NETWORK §8:: §cНа данный момент нет свободных серверов для данного режима!");
            return;
        }

        NetworkModule.getInstance().redirect(player.getName(), connectedServers.stream()
                .skip((long) (Math.random() * connectedServers.size()))
                .findFirst()
                .get());
    }

}

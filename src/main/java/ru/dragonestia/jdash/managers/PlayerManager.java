package ru.dragonestia.jdash.managers;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sql2o.Connection;
import org.sql2o.ResultSetIterable;
import org.sql2o.Sql2o;
import ru.dragonestia.jdash.exceptions.AccountException;
import ru.dragonestia.jdash.model.account.Account;
import ru.dragonestia.jdash.model.player.FullPlayerData;
import ru.dragonestia.jdash.model.player.IPlayer;
import ru.dragonestia.jdash.model.player.Player;
import ru.dragonestia.jdash.model.player.PlayerSkin;
import ru.dragonestia.jdash.model.score.ScoreStatBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

@Component
public class PlayerManager implements IPlayerManager {

    private Sql2o sql2o;

    private static final String SAVES_PATH = "./saves/";

    public PlayerManager(){
        File file = new File(SAVES_PATH);
        if(!file.exists()) file.mkdir();
    }

    @Autowired
    private void init(Sql2o sql2o) {
        this.sql2o = sql2o;
    }

    public Player getPlayer(Account account) throws AccountException {
        try (Connection conn = sql2o.open()) {
            Player player;
            player = conn.createQuery("SELECT * FROM players WHERE accountId = "+ account.getUid() +" LIMIT 1;")
                    .executeAndFetchFirst(Player.class);

            if(player == null) throw new NullPointerException("Invalid account");

            player.init(account);
            return player;
        }
    }

    public Player createPlayer(Account account) {
        Player player;
        try (Connection conn = sql2o.beginTransaction()) {
            conn.createQuery("INSERT INTO players (accountId) VALUES ("+ account.getUid() +");")
                    .executeUpdate();

            player = conn.createQuery("SELECT * FROM players WHERE accountId = "+ account.getUid() +" LIMIT 1;")
                    .executeAndFetchFirst(Player.class);

            conn.createQuery("INSERT INTO skins (player) VALUES ("+ player.getUid() +");")
                    .executeUpdate();

            conn.commit();
        }
        return player;
    }

    public void updatePlayer(Player player) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(
                    "UPDATE players SET " +
                            "stars = "+ player.getStars() +", " +
                            "demons = "+ player.getDemons() +", " +
                            "coins = "+ player.getCoins() +", " +
                            "userCoins = "+ player.getUserCoins() +", " +
                            "diamonds = "+ player.getDiamonds() +", " +
                            "orbs = " + player.getOrbs() + " " +
                            "WHERE uid = " + player.getUid() + ";"
            ).executeUpdate().commit();
        }
    }

    public PlayerSkin getSkin(Player player) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery("SELECT * FROM skins WHERE player = "+ player.getUid() + " LIMIT 1;")
                    .executeAndFetchFirst(PlayerSkin.class);
        }
    }

    public void updateSkin(PlayerSkin skin) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(
                    "UPDATE skins SET " +
                            "icon = "+ skin.getIcon() +", " +
                            "firstColor = "+ skin.getFirstColor() +", " +
                            "secondColor = "+ skin.getSecondColor() +", " +
                            "iconType = "+ skin.getIconType() +", " +
                            "special = "+ skin.getSpecial() +", " +
                            "accIcon = "+ skin.getAccIcon() +", " +
                            "accShip = "+ skin.getAccShip() +", " +
                            "accBall = "+ skin.getAccBall() +", " +
                            "accBird = "+ skin.getAccBird() +", " +
                            "accDart = "+ skin.getAccDart() +", " +
                            "accRobot = "+ skin.getAccRobot() +", " +
                            "accGlow = "+ skin.getAccGlow() +", " +
                            "accSpider = "+ skin.getAccSpider() +", " +
                            "accExplosion = "+ skin.getAccExplosion() +" " +
                            "WHERE player = " + skin.getPlayer() + ";"
            ).executeUpdate().commit();
        }
    }

    @SneakyThrows
    public void saveData(Player player, String data) {
        File file = new File(SAVES_PATH + player.getUid());

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(data);
        }
    }

    @SneakyThrows
    public String loadData(Player player) {
        File file = new File(SAVES_PATH + player.getUid());

        if(!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            try (BufferedReader buffer = new BufferedReader(reader)) {
                return buffer.readLine();
            }
        }
    }

    public FullPlayerData getFullPlayerData(int playerId) throws IllegalArgumentException {
        FullPlayerData player;
        try (Connection conn = sql2o.open()) {
            player = conn.createQuery(
                    "SELECT " +
                            "    p.uid as id, " +
                            "    p.accountId as accountId, " +
                            "    a.login as name, " +
                            "    a.creatorPoints as creatorPoints, " +
                            "    p.stars as stars, " +
                            "    p.demons as demons, " +
                            "    p.coins as coins, " +
                            "    p.userCoins as userCoins, " +
                            "    p.diamonds as diamonds, " +
                            "    s.icon as skinIcon, " +
                            "    s.firstColor as skinFirstColor, " +
                            "    s.secondColor as skinSecondColor, " +
                            "    s.iconType as skinIconType, " +
                            "    s.special as skinSpecial, " +
                            "    s.accIcon as skinAccIcon, " +
                            "    s.accShip as skinAccShip, " +
                            "    s.accBall as skinAccBall, " +
                            "    s.accBird as skinAccBird, " +
                            "    s.accDart as skinAccDart, " +
                            "    s.accRobot as skinAccRobot, " +
                            "    s.accGlow as skinAccGlow, " +
                            "    s.accSpider as skinAccSpider, " +
                            "    s.accExplosion as skinAccExplosion " +
                            "FROM players p " +
                            "    JOIN skins s ON p.uid = s.player " +
                            "    JOIN accounts a on a.uid = p.accountId " +
                            "WHERE p.uid = "+ playerId +" LIMIT 1;"
                    ).executeAndFetchFirst(FullPlayerData.class);
        }
        if (player == null) throw new IllegalArgumentException("Player not found");

        return player;
    }

    public ArrayList<ScoreStatBuilder> getTopByStars() {
        ArrayList<ScoreStatBuilder> list = new ArrayList<>();
        try (Connection conn = sql2o.open()) {
            ResultSetIterable<FullPlayerData> resultSet = conn.createQuery(
                    "SELECT " +
                            "    p.uid as id, " +
                            "    p.accountId as accountId, " +
                            "    a.login as name, " +
                            "    a.creatorPoints as creatorPoints, " +
                            "    p.stars as stars, " +
                            "    p.demons as demons, " +
                            "    p.coins as coins, " +
                            "    p.userCoins as userCoins, " +
                            "    p.diamonds as diamonds, " +
                            "    s.icon as skinIcon, " +
                            "    s.firstColor as skinFirstColor, " +
                            "    s.secondColor as skinSecondColor, " +
                            "    s.iconType as skinIconType, " +
                            "    s.special as skinSpecial, " +
                            "    s.accIcon as skinAccIcon, " +
                            "    s.accShip as skinAccShip, " +
                            "    s.accBall as skinAccBall, " +
                            "    s.accBird as skinAccBird, " +
                            "    s.accDart as skinAccDart, " +
                            "    s.accRobot as skinAccRobot, " +
                            "    s.accGlow as skinAccGlow, " +
                            "    s.accSpider as skinAccSpider, " +
                            "    s.accExplosion as skinAccExplosion " +
                            "FROM players p " +
                            "    JOIN skins s ON p.uid = s.player " +
                            "    JOIN accounts a on a.uid = p.accountId " +
                            "ORDER BY " +
                            "    stars DESC, " +
                            "    demons DESC, " +
                            "    id " +
                            "LIMIT 100;"
            ).executeAndFetchLazy(FullPlayerData.class);

            int i = 1;
            for (FullPlayerData data: resultSet) {
                list.add(new ScoreStatBuilder(i++, data));
            }
        }

        return list;
    }

    public ArrayList<ScoreStatBuilder> getTopByCreatorPoints() {
        ArrayList<ScoreStatBuilder> list = new ArrayList<>();
        try (Connection conn = sql2o.open()) {
            ResultSetIterable<FullPlayerData> resultSet = conn.createQuery(
                    "SELECT " +
                            "    p.uid as id, " +
                            "    p.accountId as accountId, " +
                            "    a.login as name, " +
                            "    a.creatorPoints as creatorPoints, " +
                            "    p.stars as stars, " +
                            "    p.demons as demons, " +
                            "    p.coins as coins, " +
                            "    p.userCoins as userCoins, " +
                            "    p.diamonds as diamonds, " +
                            "    s.icon as skinIcon, " +
                            "    s.firstColor as skinFirstColor, " +
                            "    s.secondColor as skinSecondColor, " +
                            "    s.iconType as skinIconType, " +
                            "    s.special as skinSpecial, " +
                            "    s.accIcon as skinAccIcon, " +
                            "    s.accShip as skinAccShip, " +
                            "    s.accBall as skinAccBall, " +
                            "    s.accBird as skinAccBird, " +
                            "    s.accDart as skinAccDart, " +
                            "    s.accRobot as skinAccRobot, " +
                            "    s.accGlow as skinAccGlow, " +
                            "    s.accSpider as skinAccSpider, " +
                            "    s.accExplosion as skinAccExplosion " +
                            "FROM players p " +
                            "    JOIN skins s ON p.uid = s.player " +
                            "    JOIN accounts a on a.uid = p.accountId " +
                            "WHERE a.creatorPoints != 0 " +
                            "ORDER BY " +
                            "    creatorPoints DESC, " +
                            "    id " +
                            "LIMIT 100;"
            ).executeAndFetchLazy(FullPlayerData.class);

            int i = 1;
            for (FullPlayerData data: resultSet) {
                list.add(new ScoreStatBuilder(i++, data));
            }
        }

        return list;
    }

    public int getGlobalRank(IPlayer player) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(
                    "SELECT COUNT(*) FROM players " +
                            "WHERE " +
                            "    stars > "+ player.getStars() +" OR " +
                            "    (stars = "+ player.getStars() +" AND demons > "+ player.getDemons() +") OR " +
                            "    (stars = "+ player.getStars() +" AND demons = "+ player.getDemons() +" AND uid >= "+ player.getId() +") " +
                            "ORDER BY " +
                            "         stars DESC, " +
                            "         demons DESC, " +
                            "         uid DESC;"
            ).executeScalar(Integer.class);
        }
    }

    public ArrayList<ScoreStatBuilder> getRelativeTopByPlayer(IPlayer player) {
        ArrayList<ScoreStatBuilder> list = new ArrayList<>();
        try (Connection conn = sql2o.open()) {
            ResultSetIterable<FullPlayerData> resultSet = conn.createQuery(
                    "SELECT fpd.* FROM (( " +
                            "    SELECT " +
                            "        p.uid as id, p.accountId as accountId, a.login as name, a.creatorPoints as creatorPoints, " +
                            "        p.stars as stars, p.demons as demons, p.coins as coins, p.userCoins as userCoins, " +
                            "        p.diamonds as diamonds, s.icon as skinIcon, s.firstColor as skinFirstColor, " +
                            "        s.secondColor as skinSecondColor, s.iconType as skinIconType, s.accIcon as skinAccIcon, " +
                            "        s.accShip as skinAccShip, s.accBall as skinAccBall, s.accBird as skinAccBird, " +
                            "        s.accDart as skinAccDart, s.accRobot as skinAccRobot, s.accGlow as skinAccGlow, " +
                            "        s.accSpider as skinAccSpider, s.accExplosion as skinAccExplosion " +
                            "    FROM players p " +
                            "        JOIN accounts a on a.uid = p.accountId " +
                            "        JOIN skins s on p.uid = s.player " +
                            "    WHERE " +
                            "        p.stars > "+ player.getStars() +" OR " +
                            "        (p.stars = "+ player.getStars() +" AND p.demons > "+ player.getDemons() +") OR " +
                            "        (p.stars = "+ player.getStars() +" AND p.demons = "+ player.getDemons() +" AND p.uid >= "+ player.getId() +") " +
                            "    ORDER BY " +
                            "        stars, " +
                            "        demons, " +
                            "        id " +
                            "    LIMIT 25 " +
                            ") UNION ( " +
                            "    SELECT " +
                            "        p.uid as id, p.accountId as accountId, a.login as name, a.creatorPoints as creatorPoints, " +
                            "        p.stars as stars, p.demons as demons, p.coins as coins, p.userCoins as userCoins, " +
                            "        p.diamonds as diamonds, s.icon as skinIcon, s.firstColor as skinFirstColor, " +
                            "        s.secondColor as skinSecondColor, s.iconType as skinIconType, s.accIcon as skinAccIcon, " +
                            "        s.accShip as skinAccShip, s.accBall as skinAccBall, s.accBird as skinAccBird, " +
                            "        s.accDart as skinAccDart, s.accRobot as skinAccRobot, s.accGlow as skinAccGlow, " +
                            "        s.accSpider as skinAccSpider, s.accExplosion as skinAccExplosion " +
                            "    FROM players p " +
                            "        JOIN accounts a on a.uid = p.accountId " +
                            "        JOIN skins s on p.uid = s.player " +
                            "    WHERE " +
                            "        p.stars < "+ player.getStars() +" OR " +
                            "        (p.stars = "+ player.getStars() +" AND p.demons < "+ player.getDemons() +") OR " +
                            "        (p.stars = "+ player.getStars() +" AND p.demons = "+ player.getDemons() +" AND p.uid < "+ player.getId() +") " +
                            "    ORDER BY " +
                            "        stars DESC, " +
                            "        demons DESC, " +
                            "        id DESC " +
                            "    LIMIT 25 " +
                            ")) as fpd ORDER BY " +
                            "   fpd.stars DESC, " +
                            "   fpd.demons DESC, " +
                            "   fpd.id DESC;"
            ).executeAndFetchLazy(FullPlayerData.class);

            int i = -1;
            for (FullPlayerData target: resultSet) {
                if (i == -1) i = getGlobalRank(target);

                list.add(new ScoreStatBuilder(i++, target));
            }
        }

        return list;
    }
}

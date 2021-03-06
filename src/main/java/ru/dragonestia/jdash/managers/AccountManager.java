package ru.dragonestia.jdash.managers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;
import ru.dragonestia.jdash.exceptions.AccountException;
import ru.dragonestia.jdash.exceptions.AccountRegistrationException;
import ru.dragonestia.jdash.exceptions.NoSuchAccountException;
import ru.dragonestia.jdash.model.account.Account;
import ru.dragonestia.jdash.model.account.AccountSettings;
import ru.dragonestia.jdash.model.profilecomment.ProfileComment;
import ru.dragonestia.jdash.utils.GeometryJumpSecure;

import java.util.List;

@Component
public class AccountManager implements IAccountManager {

    private Sql2o sql2o;
    private PlayerManager playerManager;

    @Autowired
    private void init(Sql2o sql2o, PlayerManager playerManager){
        this.sql2o = sql2o;
        this.playerManager = playerManager;
    }

    public void registerAccount(String login, String password, String email) throws AccountRegistrationException {
        Account account;
        try (Connection conn = sql2o.beginTransaction()) {
            try {
                conn.createQuery("INSERT INTO accounts (login, password, email) VALUES (:login, SHA2(:password, 256), :email);")
                        .addParameter("login", login)
                        .addParameter("password", password)
                        .addParameter("email", email)
                        .executeUpdate();
            } catch (Sql2oException ex) {
                throw new AccountRegistrationException("This login already used");
            }

            account = conn.createQuery("SELECT * FROM accounts WHERE login = :login LIMIT 1;")
                    .addParameter("login", login)
                    .executeAndFetchFirst(Account.class);

            conn.createQuery("INSERT INTO settings (account) VALUES ("+ account.getUid() +");")
                    .executeUpdate();

            conn.commit();

        }
        playerManager.createPlayer(account);
    }

    public Account getAccount(int id) throws NoSuchAccountException {
        try (Connection conn = sql2o.open()) {
            Account account = conn.createQuery("SELECT * FROM accounts WHERE uid = "+ id +" LIMIT 1;")
                    .executeAndFetchFirst(Account.class);

            if(account == null) throw new NoSuchAccountException("Invalid account id");
            return account;
        }
    }

    public Account getAccount(String login) throws NoSuchAccountException {
        try (Connection conn = sql2o.open()) {
            Account account = conn.createQuery("SELECT * FROM accounts WHERE login = :login LIMIT 1;")
                    .addParameter("login", login)
                    .executeAndFetchFirst(Account.class);

            if(account == null) throw new NoSuchAccountException("Invalid account id");
            return account;
        }
    }

    public Account login(String login, String password) throws NoSuchAccountException {
        Account account;
        try (Connection conn = sql2o.open()) {
            account = conn.createQuery(
                    "SELECT * FROM accounts " +
                            "WHERE login = :login AND password = SHA2(:password, 256) " +
                            "LIMIT 1;"
            ).addParameter("login", login)
                    .addParameter("password", password)
                    .executeAndFetchFirst(Account.class);
        }
        if(account == null) throw new NoSuchAccountException("Account not found");
        return account;
    }

    public Account login(int accountId, String gjp) {
        Account account;
        try (Connection conn = sql2o.open()) {
            account = conn.createQuery(
                    "SELECT * FROM accounts WHERE " +
                            "uid = " + accountId + " AND password = SHA2(:password, 256) " +
                            "LIMIT 1;"
            ).addParameter("password", GeometryJumpSecure.gjpDecode(gjp))
                    .executeAndFetchFirst(Account.class);
        }
        if (account == null) throw new AccountException("Authorisation failed");

        return account;
    }

    public AccountSettings getSettings(Account account) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery("SELECT * FROM settings WHERE account = "+ account.getUid() + " LIMIT 1;")
                    .executeAndFetchFirst(AccountSettings.class);
        }
    }

    public void updateSettings(Account account, AccountSettings settings) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(
                    "UPDATE settings SET " +
                            "messages = "+ settings.getMessages() +", " +
                            "comments = "+ settings.getComments() +", " +
                            "friend = "+ settings.getFriend() +" " +
                            "WHERE account = "+ account.getUid() +";"
                    ).executeUpdate()
                    .commit();
        }
    }

    public void publishComment(Account account, String encodedText) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("INSERT INTO profile_comments (owner, text) VALUES ("+ account.getUid() +", :text);")
                    .addParameter("text", encodedText)
                    .executeUpdate();
        }
    }

    public List<ProfileComment> getComments(Account account, int page) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery("SELECT * FROM profile_comments WHERE owner = "+ account.getUid() + " " +
                    "ORDER BY uid DESC LIMIT 10 OFFSET "+ (page * 10) +";").executeAndFetch(ProfileComment.class);
        }
    }

    public int countComments(Account account) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery("SELECT COUNT(*) FROM profile_comments WHERE owner = "+ account.getUid() +";")
                    .executeScalar(Integer.class);
        }
    }

    public ProfileComment getComment(int commentId) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery("SELECT * FROM profile_comments WHERE uid = "+ commentId +" LIMIT 1;")
                    .executeAndFetchFirst(ProfileComment.class);
        }
    }

    public void deleteComment(ProfileComment comment) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("DELETE FROM profile_comments WHERE uid = "+ comment.getUid() + ";")
                    .executeUpdate().commit();
        }
    }
}

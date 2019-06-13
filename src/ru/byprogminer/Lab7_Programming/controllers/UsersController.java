package ru.byprogminer.Lab7_Programming.controllers;

import ru.byprogminer.Lab7_Programming.Credentials;
import ru.byprogminer.Lab7_Programming.MailSender;
import ru.byprogminer.Lab7_Programming.View;
import ru.byprogminer.Lab7_Programming.logging.Loggers;
import ru.byprogminer.Lab7_Programming.models.UsersModel;
import ru.byprogminer.Lab7_Programming.views.*;

import javax.mail.MessagingException;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.byprogminer.Lab5_Programming.LabUtils.*;

public class UsersController {

    private static final class RegisterMail {

        private static final String SUBJECT = "User %1$s registered successfully";

        private static final String TEXT = "" +
                "<h2>User %1$s registered successfully!</h2>" +
                "" +
                "Password of user: <b>%2$s</b>";
    }

    private static final int RANDOM_PASSWORD_LENGTH = 12;

    private final UsersModel usersModel;

    private final Logger log = Loggers.getObjectLogger(this);

    public UsersController(UsersModel usersModel) {
        this.usersModel = usersModel;
    }

    public View checkPassword(Credentials credentials) {
        return authorizedTemplate(credentials, () -> new CheckPasswordView(true));
    }

    public UsersView get() {
        return new UsersView(usersModel.get());
    }

    public View changeUsername(String username, Credentials credentials) {
        return permissionTemplate(credentials, setOf("users.changeUsername.own", "users.changeUsername.all"), () -> {
            try {
                return new ChangeUsernameView(usersModel.setUsername(Objects
                        .requireNonNull(credentials).username, username));
            } catch (Throwable e) {
                return new ChangeUsernameView(errorMessage(e));
            }
        });
    }

    public View changeUsername(String username, String newUsername, Credentials credentials) {
        if (isOwner(username, credentials)) {
            return changeUsername(newUsername, credentials);
        }

        return permissionTemplate(credentials, setOf("users.changeUsername.all",
                "users.changeUsername.user." + username), () -> {
            try {
                return new ChangeUsernameView(usersModel.setUsername(username, newUsername));
            } catch (Throwable e) {
                return new ChangeUsernameView(errorMessage(e));
            }
        });
    }

    public View changePassword(String password, Credentials credentials) {
        return permissionTemplate(credentials, setOf("users.changePassword.own", "users.changePassword.all"), () -> {
            try {
                return new ChangePasswordView(usersModel.setPassword(Objects
                        .requireNonNull(credentials).username, password));
            } catch (Throwable e) {
                return new ChangePasswordView(errorMessage(e));
            }
        });
    }

    public View changePassword(String username, String password, Credentials credentials) {
        if (isOwner(username, credentials)) {
            return changePassword(password, credentials);
        }

        return permissionTemplate(credentials, setOf("users.changePassword.all",
                "users.changePassword.user." + username), () -> {
            try {
                return new ChangePasswordView(usersModel.setPassword(username, password));
            } catch (Throwable e) {
                return new ChangePasswordView(errorMessage(e));
            }
        });
    }

    public View register(String username, String email, Credentials credentials) {
        return permissionTemplate(credentials, "users.register", () -> {
            try {
                final SecureRandom random = new SecureRandom();

                final char[] chars = new char[RANDOM_PASSWORD_LENGTH];
                for (int i = 0; i < chars.length; ++i) {
                    chars[i] = PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length()));
                }

                final String password = new String(chars);
                final boolean userRegistered = usersModel.add(new Credentials(username, password), email);

                boolean mailSent = true;
                if (userRegistered) {
                    try {
                        sendRegisterMail(username, email, password);
                    } catch (Throwable e) {
                        log.log(Level.WARNING, "an error occurred while mail sending", e);
                        mailSent = false;
                    }
                }

                return new RegisterView(userRegistered, username, email, mailSent ? null : password);
            } catch (Throwable e) {
                return new RegisterView(errorMessage(e));
            }
        });
    }

    public View removeUser(Credentials credentials) {
        return permissionTemplate(credentials, setOf("users.removeUser.own", "users.removeUser.all"), () -> {
            try {
                return new RemoveUserView(usersModel.remove(credentials.username));
            } catch (Throwable e) {
                return new RemoveUserView(errorMessage(e));
            }
        });
    }

    public View removeUser(String username, Credentials credentials) {
        if (isOwner(username, credentials)) {
            return removeUser(credentials);
        }

        return permissionTemplate(credentials, setOf("users.changeUsername.all",
                "users.changeUsername.user." + username), () -> {
            try {
                return new RemoveUserView(usersModel.remove(username));
            } catch (Throwable e) {
                return new RemoveUserView(errorMessage(e));
            }
        });
    }

    public PermissionsView getPermissions(String username) {
        return new PermissionsView(usersModel.getPermissions(username));
    }

    public View givePermission(String username, String permission, Credentials credentials) {
        return permissionTemplate(credentials, "users.givePermission.user." + username, () -> {
            try {
                if (!usersModel.hasPermission(credentials.username, permission)) {
                    return new NotPermittedView();
                }

                return new GivePermissionView(usersModel.givePermission(username, permission));
            } catch (Throwable e) {
                return new GivePermissionView(errorMessage(e));
            }
        });
    }

    public View takePermission(String username, String permission, Credentials credentials) {
        return permissionTemplate(credentials, "users.takePermission.user." + username, () -> {
            try {
                if (!usersModel.hasPermission(credentials.username, permission)) {
                    return new NotPermittedView();
                }

                return new TakePermissionView(usersModel.takePermission(username, permission));
            } catch (Throwable e) {
                return new TakePermissionView(errorMessage(e));
            }
        });
    }

    private void sendRegisterMail(String username, String email, String password) throws MessagingException {
        MailSender.send(email,
                String.format(RegisterMail.SUBJECT, username),
                String.format(RegisterMail.TEXT, username, password)
        );
    }

    private boolean isOwner(String username, Credentials credentials) {
        return username == null || (credentials != null && credentials.username.equals(username));
    }

    private View authorizedTemplate(Credentials credentials, Supplier<View> code) {
        if (credentials == null) {
            return new NotLoggedView();
        }

        if (!usersModel.check(credentials)) {
            return new WrongCredentialsView(credentials);
        }

        return code.get();
    }

    private View permissionTemplate(Credentials credentials, String permission, Supplier<View> code) {
        return authorizedTemplate(credentials, () -> {
            if (!usersModel.hasPermission(credentials.username, permission)) {
                return new NotPermittedView();
            }

            return code.get();
        });
    }

    private View permissionTemplate(Credentials credentials, Set<String> permissions, Supplier<View> code) {
        return authorizedTemplate(credentials, () -> {
            if (!usersModel.hasPermission(credentials.username, permissions)) {
                return new NotPermittedView();
            }

            return code.get();
        });
    }
}

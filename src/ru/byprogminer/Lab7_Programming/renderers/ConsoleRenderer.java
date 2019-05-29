package ru.byprogminer.Lab7_Programming.renderers;

import ru.byprogminer.Lab5_Programming.command.Console;
import ru.byprogminer.Lab7_Programming.Renderer;
import ru.byprogminer.Lab7_Programming.View;
import ru.byprogminer.Lab7_Programming.views.*;

import java.util.Objects;

public class ConsoleRenderer implements Renderer {

    private final Console console;

    public ConsoleRenderer(Console console) {
        this.console = Objects.requireNonNull(console);
    }

    public void render(View view) {
        if (view.error != null) {
            console.printError(view.error + ". Please try again or consult a specialist");
        }

        if (view instanceof NotLoggedView) {
            console.printError("you are not logged in");
            return;
        }

        if (view instanceof WrongCredentialsView) {
            console.printError("wrong password or user is not exists");
            return;
        }

        if (view instanceof NotPermittedView) {
            console.printError("you don't have permission");
            return;
        }

        if (view instanceof InfoView) {
            final InfoView infoView = (InfoView) view;

            infoView.metadata.forEach((key, value) ->
                    console.printf("%s: %s\n", key, value));

            return;
        }

        if (view instanceof ShowView) {
            final ShowView showView = (ShowView) view;

            showView.elements.forEach(console::println);
            return;
        }

        if (view instanceof AddView) {
            final ModifyView modifyView = (ModifyView) view;

            if (view.error == null && modifyView.affectedRows == 0) {
                console.printWarning("no one elements added");
            } else if (modifyView.affectedRows == 1) {
                console.println("One element added.");
            } else {
                console.printf("%s elements added.\n", modifyView.affectedRows);
            }

            return;
        }

        if (view instanceof RemoveView) {
            final ModifyView modifyView = (ModifyView) view;

            if (view.error == null && modifyView.affectedRows == 0) {
                console.printWarning("no one elements removed");
            } else if (modifyView.affectedRows == 1) {
                console.println("One element removed.");
            } else {
                console.printf("%s elements removed.\n", modifyView.affectedRows);
            }

            return;
        }

        if (view instanceof ImportView) {
            final ModifyView modifyView = (ModifyView) view;

            if (view.error == null && modifyView.affectedRows == 0) {
                console.printWarning("no one elements imported");
            } else if (modifyView.affectedRows == 1) {
                console.println("One element imported.");
            } else {
                console.printf("%s elements imported.\n", modifyView.affectedRows);
            }

            return;
        }

        if (view instanceof ModifyView) {
            final ModifyView modifyView = (ModifyView) view;

            if (view.error == null && modifyView.affectedRows == 0) {
                console.printWarning("no one elements affected");
            } else if (modifyView.affectedRows == 1) {
                console.println("One element affected.");
            } else {
                console.printf("%s elements affected.\n", modifyView.affectedRows);
            }

            return;
        }

        if (view.error == null) {
            if (view instanceof LoadView) {
                final LoadView loadView = (LoadView) view;

                console.printf("Loaded from %s.\n", loadView.filename);
                return;
            }

            if (view instanceof SaveView) {
                final SaveView saveView = (SaveView) view;

                console.printf("Saved to %s.\n", saveView.filename);
                return;
            }
        }

        if (view instanceof ChangePasswordView) {
            final ChangePasswordView changePasswordView = (ChangePasswordView) view;

            if (changePasswordView.ok) {
                console.println("Password changed successfully.");
            }

            return;
        }

        if (view instanceof RegisterView) {
            final RegisterView registerView = (RegisterView) view;

            if (view.error == null && !registerView.ok) {
                console.printWarning(String.format("user %s wasn't registered", registerView.username));
            } else {
                console.printf("User %s was registered successfully.\n", registerView.username);

                if (registerView.password == null) {
                    console.printf("On address %s has sent a mail with password.\n", registerView.email);
                } else {
                    console.printWarning(String.format("An error occurred while mail sending. Password of user %s: %s",
                            registerView.username, registerView.password));
                }
            }
        }
    }
}

package ru.byprogminer.Lab7_Programming;

import java.util.Stack;

public class CurrentUser {

    private final Stack<Credentials> credentialsStack = new Stack<>();

    public Credentials reset() {
        if (!credentialsStack.empty()) {
            credentialsStack.pop();
        }

        if (!credentialsStack.empty()) {
            return credentialsStack.peek();
        }

        return null;
    }

    public void set(Credentials credentials) {
        final Credentials currentUser = get();

        if (currentUser != null && currentUser.username.equals(credentials.username)) {
            credentialsStack.pop();
        }

        credentialsStack.push(credentials);
    }

    public Credentials get() {
        if (credentialsStack.empty()) {
            return null;
        }

        return credentialsStack.peek();
    }
}

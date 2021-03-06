package ru.byprogminer.Lab6_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab7_Programming.Credentials;
import ru.byprogminer.Lab7_Programming.View;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

public abstract class Packet implements Serializable {

    public static abstract class Request extends Packet {

        private static abstract class _ElementContainer extends Request {

            public final LivingObject element;

            public _ElementContainer(LivingObject element, Credentials credentials) {
                super(credentials);

                this.element = Objects.requireNonNull(element);
            }
        }

        private static abstract class _FilenameContainer extends Request {

            public final String filename;

            public _FilenameContainer(String filename, Credentials credentials) {
                super(credentials);

                this.filename = Objects.requireNonNull(filename);
            }
        }

        public static final class Add extends _ElementContainer {

            public Add(LivingObject element, Credentials credentials) {
                super(element, credentials);
            }
        }

        public static final class Remove extends _ElementContainer {

            public Remove(LivingObject element, Credentials credentials) {
                super(element, credentials);
            }
        }

        public static final class RemoveLower extends _ElementContainer {

            public RemoveLower(LivingObject element, Credentials credentials) {
                super(element, credentials);
            }
        }

        public static final class RemoveGreater extends _ElementContainer {

            public RemoveGreater(LivingObject element, Credentials credentials) {
                super(element, credentials);
            }
        }

        public static final class Info extends Request {

            public Info(Credentials credentials) {
                super(credentials);
            }
        }

        public static final class ShowAll extends Request {

            public ShowAll(Credentials credentials) {
                super(credentials);
            }
        }

        public static final class Show extends Request {

            public final long count;

            public Show(long count, Credentials credentials) {
                super(credentials);

                this.count = count;
            }
        }

        public static final class Save extends _FilenameContainer {

            public Save(String filename, Credentials credentials) {
                super(filename, credentials);
            }
        }

        public static final class Load extends _FilenameContainer {

            public Load(String filename, Credentials credentials) {
                super(filename, credentials);
            }
        }

        public static final class Import extends Request {

            public final Collection<LivingObject> content;

            public Import(Collection<LivingObject> content, Credentials credentials) {
                super(credentials);

                this.content = Objects.requireNonNull(content);
            }
        }

        public static final class ChangePassword extends Request {

            public final String username;
            public final String password;

            public ChangePassword(String username, String password, Credentials credentials) {
                super(credentials);

                this.username = Objects.requireNonNull(username);
                this.password = Objects.requireNonNull(password);
            }

            public ChangePassword(String password, Credentials credentials) {
                super(credentials);

                this.username = null;
                this.password = Objects.requireNonNull(password);
            }
        }

        public static final class Register extends Request {

            public final String username;
            public final String email;

            public Register(String username, String email, Credentials credentials) {
                super(credentials);

                this.username = Objects.requireNonNull(username);
                this.email = Objects.requireNonNull(email);
            }
        }

        public final Credentials credentials;

        private Request(Credentials credentials) {
            this.credentials = credentials;
        }
    }

    public abstract static class Response extends Packet {

        public static final class Ping extends Response {}

        public static final class Done extends Response {

            public final View view;

            public Done(View view) {
                this.view = view;
            }
        }

        private Response() {}
    }

    private Packet() {}
}

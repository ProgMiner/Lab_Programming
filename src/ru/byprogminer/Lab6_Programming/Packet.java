package ru.byprogminer.Lab6_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab7_Programming.View;

import java.io.Serializable;
import java.util.Collection;

public abstract class Packet implements Serializable {

    public static abstract class Request extends Packet {

        private static abstract class _ElementContainer extends Request {

            public final LivingObject element;

            public _ElementContainer(LivingObject element) {
                this.element = element;
            }
        }

        private static abstract class _FilenameContainer extends Request {

            public final String filename;

            public _FilenameContainer(String filename) {
                this.filename = filename;
            }
        }

        public static final class Add extends _ElementContainer {

            public Add(LivingObject element) {
                super(element);
            }
        }

        public static final class Remove extends _ElementContainer {

            public Remove(LivingObject element) {
                super(element);
            }
        }

        public static final class RemoveLower extends _ElementContainer {

            public RemoveLower(LivingObject element) {
                super(element);
            }
        }

        public static final class RemoveGreater extends _ElementContainer {

            public RemoveGreater(LivingObject element) {
                super(element);
            }
        }

        public static final class Info extends Request {}

        public static final class ShowAll extends Request {}

        public static final class Show extends Request {

            public final int count;

            public Show(int count) {
                this.count = count;
            }
        }

        public static final class Save extends _FilenameContainer {

            public Save(String filename) {
                super(filename);
            }
        }

        public static final class Load extends _FilenameContainer {

            public Load(String filename) {
                super(filename);
            }
        }

        public static final class Import extends Request {

            public final Collection<LivingObject> content;

            public Import(Collection<LivingObject> content) {
                this.content = content;
            }
        }

        private Request() {}
    }

    public abstract static class Response extends Packet {

        public static class Done extends Response {

            public final View view;

            public Done(View view) {
                this.view = view;
            }
        }

        private Response() {}
    }

    private Packet() {}
}

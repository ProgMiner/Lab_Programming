package ru.byprogminer.Lab6_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;

import java.io.Serializable;

public abstract class Packet implements Serializable {

    public static abstract class Request extends Packet {

        private static abstract class _ElementContainer extends Request {

            private final LivingObject element;

            public _ElementContainer(LivingObject element) {
                this.element = element;
            }

            public LivingObject getElement() {
                return element;
            }
        }

        public static final class Add extends _ElementContainer {

            public Add(LivingObject element) {
                super(element);
            }
        }

        public static final class Load extends Request {}

        public static final class Save extends Request {}

        public static final class Show extends Request {}

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

        public static final class Remove extends _ElementContainer {

            public Remove(LivingObject element) {
                super(element);
            }
        }

        public static final class Info extends Request {}

        public static final class Import extends Request {

            private final byte[] content;

            public Import(byte[] content) {
                this.content = content;
            }

            public byte[] getContent() {
                return content;
            }
        }

        private Request() {}
    }

    public static class Response extends Packet {

        public enum Status {

            OK, WARN, ERR;

            public Status update(Status next) {
                if (next.ordinal() < ordinal()) {
                    return this;
                }

                return next;
            }
        }

        private final String content;
        private final Status status;

        public Response(String content, Status status) {
            this.content = content;
            this.status = status;
        }

        public String getContent() {
            return content;
        }

        public Status getStatus() {
            return status;
        }
    }

    private Packet() {}
}

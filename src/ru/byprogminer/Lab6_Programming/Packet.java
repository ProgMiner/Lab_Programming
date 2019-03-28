package ru.byprogminer.Lab6_Programming;

import java.io.Serializable;

public abstract class Packet implements Serializable {

    public static abstract class Request extends Packet {

        public static final class ConsoleInput extends Request {

            private final byte[] content;

            public ConsoleInput(byte[] content) {
                this.content = content;
            }

            public byte[] getContent() {
                return content;
            }
        }

        public static final class ImportFile extends Response {

            private final String filename;

            public ImportFile(String filename) {
                this.filename = filename;
            }

            public String getFilename() {
                return filename;
            }
        }

        private Request() {}
    }

    public static abstract class Response extends Packet {

        public static final class ConsoleOutput extends Response {

            private final byte[] content;

            public ConsoleOutput(byte[] content) {
                this.content = content;
            }

            public byte[] getContent() {
                return content;
            }
        }

        public static final class ImportFile extends Response {

            private final byte[] content;

            public ImportFile(byte[] content) {
                this.content = content;
            }

            public byte[] getContent() {
                return content;
            }
        }

        private Response() {}
    }

    private Packet() {}
}

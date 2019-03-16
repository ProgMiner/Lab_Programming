package ru.byprogminer.Lab6_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;

public abstract class Packet implements Serializable {

    public static abstract class Request extends Packet {

        public static final class CurrentState extends Request {}

        private Request() {}
    }

    public static abstract class Response extends Packet {

        public static final class CurrentState extends Response {

            private final List<LivingObject> content;

            public CurrentState(List<LivingObject> content) {
                this.content = content;
            }

            public List<LivingObject> getContent() {
                return content;
            }
        }

        private Response() {}
    }

    private Packet() {}

    public ByteBuffer pack() throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        objectStream.writeObject(this);

        return ByteBuffer.wrap(byteStream.toByteArray());
    }
}

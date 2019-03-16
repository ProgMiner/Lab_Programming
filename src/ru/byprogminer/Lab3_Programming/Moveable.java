package ru.byprogminer.Lab3_Programming;

public interface Moveable {

    enum Move {
        GO("идёт"), RUN("бежит"), SWIM("плывёт"), FLY("летит");

        private String actionName;

        Move(String actionName) {
            this.actionName = actionName;
        }

        public String getActionName() {
            return actionName;
        }
    }

    void moveTo(Object target, Move move);

    void moveTo(String target, Move move);

    void moveFor(Object target, Move move);

    void moveFrom(Object enemy, Move move);

    void moveFrom(String enemy, Move move);
}

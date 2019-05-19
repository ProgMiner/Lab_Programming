package ru.byprogminer.Lab7_Programming;

public abstract class Commands {

    public static abstract class Help {

        public static final String USAGE = "help [command]";
        public static final String DESCRIPTION = "" +
                "Show available commands or description of the command if provided";

        private Help() {}
    }

    public static abstract class Exit {

        public static final String DESCRIPTION = "Exit from the application";

        private Exit() {}
    }

    public static abstract class Add {

        public static final String USAGE = "add <element>";
        public static final String DESCRIPTION = "" +
                "Add the provided element to the collection\n" +
                ELEMENT_DESCRIPTION;

        private Add() {}
    }

    public static abstract class Remove {

        public static final String USAGE = "remove <element>";
        public static final String DESCRIPTION = "" +
                "Remove the provided element from the collection\n" +
                ELEMENT_DESCRIPTION;

        private Remove() {}
    }

    public static abstract class RemoveLower {

        public static final String ALIAS = "remove_lower";
        public static final String USAGE = "remove_lower <element>";
        public static final String DESCRIPTION = "" +
                "Remove all elements that lower than the provided from the collection\n" +
                ELEMENT_DESCRIPTION;

        private RemoveLower() {}
    }

    public static abstract class RemoveGreater {

        public static final String ALIAS = "remove_greater";
        public static final String USAGE = "remove_greater <element>";
        public static final String DESCRIPTION = "" +
                "Remove all elements that greater than the provided from the collection\n" +
                ELEMENT_DESCRIPTION;

        private RemoveGreater() {}
    }

    public static abstract class Info {

        public static final String DESCRIPTION = "Show information about the collection";

        private Info() {}
    }

    public static abstract class Show {

        public static final String USAGE = "show [count]";
        public static final String DESCRIPTION = "" +
                "Show elements in the collection\n" +
                "  - count - maximum count of elements, default all";

        private Show() {}
    }

    public static abstract class Ls {

        public static final String DESCRIPTION = "An alias for the `show` command";

        private Ls() {}
    }

    public static abstract class Save {

        public static final String USAGE = "save <filename>";
        public static final String DESCRIPTION = "" +
                "Save collection to file in CSV format\n" +
                "  - filename - name of file to save";

        private Save() {}
    }

    public static abstract class Load {

        public static final String USAGE = "load <filename>";
        public static final String DESCRIPTION = "" +
                "Load collection (instead of current) from file in CSV format\n" +
                "  - filename - name of file to load";

        private Load() {}
    }

    public static abstract class Import {

        public static final String ALIAS = "import";
        public static final String USAGE = "import <filename>";
        public static final String DESCRIPTION = "" +
                "Import elements from file\n" +
                "  - filename - name of file to import";

        private Import() {}
    }

    private static final String ELEMENT_DESCRIPTION = "" +
            "  - element - living object in JSON format. Available fields:\n" +
            "    - name - string - name, required;\n" +
            "    - volume - double - volume;\n" +
            "    - creatingTime - string/long - date and time of object creating in\n" +
            "                                   locale-specific (similar to printing)\n" +
            "                                   format or as unix timestamp (count of\n" +
            "                                   seconds from the 1 Jan 1970 00:00:00;\n" +
            "    - x - double - x coordinate;\n" +
            "    - y - double - y coordinate;\n" +
            "    - z - double - z coordinate;\n" +
            "    - lives - boolean - current condition (lives or not);\n" +
            "    - items - object[] - array of objects that owner is this living\n" +
            "                         object. Each objects specifies in JSON object\n" +
            "                         format. All fields from living object except\n" +
            "                         `lives` and `items` is available";

    private Commands() {}
}

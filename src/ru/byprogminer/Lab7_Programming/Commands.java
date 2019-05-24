package ru.byprogminer.Lab7_Programming;

public final class Commands {

    public static final class Help {

        public static final String USAGE = "help [command]";
        public static final String DESCRIPTION = "" +
                "Show available commands or description of the command if provided";

        private Help() {}
    }

    public static final class Exit {

        public static final String DESCRIPTION = "Exit from the application";

        private Exit() {}
    }

    public static final class Add {

        public static final String USAGE = "add <element>";
        public static final String DESCRIPTION = "" +
                "Add the provided element to the collection\n" +
                ELEMENT_DESCRIPTION;

        private Add() {}
    }

    public static final class Remove {

        public static final String USAGE = "remove <element>";
        public static final String DESCRIPTION = "" +
                "Remove the provided element from the collection\n" +
                ELEMENT_DESCRIPTION;

        private Remove() {}
    }

    public static final class Rm {

        public static final String USAGE = "rm <element>";
        public static final String DESCRIPTION = "" +
                "Alias for `remove` command\n" +
                ELEMENT_DESCRIPTION;

        private Rm() {}
    }

    public static final class RemoveLower {

        public static final String ALIAS = "remove_lower";
        public static final String USAGE = "remove_lower <element>";
        public static final String DESCRIPTION = "" +
                "Remove all elements that lower than the provided from the collection\n" +
                ELEMENT_DESCRIPTION;

        private RemoveLower() {}
    }

    public static final class RemoveGreater {

        public static final String ALIAS = "remove_greater";
        public static final String USAGE = "remove_greater <element>";
        public static final String DESCRIPTION = "" +
                "Remove all elements that greater than the provided from the collection\n" +
                ELEMENT_DESCRIPTION;

        private RemoveGreater() {}
    }

    public static final class Info {

        public static final String DESCRIPTION = "Show information about the collection";

        private Info() {}
    }

    public static final class Show {

        public static final String USAGE = "show [count]";
        public static final String DESCRIPTION = "" +
                "Show elements in the collection\n" +
                "  - count - maximum count of elements, default all";

        private Show() {}
    }

    public static final class Ls {

        public static final String DESCRIPTION = "An alias for the `show` command";

        private Ls() {}
    }

    public static final class Save {

        public static final String USAGE = "save <filename>";
        public static final String DESCRIPTION = "" +
                "Save collection to file in CSV format\n" +
                "  - filename - name of file to save";

        private Save() {}
    }

    public static final class Load {

        public static final String USAGE = "load <filename>";
        public static final String DESCRIPTION = "" +
                "Load collection (instead of current) from file in CSV format\n" +
                "  - filename - name of file to load";

        private Load() {}
    }

    public static final class Import {

        public static final String ALIAS = "import";
        public static final String USAGE = "import <filename>";
        public static final String DESCRIPTION = "" +
                "Import elements from file\n" +
                "  - filename - name of file to import";

        private Import() {}
    }

    public static final class Su {

        public static final String USAGE = "su [username]";
        public static final String DESCRIPTION = "" +
                "Set current user to provided or default\n" +
                "  - username - name of user to set";

        private Su() {}
    }

    public static final class Passwd {

        public static final String USAGE = "passwd [username]";
        public static final String DESCRIPTION = "" +
                "Change password of current or provided user\n" +
                "  - username - not required name of user for change password";

        private Passwd() {}
    }

    public static final class Register {

        public static final String USAGE = "register <username>";
        public static final String DESCRIPTION = "" +
                "Register user with provided name\n" +
                "  - username - name for new user";

        private Register() {}
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

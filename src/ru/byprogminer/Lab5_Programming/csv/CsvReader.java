package ru.byprogminer.Lab5_Programming.csv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class CsvReader implements Iterable<List<String>>, Iterator<List<String>> {

    private final Scanner scanner;

    public CsvReader(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public List<String> next() {
        return parseLine(scanner.nextLine());
    }

    @Override
    public boolean hasNext() {
        return scanner.hasNextLine();
    }

    private static List<String> parseLine(final String str) {
        final List<String> ret = new ArrayList<>();

        boolean quote = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < str.length(); ++i) {
            if (quote) {
                if (str.charAt(i) == '"') {
                    if (i + 1 < str.length() && str.charAt(i + 1) == '"') {
                        current.append('"');
                        ++i;

                        continue;
                    }

                    quote = false;
                } else {
                    current.append(str.charAt(i));
                }

                continue;
            }

            if (str.charAt(i) == '"') {
                quote = true;
                continue;
            }

            if (str.charAt(i) == ',') {
                ret.add(current.toString());
                current = new StringBuilder();
                continue;
            }

            current.append(str.charAt(i));
        }

        ret.add(current.toString());
        return ret;
    }

    @Override
    public Iterator<List<String>> iterator() {
        return this;
    }

    public Scanner getScanner() {
        return scanner;
    }
}

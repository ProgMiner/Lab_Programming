package ru.byprogminer.Lab5_Programming.command;

public interface StatusPrinter {

    void print(Object text);
    void println(Object text);
    void printf(String format, Object... text);
    void printWarning(Object text);
    void printError(Object text);
}

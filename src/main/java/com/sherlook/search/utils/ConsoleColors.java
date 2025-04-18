package com.sherlook.search.utils;

public class ConsoleColors {
  public static final String BOLD_RED = "\u001B[1;31m";
  public static final String RESET = "\u001B[0m";
  public static final String BOLD_YELLOW = "\u001B[1;33m";
  public static final String BOLD_GREEN = "\u001B[1;32m";
  public static final String BOLD_BLUE = "\u001B[1;34m";
  public static final String BOLD_PURPLE = "\u001B[1;35m";
  public static final String BOLD_CYAN = "\u001B[1;36m";
  public static final String BOLD_WHITE = "\u001B[1;37m";
  public static final String UNDERLINE = "\u001B[4m";

  public static void printError(String message) {
    System.err.print("[" + BOLD_RED + message + RESET + "] ");
  }

  public static void printWarning(String message) {
    System.out.print("[" + BOLD_YELLOW + message + RESET + "] ");
  }

  public static void printInfo(String message) {
    System.out.print("[" + BOLD_BLUE + message + RESET + "] ");
  }

  public static void printSuccess(String message) {
    System.out.print("[" + BOLD_GREEN + message + RESET + "] ");
  }
}

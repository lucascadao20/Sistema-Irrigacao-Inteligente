package com.irrigacao;

import com.irrigacao.presentation.console.ConsoleFormatter;
import com.irrigacao.presentation.console.ConsoleInputHandler;
import com.irrigacao.presentation.console.ConsoleUI;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ConsoleFormatter formatter = new ConsoleFormatter();
        ConsoleInputHandler input = new ConsoleInputHandler(new Scanner(System.in));
        ConsoleUI ui = new ConsoleUI(formatter, input);

        if (args.length > 0 && args[0].equals("--demo")) {
            ui.executarModoDemo();
        } else {
            ui.executarModoTempoReal();
        }
    }
}

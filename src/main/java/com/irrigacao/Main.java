package com.irrigacao;

import com.irrigacao.presentation.console.ConsoleFormatter;
import com.irrigacao.presentation.console.ConsoleInputHandler;
import com.irrigacao.presentation.console.ConsoleUI;
import com.irrigacao.presentation.web.WebServer;

import java.util.Scanner;

public class Main {
    private static final int PORTA_WEB_PADRAO = 7070;

    public static void main(String[] args) {
        String modo = args.length > 0 ? args[0] : "";

        if (modo.equals("--web")) {
            int porta = PORTA_WEB_PADRAO;
            if (args.length > 1) {
                try { porta = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
            }
            new WebServer().iniciar(porta);
            return;
        }

        ConsoleFormatter formatter = new ConsoleFormatter();
        ConsoleInputHandler input = new ConsoleInputHandler(new Scanner(System.in));
        ConsoleUI ui = new ConsoleUI(formatter, input);

        if (modo.equals("--demo")) {
            ui.executarModoDemo();
        } else {
            ui.executarModoTempoReal();
        }
    }
}

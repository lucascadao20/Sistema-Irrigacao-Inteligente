package com.irrigacao;

import com.irrigacao.infrastructure.console.FormatadorDeConsole;
import com.irrigacao.infrastructure.console.LeitorDeEntrada;
import com.irrigacao.infrastructure.console.InterfaceConsole;
import com.irrigacao.infrastructure.web.ServidorWeb;

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
            new ServidorWeb().iniciar(porta);
            return;
        }

        FormatadorDeConsole formatador = new FormatadorDeConsole();
        LeitorDeEntrada leitor = new LeitorDeEntrada(new Scanner(System.in));
        InterfaceConsole ui = new InterfaceConsole(formatador, leitor);

        if (modo.equals("--demo")) {
            ui.executarModoDemo();
        } else {
            ui.executarModoTempoReal();
        }
    }
}

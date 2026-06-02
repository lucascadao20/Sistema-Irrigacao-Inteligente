package com.irrigacao.infrastructure.console;

import com.irrigacao.modelo.Cultura;

import java.util.Map;
import java.util.Scanner;

public class LeitorDeEntrada {
    private final Scanner scanner;

    public LeitorDeEntrada(Scanner scanner) {
        this.scanner = scanner;
    }

    public String lerCultura(Map<String, Cultura> disponiveis) {
        System.out.print("\n[CONFIG] Digite a cultura monitorada (ex: milho): ");
        String input = scanner.nextLine().trim().toLowerCase();
        if (!disponiveis.containsKey(input)) {
            System.out.println("[AVISO] Cultura nao encontrada. Usando 'milho' como padrao.");
            return "milho";
        }
        return input;
    }

    public String lerNomeAgricultor() {
        System.out.print("[CONFIG] Nome do agricultor: ");
        String nome = scanner.nextLine().trim();
        if (nome.isEmpty()) {
            return "Agricultor";
        }
        return nome;
    }

    public int lerIntervalo(int padrao) {
        System.out.printf("[CONFIG] Intervalo entre ciclos em segundos (padrao: %d): ", padrao);
        String input = scanner.nextLine().trim();
        try {
            if (!input.isEmpty()) {
                return Integer.parseInt(input);
            }
        } catch (NumberFormatException e) {
            System.out.println("[AVISO] Valor invalido. Usando " + padrao + " segundos.");
        }
        return padrao;
    }
}

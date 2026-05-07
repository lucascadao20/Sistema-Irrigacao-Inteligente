package com.irrigacao.presentation.console;

import com.irrigacao.application.service.CicloIrrigacaoService;
import com.irrigacao.application.service.GerenciadorSensores;
import com.irrigacao.domain.model.Cultura;
import com.irrigacao.domain.model.DadosClimaticos;
import com.irrigacao.domain.model.Irrigacao;
import com.irrigacao.infrastructure.config.AppConfig;
import com.irrigacao.infrastructure.simulator.SimuladorSensores;

import java.time.LocalDateTime;
import java.util.Map;

public class ConsoleUI {
    private final ConsoleFormatter formatter;
    private final ConsoleInputHandler input;

    public ConsoleUI(ConsoleFormatter formatter, ConsoleInputHandler input) {
        this.formatter = formatter;
        this.input = input;
    }

    public void executarModoTempoReal() {
        System.out.println(formatter.formatarCabecalho(
                "SISTEMA DE IRRIGACAO INTELIGENTE - Modo: TEMPO REAL"));

        AppConfig config = new AppConfig();

        Map<String, Cultura> culturas = config.criarCulturaRepository().listarTodas();
        System.out.println(formatter.formatarListaCulturas(culturas));

        String cultura = input.lerCultura(culturas);
        String agricultor = input.lerNomeAgricultor();
        int intervalo = input.lerIntervalo(60);

        CicloIrrigacaoService cicloService = config.criarSistema(agricultor);

        GerenciadorSensores gerenciador = new GerenciadorSensores();
        SimuladorSensores simulador = new SimuladorSensores(gerenciador);
        simulador.inicializarSensores();

        System.out.printf("%n[SISTEMA] Monitorando cultura: %s%n", cultura.toUpperCase());
        System.out.printf("[SISTEMA] Intervalo: %d segundos%n", intervalo);
        System.out.println("[SISTEMA] Pressione Ctrl+C para encerrar.\n");

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                System.out.println(formatter.formatarEncerramento())));

        int ciclo = 1;
        double ultimoVolume = 0;
        DadosClimaticos ultimoClima = null;

        while (true) {
            System.out.println(formatter.formatarCiclo(ciclo, LocalDateTime.now()));

            double umidade = simulador.simularLeituraProgressiva(ultimoClima, ultimoVolume);
            System.out.printf("%n[SOLO] Umidade do Solo: %.1f%%%n", umidade);

            Irrigacao resultado = cicloService.executarCiclo(cultura, umidade);
            System.out.println(formatter.formatarResultado(resultado));

            if (cicloService.getMotorRegras().getStrategyAtual() != null) {
                System.out.printf("   Estrategia utilizada: %s%n",
                        cicloService.getMotorRegras().getStrategyAtual().getNome());
            }

            ultimoVolume = resultado.getVolumeAgua();
            ciclo++;

            try {
                System.out.println(formatter.formatarAguardando(intervalo));
                Thread.sleep(intervalo * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void executarModoDemo() {
        System.out.println(formatter.formatarCabecalho(
                "SISTEMA DE IRRIGACAO INTELIGENTE - Modo: DEMONSTRACAO"));

        AppConfig config = new AppConfig();
        CicloIrrigacaoService cicloService = config.criarSistema("Joao Silva");

        System.out.println("\n+--------------------------------------------------------------+");
        System.out.println("|  CENARIO 1: Umidade baixa - Milho                            |");
        System.out.println("+--------------------------------------------------------------+");
        Irrigacao r1 = cicloService.executarCiclo("milho", 22.0);
        System.out.println(formatter.formatarResultado(r1));

        System.out.println("\n+--------------------------------------------------------------+");
        System.out.println("|  CENARIO 2: Umidade adequada - Soja                          |");
        System.out.println("+--------------------------------------------------------------+");
        Irrigacao r2 = cicloService.executarCiclo("soja", 55.0);
        System.out.println(formatter.formatarResultado(r2));

        System.out.println("\n+--------------------------------------------------------------+");
        System.out.println("|  CENARIO 3: Umidade critica - Arroz                          |");
        System.out.println("+--------------------------------------------------------------+");
        Irrigacao r3 = cicloService.executarCiclo("arroz", 20.0);
        System.out.println(formatter.formatarResultado(r3));

        System.out.println("\n==============================================================");
        System.out.println("[BANCO] CULTURAS CADASTRADAS (Dados FAO):");
        System.out.println("--------------------------------------------------------------");
        cicloService.listarCulturas().values()
                .forEach(c -> System.out.println("   " + c));

        System.out.println("\n==============================================================");
        System.out.println("[FIM] Sistema de Irrigacao Inteligente - Execucao finalizada");
        System.out.println("==============================================================");
    }
}

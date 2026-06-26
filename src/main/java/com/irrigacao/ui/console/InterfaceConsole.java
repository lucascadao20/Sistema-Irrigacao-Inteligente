package com.irrigacao.ui.console;

import com.irrigacao.negocio.ServicoDeCicloIrrigacao;
import com.irrigacao.dados.sensores.GerenciadorDeSensores;
import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;
import com.irrigacao.ConfiguracaoApp;
import com.irrigacao.dados.sensores.SimuladorSensores;

import java.time.LocalDateTime;
import java.util.Map;

public class InterfaceConsole {
    private final FormatadorDeConsole formatador;
    private final LeitorDeEntrada leitor;

    public InterfaceConsole(FormatadorDeConsole formatador, LeitorDeEntrada leitor) {
        this.formatador = formatador;
        this.leitor = leitor;
    }

    @SuppressWarnings("BusyWait")
    public void executarModoTempoReal() {
        System.out.println(formatador.formatarCabecalho(
                "SISTEMA DE IRRIGACAO INTELIGENTE - Modo: TEMPO REAL"));

        ConfiguracaoApp config = new ConfiguracaoApp();

        Map<String, Cultura> culturas = config.criarRepositorioDeCultura().listarTodas();
        System.out.println(formatador.formatarListaCulturas(culturas));

        String cultura = leitor.lerCultura(culturas);
        String agricultor = leitor.lerNomeAgricultor();
        int intervalo = leitor.lerIntervalo(60);

        ServicoDeCicloIrrigacao cicloService = config.criarSistema(agricultor);

        // Os sensores reais ja foram inicializados dentro de criarSistema(); aqui o simulador
        // serve apenas para gerar as leituras de umidade que alimentam cada ciclo.
        SimuladorSensores simulador = new SimuladorSensores(new GerenciadorDeSensores());

        System.out.printf("%n[SISTEMA] Monitorando cultura: %s%n", cultura.toUpperCase());
        System.out.printf("[SISTEMA] Intervalo: %d segundos%n", intervalo);
        System.out.println("[SISTEMA] Pressione Ctrl+C para encerrar.\n");

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                System.out.println(formatador.formatarEncerramento())));

        int ciclo = 1;
        double ultimoVolume = 0;
        DadosClimaticos ultimoClima = null;

        while (true) {
            System.out.println(formatador.formatarCiclo(ciclo, LocalDateTime.now()));

            double umidade = simulador.simularLeituraProgressiva(ultimoClima, ultimoVolume);
            System.out.printf("%n[SOLO] Umidade do Solo: %.1f%%%n", umidade);

            Irrigacao resultado = cicloService.executarCiclo(cultura, umidade);
            System.out.println(formatador.formatarResultado(resultado));

            if (cicloService.getMotorDeRegras().getEstrategiaAtual() != null) {
                System.out.printf("   Estrategia utilizada: %s%n",
                        cicloService.getMotorDeRegras().getEstrategiaAtual().getNome());
            }

            ultimoVolume = resultado.getVolumeAgua();
            ciclo++;

            try {
                System.out.println(formatador.formatarAguardando(intervalo));
                Thread.sleep(intervalo * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void executarModoDemo() {
        System.out.println(formatador.formatarCabecalho(
                "SISTEMA DE IRRIGACAO INTELIGENTE - Modo: DEMONSTRACAO"));

        ConfiguracaoApp config = new ConfiguracaoApp();
        ServicoDeCicloIrrigacao cicloService = config.criarSistema("Joao Silva");

        System.out.println("\n+--------------------------------------------------------------+");
        System.out.println("|  CENARIO 1: Umidade baixa - Milho                            |");
        System.out.println("+--------------------------------------------------------------+");
        Irrigacao r1 = cicloService.executarCiclo("milho", 22.0);
        System.out.println(formatador.formatarResultado(r1));

        System.out.println("\n+--------------------------------------------------------------+");
        System.out.println("|  CENARIO 2: Umidade adequada - Soja                          |");
        System.out.println("+--------------------------------------------------------------+");
        Irrigacao r2 = cicloService.executarCiclo("soja", 55.0);
        System.out.println(formatador.formatarResultado(r2));

        System.out.println("\n+--------------------------------------------------------------+");
        System.out.println("|  CENARIO 3: Umidade critica - Arroz                          |");
        System.out.println("+--------------------------------------------------------------+");
        Irrigacao r3 = cicloService.executarCiclo("arroz", 20.0);
        System.out.println(formatador.formatarResultado(r3));

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

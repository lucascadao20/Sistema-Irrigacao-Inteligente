package com.irrigacao.ui.console;

import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class FormatadorDeConsole {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public String formatarCabecalho(String titulo) {
        return String.format("""

            ==============================================================
               %s
            ==============================================================""", titulo);
    }

    public String formatarCiclo(int numeroCiclo, LocalDateTime timestamp) {
        return String.format("""

            +--------------------------------------------------------------+
            |  CICLO #%d - %s
            +--------------------------------------------------------------+""",
                numeroCiclo, timestamp.format(FORMATTER));
    }

    public String formatarResultado(Irrigacao irrigacao) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n[RESULTADO] DECISAO:\n");
        sb.append("   ").append(irrigacao.toString());
        return sb.toString();
    }

    public String formatarListaCulturas(Map<String, Cultura> culturas) {
        StringBuilder sb = new StringBuilder();
        sb.append("[CONFIG] Culturas disponiveis:\n");
        culturas.forEach((key, c) -> sb.append(String.format("   - %s (%s)%n", key, c.getNome())));
        return sb.toString();
    }

    public String formatarDadosClimaticos(DadosClimaticos clima) {
        return "[CLIMA] " + clima.toString();
    }

    public String formatarSensoresInicializados(int total) {
        return String.format("[SENSORES] Sensores IoT inicializados. Total: %d", total);
    }

    public String formatarAguardando(int intervaloSegundos) {
        return String.format("[AGUARDANDO] Proximo ciclo em %d segundos...", intervaloSegundos);
    }

    public String formatarEncerramento() {
        return """

            ==============================================================
            [SISTEMA] Encerrando monitoramento...
            [SISTEMA] Sistema de Irrigacao Inteligente finalizado.
            ==============================================================""";
    }
}

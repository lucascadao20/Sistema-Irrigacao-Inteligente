package com.irrigacao.infrastructure.web;

import com.irrigacao.domain.modelo.Alerta;
import com.irrigacao.domain.modelo.DadosClimaticos;
import com.irrigacao.domain.modelo.Irrigacao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class EstadoDoDashboard {
    private static final int HISTORICO_MAX = 60;
    private static final int ALERTAS_MAX = 30;

    private volatile String culturaAtiva = "milho";
    private volatile double ultimaUmidadeSolo = 50.0;
    private volatile DadosClimaticos ultimoClima;
    private volatile Irrigacao ultimaIrrigacao;
    private volatile String estrategiaAtual = "-";
    private volatile LocalDateTime atualizadoEm = LocalDateTime.now();

    private final Deque<HistoricoEntry> historico = new ConcurrentLinkedDeque<>();
    private final Deque<Alerta> alertas = new ConcurrentLinkedDeque<>();

    public synchronized void registrarCiclo(double umidadeSolo, DadosClimaticos clima,
                                            Irrigacao irrigacao, String estrategia) {
        this.ultimaUmidadeSolo = umidadeSolo;
        this.ultimoClima = clima;
        this.ultimaIrrigacao = irrigacao;
        this.estrategiaAtual = estrategia;
        this.atualizadoEm = LocalDateTime.now();

        historico.addLast(new HistoricoEntry(atualizadoEm, umidadeSolo, irrigacao.getVolumeAgua(),
                irrigacao.getStatus().name()));
        while (historico.size() > HISTORICO_MAX) historico.pollFirst();
    }

    public void registrarAlerta(Alerta alerta) {
        alertas.addLast(alerta);
        while (alertas.size() > ALERTAS_MAX) alertas.pollFirst();
    }

    public String getCulturaAtiva() { return culturaAtiva; }
    public void setCulturaAtiva(String c) { this.culturaAtiva = c; }
    public double getUltimaUmidadeSolo() { return ultimaUmidadeSolo; }
    public DadosClimaticos getUltimoClima() { return ultimoClima; }
    public Irrigacao getUltimaIrrigacao() { return ultimaIrrigacao; }
    public String getEstrategiaAtual() { return estrategiaAtual; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public List<HistoricoEntry> getHistorico() { return new ArrayList<>(historico); }
    public List<Alerta> getAlertas() {
        List<Alerta> reverso = new ArrayList<>(alertas);
        Collections.reverse(reverso);
        return reverso;
    }

    public record HistoricoEntry(LocalDateTime timestamp, double umidadeSolo,
                                 double volumeAgua, String status) {}
}

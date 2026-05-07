package com.irrigacao.application.service;

import com.irrigacao.application.factory.StrategyFactory;
import com.irrigacao.application.port.NotificadorAlerta;
import com.irrigacao.domain.enums.NivelAlerta;
import com.irrigacao.domain.enums.StatusIrrigacao;
import com.irrigacao.domain.model.Alerta;
import com.irrigacao.domain.model.Cultura;
import com.irrigacao.domain.model.DadosClimaticos;
import com.irrigacao.domain.model.Irrigacao;
import com.irrigacao.domain.strategy.IrrigacaoStrategy;

import java.util.UUID;

public class MotorRegras {
    private final StrategyFactory factory;
    private final NotificadorAlerta notificador;
    private IrrigacaoStrategy strategyAtual;

    public MotorRegras(StrategyFactory factory, NotificadorAlerta notificador) {
        this.factory = factory;
        this.notificador = notificador;
    }

    public Irrigacao avaliarEExecutar(double umidadeSolo, DadosClimaticos clima, Cultura cultura) {
        strategyAtual = factory.criar(umidadeSolo, clima, cultura);

        if (strategyAtual == null) {
            emitirAlerta(NivelAlerta.INFO,
                    String.format("Umidade adequada: %.0f%% - Nenhuma acao necessaria", umidadeSolo));
            return Irrigacao.builder()
                    .id(gerarId())
                    .cultura(cultura)
                    .status(StatusIrrigacao.AGUARDANDO)
                    .volumeAgua(0)
                    .motivo("Umidade do solo adequada - aguardando proximo ciclo")
                    .build();
        }

        emitirAlertaPreStrategy(umidadeSolo, clima, cultura);

        Irrigacao irrigacao = strategyAtual.calcularIrrigacao(umidadeSolo, clima, cultura);

        Irrigacao resultado = Irrigacao.builder()
                .id(gerarId())
                .cultura(irrigacao.getCultura())
                .status(irrigacao.getStatus())
                .volumeAgua(irrigacao.getVolumeAgua())
                .motivo(irrigacao.getMotivo())
                .build();

        emitirAlertaPosStrategy(resultado, cultura);

        return resultado;
    }

    private void emitirAlertaPreStrategy(double umidadeSolo, DadosClimaticos clima, Cultura cultura) {
        if (umidadeSolo < cultura.getUmidadeMinima() * 0.7) {
            emitirAlerta(NivelAlerta.EMERGENCIA,
                    String.format("Umidade do solo CRITICA: %.0f%% (minimo: %.0f%%) para %s",
                            umidadeSolo, cultura.getUmidadeMinima(), cultura.getNome()));
        } else if (clima.isPrevisaoChuva()) {
            emitirAlerta(NivelAlerta.INFO,
                    String.format("Previsao de chuva detectada (%.1f mm) - Avaliando suspensao", clima.getVolumeChuva()));
        } else if (umidadeSolo < cultura.getUmidadeMinima()) {
            emitirAlerta(NivelAlerta.AVISO,
                    String.format("Umidade abaixo do minimo: %.0f%% (minimo: %.0f%%) - Irrigacao necessaria",
                            umidadeSolo, cultura.getUmidadeMinima()));
        }
    }

    private void emitirAlertaPosStrategy(Irrigacao irrigacao, Cultura cultura) {
        if (irrigacao.getStatus() == StatusIrrigacao.ATIVADA) {
            emitirAlerta(NivelAlerta.CRITICO,
                    String.format("IRRIGACAO ATIVADA: %.1fL para %s | Estrategia: %s",
                            irrigacao.getVolumeAgua(), cultura.getNome(), strategyAtual.getNome()));
        } else if (irrigacao.getStatus() == StatusIrrigacao.SUSPENSA) {
            emitirAlerta(NivelAlerta.INFO,
                    String.format("Irrigacao SUSPENSA para %s - %s", cultura.getNome(), irrigacao.getMotivo()));
        }
    }

    private void emitirAlerta(NivelAlerta nivel, String mensagem) {
        Alerta alerta = new Alerta(gerarId(), nivel, mensagem);
        notificador.notificar(alerta);
    }

    private String gerarId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public IrrigacaoStrategy getStrategyAtual() {
        return strategyAtual;
    }
}

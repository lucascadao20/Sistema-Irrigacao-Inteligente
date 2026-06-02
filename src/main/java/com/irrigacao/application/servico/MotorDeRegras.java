package com.irrigacao.application.servico;

import com.irrigacao.application.fabrica.FabricaDeEstrategia;
import com.irrigacao.application.interfaces.NotificadorDeAlerta;
import com.irrigacao.modelo.NivelAlerta;
import com.irrigacao.modelo.StatusIrrigacao;
import com.irrigacao.modelo.Alerta;
import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;
import com.irrigacao.domain.estrategia.EstrategiaDeIrrigacao;

import java.util.UUID;

public class MotorDeRegras {
    private final FabricaDeEstrategia fabrica;
    private final NotificadorDeAlerta notificador;
    private EstrategiaDeIrrigacao estrategiaAtual;

    public MotorDeRegras(FabricaDeEstrategia fabrica, NotificadorDeAlerta notificador) {
        this.fabrica = fabrica;
        this.notificador = notificador;
    }

    public Irrigacao avaliarEExecutar(double umidadeSolo, DadosClimaticos clima, Cultura cultura) {
        estrategiaAtual = fabrica.criar(umidadeSolo, clima, cultura);

        if (estrategiaAtual == null) {
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

        emitirAlertaPreEstrategia(umidadeSolo, clima, cultura);

        Irrigacao irrigacao = estrategiaAtual.calcularIrrigacao(umidadeSolo, clima, cultura);

        Irrigacao resultado = Irrigacao.builder()
                .id(gerarId())
                .cultura(irrigacao.getCultura())
                .status(irrigacao.getStatus())
                .volumeAgua(irrigacao.getVolumeAgua())
                .motivo(irrigacao.getMotivo())
                .build();

        emitirAlertaPosEstrategia(resultado, cultura);

        return resultado;
    }

    private void emitirAlertaPreEstrategia(double umidadeSolo, DadosClimaticos clima, Cultura cultura) {
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

    private void emitirAlertaPosEstrategia(Irrigacao irrigacao, Cultura cultura) {
        if (irrigacao.getStatus() == StatusIrrigacao.ATIVADA) {
            emitirAlerta(NivelAlerta.CRITICO,
                    String.format("IRRIGACAO ATIVADA: %.1fL para %s | Estrategia: %s",
                            irrigacao.getVolumeAgua(), cultura.getNome(), estrategiaAtual.getNome()));
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

    public EstrategiaDeIrrigacao getEstrategiaAtual() {
        return estrategiaAtual;
    }
}

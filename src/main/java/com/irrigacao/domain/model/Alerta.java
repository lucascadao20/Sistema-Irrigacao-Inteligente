package com.irrigacao.domain.model;

import com.irrigacao.domain.enums.NivelAlerta;
import java.time.LocalDateTime;

public class Alerta {
    private final String id;
    private final NivelAlerta nivel;
    private final String mensagem;
    private final LocalDateTime timestamp;
    private boolean lido;

    public Alerta(String id, NivelAlerta nivel, String mensagem) {
        this.id = id;
        this.nivel = nivel;
        this.mensagem = mensagem;
        this.timestamp = LocalDateTime.now();
        this.lido = false;
    }

    public String getId() { return id; }
    public NivelAlerta getNivel() { return nivel; }
    public String getMensagem() { return mensagem; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isLido() { return lido; }
    public void marcarComoLido() { this.lido = true; }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", nivel.getDescricao(), timestamp.toLocalTime(), mensagem);
    }
}

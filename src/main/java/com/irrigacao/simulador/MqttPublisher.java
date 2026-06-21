package com.irrigacao.simulador;

public interface MqttPublisher {
    void publicar(String topico, String payload);
    void desconectar();
}

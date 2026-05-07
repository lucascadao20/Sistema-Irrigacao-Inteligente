package com.irrigacao.infrastructure.api;

import com.irrigacao.application.port.ClimaService;
import com.irrigacao.domain.model.DadosClimaticos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class OpenWeatherMapClimaService implements ClimaService {
    private static final Logger logger = LoggerFactory.getLogger(OpenWeatherMapClimaService.class);

    private final OpenWeatherMapClient client;
    private DadosClimaticos cache;

    public OpenWeatherMapClimaService(OpenWeatherMapClient client) {
        this.client = client;
    }

    @Override
    public DadosClimaticos obterDados(String cidade, String pais) {
        try {
            logger.info("Consultando OpenWeatherMap para {}, {}", cidade, pais);
            DadosClimaticos clima = client.buscarClima(cidade, pais);

            boolean previsaoChuva = clima.isPrevisaoChuva() || client.verificarPrevisaoChuva(cidade, pais);
            double volumeChuva = clima.getVolumeChuva();
            if (volumeChuva == 0 && previsaoChuva) {
                volumeChuva = client.getVolumeChuvaPrevisao(cidade, pais);
            }

            cache = DadosClimaticos.builder()
                    .temperatura(clima.getTemperatura())
                    .umidadeAr(clima.getUmidadeAr())
                    .velocidadeVento(clima.getVelocidadeVento())
                    .descricaoClima(clima.getDescricaoClima())
                    .previsaoChuva(previsaoChuva)
                    .volumeChuva(volumeChuva)
                    .cidade(cidade)
                    .build();

            return cache;
        } catch (IOException | InterruptedException e) {
            logger.warn("Erro ao consultar API: {}. Usando cache.", e.getMessage());

            if (cache != null) {
                return cache;
            }

            return DadosClimaticos.builder()
                    .temperatura(25.0)
                    .umidadeAr(50.0)
                    .velocidadeVento(5.0)
                    .descricaoClima("indisponivel")
                    .previsaoChuva(false)
                    .volumeChuva(0)
                    .cidade(cidade)
                    .build();
        }
    }
}

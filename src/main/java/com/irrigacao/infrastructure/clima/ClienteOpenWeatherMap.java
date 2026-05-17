package com.irrigacao.infrastructure.clima;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.irrigacao.domain.modelo.DadosClimaticos;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ClienteOpenWeatherMap {
    private final String apiKey;
    private final String baseUrl;
    private final HttpClient httpClient;

    public ClienteOpenWeatherMap(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public DadosClimaticos buscarClima(String cidade, String pais) throws IOException, InterruptedException {
        String url = String.format("%s/weather?q=%s,%s&appid=%s&units=metric&lang=pt_br",
                baseUrl, cidade.replace(" ", "%20"), pais, apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Erro na API OpenWeatherMap: HTTP " + response.statusCode());
        }

        return parseClimaAtual(response.body(), cidade);
    }

    public boolean verificarPrevisaoChuva(String cidade, String pais) throws IOException, InterruptedException {
        String url = String.format("%s/forecast?q=%s,%s&appid=%s&units=metric&cnt=8&lang=pt_br",
                baseUrl, cidade.replace(" ", "%20"), pais, apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return false;
        }

        return parsePrevisaoChuva(response.body());
    }

    public double getVolumeChuvaPrevisao(String cidade, String pais) throws IOException, InterruptedException {
        String url = String.format("%s/forecast?q=%s,%s&appid=%s&units=metric&cnt=8&lang=pt_br",
                baseUrl, cidade.replace(" ", "%20"), pais, apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return 0.0;
        }

        return parseVolumeChuva(response.body());
    }

    private DadosClimaticos parseClimaAtual(String json, String cidade) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        JsonObject main = root.getAsJsonObject("main");
        double temperatura = main.get("temp").getAsDouble();
        double umidadeAr = main.get("humidity").getAsDouble();

        JsonObject wind = root.getAsJsonObject("wind");
        double velocidadeVento = wind.get("speed").getAsDouble();

        JsonArray weatherArray = root.getAsJsonArray("weather");
        String descricao = weatherArray.get(0).getAsJsonObject().get("description").getAsString();

        boolean chovendo = root.has("rain");
        double volumeChuva = 0;
        if (chovendo) {
            JsonObject rain = root.getAsJsonObject("rain");
            if (rain.has("1h")) {
                volumeChuva = rain.get("1h").getAsDouble();
            } else if (rain.has("3h")) {
                volumeChuva = rain.get("3h").getAsDouble();
            }
        }

        return DadosClimaticos.builder()
                .temperatura(temperatura)
                .umidadeAr(umidadeAr)
                .velocidadeVento(velocidadeVento)
                .descricaoClima(descricao)
                .previsaoChuva(chovendo)
                .volumeChuva(volumeChuva)
                .cidade(cidade)
                .build();
    }

    private boolean parsePrevisaoChuva(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray list = root.getAsJsonArray("list");

        for (int i = 0; i < list.size(); i++) {
            JsonObject item = list.get(i).getAsJsonObject();
            if (item.has("rain")) {
                return true;
            }
            JsonArray weather = item.getAsJsonArray("weather");
            String mainWeather = weather.get(0).getAsJsonObject().get("main").getAsString().toLowerCase();
            if (mainWeather.contains("rain") || mainWeather.contains("drizzle") || mainWeather.contains("thunderstorm")) {
                return true;
            }
        }
        return false;
    }

    private double parseVolumeChuva(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray list = root.getAsJsonArray("list");
        double totalChuva = 0;

        for (int i = 0; i < list.size(); i++) {
            JsonObject item = list.get(i).getAsJsonObject();
            if (item.has("rain")) {
                JsonObject rain = item.getAsJsonObject("rain");
                if (rain.has("3h")) {
                    totalChuva += rain.get("3h").getAsDouble();
                }
            }
        }
        return totalChuva;
    }
}

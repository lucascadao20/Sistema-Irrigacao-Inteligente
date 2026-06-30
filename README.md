# Sistema de Irrigação Inteligente

Sistema Java que automatiza decisões de irrigação combinando leituras de sensores IoT publicadas via MQTT, dados climáticos reais da API do OpenWeatherMap e regras agronômicas da FAO. Cada cultura é tratada como um talhão independente, com histórico de decisões persistido em banco H2.

Roda em **três modos**: dashboard web em tempo real, console interativo e demonstração com cenários pré-definidos.

Alunos: Filipe Andrade, Lucas Goncalves, Lucas Jesus

## Funcionalidades

- Coleta de leituras de sensores IoT (umidade do solo, temperatura, umidade do ar, pH) via **MQTT**
- **Umidade do solo independente por cultura** (10 talhões simulados, cada um com sua trajetória)
- Decisão automática entre três estratégias: **Emergencial**, **Seco** e **Úmido**
- Cálculo de volume de água baseado em coeficiente de cultura (Kc) e dados FAO
- Sistema de alertas multi-canal (console + log + dashboard web)
- **Persistência H2** de leituras e decisões — base para os relatórios de consumo
- **Relatório de consumo por cultura e período** — via dashboard ou API REST
- Catálogo de 10 culturas (milho, soja, arroz, feijão, trigo, café, cana, algodão, tomate, alface)
- Recuperação automática a falhas de broker MQTT, API de clima e payloads inválidos

## Stack

- Java 17, Maven
- **Eclipse Paho 1.2.5** (cliente MQTT)
- **Mosquitto** via Docker (broker MQTT)
- **H2 2.2.224** (banco embarcado, modo arquivo)
- Javalin 6.1.6 (servidor web embutido)
- Gson 2.10.1 (JSON)
- SLF4J 2.0.9 + Logback 1.4.14 (logging)
- Chart.js 4.4 (frontend, via CDN)
- JUnit 5.10.1 (testes)

## Arquitetura

Arquitetura em 4 camadas com dependências fluindo de fora para dentro:

```
ui  →  negocio  →  dados  →  modelo
```

| Camada | Responsabilidade |
|---|---|
| **modelo** | Entidades e enums do domínio. Não depende de ninguém |
| **negocio** | Regras de irrigação — estratégias, motor de regras, serviço de ciclo |
| **dados** | Fontes externas e persistência — MQTT, OpenWeatherMap, H2, sensores |
| **ui** | Apresentação — console e dashboard web |

```
com.irrigacao
├── Main.java
├── ConfiguracaoApp.java          montagem (injeção de dependências manual)
├── modelo/                       entidades + enums
├── negocio/                      regras
│   ├── EstrategiaDeIrrigacao + 3 implementações (Emergencial/Seco/Umido)
│   ├── FabricaDeEstrategia · MotorDeRegras · ServicoDeCicloIrrigacao
├── dados/                        fontes externas e persistência
│   ├── bd/                       repositórios H2 + ConexaoH2
│   ├── clima/                    OpenWeatherMap (HTTP)
│   ├── cultura/                  catálogo FAO
│   ├── mqtt/                     coletor + cache de leituras
│   ├── notificacao/              alertas (console / log / web / composto)
│   └── sensores/                 gerenciador, processador, simulador local
├── simulador/                    processo publicador MQTT (separado)
│   ├── PublisherMain · GeradorLeituraProgressivo
│   ├── AgendadorPublicacao · PublicadorMqtt · MqttPublisher
└── ui/
    ├── console/                  CLI interativa
    └── web/                      Javalin + dashboard
```

Frontend estático em `src/main/resources/static/` (`index.html`, `app.js`, `style.css`).

## Documentação adicional

- [`docs/requisitos.md`](docs/requisitos.md) — requisitos funcionais e não-funcionais
- [`docs/arquitetura.svg`](docs/arquitetura.svg) — diagrama em camadas
- [`docs/simulador.svg`](docs/simulador.svg) — padrão Publish/Subscribe com MQTT
- [`docs/calculo-irrigacao.md`](docs/calculo-irrigacao.md) — detalhamento da lógica de decisão e cálculo de volume

## Design Patterns

| Pattern | Aplicação |
|---|---|
| Strategy | `EstrategiaDeIrrigacao` + 3 modos |
| Factory Method | `FabricaDeEstrategia` |
| Builder | `Irrigacao.builder()`, `DadosClimaticos.builder()` |
| Composite | `NotificadorComposto` despacha para múltiplos destinos |
| Repository | `RepositorioDeCultura`, `RepositorioDeIrrigacao`, `RepositorioDeLeituraSensor` |
| Adapter | `ServicoDeClimaOpenWeatherMap` adapta API externa à interface interna |
| Facade | `ServicoDeCicloIrrigacao` simplifica uso do sistema |
| Publish/Subscribe | Sensores publicam em tópicos MQTT; dashboard assina via wildcard |

## Pré-requisitos

- Java 17+, Maven 3.8+
- **Docker** (para o broker Mosquitto, necessário no modo `--web`)
- Chave de API do [OpenWeatherMap](https://openweathermap.org/api) (gratuita) — sem chave, o sistema usa clima padrão de fallback

## Configuração

```powershell
copy src\main\resources\config.properties.example src\main\resources\config.properties
```

Edite `src/main/resources/config.properties` e preencha sua chave OpenWeatherMap. As chaves `mqtt.*` já vêm com defaults adequados para o broker local.

## Como executar

> **No PowerShell, sempre coloque o argumento `-Dexec.args` entre aspas** — o shell consome aspas internas. Use `"-Dexec.args=--demo"` ou o stop-parsing token `--%`.

### Modo dashboard web (recomendado — distribuído via MQTT)

Requer **3 terminais** (broker + publisher + dashboard):

**Terminal 1 — broker MQTT:**
```bash
docker compose up -d
```

**Terminal 2 — publicador (faz o papel dos sensores IoT):**
```bash
mvn compile
mvn exec:java "-Dexec.mainClass=com.irrigacao.simulador.PublisherMain"
```

**Terminal 3 — dashboard web:**
```bash
mvn exec:java "-Dexec.args=--web"
```

Abra **http://localhost:7070**. Para outra porta: `"-Dexec.args=--web 8080"`.

### Modo demonstração

```bash
mvn exec:java "-Dexec.args=--demo"
```

Roda 3 cenários pré-definidos (umidade baixa, adequada e crítica). Não usa MQTT — gera dados internamente com `SimuladorSensores`.

### Modo tempo real (console)

```bash
mvn exec:java
```

CLI interativa que pede cultura, nome do agricultor e intervalo. Roda em loop até `Ctrl+C`. Não usa MQTT.

### Via JAR empacotado

```bash
mvn package -DskipTests
java -jar target/sistema-irrigacao-inteligente-1.0.0.jar --web
```

## Arquitetura do modo web (Pub/Sub via MQTT)

```
[Publicador]  ──MQTT publish──→  [Mosquitto]  ──MQTT subscribe──→  [Dashboard]
 (PublisherMain)                    Docker                          (ServidorWeb)
 publica 13 tópicos               localhost:1883                  assina via wildcard
  · 10 culturas × umidade_solo                                      · cache por cultura
  · 3 globais (temp/ar/pH)                                          · ciclo a cada 30 s
                                                                    · persiste em H2
```

**Componentes:**

- **Broker:** Mosquitto via [docker-compose.yml](docker-compose.yml).
- **Publicador:** [`com.irrigacao.simulador.PublisherMain`](src/main/java/com/irrigacao/simulador/PublisherMain.java) — processo Java separado que mantém estado independente por cultura e publica a cada 5 s.
- **Assinante:** [`ColetorMqttSensores`](src/main/java/com/irrigacao/dados/mqtt/ColetorMqttSensores.java) — integrado ao modo `--web`, assina `irrigacao/sensores/+/umidade_solo` (wildcard) + 3 tópicos fixos.
- **Cache:** [`EstadoUltimasLeituras`](src/main/java/com/irrigacao/dados/mqtt/EstadoUltimasLeituras.java) — chave composta `(tipo, cultura)`; thread-safe via `ConcurrentHashMap`.

**Observações:**

- Mosquitto roda sem autenticação e sem TLS — **uso local apenas**. Não exponha a porta 1883.
- Se o broker subir **depois** do dashboard, reinicie o dashboard — `setAutomaticReconnect` do Paho só reconecta depois de uma conexão inicial bem-sucedida.
- Encerrar tudo: `Ctrl+C` nos terminais 2 e 3 e `docker compose down`.

## Persistência (H2 embarcado)

Banco H2 em arquivo `data/irrigacao.mv.db` (gitignored). Schema em [`src/main/resources/schema.sql`](src/main/resources/schema.sql), aplicado idempotentemente no startup.

| Tabela | Gravada por | Conteúdo |
|---|---|---|
| `leitura_sensor` | `ColetorMqttSensores` a cada mensagem MQTT | sensor_id, tipo, valor, valida, recebido_em |
| `irrigacao` | `ServicoDeCicloIrrigacao` ao final de cada ciclo | id, cultura_nome, status, volume_agua, motivo, estrategia_nome, umidade_solo, decidido_em |

Para alterar o schema, apague o arquivo do banco e deixe o app recriar — não há migrações versionadas (Flyway/Liquibase) por design.

## API REST

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/state` | Snapshot do dashboard (cultura ativa, clima, irrigação, histórico, alertas) |
| `GET` | `/api/culturas` | Lista de culturas com parâmetros agronômicos |
| `POST` | `/api/cultura` | Troca a cultura ativa. Body: `{ "cultura": "soja" }` |
| `GET` | `/api/relatorios/consumo` | Relatório de consumo. Query: `cultura` (opcional), `inicio`, `fim` em `ISO_LOCAL_DATE_TIME`. Defaults: últimos 7 dias, todas as culturas |

O frontend faz polling em `/api/state` a cada 3 s.

## Testes

```bash
mvn test
```

**98 testes unitários** cobrindo todas as camadas (modelo, negocio, dados, ui, simulador). Repositórios H2 são testados contra banco em memória (`jdbc:h2:mem:...`). O coletor MQTT é testado sem broker via handler `processarMensagem` extraído.

## Build

```bash
mvn clean package
```

Gera `target/sistema-irrigacao-inteligente-1.0.0.jar` (fat-jar com dependências, executável diretamente).

## Logs

Em `logs/irrigacao.log` (rotação diária, retenção de 30 dias). Configuração em [`logback.xml`](src/main/resources/logback.xml).

## Lógica de decisão

A `FabricaDeEstrategia` seleciona a estratégia na seguinte ordem — **solo tem prioridade sobre clima**:

1. **Emergencial** — umidade do solo < 70% da mínima da cultura
2. **Seco** — umidade do solo < mínima da cultura
3. **Úmido** — umidade adequada, mas previsão de chuva ou umidade do ar > 80%
4. **Aguardando** — umidade adequada e clima sem chuva, nenhuma ação

Cada estratégia calcula o volume de água considerando déficit hídrico, coeficiente de cultura (Kc) e ajustes pela situação. Detalhamento em [`docs/calculo-irrigacao.md`](docs/calculo-irrigacao.md).

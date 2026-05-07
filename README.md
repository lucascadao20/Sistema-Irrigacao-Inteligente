# Sistema de Irrigação Inteligente

Sistema Java que automatiza decisões de irrigação combinando dados de sensores simulados (umidade do solo, temperatura, umidade do ar, pH) com dados climáticos reais consumidos da API do OpenWeatherMap.

## Funcionalidades

- Leitura e validação de dados de sensores IoT (com simulador progressivo)
- Consulta de clima em tempo real via OpenWeatherMap (com cache em caso de falha)
- Decisão automática de irrigação por meio de três estratégias: **Modo Seco**, **Modo Úmido**, **Modo Emergencial**
- Cálculo de volume de água com base em coeficiente de cultura (Kc) e dados FAO
- Sistema de alertas em múltiplos canais (console + log)
- Catálogo de 10 culturas pré-cadastradas (milho, soja, arroz, feijão, trigo, café, cana, algodão, tomate, alface)
- Dois modos de execução: **demonstração** (cenários pré-definidos) e **tempo real** (loop contínuo)

## Stack

- Java 17
- Maven
- Gson 2.10.1 (parsing JSON)
- SLF4J 2.0.9 + Logback 1.4.14 (logging)
- JUnit 5.10.1 (testes)

## Arquitetura

Layered Architecture com 4 camadas e dependências fluindo de fora para dentro:

```
presentation  →  application  →  domain
                      ↑
              infrastructure
```

```
com.irrigacao
├── Main.java
├── domain/                  Entidades puras + regras de cálculo
│   ├── model/               Sensor, Cultura, Irrigacao, DadosClimaticos, Alerta, LeituraSensor
│   ├── enums/               TipoSensor, StatusIrrigacao, NivelAlerta
│   └── strategy/            IrrigacaoStrategy + 3 modos
├── application/             Orquestração de negócio
│   ├── service/             CicloIrrigacaoService, MotorRegras, GerenciadorSensores, ProcessadorDados
│   ├── port/                ClimaService, CulturaRepository, NotificadorAlerta (interfaces)
│   └── factory/             StrategyFactory
├── infrastructure/          Implementações concretas
│   ├── api/                 OpenWeatherMapClient + OpenWeatherMapClimaService (Adapter)
│   ├── persistence/         InMemoryCulturaRepository
│   ├── notification/        ConsoleNotificadorAlerta, LogNotificadorAlerta, CompositeNotificador
│   ├── config/              AppConfig (injeção de dependências manual)
│   └── simulator/           SimuladorSensores
└── presentation/            Interface de usuário
    └── console/             ConsoleUI, ConsoleFormatter, ConsoleInputHandler
```

## Design Patterns

| Pattern | Aplicação |
|---------|-----------|
| Strategy | `IrrigacaoStrategy` + 3 modos de irrigação |
| Factory Method | `StrategyFactory` seleciona estratégia conforme contexto |
| Builder | `Irrigacao.builder()`, `DadosClimaticos.builder()` |
| Composite | `CompositeNotificador` despacha alertas para múltiplos destinos |
| Repository | `CulturaRepository` / `InMemoryCulturaRepository` |
| Adapter | `OpenWeatherMapClimaService` adapta API externa ao port interno |
| Facade | `CicloIrrigacaoService` simplifica uso do sistema |

## Pré-requisitos

- Java 17+
- Maven 3.8+
- Chave de API do [OpenWeatherMap](https://openweathermap.org/api) (gratuita)

## Configuração

Copie o arquivo de exemplo e preencha com sua chave:

```powershell
copy src\main\resources\config.properties.example src\main\resources\config.properties
```

Edite `src/main/resources/config.properties`:

```properties
openweathermap.api.key=SUA_CHAVE_AQUI
openweathermap.base.url=https://api.openweathermap.org/data/2.5
cidade.padrao=Sao Paulo
cidade.pais=BR
```

> Sem chave válida, o sistema continua funcionando usando dados climáticos padrão de fallback.

## Como executar

### Modo demonstração

Roda 3 cenários pré-definidos (umidade baixa, adequada e crítica):

```powershell
mvn exec:java "-Dexec.args=--demo"
```

### Modo tempo real

Pede cultura, nome do agricultor e intervalo entre ciclos. Roda em loop até `Ctrl+C`:

```powershell
mvn exec:java
```

### Via JAR empacotado

```powershell
mvn package -DskipTests
java -jar target/sistema-irrigacao-inteligente-1.0.0.jar --demo
```

## Testes

```powershell
mvn test
```

Cobertura: 44 testes unitários cobrindo domain, application e infrastructure.

## Build

```powershell
mvn clean package
```

Gera `target/sistema-irrigacao-inteligente-1.0.0.jar`.

## Logs

Logs são gravados em `logs/irrigacao.log` (rotação diária, retenção de 30 dias). Configuração em [logback.xml](src/main/resources/logback.xml).

## Lógica de decisão

A `StrategyFactory` seleciona a estratégia conforme o estado:

1. **Emergencial** — umidade do solo < 70% do mínimo da cultura
2. **Úmido** — previsão de chuva ou umidade do ar > 80%
3. **Seco** — umidade do solo abaixo do mínimo (sem chuva)
4. **Aguardando** — umidade adequada, nenhuma ação necessária

Cada estratégia calcula o volume de água considerando déficit hídrico, coeficiente de cultura e ajustes por temperatura.

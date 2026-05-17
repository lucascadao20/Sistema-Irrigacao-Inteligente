# Sistema de Irrigação Inteligente

Sistema Java que automatiza decisões de irrigação combinando dados de sensores simulados (umidade do solo, temperatura, umidade do ar, pH) com dados climáticos reais consumidos da API do OpenWeatherMap. Roda em modo console ou em modo **dashboard web** com visualização em tempo real.

## Funcionalidades

- Leitura e validação de dados de sensores IoT (com simulador progressivo)
- Consulta de clima em tempo real via OpenWeatherMap (com cache em caso de falha)
- Decisão automática de irrigação por meio de três estratégias: **Modo Seco**, **Modo Úmido**, **Modo Emergencial**
- Cálculo de volume de água com base em coeficiente de cultura (Kc) e dados FAO
- Sistema de alertas em múltiplos canais (console + log + dashboard web)
- Catálogo de 10 culturas pré-cadastradas (milho, soja, arroz, feijão, trigo, café, cana, algodão, tomate, alface)
- **Três modos de execução**:
  - **Demonstração** (`--demo`) — cenários pré-definidos
  - **Tempo real** — loop interativo no console
  - **Web** (`--web`) — dashboard HTTP com gauge animado, gráfico histórico e feed de alertas

## Stack

- Java 17
- Maven
- Gson 2.10.1 (parsing JSON, mapper do Javalin)
- SLF4J 2.0.9 + Logback 1.4.14 (logging)
- **Javalin 6.1.6** (servidor web embutido)
- Chart.js 4.4 (frontend, via CDN)
- Inter + JetBrains Mono (Google Fonts)
- JUnit 5.10.1 (testes)

## Arquitetura

Arquitetura em 3 camadas com dependências fluindo de fora para dentro:

```
infrastructure  →  application  →  domain
```

| Camada | Responsabilidade |
|--------|-----------------|
| **domain** | O QUE o sistema É — entidades puras, regras de cálculo e estratégias |
| **application** | O QUE o sistema FAZ — orquestração de casos de uso e interfaces (contratos) |
| **infrastructure** | COMO o sistema se conecta ao mundo — adaptadores de entrada e saída |

```
com.irrigacao
├── Main.java
├── domain/                     Entidades puras + regras de cálculo
│   ├── modelo/                 Sensor, Cultura, Irrigacao, DadosClimaticos, Alerta, LeituraSensor
│   ├── enums/                  TipoSensor, StatusIrrigacao, NivelAlerta
│   └── estrategia/            EstrategiaDeIrrigacao + 3 modos
├── application/                Orquestração de negócio
│   ├── servico/                ServicoDeCicloIrrigacao, MotorDeRegras, GerenciadorDeSensores, ProcessadorDeDados
│   ├── interfaces/             ServicoDeClima, RepositorioDeCultura, NotificadorDeAlerta
│   └── fabrica/                FabricaDeEstrategia
└── infrastructure/             Adaptadores com o mundo externo
    ├── clima/                  ClienteOpenWeatherMap + ServicoDeClimaOpenWeatherMap
    ├── persistencia/           RepositorioDeCulturaEmMemoria
    ├── notificacao/            NotificadorDeAlertaConsole, NotificadorDeAlertaLog, NotificadorComposto
    ├── simulador/              SimuladorSensores
    ├── config/                 ConfiguracaoApp (injeção de dependências manual)
    ├── console/                InterfaceConsole, FormatadorDeConsole, LeitorDeEntrada
    └── web/                    ServidorWeb, EstadoDoDashboard, ExecutorDeSimulacao, NotificadorDeAlertaWeb
```

Frontend estático em `src/main/resources/static/` (`index.html`, `app.js`, `style.css`).

## Design Patterns

| Pattern | Aplicação |
|---------|-----------|
| Strategy | `EstrategiaDeIrrigacao` + 3 modos de irrigação |
| Factory Method | `FabricaDeEstrategia` seleciona estratégia conforme contexto |
| Builder | `Irrigacao.builder()`, `DadosClimaticos.builder()` |
| Composite | `NotificadorComposto` despacha alertas para múltiplos destinos |
| Repository | `RepositorioDeCultura` / `RepositorioDeCulturaEmMemoria` |
| Adapter | `ServicoDeClimaOpenWeatherMap` adapta API externa à interface interna |
| Facade | `ServicoDeCicloIrrigacao` simplifica uso do sistema |

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

> **No PowerShell, sempre coloque o argumento `-Dexec.args` entre aspas** — o shell consome aspas internas. Use `"-Dexec.args=--demo"` ou o stop-parsing token `--%`.

### Modo dashboard web (recomendado)

Abre um servidor HTTP local com simulação automática rodando a cada 5s:

```powershell
mvn exec:java "-Dexec.args=--web"
```

Acesse **http://localhost:7070**. 

Para usar outra porta:

```powershell
mvn exec:java "-Dexec.args=--web 8080"
```

O dashboard mostra:
- Gauge circular animado de umidade do solo (verde / amarelo / laranja / vermelho conforme faixa)
- Status atual da irrigação (ATIVADA / SUSPENSA / AGUARDANDO) com badge colorido
- Estratégia ativa em destaque
- Card de clima com emoji dinâmico, temperatura, vento, umidade do ar e previsão de chuva
- Gráfico de linha com histórico de umidade e volume de água (Chart.js)
- Feed de alertas em tempo real com cores por nível (INFO / AVISO / CRITICO / EMERGENCIA)
- Seletor de cultura ao vivo (troca não reinicia o simulador)
- Indicador de conexão pulsante e relógio em monospace

### Modo demonstração

Roda 3 cenários pré-definidos (umidade baixa, adequada e crítica):

```powershell
mvn exec:java "-Dexec.args=--demo"
```

### Modo tempo real (console)

Pede cultura, nome do agricultor e intervalo entre ciclos. Roda em loop até `Ctrl+C`:

```powershell
mvn exec:java
```

### Via JAR empacotado

```powershell
mvn package -DskipTests
java -jar target/sistema-irrigacao-inteligente-1.0.0.jar --web
java -jar target/sistema-irrigacao-inteligente-1.0.0.jar --demo
```

## API REST (modo web)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/state` | Snapshot completo (clima, irrigação, histórico, alertas) |
| GET | `/api/culturas` | Lista de culturas com parâmetros agronômicos |
| POST | `/api/cultura` | Troca a cultura ativa. Body: `{ "cultura": "soja" }` |

O frontend faz polling em `/api/state` a cada 3s.

## Testes

```powershell
mvn test
```

Cobertura: **44 testes unitários** cobrindo domain, application e infrastructure.

## Build

```powershell
mvn clean package
```

Gera `target/sistema-irrigacao-inteligente-1.0.0.jar`.

## Logs

Logs são gravados em `logs/irrigacao.log` (rotação diária, retenção de 30 dias). Configuração em [logback.xml](src/main/resources/logback.xml).

## Lógica de decisão

A `FabricaDeEstrategia` seleciona a estratégia conforme o estado:

1. **Emergencial** — umidade do solo < 70% do mínimo da cultura
2. **Úmido** — previsão de chuva ou umidade do ar > 80%
3. **Seco** — umidade do solo abaixo do mínimo (sem chuva)
4. **Aguardando** — umidade adequada, nenhuma ação necessária

Cada estratégia calcula o volume de água considerando déficit hídrico, coeficiente de cultura e ajustes por temperatura.

## Estrutura do dashboard web

```
src/main/resources/static/
├── index.html       Layout responsivo com hero, cards, gauge SVG e gráfico
├── style.css        Variáveis CSS, paleta verde/azul, animações suaves
└── app.js           Polling + Chart.js + atualização incremental do gauge
```

O servidor (`ServidorWeb`) usa Gson como `JsonMapper` do Javalin para serializar respostas. A simulação roda em thread daemon (`ExecutorDeSimulacao`) que dispara um ciclo a cada 5 segundos e atualiza o `EstadoDoDashboard` (estado em memória, thread-safe via `ConcurrentLinkedDeque`). Alertas são empurrados ao estado por `NotificadorDeAlertaWeb`, que implementa a interface `NotificadorDeAlerta` e é combinado com `NotificadorDeAlertaLog` via `NotificadorComposto`.

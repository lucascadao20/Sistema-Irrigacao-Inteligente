# Requisitos do Sistema de Irrigação Inteligente

## Visão geral

Sistema acadêmico que automatiza decisões de irrigação para múltiplas culturas
agrícolas com base em leituras de sensores IoT simulados e dados climáticos
reais. Cada cultura é tratada como um talhão independente, com sua própria
trajetória de umidade do solo e histórico de decisões persistido em banco.

---

## Requisitos funcionais

### RF01 — Cadastro de culturas
O sistema deve manter o cadastro de 10 culturas (milho, soja, arroz, feijão,
trigo, café, cana, algodão, tomate, alface), cada uma com parâmetros
agronômicos da FAO: umidade mínima, umidade ideal, necessidade hídrica e
coeficiente de cultura (Kc).

### RF02 — Coleta de dados de sensores via MQTT
O sistema deve consumir leituras de sensores IoT publicadas em tópicos MQTT.
As leituras de umidade do solo devem ser **isoladas por cultura** (um sensor
por talhão). Temperatura, umidade do ar e pH são compartilhados globalmente.

### RF03 — Integração com dados climáticos reais
O sistema deve consultar a API do OpenWeatherMap para obter temperatura,
umidade do ar e previsão de chuva da cidade configurada, e usar esses dados
na escolha da estratégia de irrigação.

### RF04 — Estratégias de irrigação
O sistema deve aplicar uma de três estratégias por ciclo:

- **Modo Emergencial:** umidade do solo abaixo de 70% da mínima da cultura.
- **Modo Seco:** umidade abaixo da mínima, sem previsão de chuva.
- **Modo Úmido:** previsão de chuva ou umidade do ar alta (> 80%).

A escolha deve priorizar a necessidade do solo sobre o clima.

### RF05 — Ciclo de avaliação periódico
A cada 30 segundos, o sistema deve executar um ciclo que: lê a umidade do solo
da cultura ativa, consulta o clima, escolhe a estratégia, calcula o volume de
água e registra a decisão.

### RF06 — Persistência de leituras e decisões
Todas as leituras de sensores recebidas via MQTT e todas as decisões de
irrigação devem ser persistidas em banco local (H2 embarcado), permitindo
consulta histórica posterior.

### RF07 — Dashboard web em tempo real
O sistema deve oferecer interface web que exibe: status do ciclo atual,
umidade do solo da cultura ativa, dados climáticos, estratégia escolhida,
gráfico histórico recente e lista de alertas. A atualização deve ocorrer sem
recarga manual.

### RF08 — Relatórios de consumo
O sistema deve permitir consultar o consumo de água por cultura e por
período, retornando volume total, número de decisões e lista detalhada das
irrigações no intervalo solicitado.

### RF09 — Troca de cultura monitorada
O usuário deve poder, via dashboard, alternar qual cultura está sendo
monitorada no momento; o ciclo seguinte usa os dados dessa cultura.

### RF10 — Alertas operacionais
O sistema deve emitir alertas categorizados (INFO, AVISO, CRÍTICO,
EMERGÊNCIA) para situações relevantes: umidade crítica, irrigação ativada,
irrigação suspensa por chuva, leituras ausentes.

---

## Requisitos não-funcionais

### RNF01 — Plataforma
Java 17+, Maven, multiplataforma (Windows, Linux, macOS).

### RNF02 — Broker MQTT
O sistema deve usar broker MQTT padrão (Mosquitto via Docker), permitindo
substituição por qualquer broker MQTT 3.1.1 compatível.

### RNF03 — Resiliência
Falhas pontuais (broker offline, API de clima indisponível, JSON inválido)
não devem derrubar o sistema. O dashboard deve continuar respondendo e o
ciclo deve ser retomado quando os dados voltarem.

### RNF04 — Persistência local
Banco H2 em modo arquivo (`data/irrigacao.mv.db`); zero infraestrutura
externa para persistência. Schema versionado em SQL no projeto.

### RNF05 — Configurabilidade
Parâmetros sensíveis (chave da API, endereços, tópicos, intervalos) devem
vir de arquivo de configuração externo ao código.

### RNF06 — Testabilidade
Lógica de negócio e infraestrutura cobertas por testes unitários
automatizados executáveis via `mvn test`, sem necessidade de broker ou
banco externo rodando.

---

## Fora de escopo

- Acionamento físico de válvulas/bombas reais.
- Múltiplos usuários simultâneos ou autenticação.
- Sincronização entre instâncias do sistema.
- Banco remoto (PostgreSQL, MySQL) — uso de H2 local é suficiente.
- Migrações de schema versionadas (Flyway/Liquibase).
- Mobile.

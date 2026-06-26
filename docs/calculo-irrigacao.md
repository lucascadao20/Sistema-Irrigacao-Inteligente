# Como o Cálculo de Irrigação é Feito

Documento de referência sobre a lógica de decisão e o cálculo do volume de água do
Sistema de Irrigação Inteligente. Independente da apresentação de slides.

O cálculo acontece em **duas etapas separadas**:

1. **Escolher a estratégia** — decide *o que fazer* (irrigar, suspender ou aguardar).
2. **Calcular o volume** — a estratégia escolhida decide *quanto de água*.

---

## Os ingredientes

Cada cultura (`src/main/java/com/irrigacao/modelo/Cultura.java`) carrega 4 parâmetros
agronômicos, pré-cadastrados a partir de dados FAO em
`src/main/java/com/irrigacao/dados/RepositorioDeCulturaEmMemoria.java`:

| Cultura        | Umid. mínima | Umid. ideal | Necessidade hídrica | Kc   |
|----------------|:------------:|:-----------:|:-------------------:|:----:|
| Milho          | 30%          | 60%         | 650 mm              | 1,15 |
| Soja           | 35%          | 65%         | 550 mm              | 1,10 |
| Arroz          | 70%          | 90%         | 1500 mm             | 1,20 |
| Feijão         | 30%          | 55%         | 400 mm              | 1,05 |
| Trigo          | 25%          | 55%         | 500 mm              | 1,10 |
| Café           | 40%          | 70%         | 800 mm              | 0,95 |
| Cana-de-açúcar | 45%          | 75%         | 1200 mm             | 1,25 |
| Algodão        | 30%          | 55%         | 700 mm              | 1,15 |
| Tomate         | 40%          | 70%         | 600 mm              | 1,05 |
| Alface         | 50%          | 80%         | 300 mm              | 0,90 |

A decisão usa três entradas:

- **Umidade do solo** — vinda do sensor (hoje simulada).
- **Dados de clima** — temperatura, umidade do ar, previsão e volume de chuva.
- **Parâmetros da cultura** — a tabela acima.

---

## Etapa 1 — Escolher a estratégia

Arquivo: `src/main/java/com/irrigacao/negocio/FabricaDeEstrategia.java`

A fábrica testa as condições **em ordem de prioridade** — a primeira que bater vence:

```
1. solo < (mínimo × 0,7)            → Modo Emergencial   (solo crítico)
2. solo < mínimo                    → Modo Seco          (abaixo do ideal)
3. chuva prevista OU umid. ar > 80% → Modo Úmido
4. nenhuma das anteriores           → AGUARDANDO         (não irriga)
```

**Detalhe mais importante:** a ordem significa que **solo seco vence previsão de chuva**.
Se o solo está abaixo do mínimo, o sistema irriga (Modo Seco/Emergencial) *mesmo que vá
chover* — porque o teste de chuva só é alcançado quando o solo já está em nível adequado.
É uma decisão consciente, alinhada com a lógica de alertas.

O limiar de emergência é **70% do mínimo**. Exemplo: para o Milho (mínimo 30%), abaixo de
**21%** já é emergência.

---

## Etapa 2 — Calcular o volume

Todas as estratégias que irrigam partem da mesma base, o **déficit hídrico**:

> **déficit = umidade ideal − umidade do solo**
> (quantos pontos percentuais faltam para o solo chegar ao ideal)

A partir daí, cada estratégia aplica um **fator de urgência** diferente:

| Estratégia                          | Fórmula do volume                         | Piso | Status     |
|-------------------------------------|-------------------------------------------|:----:|------------|
| **Emergencial**                     | `déficit × Kc × 15`                       |  —   | ATIVADA    |
| **Seco**                            | `déficit × Kc × 10` (**×1,3** se T > 30°C) | 5 L  | ATIVADA    |
| **Úmido** — chuva forte (> 5 mm)    | `0`                                       |  —   | SUSPENSA   |
| **Úmido** — chuva leve (≤ 5 mm)     | `déficit × Kc × 3`                        | 2 L  | ATIVADA    |
| **Úmido** — só ar úmido (sem chuva) | `0`                                       |  —   | AGUARDANDO |

O **fator** (15 › 10 › 3) codifica a urgência: a emergência rega muito mais que uma rega
reduzida em dia de chuva leve. O **Kc** (coeficiente de cultura) faz culturas mais
"sedentas" (arroz 1,20; cana 1,25) receberem mais água que as econômicas (alface 0,90;
café 0,95) para o mesmo déficit.

### Onde cada fórmula vive no código

- **Emergencial** — `src/main/java/com/irrigacao/negocio/EstrategiaModoEmergencial.java`
- **Seco** (+30% se quente, piso de 5 L) — `src/main/java/com/irrigacao/negocio/EstrategiaModoSeco.java`
- **Úmido** (suspende / reduz / aguarda) — `src/main/java/com/irrigacao/negocio/EstrategiaModoUmido.java`

O orquestrador `src/main/java/com/irrigacao/negocio/MotorDeRegras.java` costura tudo:
pede a estratégia à fábrica, dispara o alerta de pré-decisão, chama `calcularIrrigacao(...)`
e emite o alerta final (ATIVADA / SUSPENSA / AGUARDANDO).

---

## Exemplos numéricos (Milho: ideal 60%, mínimo 30%, Kc 1,15)

**① Solo 18% (crítico) → Emergencial**
déficit = 60 − 18 = 42 → 42 × 1,15 × **15** = **724,5 L** · ATIVADA

**② Solo 28%, dia ameno (25°C) → Seco**
déficit = 60 − 28 = 32 → 32 × 1,15 × **10** = **368 L** · ATIVADA

**③ Solo 25%, dia quente (33°C) → Seco com bônus de calor**
déficit = 35 → 35 × 1,15 × 10 = 402,5 → **×1,3** = **523,25 L** · ATIVADA

**④ Solo 50% (≥ mínimo), chuva forte 8 mm → Úmido**
8 mm > 5 mm → **0 L · SUSPENSA** (não desperdiça; a chuva vai molhar)

**⑤ Solo 50%, chuva leve 3 mm → Úmido reduzido**
déficit = 10 → 10 × 1,15 × **3** = **34,5 L** · ATIVADA

**⑥ Solo 50%, sem chuva, ar a 85% → Úmido (aguarda)**
**0 L · AGUARDANDO** (ar úmido reduz a evaporação; sem necessidade imediata)

---

## Ressalvas (limitações do modelo atual)

1. **É um modelo simplificado/didático** inspirado nos conceitos da FAO (Kc, déficit
   hídrico, ajuste por temperatura) — **não** é a equação completa de evapotranspiração
   FAO-56 (Penman-Monteith). Os fatores **×10 / ×15 / ×3** são constantes de
   escala/urgência, não grandezas físicas derivadas.
2. **`necessidadeHidrica` (mm) não entra na fórmula atual** — está cadastrada na cultura e
   é exposta na API, mas hoje serve apenas como metadado. É um forte candidato para uma
   fórmula mais rigorosa no futuro.
3. O déficit está em **pontos percentuais** de umidade e o resultado sai em "litros"
   abstratos — as unidades são propositalmente frouxas neste protótipo.
4. Há **pisos** (mínimo de 5 L no Seco, 2 L na chuva leve), mas **não há teto** — o
   Emergencial pode gerar volumes altos.

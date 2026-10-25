# 🎫 FamiliaConnect - Processador de Carteirinhas

Serviço assíncrono que consome mensagens de uma fila **RabbitMQ** e gera as **carteirinhas das famílias** cadastradas, apoiando o processo de entrega de cestas básicas.

O banco de dados e o backend que publica as mensagens ficam em repositórios separados (veja [Projetos relacionados](#-projetos-relacionados)).

Para cada mensagem recebida, o serviço gera um identificador único, grava esse identificador no banco, cria um QR Code e insere a carteirinha em um PDF A4 em lote, pronto para impressão.

---

## 🔗 Projetos relacionados

| Projeto | Descrição |
|---|---|
| 🗄 [Familia-Connect-BD](https://github.com/fsFernando072/Familia-Connect-BD) | Scripts e estrutura do banco de dados (MySQL) usado por este serviço |
| 🖥 [Familia-Connect-Back](https://github.com/fsFernando072/Familia-Connect-Back) | Backend que publica na fila RabbitMQ as mensagens consumidas por este serviço |

---

## 📑 Sumário

- 🔗 [Projetos relacionados](#-projetos-relacionados)
- ✨ [Funcionalidades](#-funcionalidades)
- 🔧 [Como funciona](#-como-funciona)
- 🧰 [Tecnologias](#-tecnologias)
- 📂 [Estrutura do projeto](#-estrutura-do-projeto)
- 📋 [Pré-requisitos](#-pré-requisitos)
- 🔐 [Variáveis de ambiente](#-variáveis-de-ambiente)
- ☁️ [Armazenamento das carteirinhas (local ou S3)](#-armazenamento-das-carteirinhas-local-ou-s3)
- 🧩 [Serviços auxiliares](#-serviços-auxiliares)
- 🚀 [Executando o projeto](#-executando-o-projeto)
- 📨 [Formato da mensagem](#-formato-da-mensagem)
- 📤 [Formato da resposta](#-formato-da-resposta)
- 📄 [Saída gerada](#-saída-gerada)
- 🩺 [Solução de problemas](#-solução-de-problemas)

---

## ✨ Funcionalidades

- Consumo de mensagens da fila RabbitMQ (JSON).
- Validação dos dados de entrada (`idFamilia`, `nomeResponsavel`, `cpfResponsavel`, `telefone`).
- Geração de identificador único da carteirinha (SHA-256).
- Atualização do identificador na tabela `familia` (MySQL).
- Geração de QR Code via API externa ([goqr.me](https://goqr.me/api/)).
- Montagem da carteirinha a partir de um PDF modelo (template).
- Organização em **lotes de PDF A4**, com várias carteirinhas por folha.
- **Uma pasta por data**: a cada novo dia a contagem de lotes reinicia em `lote-1.pdf`.
- **Armazenamento configurável** do PDF do lote: disco local ou bucket **S3** da AWS, escolhido por variável de ambiente.
- Envio de uma **mensagem de resposta** para o backend (via fila RabbitMQ própria), informando sucesso ou falha do processamento.

## 🔧 Como funciona

```
Cadastro de famílias ──► RabbitMQ (exchange → queue) ──► FamiliaConnect
                                                              │
                              ┌───────────────────────────────┼───────────────────────────────┐
                              ▼                               ▼                               ▼
                     MySQL (atualiza idCarteirinha)   API de QR Code (qrserver)      RabbitMQ (fila de resposta)
                                                              │                               │
                                                              ▼                               ▼
                                     Disco local  OU  Bucket S3 (AWS)         Familia-Connect-Back (consome a resposta)
                                     carteirinhas/AAAA-MM-DD/lote-N.pdf
```

1. O consumidor recebe a mensagem e converte para a entidade `Familia`.
2. O serviço gera o `idCarteirinha` e o atualiza no banco.
3. O QR Code é baixado localmente, desenhado na carteirinha e depois removido (é sempre temporário, independente da configuração de armazenamento).
4. A carteirinha é inserida no próximo espaço livre do lote do dia, e o PDF do lote é salvo **localmente ou em um bucket S3**, conforme `STORAGE_LOCAL_ENABLED` (veja [Armazenamento das carteirinhas](#-armazenamento-das-carteirinhas-local-ou-s3)).
5. Ao final (sucesso **ou** falha), o serviço publica uma **mensagem de resposta** em uma fila própria do RabbitMQ (`RABBITMQ_QUEUE_RESPOSTA`), repassando o mesmo `correlationIdRedis` recebido na mensagem original. É essa fila que o backend ([Familia-Connect-Back](https://github.com/fsFernando072/Familia-Connect-Back)) consome para saber o resultado do processamento (veja [Formato da resposta](#-formato-da-resposta)).

## 🧰 Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 21 | Linguagem |
| Spring Boot 3.5.6 | Framework |
| Spring AMQP | Integração com RabbitMQ |
| Spring Data JPA / JDBC | Acesso ao MySQL |
| OpenFeign | Cliente HTTP da API de QR Code |
| Apache PDFBox 3 | Geração dos PDFs |
| AWS SDK v2 (S3) | Armazenamento opcional do PDF do lote em bucket S3 |
| MapStruct + Lombok | Mapeamento e redução de código repetitivo |
| Maven (wrapper) | Build |
| Podman / Docker Compose | Subida do RabbitMQ |

## 📂 Estrutura do projeto

```
familiaconnect-processar-carteirinhas/
├── .gitignore                      # Ignora /carteirinhas/* (lotes gerados), mantém /carteirinhas/example
├── compose.yml                     # RabbitMQ (Podman/Docker Compose)
├── carteirinhas/
│   ├── example/
│   │   └── carteirinha-example.pdf  # Template da carteirinha (obrigatório, versionado)
│   └── AAAA-MM-DD/                  # Lotes gerados, uma pasta por dia (gitignored; só existe no modo local)
│       └── lote-N.pdf
└── familia-connect/                 # Aplicação Spring Boot
    ├── .env                         # Variáveis de ambiente (não versionado)
    ├── pom.xml
    └── src/main/java/school/sptech/familia_connect/
        ├── domain/                  # Entidade Familia
        ├── application/
        │   ├── port/in/             # Casos de uso (CarteirinhaUseCase)
        │   ├── port/out/            # Portas de saída: FamiliaPortOut, StoragePortOut
        │   └── service/             # CarteirinhaService, LoteService, QrCodeService
        └── infraestructure/
            ├── config/              # RabbitMQ e beans (inclui escolha do adapter de storage)
            ├── consumer/            # Listener, DTO e mapper
            ├── persistence/         # Repositório JDBC
            ├── storage/             # LocalStorageAdapter e S3StorageAdapter (StoragePortOut)
            ├── web/client/          # Cliente Feign do QR Code
            └── exception/
```

A aplicação segue uma arquitetura em camadas (portas e adaptadores): `domain` e `application` não dependem de detalhes de infraestrutura. O armazenamento do PDF do lote é abstraído pela porta `StoragePortOut`, implementada por `LocalStorageAdapter` (disco) ou `S3StorageAdapter` (bucket AWS S3) — a implementação ativa é escolhida em `BeanConfig` via `@ConditionalOnProperty`, de acordo com `app.storage.local-enabled`.

## 📋 Pré-requisitos

- **JDK 21**
- **Podman** (com `podman compose`) ou **Docker** (com `docker compose`)
- **MySQL 8+** com o banco e a tabela `familia` criados
- **IntelliJ IDEA** (opcional, recomendado)
- Acesso à internet (API de QR Code)
- Se for usar armazenamento em **S3** (`STORAGE_LOCAL_ENABLED=false`): uma conta AWS com um bucket S3 criado e credenciais configuradas na máquina (veja [Armazenamento das carteirinhas](#-armazenamento-das-carteirinhas-local-ou-s3))

## 🔐 Variáveis de ambiente

Crie o arquivo `familia-connect/.env` com as variáveis abaixo (apenas os nomes são listados; defina os valores no seu ambiente):

| Variável | Descrição | Obrigatória |
|---|---|---|
| `RABBITMQ_USER` | Usuário do RabbitMQ (deve ser igual ao definido em `compose.yml`) | Sim |
| `RABBITMQ_PASS` | Senha do RabbitMQ (deve ser igual ao definido em `compose.yml`) | Sim |
| `RABBITMQ_EXCHANGE` | Nome da exchange | Sim |
| `RABBITMQ_BINDING` | Routing key do binding | Sim |
| `RABBITMQ_QUEUE` | Nome da fila | Sim |
| `RABBITMQ_QUEUE_RESPOSTA` | Nome da fila de resposta (producer) | Sim |
| `RABBITMQ_ROUTING_RESPOSTA` | Routing key da resposta (producer) | Sim |
| `DB_URL` | URL JDBC do MySQL | Sim |
| `DB_USER` | Usuário do banco | Sim |
| `DB_PASSWORD` | Senha do banco | Sim |
| `STORAGE_LOCAL_ENABLED` | `true` salva o PDF do lote localmente; `false` salva em um bucket S3 | Não (padrão `true`) |
| `AWS_S3_BUCKET` | Nome do bucket S3 onde os lotes serão salvos | Só se `STORAGE_LOCAL_ENABLED=false` |
| `AWS_REGION` | Região AWS do bucket (ex.: `us-east-1`) | Não (padrão `us-east-1`) |

Exemplo da estrutura do `.env` (substitua `<valor>`):

```env
RABBITMQ_USER=<valor>
RABBITMQ_PASS=<valor>
RABBITMQ_EXCHANGE=<valor>
RABBITMQ_BINDING=<valor>
RABBITMQ_QUEUE=<valor>
RABBITMQ_QUEUE_RESPOSTA=<valor>
RABBITMQ_ROUTING_RESPOSTA=<valor>

DB_URL=<valor>
DB_USER=<valor>
DB_PASSWORD=<valor>

# Opcional: só necessário se quiser salvar os lotes em S3 em vez de localmente
STORAGE_LOCAL_ENABLED=true
AWS_S3_BUCKET=<valor>
AWS_REGION=us-east-1
```

> ⚠ O `.env` não deve ser versionado (já está no `.gitignore` de `familia-connect`). Não use aspas nos valores.

### 📥 Carregando o `.env`

O Spring Boot **não lê o `.env` automaticamente**: ele só enxerga variáveis presentes no ambiente do processo. Sem isso, o erro típico é `Driver ... claims to not accept jdbcUrl, ${DB_URL}`.

**🧠 IntelliJ IDEA (plugin EnvFile)**

1. Instale o plugin **EnvFile** em *Settings > Plugins*.
2. Abra *Run > Edit Configurations* e selecione a configuração da aplicação (`FamiliaConnectApplication`).
3. Na aba **EnvFile**, marque **Enable EnvFile**.
4. Clique em **+**, escolha `.env file` e selecione `familia-connect/.env`.
5. Aplique e execute.

**💻 PowerShell (terminal)**

```powershell
Get-Content familia-connect\.env | Where-Object { $_ -match '^\s*[^#\s][^=]*=' } | ForEach-Object {
  $k, $v = $_ -split '=', 2
  Set-Item "env:$($k.Trim())" ($v.Trim().Trim("'").Trim('"'))
}
```

As variáveis valem apenas para a sessão atual do terminal. Para definir manualmente: `$env:NOME_DA_VARIAVEL = "valor"`.

## ☁️ Armazenamento das carteirinhas (local ou S3)

O PDF de cada **lote de carteirinhas** (`lote-N.pdf`) pode ser salvo de duas formas, escolhidas por uma única variável de ambiente:

| `STORAGE_LOCAL_ENABLED` | Onde o lote é salvo | Adapter usado |
|---|---|---|
| `true` (padrão) | Disco local, em `carteirinhas/AAAA-MM-DD/lote-N.pdf` | `LocalStorageAdapter` |
| `false` | Bucket S3 (`AWS_S3_BUCKET`), sob a chave `carteirinhas/AAAA-MM-DD/lote-N.pdf` | `S3StorageAdapter` |

- A troca é feita automaticamente pelo Spring (`BeanConfig`), sem precisar alterar código: cada adapter implementa a porta `StoragePortOut` e só um dos dois é registrado como bean, conforme a propriedade `app.storage.local-enabled` (mapeada de `STORAGE_LOCAL_ENABLED`).
- O **QR Code** continua sempre sendo salvo localmente (é um arquivo temporário, apagado logo após ser desenhado na carteirinha), independente dessa configuração.
- Para usar S3, defina no `.env`:
  ```env
  STORAGE_LOCAL_ENABLED=false
  AWS_S3_BUCKET=<nome-do-bucket>
  AWS_REGION=us-east-1
  ```
- **Credenciais da AWS**: o `S3StorageAdapter` usa o `S3Client` padrão do AWS SDK v2, que resolve as credenciais automaticamente pela [cadeia padrão de credenciais](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html) (variáveis `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`, arquivo `~/.aws/credentials`, role da instância/EC2, etc.). Não é necessário colocar essas chaves no `.env` do projeto — configure-as no ambiente/máquina onde a aplicação roda.
- Não é necessário instalar nada extra: a dependência `software.amazon.awssdk:s3` já está no `pom.xml` e é baixada pelo Maven.

## 🧩 Serviços auxiliares

### 🐇 RabbitMQ

O `compose.yml` sobe o RabbitMQ com painel de gerenciamento e volume persistente (`rabbitmq_data`), usando as credenciais fixas definidas nele (`RABBITMQ_DEFAULT_USER`/`RABBITMQ_DEFAULT_PASS`). Garanta que `RABBITMQ_USER`/`RABBITMQ_PASS` no `.env` da aplicação tenham **os mesmos valores** configurados em `compose.yml`, para a aplicação conseguir autenticar na fila.

| Porta | Uso |
|---|---|
| `5672` | AMQP (usada pela aplicação) |
| `15672` | Painel de administração (http://localhost:15672) |

**🦭 Podman**

```powershell
podman machine start                 # apenas no Windows/macOS, se a VM estiver parada
podman compose up -d
podman compose ps
```

**🐳 Docker**

```bash
docker compose up -d
```

🛑 Para parar mantendo os dados: `podman compose down`. **Não use `-v`**, pois ele apaga o volume e as filas persistidas.

### 🐬 MySQL

O MySQL **não** faz parte do `compose.yml`. Suba uma instância local ou remota e aponte `DB_URL`, `DB_USER` e `DB_PASSWORD` para ela. A aplicação espera a tabela `familia` com, no mínimo, as colunas `id` e `idCarteirinha`.

## 🚀 Executando o projeto

1. Suba o RabbitMQ (veja acima).
2. Garanta que o MySQL esteja disponível.
3. Confirme que o template existe em `carteirinhas/example/carteirinha-example.pdf`. O caminho é relativo ao diretório de execução, então rode a aplicação a partir da **raiz do repositório**.
4. Carregue as variáveis de ambiente.
5. Inicie a aplicação:

```powershell
cd familia-connect
.\mvnw spring-boot:run
```

Ou execute `FamiliaConnectApplication` pelo IntelliJ (com o EnvFile configurado e *Working directory* na raiz do repositório).

✅ A aplicação sobe na porta `8081`.

## 📨 Formato da mensagem

Publique no RabbitMQ (exchange e routing key definidos no `.env`) um JSON como:

```json
{
  "idFamilia": 1,
  "nomeResponsavel": "Maria da Silva",
  "cpfResponsavel": "000.000.000-00",
  "telefone": "(11) 90000-0000"
}
```

Todos os campos são obrigatórios. O `idFamilia` deve existir na tabela `familia`, caso contrário o processamento falha com `Erro ao atualizar idFamilia da carteirinha no banco de dados`.

A mensagem deve incluir também o **header AMQP** `correlationIdRedis`, usado para correlacionar a resposta com a requisição original no backend. Se esse header vier ausente ou em branco, o serviço **descarta a mensagem e não envia resposta** (nem de sucesso, nem de erro) — fique atento a esse detalhe ao testar manualmente publicações na fila.

## 📤 Formato da resposta

Ao final do processamento (com **sucesso ou falha**), o serviço publica automaticamente uma mensagem de resposta na fila configurada por `RABBITMQ_QUEUE_RESPOSTA` (ligada à mesma exchange `RABBITMQ_EXCHANGE`, através da routing key `RABBITMQ_ROUTING_RESPOSTA`). É essa fila que o backend ([Familia-Connect-Back](https://github.com/fsFernando072/Familia-Connect-Back)) consome para saber o resultado do processamento.

A mensagem é enviada com o mesmo header `correlationIdRedis` recebido na requisição original, e o corpo segue o formato:

```json
{
  "status": "SUCESSO",
  "message": "Carteirinha da familia gerada com sucesso!",
  "nomeResponsavel": "Maria da Silva"
}
```

| Campo | Descrição |
|---|---|
| `status` | `SUCESSO` ou `ERRO` |
| `message` | Mensagem descritiva do resultado (ou da causa do erro) |
| `nomeResponsavel` | Nome do responsável informado na mensagem original |

Em caso de falha (`GlobalModelException`), `status` vira `ERRO` e `message` passa a conter a causa da falha (ex.: `idFamilia` inexistente, erro ao salvar no S3/local, etc.).

## 📄 Saída gerada

Os PDFs são organizados em "pastas" por data (`carteirinhas/AAAA-MM-DD/lote-N.pdf`), seja no disco local ou como prefixo das chaves no bucket S3, conforme `STORAGE_LOCAL_ENABLED` (veja [Armazenamento das carteirinhas](#-armazenamento-das-carteirinhas-local-ou-s3)).

**Modo local** (`STORAGE_LOCAL_ENABLED=true`, padrão):

```
carteirinhas/
├── 2026-10-17/
│   ├── lote-1.pdf
│   └── lote-2.pdf
└── 2026-10-18/
    └── lote-1.pdf      # contagem reiniciada no novo dia
```

**Modo S3** (`STORAGE_LOCAL_ENABLED=false`): a mesma estrutura, porém como chaves dentro do bucket `AWS_S3_BUCKET` (ex.: `carteirinhas/2026-10-17/lote-1.pdf`).

Cada `lote-N.pdf` é uma folha A4 com várias carteirinhas. Quando o lote enche, o próximo é criado automaticamente.

## 🩺 Solução de problemas

| Sintoma | Causa provável | Solução |
|---|---|---|
| 🔌 `Cannot connect to Podman` | VM do Podman parada | `podman machine start` |
| 🔒 RabbitMQ sai com `eacces` em `.erlang.cookie` | Permissão no volume | Recrie o volume com `podman compose down -v` e suba de novo |
| 📄 `claims to not accept jdbcUrl, ${DB_URL}` | `.env` não carregado | Configure o EnvFile ou exporte as variáveis |
| 🗃 `Erro ao atualizar idFamilia da carteirinha` | `idFamilia` inexistente na tabela | Cadastre a família ou publique um id válido |
| 📭 Mensagem consumida mas sem PDF | Template ausente ou diretório de execução errado | Verifique `carteirinhas/example/carteirinha-example.pdf` |
| 🔑 RabbitMQ recusa a conexão da aplicação (`ACCESS_REFUSED`) | `RABBITMQ_USER`/`RABBITMQ_PASS` do `.env` diferentes dos definidos em `compose.yml` | Alinhe os valores do `.env` com `RABBITMQ_DEFAULT_USER`/`RABBITMQ_DEFAULT_PASS` do `compose.yml` |
| ☁️ `S3Exception` / `NoSuchBucket` / `AccessDenied` ao gerar a carteirinha | Bucket errado/inexistente ou credenciais AWS ausentes | Confira `AWS_S3_BUCKET`/`AWS_REGION` e as credenciais AWS da máquina (veja [Armazenamento das carteirinhas](#-armazenamento-das-carteirinhas-local-ou-s3)) |

## 📜 Licença

Distribuído sob a licença descrita no arquivo [LICENSE](LICENSE).

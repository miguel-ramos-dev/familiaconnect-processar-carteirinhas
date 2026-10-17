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
- 🧩 [Serviços auxiliares](#-serviços-auxiliares)
- 🚀 [Executando o projeto](#-executando-o-projeto)
- 📨 [Formato da mensagem](#-formato-da-mensagem)
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

## 🔧 Como funciona

```
Cadastro de famílias ──► RabbitMQ (exchange → queue) ──► FamiliaConnect
                                                              │
                              ┌───────────────────────────────┤
                              ▼                               ▼
                     MySQL (atualiza idCarteirinha)   API de QR Code (qrserver)
                                                              │
                                                              ▼
                                          carteirinhas/AAAA-MM-DD/lote-N.pdf
```

1. O consumidor recebe a mensagem e converte para a entidade `Familia`.
2. O serviço gera o `idCarteirinha` e o atualiza no banco.
3. O QR Code é baixado, desenhado na carteirinha e depois removido.
4. A carteirinha é inserida no próximo espaço livre do lote do dia.

## 🧰 Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 21 | Linguagem |
| Spring Boot 3.5.6 | Framework |
| Spring AMQP | Integração com RabbitMQ |
| Spring Data JPA / JDBC | Acesso ao MySQL |
| OpenFeign | Cliente HTTP da API de QR Code |
| Apache PDFBox 3 | Geração dos PDFs |
| MapStruct + Lombok | Mapeamento e redução de código repetitivo |
| Maven (wrapper) | Build |
| Podman / Docker Compose | Subida do RabbitMQ |

## 📂 Estrutura do projeto

```
familiaconnect-processar-carteirinhas/
├── compose.yml                    # RabbitMQ (Podman/Docker Compose)
├── carteirinhas/
│   ├── example/
│   │   └── carteirinha-example.pdf  # Template da carteirinha (obrigatório)
│   └── AAAA-MM-DD/                  # Lotes gerados, uma pasta por dia
│       └── lote-N.pdf
└── familia-connect/                 # Aplicação Spring Boot
    ├── .env                         # Variáveis de ambiente (não versionado)
    ├── pom.xml
    └── src/main/java/school/sptech/familia_connect/
        ├── domain/                  # Entidade Familia
        ├── application/
        │   ├── port/in/             # Casos de uso
        │   ├── port/out/            # Portas de saída (persistência)
        │   └── service/             # CarteirinhaService, LoteService, QrCodeService
        └── infraestructure/
            ├── config/              # RabbitMQ e beans
            ├── consumer/            # Listener, DTO e mapper
            ├── persistence/         # Repositório JDBC
            ├── web/client/          # Cliente Feign do QR Code
            └── exception/
```

A aplicação segue uma arquitetura em camadas (portas e adaptadores): `domain` e `application` não dependem de detalhes de infraestrutura.

## 📋 Pré-requisitos

- **JDK 21**
- **Podman** (com `podman compose`) ou **Docker** (com `docker compose`)
- **MySQL 8+** com o banco e a tabela `familia` criados
- **IntelliJ IDEA** (opcional, recomendado)
- Acesso à internet (API de QR Code)

## 🔐 Variáveis de ambiente

Crie o arquivo `familia-connect/.env` com as variáveis abaixo (apenas os nomes são listados; defina os valores no seu ambiente):

| Variável | Descrição |
|---|---|
| `RABBITMQ_USER` | Usuário do RabbitMQ |
| `RABBITMQ_PASS` | Senha do RabbitMQ |
| `RABBITMQ_EXCHANGE` | Nome da exchange |
| `RABBITMQ_BINDING` | Routing key do binding |
| `RABBITMQ_QUEUE` | Nome da fila |
| `DB_URL` | URL JDBC do MySQL |
| `DB_USER` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |

Exemplo da estrutura do `.env` (substitua `<valor>`):

```env
RABBITMQ_USER=<valor>
RABBITMQ_PASS=<valor>
RABBITMQ_EXCHANGE=<valor>
RABBITMQ_BINDING=<valor>
RABBITMQ_QUEUE=<valor>

DB_URL=<valor>
DB_USER=<valor>
DB_PASSWORD=<valor>
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

## 🧩 Serviços auxiliares

### 🐇 RabbitMQ

O `compose.yml` sobe o RabbitMQ com painel de gerenciamento e volume persistente (`rabbitmq_data`).

| Porta | Uso |
|---|---|
| `5672` | AMQP (usada pela aplicação) |
| `15672` | Painel de administração (http://localhost:15672) |

O compose usa `RABBITMQ_USER` e `RABBITMQ_PASS`, que precisam estar definidas **no terminal** antes de subir (o compose lê o `.env` do diretório onde é executado, e o `.env` do projeto fica em `familia-connect/`).

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

## 📄 Saída gerada

Os PDFs são salvos em pastas por data:

```
carteirinhas/
├── 2026-10-17/
│   ├── lote-1.pdf
│   └── lote-2.pdf
└── 2026-10-18/
    └── lote-1.pdf      # contagem reiniciada no novo dia
```

Cada `lote-N.pdf` é uma folha A4 com várias carteirinhas. Quando o lote enche, o próximo é criado automaticamente.

## 🩺 Solução de problemas

| Sintoma | Causa provável | Solução |
|---|---|---|
| 🔌 `Cannot connect to Podman` | VM do Podman parada | `podman machine start` |
| 🔒 RabbitMQ sai com `eacces` em `.erlang.cookie` | Permissão no volume | O `compose.yml` já usa `user: rabbitmq`; recrie com `podman compose down -v` e suba de novo |
| 📄 `claims to not accept jdbcUrl, ${DB_URL}` | `.env` não carregado | Configure o EnvFile ou exporte as variáveis |
| 🗃 `Erro ao atualizar idFamilia da carteirinha` | `idFamilia` inexistente na tabela | Cadastre a família ou publique um id válido |
| 📭 Mensagem consumida mas sem PDF | Template ausente ou diretório de execução errado | Verifique `carteirinhas/example/carteirinha-example.pdf` |

## 📜 Licença

Distribuído sob a licença descrita no arquivo [LICENSE](LICENSE).

![Build](https://github.com/embabel/embabel-agent/actions/workflows/maven.yml/badge.svg)

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Apache Tomcat](https://img.shields.io/badge/apache%20tomcat-%23F8DC75.svg?style=for-the-badge&logo=apache-tomcat&logoColor=black)
![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)
![ChatGPT](https://img.shields.io/badge/chatGPT-74aa9c?style=for-the-badge&logo=openai&logoColor=white)
![JSON](https://img.shields.io/badge/JSON-000?logo=json&logoColor=fff)
![GitHub Actions](https://img.shields.io/badge/github%20actions-%232671E5.svg?style=for-the-badge&logo=githubactions&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJIDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white)

# Lore : Chat and MCP Server

<img src="https://github.com/embabel/embabel-agent/blob/main/embabel-agent-api/images/315px-Meister_der_Weltenchronik_001.jpg?raw=true" width="180">

Lore exposes curated knowledge bases as documentation, relevant blogs and other content, and
up-to-the-minute API information. It ships several knowledge bases selected by profile — the Embabel
Agent Framework and a Domain-Driven Design / Spring Modulith reference (see
[Knowledge base profiles](#knowledge-base-profiles)).

<p align="center">
  <a href="https://www.youtube.com/watch?v=hY6ZFMIJdd4" target="_blank">
    <img src="./guide-demo.png" alt="The Voice, The Word, and The Wheel" width="700">
  </a>
</p>

This is exposed in two ways:

- Via a chat server (WebSocket/STOMP) for custom front-ends
- Via spring shell
- Via an MCP server for integration with Claude Desktop, Claude Code and
  other MCP clients

**Blog:** [The Voice, The Word, and The Wheel](https://medium.com/embabel/the-voice-the-word-and-the-wheel-d6e2ef2ab26e) — adding voice interaction (TTS/STT) to the Guide with Deepgram, a narrator agent, and natural-language commands.

> **Note:** The chat server and Spring Shell conflict with each other. By default, the chat server is enabled. To use
> Spring Shell instead, uncomment the relevant lines in `pom.xml`.

## Quickstart

Requires **Docker** running, **JDK 26** (`mise install` — see [Prerequisites](#prerequisites)), and an
**Anthropic API key**. The Spring Boot app starts Neo4j itself via `spring-boot-docker-compose` (the
default `neo4j` service in `compose.yaml`), so there is no separate `docker compose` step.

```bash
export ANTHROPIC_API_KEY=sk-ant-...

# Auto-starts Neo4j, ingests the DDD knowledge base on startup, then serves on :1337.
SPRING_PROFILES_ACTIVE=local,ddd ./mvnw spring-boot:run
```

- **`local`** turns on the managed Neo4j container
  (`spring.docker.compose.lifecycle-management=start-only` — Neo4j starts on boot and is left running
  when the app stops).
- **`ddd`** selects the knowledge base (use `embabel` for the Embabel one).
- **`ANTHROPIC_API_KEY`** is auto-detected (via `embabel-agent-anthropic-autoconfigure`); swap in
  `OPENAI_API_KEY` / `MISTRAL_API_KEY` / `DEEPSEEK_API_KEY` for another provider.
- Content ingests on startup by default (`guide.reload-content-on-startup=true`); set
  `GUIDE_RELOADCONTENTONSTARTUP=false` on later runs to skip re-ingestion and start faster.

The app serves on `http://localhost:1337` — chat WebSocket at `/ws`, MCP (streamable HTTP) at `/mcp`. On the first
run, watch for the `INGESTION COMPLETE` banner. Stop with `Ctrl+C`; Neo4j keeps running (stop it with
`docker compose down`).

## Knowledge base profiles

Guide ships more than one curated knowledge base, selected by Spring profile via the
`GUIDE_PROFILE` environment variable. Each profile has its own ingested content, reference tools, and
system-prompt branding (`guide.domain`), so the chatbot introduces itself appropriately for the active
domain.

| Profile   | `GUIDE_PROFILE` | Content                                                  | Config files |
|-----------|-----------------|----------------------------------------------------------|--------------|
| Embabel   | `embabel`       | The Embabel Agent Framework — docs, blogs, examples      | `application-embabel.yml`, `references-embabel.yml` |
| DDD       | `ddd`           | Domain-Driven Design, Sliced Onion & Spring Modulith     | `application-ddd.yml`, `references-ddd.yml` |

All profiles share the single Neo4j `neo4j` database — Neo4j Community supports only one database, so
switch profiles by re-ingesting (per-profile isolated databases would require Neo4j Enterprise).

The `embabel` profile **extends** `ddd` (`guide.extends: ddd`, resolved by `GuideComposition`): it
inherits the DDD reference tools and domain branding — so the Embabel assistant presents itself as
built on Spring/DDD — while keeping its own ingested content. The `ddd` profile is standalone.

Branding for each profile lives in its `guide.domain` block (name, description, key references,
tool guidance, and TTS pronunciations); the base fallback is in `application.yml`. To add curated
resources to a profile, edit `guide.content.supplementary` (articles/docs), `guide.repositories`
(repos to clone + ingest), or the profile's `references-*.yml` (lazy code-browsing tools).

Re-ingest a profile after editing its resources:

```bash
GUIDE_PROFILE=ddd ./scripts/fresh-ingest.sh    # or append-ingest.sh to add without wiping
```

When creating a Claude Project / MCP project for a profile, point it at the matching briefing:
[claude_project.md](docs/claude_project.md) (Embabel) or
[claude_project_ddd.md](docs/claude_project_ddd.md) (DDD).

### Pinning referenced versions

Referenced repositories can be pinned to a specific release so the knowledge base reflects a known
version instead of whatever is currently on `main`:

- **Ingested repos** (`guide.repositories`): add `tag: <git-tag>` (e.g. `spring-ai` → `v1.1.1`,
  `arch-evident-spring` → `steps/7`). Without a `tag:`, the repo tracks its default branch.
- **Code-browsing tools** (`references-*.yml`): use
  `fqn: com.embabel.guide.references.PinnedGitHubRepository` with `ref: <tag/branch/commit>`
  (e.g. ArchUnit `v1.4.2`, spring-modulith `2.0.6`, jMolecules `2.0.1`, embabel-agent `v0.4.0`).
  Entries that keep the plain `com.embabel.coding.tools.git.GitHubRepository` fqn track the default branch.

Libraries are pinned to their latest release; example/demo apps and repos that publish no releases
intentionally track `main`. Bump a pin deliberately when upgrading.

## Prerequisites

- **JDK 26.** The build targets `--release 26` and runs Error Prone / NullAway during compilation.
  The toolchain is pinned with [mise](https://mise.jdx.dev) (`mise.toml` → Temurin 26): run
  `mise install`, or point `JAVA_HOME` at a JDK 26 yourself. The Maven wrapper (`./mvnw`) and the
  committed `.mvn/jvm.config` (Error Prone `--add-exports`) mean no further build setup is needed.
- **Docker** — for Neo4j, the default graph backend.
- **An LLM provider key** — set one of `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, `MISTRAL_API_KEY`, or
  `DEEPSEEK_API_KEY` (hub/web deployments instead let each user bring their own key via
  **Settings → Integrations**).

## Running the app

The ingest scripts are the simplest way to run locally — each one starts Neo4j (Docker), optionally
clears it, ingests the selected profile's content on startup, and runs the app in the foreground:

```bash
GUIDE_PROFILE=ddd ./scripts/fresh-ingest.sh      # wipe Neo4j content, then ingest + run
GUIDE_PROFILE=ddd ./scripts/append-ingest.sh     # keep existing data, ingest new + run
```

Set `GUIDE_PROFILE` to `embabel` or `ddd` (or your own `application-<name>.yml`); it can also live in
a local `.env`. The scripts export `SPRING_PROFILES_ACTIVE=local,<profile>` and run
`./mvnw spring-boot:run`. The server starts on `http://localhost:1337` — chat WebSocket at `/ws`,
MCP (streamable HTTP) at `/mcp`, REST under `/api`. Watch for the `INGESTION COMPLETE` banner.

To run **without** re-ingesting (serve the data already in Neo4j), start Neo4j and the app directly:

```bash
docker compose up neo4j -d
SPRING_PROFILES_ACTIVE=local,ddd ./mvnw spring-boot:run
```

Or run the whole stack (Neo4j + the Java app) in Docker — see [Docker](#docker) below.

## Loading data

```bash
curl -X POST http://localhost:1337/api/v1/data/load-references
```

To see stats on data, make a GET request or browse to http://localhost:1337/api/v1/data/stats

RAG content storage uses the `ChunkingContentElementRepository` interface from the `embabel-agent-rag-core` library. The default backend is Neo4j via `DrivineStore`. You can plug in other backends by providing a different `ChunkingContentElementRepository` bean.

## Graph database

The RAG vector store is **Neo4j**, accessed via `DrivineStore` (from `embabel-agent-rag-graph`).
`RagConfiguration` wires it up at startup from `database.dataSources.neo` in
`src/main/resources/application.yml`.

Start Neo4j with Docker Compose:

```bash
docker compose up neo4j -d
```

Neo4j listens on `7687` (bolt); browse it at http://localhost:7474/browser/ — see
[Viewing and Deleting Data](#viewing-and-deleting-data) below.

## Viewing and Deleting Data

Go to the Neo Browser at http://localhost:7474/browser/

Log in with username `neo4j` and password `brahmsian` (or your custom password if you set one).

To delete all data run the following query:

```cypher

MATCH (n:ContentElement)
DETACH DELETE n
```

## Exposing MCP Tools

Starting the server exposes the Lore MCP tools over **streamable HTTP** at
`http://localhost:1337/mcp`. (Earlier versions served SSE on `/sse`; that transport has been
retired and `/sse` now returns 403.)

The server runs in **stateful** streamable-HTTP mode: it issues an `Mcp-Session-Id` on `initialize`
that the client must echo on later requests. Most clients handle this transparently, but a few
(notably Antigravity's built-in client) complete `initialize` and then stall before listing tools —
those must be bridged through [`mcp-remote`](https://github.com/geelen/mcp-remote) (see below).

### Verifying With MCP Inspector (Optional)

An easy way to verify the tools are exposed and experiment with calling them is by running the MCP inspector:

```bash
npx @modelcontextprotocol/inspector
```

Within the inspector UI, choose the **Streamable HTTP** transport and connect to
`http://localhost:1337/mcp`.

## Consuming the Lore MCP Server

The endpoint is `http://localhost:1337/mcp` (streamable HTTP, no auth on loopback). Two rules apply
to every client:

- **No trailing slash** — `POST /mcp/` returns 403; use `/mcp` exactly.
- **Native vs. bridged** — clients with a solid streamable-HTTP implementation connect directly to
  the URL. Clients whose built-in client stalls on the stateful session handshake must bridge
  through `mcp-remote` with `--transport http-only`.

| Client         | Connects        | Config shape                            |
|----------------|-----------------|-----------------------------------------|
| Claude Code    | native          | `type: http`, `url: …/mcp`              |
| Codex          | native          | `url = "…/mcp"`                         |
| Copilot CLI    | native          | `type: http`, `url: …/mcp`              |
| Claude Desktop | `mcp-remote`    | `command: npx … mcp-remote … http-only` |
| Antigravity    | `mcp-remote`    | `command: npx … mcp-remote … http-only` |
| Cursor         | `mcp-remote`    | `command: npx … mcp-remote … http-only` |

- [Claude Code](#claude-code)
- [Codex](#codex)
- [Antigravity](#antigravity)
- [Claude Desktop](#claude-desktop)
- [Cursor](#cursor)
- [Copilot CLI](#copilot-cli)

### Verifying the Server is Running

Before configuring a client, confirm the streamable endpoint answers an `initialize` POST:

```bash
curl -i --max-time 5 -X POST http://localhost:1337/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"curl","version":"0"}}}'
```

A `200` response carrying an `Mcp-Session-Id` header means the server is healthy. If you're running
on a different port, update the URL accordingly.

### Claude Desktop

Claude Desktop's config only supports stdio servers, so bridge the streamable endpoint through
`mcp-remote`. Add this stanza to `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "lore-dev": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "http://localhost:1337/mcp",
        "--transport",
        "http-only"
      ]
    }
  }
}
```

See [Connect Local Servers](https://modelcontextprotocol.io/docs/develop/connect-local-servers) for detailed
documentation.

You should create a [Project](https://www.anthropic.com/news/projects) to ensure that Claude knows its purpose and how
to use tools.
See [claude_project.md](docs/claude_project.md) (Embabel) or
[claude_project_ddd.md](docs/claude_project_ddd.md) (DDD) for suggested content, matching the
`GUIDE_PROFILE` you are running.

### Claude Code

If you're using Claude Code, adding the Lore MCP server will
powerfully augment its capabilities for working on Embabel applications
and helping you learn Embabel.

Claude Code speaks streamable HTTP natively:

```bash
claude mcp add lore --transport http http://localhost:1337/mcp
```

This writes a `{ "type": "http", "url": "http://localhost:1337/mcp" }` entry. Within the Claude Code
shell, type `/mcp` to test the connection. Choose the number of the `lore` server to check its
status.

Start via `claude --debug` to see more logging.

See [Claude Code MCP documentation](https://code.claude.com/docs/en/mcp) for further information.

#### Auto-Approving Lore MCP Tools

By default, Claude Code asks for confirmation before running MCP tools. When you accept a tool with "Yes, don't ask
again", Claude Code saves that permission to your local `.claude/settings.local.json` file (which is auto-ignored by
git).

**Note:** Wildcards do not work for MCP tool permissions. Each tool must be approved individually or listed explicitly
in your settings.

**Tool naming:** By default, `guide.toolPrefix` is empty, so MCP tools are exposed with their original names (e.g.,
`mcp__lore__docs_vectorSearch`). You can set a custom prefix in your application configuration to namespace your
tools.

See [Claude Code Permission Modes](https://code.claude.com/docs/en/iam#permission-modes) for detailed documentation on
how permissions work.

### Codex

Codex connects to the streamable URL directly. Create or update `~/.codex/config.toml` and add:

```toml
[mcp_servers.lore]
url = "http://127.0.0.1:1337/mcp"
# Optional timeouts:
# startup_timeout_sec = 60
# tool_timeout_sec = 120
```

### Cursor

#### Configuration

Cursor MCP config (Linux):

- `~/.cursor/mcp.json`

Bridge the streamable endpoint through `mcp-remote` (Cursor's built-in client is unreliable with
the stateful session):

```json
{
  "mcpServers": {
    "lore-dev": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "http://localhost:1337/mcp",
        "--transport",
        "http-only"
      ]
    }
  }
}
```

### Antigravity

Antigravity's built-in MCP client (a Go HTTP client) completes `initialize` against a direct
`serverUrl` but then **stalls** — it never lists tools, leaving the server stuck on an orange
status. Bridge through `mcp-remote` instead — Antigravity accepts a `command`-based stdio server.
Config lives in `~/.gemini/config/mcp_config.json`:

```json
{
  "mcpServers": {
    "lore-dev": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "http://127.0.0.1:1337/mcp",
        "--transport",
        "http-only"
      ]
    }
  }
}
```

> **Do not** use `"serverUrl": "…/mcp"` here: it connects but exposes no tools (the stall described
> above). `"httpUrl"` is also rejected — Antigravity only accepts `serverUrl` or `command`.

### Copilot CLI

#### Configuration

- Modify the $HOME/.copilot/mcp-config.json with Lore MCP server configuration

```json
{
  "mcpServers": {
    "lore-dev": {
      "type": "http",
      "url": "http://localhost:1337/mcp",
      "tools": [
        "*"
      ]
    }
  }
}
```

## Writing a Client

The backend supports any client via WebSocket (for real-time chat) and REST (for authentication).

### WebSocket Chat API

**Endpoint:** `ws://localhost:1337/ws`

Uses STOMP protocol over WebSocket with SockJS fallback. Any STOMP client library works (e.g., `@stomp/stompjs` for
JavaScript, `stomp.py` for Python).

**Authentication:** Pass an optional JWT token as a query parameter:

```
ws://localhost:1337/ws?token=<JWT>
```

If no token is provided, an anonymous user is created automatically.

#### STOMP Channels

| Direction | Destination             | Purpose                       |
|-----------|-------------------------|-------------------------------|
| Subscribe | `/user/queue/messages`  | Receive chat responses        |
| Subscribe | `/user/queue/status`    | Receive typing/status updates |
| Publish   | `/app/chat.sendToJesse` | Send message to AI bot        |
| Publish   | `/app/presence.ping`    | Keep-alive (send every 30s)   |

#### Message Formats

**Sending a message:**

```json
{
  "body": "your message here"
}
```

**Receiving a message:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "content": "response text",
  "userId": "bot:jesse",
  "userName": "Jesse",
  "timestamp": "2025-12-16T10:30:00Z"
}
```

**Receiving a status update:**

```json
{
  "fromUserId": "bot:jesse",
  "status": "typing"
}
```

### REST API

CORS is open (`*`), no special headers required beyond `Content-Type: application/json`.

#### Authentication Endpoints

**Register:**

```
POST /api/hub/register
{
  "userDisplayName": "Jane Doe",
  "username": "jane",
  "userEmail": "jane@example.com",
  "password": "secret",
  "passwordConfirmation": "secret"
}
```

**Login:**

```
POST /api/hub/login
{ "username": "jane", "password": "secret" }

Response:
{ "token": "eyJhbG...", "userId": "...", "username": "jane", ... }
```

**List Personas:**

```
GET /api/hub/personas
```

**Update Persona** (requires auth):

```
PUT /api/hub/persona/mine
Authorization: Bearer <JWT>
{ "persona": "persona_name" }
```

### Example: Minimal JavaScript Client

```javascript
import {Client} from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
    webSocketFactory: () => new SockJS('http://localhost:1337/ws'),
    onConnect: () => {
        // Subscribe to responses
        client.subscribe('/user/queue/messages', (frame) => {
            const message = JSON.parse(frame.body);
            console.log('Received:', message.content);
        });

        // Send a message
        client.publish({
            destination: '/app/chat.sendToJesse',
            body: JSON.stringify({body: 'Hello!'})
        });
    }
});

client.activate();
```

## Docker

### Start (Docker Compose)

Start `neo4j` + `guide` (the Java application):

```bash
docker compose --profile java up --build -d
```

#### Docker Build Details

The Dockerfile uses a multi-stage build that compiles the application from source inside the container. This means:

- ✅ Works from a fresh clone (no Java/Maven installation required locally)
- ✅ Only Docker is needed to build and run
- ⚠️ First build takes ~2-3 minutes (Maven compilation inside Docker)

The build process:
1. Stage 1: Uses `maven:3.9.9-eclipse-temurin-21` to compile the application
2. Stage 2: Uses lightweight `eclipse-temurin:21-jre-jammy` runtime image with the compiled JAR

This approach ensures consistency across environments and simplifies onboarding for new contributors.

#### Running Neo4j only (for local Java development)

If you're running the Java application locally (e.g., from your IDE), you can start only Neo4j:

```bash
COMPOSE_PROFILES= docker compose up -d
```

Or equivalently:

```bash
docker compose up neo4j -d
```

This is useful during development when you want faster iteration with your local Java process.

#### Port conflicts

If port `1337` is already in use (for example, the `chatbot` app is running), override the exposed port:

```bash
GUIDE_PORT=1338 docker compose --profile java up --build -d
```

This maps container port `1337` → host port `1338`, so the MCP endpoint becomes:

- `http://localhost:1338/mcp`

#### Compose config overrides

Docker Compose supports environment variable overrides. You can set them inline (shown below) or put them in a local
`.env` file next to `compose.yaml` (Docker Compose auto-loads it).

- **`GUIDE_PORT`**: override host port mapping (default `1337`)
- **`OPENAI_API_KEY`**: required for LLM calls
- **`NEO4J_VERSION` / `NEO4J_USERNAME` / `NEO4J_PASSWORD`**: Neo4j settings (optional)
- **`DISCORD_TOKEN`**: optional, to enable the Discord bot

#### LLM API key

For local/MCP use, the `guide` container needs at least one LLM provider key. Supported providers (in auto-detection order): `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, `MISTRAL_API_KEY`, `DEEPSEEK_API_KEY`.

For hub/web deployments, no server-side key is needed — users bring their own via **Settings → Integrations**.

1. **Create a `.env` file** next to `compose.yaml`:

```bash
OPENAI_API_KEY=sk-your-key-here
```

2. **Or pass it inline**:

```bash
OPENAI_API_KEY=sk-... docker compose --profile java up --build -d
```

#### Verify MCP

```bash
PORT=${GUIDE_PORT:-1337}
curl -i --max-time 5 -X POST "http://localhost:${PORT}/mcp" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"curl","version":"0"}}}'
```

You should get a `200` with an `Mcp-Session-Id` response header.

#### Stop

```bash
docker compose --profile java down --remove-orphans
```

### Environment Variables

| Variable           | Default                        | Description                                      |
|--------------------|--------------------------------|--------------------------------------------------|
| `COMPOSE_PROFILES` | `java`                         | Set to empty to run Neo4j only (no Java service) |
| `NEO4J_VERSION`    | `2025.10.1-community-bullseye` | Neo4j Docker image tag                           |
| `NEO4J_USERNAME`   | `neo4j`                        | Neo4j username                                   |
| `NEO4J_PASSWORD`   | `brahmsian`                    | Neo4j password                                   |
| `NEO4J_HTTP_PORT`  | `7474`                         | Neo4j HTTP port                                  |
| `NEO4J_BOLT_PORT`  | `7687`                         | Neo4j Bolt port                                  |
| `NEO4J_HTTPS_PORT` | `7473`                         | Neo4j HTTPS port                                 |
| `OPENAI_API_KEY`   | (optional)                     | OpenAI API key (or any one provider key below)   |
| `ANTHROPIC_API_KEY`| (optional)                     | Anthropic API key                                |
| `MISTRAL_API_KEY`  | (optional)                     | Mistral API key                                  |
| `DEEPSEEK_API_KEY` | (optional)                     | DeepSeek API key                                 |
| `EMBABEL_KEY_SECRET`| (recommended)                 | AES key for BYOK key encryption (`openssl rand -base64 32`) |
| `DISCORD_TOKEN`    | (optional)                     | Discord bot token                                |

Example:

```bash
NEO4J_PASSWORD=mysecretpassword OPENAI_API_KEY=sk-... GUIDE_PORT=1338 docker compose --profile java up --build -d
```

## Testing

### Prerequisites

Tests require the following (plus **JDK 26** — see [Prerequisites](#prerequisites)):

1. **LLM API Key**: Set at least one provider key in your environment before running tests:

```bash
export OPENAI_API_KEY=sk-your-key-here
```

2. **Neo4j**: See the [Local vs CI Testing](#local-vs-ci-testing) section below.

### Local vs CI Testing

The test suite uses Neo4j, which can be provided in two ways:

| Mode                  | `USE_LOCAL_NEO4J` | How Neo4j is provided                       | Best for                           |
|-----------------------|-------------------|---------------------------------------------|------------------------------------|
| **CI (default)**      | unset/`false`     | Testcontainers spins up Neo4j automatically | GitHub Actions, fresh environments |
| **Local development** | `true`            | You run Neo4j via Docker Compose            | Faster iteration                   |

#### For Local Development

For faster test runs during development, use a local Neo4j instance:

1. **Start Neo4j**:

```bash
docker compose up neo4j -d
```

2. **Run tests with `USE_LOCAL_NEO4J=true`**:

```bash
export OPENAI_API_KEY=sk-your-key-here
USE_LOCAL_NEO4J=true ./mvnw test
```

Or add to your shell profile for persistence:

```bash
export USE_LOCAL_NEO4J=true
```

#### For CI

Leave `USE_LOCAL_NEO4J` unset (the default). GitHub Actions uses Testcontainers to automatically spin up Neo4j.

### Running Tests

```bash
./mvnw test
```

All tests should pass, including:

- Hub API controller tests
- User service tests
- Neo4j repository tests
- **MCP Security regression tests** (verifies `/sse` and `/mcp` endpoints are not blocked by Spring Security)

## Miscellaneous

Sometimes (for example if your IDE crashes) you will be left with an orphaned server process and won't be able to
restart.
To kill the server:

```aiignore
lsof -ti:1337 | xargs kill -9
```

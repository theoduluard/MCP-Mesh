# MCP Mesh

Plateforme d'orchestration et d'observabilité pour serveurs MCP (Model Context Protocol). Connecte plusieurs serveurs MCP, permet de chaîner leurs tools en workflows visuels, et observe en temps réel chaque appel (latence, payload, erreurs).

## 1. Pitch

Le protocole MCP est encore jeune : il n'existe pas aujourd'hui d'outil grand public dédié à l'observabilité MCP (à la manière de ce que LangSmith fait pour LangChain). MCP Mesh comble ce vide en proposant :

- Un **registre** de serveurs MCP connectés
- Un **constructeur de workflows** visuel pour chaîner des tools entre serveurs
- Un **collecteur de traces** qui journalise chaque appel (latence, payloads, erreurs) dans une base time-series
- Un **monitoring live** avec animation du graphe d'exécution en temps réel
- 
## 2. Stack technique

| Couche | Techno | Justification |
|---|---|---|
| Frontend | Angular (Signals, standalone components, nouveau control flow) | Imposé — objectif d'apprentissage |
| Backend | Spring Boot / Spring AI | Orchestration, proxy MCP, montée en compétence Spring AI |
| Traces | QuestDB / InfluxDB | Réutilisation d'une expertise time-series déjà acquise |
| Visualisation graphe | Cytoscape.js ou ngx-graph | Rendu du graphe de workflow et son animation live |
| Temps réel | WebSocket | Streaming des traces d'exécution vers le front |
| Déploiement | Docker sur TrueNAS | Cohérent avec l'infra personnelle existante |

## 3. Architecture générale

```mermaid
flowchart TB
    subgraph Frontend["Angular Frontend"]
        Registry["Registry<br/>(liste des serveurs)"]
        Builder["Workflow Builder<br/>(canvas)"]
        Explorer["Trace Explorer<br/>(historique)"]
        Monitor["Live Monitor<br/>(WebSocket)"]
    end
 
    subgraph Backend["Spring Boot Backend (Orchestrator)"]
        RegSvc["MCP Registry Service"]
        WfEngine["Workflow Engine"]
        TraceCol["Trace Collector"]
        ClientLayer["MCP Client Layer (proxy)"]
    end
 
    subgraph MCPServers["Serveurs MCP"]
        SrvA["MCP Server A<br/>(Slack)"]
        SrvB["MCP Server B<br/>(GitHub)"]
        SrvC["MCP Server C<br/>(perso)"]
    end
 
    TSDB[("QuestDB / InfluxDB<br/>(traces)")]
 
    Frontend -- "REST + WebSocket" --> Backend
    RegSvc --> ClientLayer
    WfEngine --> ClientLayer
    ClientLayer --> TraceCol
    TraceCol --> TSDB
    ClientLayer --> SrvA
    ClientLayer --> SrvB
    ClientLayer --> SrvC
```

## 4. Modèle de données (simplifié)

```mermaid
erDiagram
    McpServer ||--o{ Tool : expose
    McpServer ||--o{ ExecutionTrace : genere
    Workflow ||--o{ ExecutionTrace : produit
    Tool ||--o{ ExecutionTrace : concerne
 
    McpServer {
        string id
        string name
        string url
        string transport
        string status
    }
    Tool {
        string name
        string serverId
        json inputSchema
        json outputSchema
    }
    Workflow {
        string id
        string name
        json nodes
        json edges
    }
    ExecutionTrace {
        string id
        string workflowId
        string toolName
        string serverId
        datetime startTime
        int durationMs
        string status
        json payloadIn
        json payloadOut
        string error
    }
```

## 5. Flux d'un appel de tool (séquence)

```mermaid
sequenceDiagram
    participant UI as Angular UI
    participant BE as Spring Boot Backend
    participant TS as Time-Series DB
    participant MCP as Serveur MCP
 
    UI->>BE: POST /tools/call (toolName, params)
    BE->>MCP: JSON-RPC tools/call
    activate MCP
    MCP-->>BE: résultat / erreur
    deactivate MCP
    BE->>TS: log trace (latence, payload, statut)
    BE-->>UI: réponse REST
    BE-)UI: push WebSocket (trace live)
```

## 6. Roadmap

```mermaid
gantt
    dateFormat  YYYY-MM-DD
    title Roadmap MCP Mesh
    section Phase 1 - Fondations
    Client JSON-RPC maison (stdio)      :p1a, 2026-07-14, 3d
    Endpoints REST + Angular minimal    :p1c, after p1a, 5d
    section Phase 2 - Observabilité
    Migration logs vers time-series DB  :p2a, after p1c, 5d
    Dashboard latence / erreurs         :p2b, after p2a, 5d
    WebSocket temps réel                :p2c, after p2b, 4d
    section Phase 3 - Workflow Engine
    Canvas visuel (drag & drop)         :p3a, after p2c, 10d
    Moteur d'exécution séquentiel       :p3b, after p3a, 8d
    Animation live du graphe            :p3c, after p3b, 6d
    section Phase 4 - Polish
    Auth multi-utilisateurs             :p4a, after p3c, 5d
    Mode replay                         :p4b, after p4a, 4d
    Intégration LLM (Claude API)        :p4c, after p4b, 5d
```

## 7. Tester manuellement l'endpoint `/servers/connect`

En attendant un test automatisé (`MockMvc`/`TestRestTemplate`), voici comment vérifier à la main que l'orchestrateur parle bien à un vrai serveur MCP via `McpClient`. On utilise `FakeMcpServer` (situé dans les sources de test de `client`) comme faux serveur MCP à connecter.

**1. Compiler `client`** (pour avoir les `.class` de `FakeMcpServer`, y compris ses sources de test) :
```bash
cd backend/client
./gradlew compileTestJava
```

**2. Construire le classpath de `FakeMcpServer`** — il a besoin des classes compilées de `client` (main + test) et des jars Jackson (`tools.jackson`) :
```bash
JACKSON_DATABIND=$(find ~/.gradle/caches -iname "jackson-databind-3*.jar" | grep -v sources | grep -v javadoc | head -1)
JACKSON_CORE=$(find ~/.gradle/caches -iname "jackson-core-3*.jar" | grep -v sources | grep -v javadoc | head -1)
JACKSON_ANNOT=$(find ~/.gradle/caches -iname "jackson-annotations-2*.jar" | grep -v sources | grep -v javadoc | head -1)
CP="backend/client/build/classes/java/test:backend/client/build/classes/java/main:$JACKSON_DATABIND:$JACKSON_CORE:$JACKSON_ANNOT"
```

**3. Démarrer l'orchestrateur** (depuis `backend/orchestrator`) :
```bash
cd backend/orchestrator
./gradlew bootRun
```

**4. Appeler l'endpoint**, dans un autre terminal — la commande à connecter est celle qui lance `FakeMcpServer` avec le classpath construit à l'étape 2 :
```bash
curl -X POST http://localhost:8080/servers/connect \
  -H "Content-Type: application/json" \
  -d "{\"command\": [\"java\", \"-cp\", \"$CP\", \"com.mcpmesh.client.FakeMcpServer\"]}"
```

**Résultat attendu (succès)** — `200 OK` :
```json
{"result":{"protocolVersion":"2024-11-05","capabilities":{},"serverInfo":{"name":"fake-mcp-server","version":"0.0.1"}}}
```

**Vérifier aussi le chemin d'erreur**, avec une commande invalide — `400 Bad Request` attendu :
```bash
curl -X POST http://localhost:8080/servers/connect \
  -H "Content-Type: application/json" \
  -d '{"command": ["this-command-does-not-exist"]}'
```
```json
{"message":"Cannot run program \"this-command-does-not-exist\": ..."}
```

# Contribution

Aucune contribution n'est attendue sur ce projet.
Développeur principal : [Théo DULUARD](mailto:theo.duluard7@gmail.com)
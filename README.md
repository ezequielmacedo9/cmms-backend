<div align="center">

<img src="https://img.shields.io/badge/CMMS-Industrial%20Suite-1a1a2e?style=for-the-badge&logo=spring&logoColor=6db33f" alt="CMMS" height="40"/>

# ⚙️ CMMS Backend — Industrial Suite

**API REST · Spring Boot 3.5 · JWT Stateless · PostgreSQL · Render**

*Segurança em camadas · Rate Limiting · CORS configurado · Dual Database (H2/PostgreSQL)*

<br/>

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org)

<br/>

[![Version](https://img.shields.io/badge/version-1.0.0-blue?style=for-the-badge)](https://github.com/ezequielmacedo9/cmms-backend/releases)
[![Deploy](https://img.shields.io/badge/deploy-Render-46E3B7?style=for-the-badge&logo=render&logoColor=white)](https://cmms-backend-8y7h.onrender.com)
[![License](https://img.shields.io/badge/license-MIT-brightgreen?style=for-the-badge)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-orange?style=for-the-badge)](https://github.com/ezequielmacedo9/cmms-backend/issues)

<br/>

[![📖 Swagger UI](https://img.shields.io/badge/📖%20Swagger%20UI-Interativo-blue?style=for-the-badge)](https://cmms-backend-8y7h.onrender.com/swagger-ui.html)
[![🏓 Health Check](https://img.shields.io/badge/🏓%20Health%20Check-/ping-brightgreen?style=for-the-badge)](https://cmms-backend-8y7h.onrender.com/ping)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Ezequiel%20Macedo-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/ezequielmacedo444)

</div>

---

## 🎯 Responsabilidades desta API

Este backend serve como núcleo do CMMS Industrial Suite — responsável por:

- **Autenticação e autorização** — JWT stateless com refresh token de 7 dias
- **Domínio industrial** — máquinas, ordens de manutenção e estoque de peças
- **Segurança em múltiplas camadas** — Spring Security + Rate Limiting por IP
- **Persistência dual** — H2 em memória (dev) + PostgreSQL (produção)
- **API documentada** — Swagger UI gerado automaticamente via SpringDoc OpenAPI

---

## 🌐 Endpoints Base

| Recurso | URL |
|:---|:---|
| 🔧 **API Base** | https://cmms-backend-8y7h.onrender.com |
| 📖 **Swagger UI** | https://cmms-backend-8y7h.onrender.com/swagger-ui.html |
| 🏓 **Health Check** | https://cmms-backend-8y7h.onrender.com/ping |
| 🖥️ **Frontend** | https://cmms-frontend-ezequielmacedo9s-projects.vercel.app |

> ⚠️ **Render Free Tier:** o serviço hiberna após 15 min de inatividade. O frontend faz pings a cada 14 min para manter o servidor ativo.

---

## 🏗️ Arquitetura da Aplicação

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         Spring Boot Application                            │
│                                                                            │
│  ┌────────────────────────────────────────────────────────────────────┐   │
│  │                      Filter Chain (ordem de execução)              │   │
│  │                                                                     │   │
│  │   Request ──▶ CorsFilter ──▶ RateLimitFilter ──▶ JwtAuthFilter    │   │
│  │                                                        │            │   │
│  │               (OPTIONS: skip)  (10 req/min/IP)    valida JWT       │   │
│  │                                                        │            │   │
│  │                                              SecurityContextHolder │   │
│  └────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│  ┌─────────────────────────────────▼──────────────────────────────────┐   │
│  │                         DispatcherServlet                           │   │
│  │                                                                     │   │
│  │   AuthController   MaquinaController   ManutencaoController        │   │
│  │   PecaController                                                    │   │
│  └──────────────────────────────┬──────────────────────────────────────┘  │
│                                  │                                          │
│  ┌───────────────────────────────▼─────────────────────────────────────┐  │
│  │                         Service Layer                                │  │
│  │                                                                      │  │
│  │   AuthService   MaquinaService   ManutencaoService   PecaService    │  │
│  │   JwtService    RefreshTokenService                                  │  │
│  └──────────────────────────────┬───────────────────────────────────────┘ │
│                                  │                                          │
│  ┌───────────────────────────────▼─────────────────────────────────────┐  │
│  │                       Repository Layer (JPA)                         │  │
│  │                                                                      │  │
│  │   UsuarioRepository   MaquinaRepository   ManutencaoRepository      │  │
│  │   PecaRepository      RefreshTokenRepository                         │  │
│  └──────────────────────────────┬───────────────────────────────────────┘ │
└─────────────────────────────────┼──────────────────────────────────────────┘
                                   │
                    ┌──────────────▼────────────────┐
                    │     H2 (dev) / PostgreSQL (prod) │
                    └──────────────────────────────────┘
```

### Fluxo JWT detalhado

```
  Cliente                          API
    │                               │
    │──POST /api/auth/login─────────▶│
    │   { email, senha }            │  BCrypt.matches(senha, hash)
    │                               │  gera accessToken  (HS256, 24h)
    │                               │  gera refreshToken (7 dias, salvo no DB)
    │◀──{ accessToken, refreshToken }│
    │                               │
    │──GET /api/maquinas────────────▶│  JwtAuthFilter.doFilterInternal()
    │   Authorization: Bearer <at>  │  → extrai subject (email)
    │                               │  → carrega UserDetails
    │                               │  → seta SecurityContextHolder
    │◀──[ { id, nome, status, ... } ]│
    │                               │
    │──GET /api/maquinas (401)───────▶│  accessToken expirado
    │◀──401 Unauthorized             │
    │                               │
    │──POST /api/auth/refresh───────▶│  RefreshTokenService.verify()
    │   { refreshToken }            │  → valida no banco + expiração
    │                               │  → gera novo accessToken
    │◀──{ accessToken }              │
    │                               │
    │──retry GET /api/maquinas──────▶│  Bearer <novo accessToken>
    │◀──[ { ... } ]                  │
```

---

## 📡 API Reference Completa

**Base URL:** `https://cmms-backend-8y7h.onrender.com`

### 🔓 Autenticação (público)

| Método | Endpoint | Descrição |
|:---:|:---|:---|
| `POST` | `/api/auth/login` | Login com email e senha |
| `POST` | `/api/auth/refresh` | Renova accessToken com refreshToken |
| `GET` | `/ping` | Health check — retorna `pong` |

**POST /api/auth/login**
```json
// Request
{ "email": "admin@email.com", "senha": "123456" }

// Response 200
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**POST /api/auth/refresh**
```json
// Request
{ "refreshToken": "550e8400-e29b-41d4-a716-446655440000" }

// Response 200
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9..." }
```

---

### 🏭 Máquinas `— requer Authorization: Bearer <token>`

| Método | Endpoint | Descrição |
|:---:|:---|:---|
| `GET` | `/api/maquinas` | Lista todas as máquinas |
| `POST` | `/api/maquinas` | Cadastra nova máquina |
| `PUT` | `/api/maquinas/{id}` | Atualiza máquina existente |
| `DELETE` | `/api/maquinas/{id}` | Remove máquina |

**Modelo Maquina:**
```json
{
  "id": 1,
  "nome": "Compressor Atlas Copco GA 22",
  "localizacao": "Linha A — Setor 3",
  "status": "ATIVA",
  "dataUltimaManutencao": "2024-11-15",
  "intervaloPreventivaDias": 90,
  "observacoes": "Verificar filtro de óleo a cada ciclo"
}
```

**Status válidos:** `ATIVA` · `EM_MANUTENCAO` · `INATIVA`

---

### 🔧 Manutenções `— requer Authorization: Bearer <token>`

| Método | Endpoint | Descrição |
|:---:|:---|:---|
| `GET` | `/api/manutencoes` | Lista todas as ordens |
| `POST` | `/api/manutencoes/{maquinaId}` | Cria ordem vinculada a uma máquina |

**Modelo Manutencao:**
```json
{
  "id": 42,
  "maquinaId": 1,
  "maquinaNome": "Compressor Atlas Copco GA 22",
  "tipo": "PREVENTIVA",
  "descricao": "Troca de filtro de óleo e correias",
  "status": "CONCLUIDA",
  "dataManutencao": "2024-11-15",
  "tecnico": "Carlos Silva"
}
```

**Tipos:** `PREVENTIVA` · `CORRETIVA` · `PREDITIVA`
**Status:** `PENDENTE` · `EM_ANDAMENTO` · `CONCLUIDA`

---

### 📦 Peças / Estoque `— requer Authorization: Bearer <token>`

| Método | Endpoint | Descrição |
|:---:|:---|:---|
| `GET` | `/api/pecas` | Lista todo o estoque |
| `POST` | `/api/pecas` | Cadastra nova peça |
| `PUT` | `/api/pecas/{id}` | Atualiza peça existente |
| `DELETE` | `/api/pecas/{id}` | Remove peça do estoque |

**Modelo Peca:**
```json
{
  "id": 7,
  "nome": "Filtro de Óleo HF6540",
  "codigo": "FO-HF6540",
  "quantidade": 12,
  "custoUnitario": 89.90,
  "vidaUtilHoras": 2000,
  "quantidadeMinima": 3
}
```

📖 **Documentação interativa completa:** [swagger-ui.html](https://cmms-backend-8y7h.onrender.com/swagger-ui.html)

---

## 🛠️ Stack Tecnológica

| Tecnologia | Versão | Papel no projeto |
|:---|:---:|:---|
| ☕ **Java** | 17 | LTS — records, sealed classes, pattern matching |
| 🍃 **Spring Boot** | 3.5.x | Auto-configuration, embedded Tomcat, production-ready |
| 🔒 **Spring Security** | 6.x | Filtro JWT stateless — zero sessão server-side |
| 🗄️ **Spring Data JPA** | 6.x | Repositórios tipados e queries derivadas por convenção |
| 🔑 **JJWT** | 0.11.5 | Geração, assinatura HS256 e validação de tokens |
| 🐘 **PostgreSQL** | 15 | Banco de produção no Render |
| 💾 **H2** | embedded | Banco in-memory para desenvolvimento local |
| 🚦 **Bucket4j** | latest | Rate limiting por IP — 10 requisições/minuto |
| 📖 **SpringDoc OpenAPI** | latest | Swagger UI gerado automaticamente |
| 📦 **Maven** | 3.x | Gerenciamento de dependências e build lifecycle |
| 🐳 **Docker** | — | Containerização para deploy no Render |

---

## 📁 Estrutura do Projeto

```
cmms/
└── src/main/java/br/com/cmms/cmms/
    ├── 🔒 Security/
    │   ├── JwtAuthFilter.java            # Valida Bearer Token em toda request
    │   ├── JwtService.java               # Geração e parsing de JWT (HS256)
    │   ├── SecurityConfig.java           # Spring Security + CORS + headers
    │   ├── RateLimitFilter.java          # Bucket4j — 10 req/min por IP
    │   └── UserDetailsServiceImpl.java   # Carrega usuário do banco para o contexto
    │
    ├── 🎮 controller/
    │   ├── AuthController.java           # /api/auth/login, /api/auth/refresh
    │   ├── MaquinaController.java        # /api/maquinas CRUD
    │   ├── ManutencaoController.java     # /api/manutencoes
    │   └── PecaController.java           # /api/pecas CRUD
    │
    ├── 📋 model/
    │   ├── Usuario.java                  # Entidade JPA + UserDetails
    │   ├── Maquina.java
    │   ├── Manutencao.java
    │   ├── Peca.java
    │   └── RefreshToken.java             # Token persistido no banco (7 dias)
    │
    ├── ⚙️ service/
    │   ├── AuthService.java
    │   ├── MaquinaService.java
    │   ├── ManutencaoService.java
    │   ├── PecaService.java
    │   └── RefreshTokenService.java      # Validação e rotação de refresh tokens
    │
    ├── 🗄️ repository/
    │   ├── UsuarioRepository.java
    │   ├── MaquinaRepository.java
    │   ├── ManutencaoRepository.java
    │   ├── PecaRepository.java
    │   └── RefreshTokenRepository.java
    │
    ├── 🧱 config/
    │   └── DataInitializer.java          # Seed de usuário admin (first-run only)
    │
    └── ⚠️ expection/
        └── GlobalExceptionHandler.java   # 404 / 400 / 500 padronizados

src/main/resources/
    ├── application.properties            # Config base (H2, perfil dev)
    └── application-prod.properties       # Config produção (PostgreSQL, vars de env)
```

---

## 🚀 Como Rodar Localmente

### Pré-requisitos

- **Java 17+** — `java --version`
- **Maven 3.x** — `mvn --version` (ou use o wrapper `./mvnw`)

### Executar (modo dev — H2 in-memory)

```bash
# 1. Clone o repositório
git clone https://github.com/ezequielmacedo9/cmms-backend
cd cmms-backend/cmms

# 2. Execute com Maven Wrapper (sem instalar Maven)
./mvnw spring-boot:run

# Windows:
mvnw.cmd spring-boot:run
```

Recursos disponíveis localmente:

| Recurso | URL |
|:---|:---|
| 🌐 API | http://localhost:8080 |
| 📖 Swagger UI | http://localhost:8080/swagger-ui.html |
| 🗄️ H2 Console | http://localhost:8080/h2-console |
| 🏓 Health | http://localhost:8080/ping |

**H2 Console:** JDBC URL `jdbc:h2:mem:testdb` · User: `sa` · Password: *(vazio)*

**Credenciais demo:** `admin@email.com` / `123456`

---

## ⚙️ Variáveis de Ambiente (Produção)

Configure estas variáveis no painel do Render (ou no seu `.env` para deploy customizado):

| Variável | Obrigatória | Descrição |
|:---|:---:|:---|
| `SPRING_PROFILES_ACTIVE` | ✅ | Deve ser `prod` para usar PostgreSQL |
| `PORT` | ✅ | Porta do servidor (Render injeta automaticamente) |
| `DATABASE_URL` | ✅ | `jdbc:postgresql://<host>:5432/<database>` |
| `SPRING_DATASOURCE_USERNAME` | ✅ | Usuário do PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | ✅ | Senha do PostgreSQL |
| `JWT_SECRET` | ✅ | Chave HS256 — mínimo 256 bits (32 caracteres) |

> ⚠️ **Nunca use valor padrão para `JWT_SECRET` em produção.** A aplicação falha intencionalmente na inicialização se a variável estiver ausente.

**Exemplo de geração de JWT_SECRET seguro:**
```bash
# Linux/macOS
openssl rand -base64 64

# Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
```

---

## ☁️ Deploy no Render

### Deploy automático via Git

1. **Conecte o repositório** no painel do Render → New Web Service
2. **Selecione** `cmms-backend` → branch `main`
3. **Configure o build:**
   - **Build Command:** `./mvnw clean package -DskipTests`
   - **Start Command:** `java -jar target/*.jar`
4. **Configure as variáveis de ambiente** (tabela acima)
5. **Crie o banco:** New PostgreSQL → copie a `Internal Connection String` para `DATABASE_URL`

### Forçar redeploy

```bash
# Via CLI do Render (ou pelo painel → Manual Deploy)
git commit --allow-empty -m "chore: trigger redeploy"
git push
```

---

## 🔐 Segurança em Camadas

| Camada | Implementação | Proteção |
|:---|:---|:---|
| **CORS** | `setAllowedOriginPatterns` com `*.vercel.app` | Bloqueia origens não autorizadas |
| **Rate Limit** | Bucket4j — 10 req/min por IP | Brute-force e DDoS |
| **JWT** | HS256, expiração 24h, secret obrigatório | Tokens não forjáveis |
| **Refresh Token** | UUID + expiração 7d + validação no banco | Renovação rastreável |
| **BCrypt** | Custo 10 para hash de senhas | Rainbow tables |
| **Headers HTTP** | `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy` | Clickjacking, data leaks |
| **STATELESS** | `SessionCreationPolicy.STATELESS` | Zero estado server-side |
| **Sem logs sensíveis** | JWT parsing sem `System.out.println` | Proteção de tokens em logs |
| **Fail-fast** | JWT_SECRET sem fallback | Impossível subir sem configuração |

### CORS — Origens Permitidas

```java
List.of(
    "https://*.vercel.app",                                                    // todos os deploys Vercel
    "https://cmms-frontend-ezequielmacedo9s-projects.vercel.app",    // URL atual de produção
    "http://localhost:4200"                                                     // desenvolvimento local
)
```

---

## 🗄️ Diagrama de Entidades

```
┌─────────────┐         ┌──────────────────┐         ┌──────────────┐
│   Usuario   │         │     Maquina      │         │  Manutencao  │
│─────────────│         │──────────────────│         │──────────────│
│ id (PK)     │         │ id (PK)          │◀────────│ id (PK)      │
│ email       │         │ nome             │  1    N │ maquina (FK) │
│ senha       │         │ localizacao      │         │ tipo         │
│ nome        │         │ status           │         │ descricao    │
│ role        │         │ dataUltManut     │         │ status       │
└─────────────┘         │ intervPrevDias   │         │ dataManut    │
                        │ observacoes      │         │ tecnico      │
┌──────────────┐        └──────────────────┘         └──────────────┘
│ RefreshToken │
│──────────────│         ┌──────────────┐
│ id (PK)      │         │    Peca      │
│ token        │         │──────────────│
│ usuario (FK) │         │ id (PK)      │
│ expiresAt    │         │ nome         │
└──────────────┘         │ codigo       │
                         │ quantidade   │
                         │ custoUnit    │
                         │ vidaUtilHrs  │
                         │ qtdMinima    │
                         └──────────────┘
```

---

## 📊 Configuração HikariCP (Produção)

Otimizado para o plano gratuito do Render (limite de conexões PostgreSQL):

```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.connection-init-sql=SET search_path=public
```

---

## 🗺️ Roadmap

- [x] ✅ JWT stateless com access + refresh token
- [x] ✅ CRUD completo — máquinas, manutenções, peças
- [x] ✅ Rate limiting por IP (Bucket4j)
- [x] ✅ Dual database H2 (dev) + PostgreSQL (prod)
- [x] ✅ Swagger UI auto-gerado
- [x] ✅ Deploy containerizado no Render
- [ ] 📄 Paginação nas listagens (Pageable)
- [ ] 📧 Notificações por email — JavaMailSender
- [ ] 📑 Relatórios PDF — JasperReports
- [ ] 🏢 Multi-tenant com Row-Level Security no PostgreSQL
- [ ] 🤖 Integração com IA para predição de falhas
- [ ] 📡 WebSocket para atualizações em tempo real

---

## 🤝 Contribuindo

```bash
# 1. Faça um fork do repositório

# 2. Crie uma branch descritiva
git checkout -b feature/paginacao-maquinas

# 3. Implemente e teste
./mvnw test

# 4. Commit com mensagem semântica
git commit -m "feat: adiciona paginação server-side no endpoint de máquinas"

# 5. Push e abra um Pull Request
git push origin feature/paginacao-maquinas
```

**Padrões do projeto:**
- Commits: [Conventional Commits](https://www.conventionalcommits.org/pt-br/)
- Injeção de dependências: constructor injection (sem `@Autowired` em campo)
- Logs: `SLF4J` — sem `System.out.println`
- Exceções: `GlobalExceptionHandler` para respostas padronizadas

---

## 📄 Licença

Distribuído sob a licença MIT. Veja [`LICENSE`](LICENSE) para mais informações.

---

<div align="center">

### 👨‍💻 Desenvolvido por Ezequiel Macedo

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Ezequiel%20Macedo-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/ezequielmacedo444)
[![GitHub](https://img.shields.io/badge/GitHub-ezequielmacedo9-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/ezequielmacedo9)

*CMMS Industrial Suite v1.0.0 · ☕ Spring Boot + ⚡ Angular*

⭐ **Se este projeto te ajudou, deixa uma estrela no repositório!**

</div>

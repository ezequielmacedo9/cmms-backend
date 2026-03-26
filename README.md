# ⚙️ CMMS Industrial Suite

> **Computerized Maintenance Management System** — Sistema profissional de gestão de manutenção industrial com autenticação JWT, CRUD completo e deploy em nuvem.

![Status](https://img.shields.io/badge/status-production-brightgreen)
![Backend](https://img.shields.io/badge/backend-Spring%20Boot%203.5-brightgreen)
![Frontend](https://img.shields.io/badge/frontend-Angular%2017+-red)
![Database](https://img.shields.io/badge/database-PostgreSQL-blue)
![Deploy](https://img.shields.io/badge/deploy-Render%20%2B%20Vercel-black)

---

## 🌐 Live Demo

| Serviço | URL |
|---|---|
| 🖥️ Frontend | [cmms-frontend-sr11.vercel.app](https://cmms-frontend-sr11.vercel.app) |
| 🔧 Backend API | [cmms-backend-8y7h.onrender.com](https://cmms-backend-8y7h.onrender.com) |

**Credenciais de demonstração:**
```
Email: admin@email.com
Senha: 123456
```

---

## 📋 Sobre o Projeto

O **CMMS Industrial Suite** é uma aplicação full-stack para controle de manutenção industrial. Permite o gerenciamento completo de máquinas, ordens de manutenção e estoque de peças, com autenticação segura via JWT e interface dark premium com Angular Material.

### Funcionalidades

- 🔐 **Autenticação JWT** com Access Token + Refresh Token
- 🏭 **Gestão de Máquinas** — cadastro, edição, exclusão e controle de status
- 🔧 **Ordens de Manutenção** — preventiva, corretiva e preditiva vinculadas à máquina
- 📦 **Estoque de Peças** — controle de quantidade, custo unitário e vida útil
- 📊 **Dashboard** — KPIs em tempo real com sidebar animada e quick actions
- 🌙 **Dark Theme Premium** — glassmorphism, animações e layout responsivo

---

## 🛠️ Stack Tecnológica

### Backend
| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 17 | Linguagem principal |
| Spring Boot | 3.5.9 | Framework principal |
| Spring Security | 6.x | Autenticação e autorização |
| Spring Data JPA | 6.x | Persistência de dados |
| JJWT | 0.11.5 | Geração e validação de tokens JWT |
| PostgreSQL | 15 | Banco de dados em produção |
| H2 | - | Banco de dados em desenvolvimento |
| SpringDoc OpenAPI | - | Documentação Swagger |
| Maven | 3.x | Gerenciamento de dependências |

### Frontend
| Tecnologia | Versão | Uso |
|---|---|---|
| Angular | 17+ | Framework principal (standalone) |
| TypeScript | 5.x | Linguagem principal |
| Angular Material | 17+ | Componentes UI |
| RxJS | 7.x | Programação reativa |
| HTTP Interceptor | - | Injeção automática do JWT |
| Auth Guard | - | Proteção de rotas |

### DevOps & Infraestrutura
| Serviço | Uso |
|---|---|
| Render | Deploy do backend + PostgreSQL |
| Vercel | Deploy do frontend |
| Docker | Containerização do backend |
| GitHub | Controle de versão |

---

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────────────────────┐
│                    CMMS Industrial Suite                  │
├──────────────────────────┬──────────────────────────────┤
│   Frontend (Vercel)      │    Backend (Render)           │
│   Angular 17+ SPA        │    Spring Boot 3.5            │
│                          │                               │
│  ┌─────────────────┐     │   ┌─────────────────────┐    │
│  │   Auth Guard    │     │   │  Spring Security     │    │
│  │   JWT Intercept │────▶│   │  JWT Filter          │    │
│  │   Services      │     │   │  Controllers         │    │
│  │   Components    │     │   │  Services            │    │
│  └─────────────────┘     │   │  Repositories        │    │
│                          │   └──────────┬──────────┘    │
│                          │              │                │
│                          │   ┌──────────▼──────────┐    │
│                          │   │  PostgreSQL (Render) │    │
│                          │   └─────────────────────┘    │
└──────────────────────────┴──────────────────────────────┘
```

---

## 📁 Estrutura do Projeto

### Backend
```
br.com.cmms.cmms
├── Security/
│   ├── JwtAuthFilter.java
│   ├── JwtService.java
│   ├── SecurityConfig.java
│   └── UserDetailsServiceImpl.java
├── controller/
│   ├── AuthController.java
│   ├── MaquinaController.java
│   ├── ManutencaoController.java
│   └── PecaController.java
├── model/
│   ├── Usuario.java
│   ├── Maquina.java
│   ├── Manutencao.java
│   └── Peca.java
├── service/
└── repository/
```

### Frontend
```
src/app
├── guards/
│   └── auth-guard.ts
├── interceptors/
│   └── auth-interceptor.ts
├── models/
├── pages/
│   ├── login/
│   ├── dashboard/
│   ├── maquinas/
│   ├── manutencoes/
│   └── estoque/
└── services/
```

---

## 🔌 API Endpoints

```
POST   /api/auth/login              → Autenticação
POST   /api/auth/refresh            → Renovar token

GET    /api/maquinas                → Listar máquinas
POST   /api/maquinas                → Cadastrar máquina
PUT    /api/maquinas/{id}           → Atualizar máquina
DELETE /api/maquinas/{id}           → Remover máquina

GET    /api/manutencoes             → Listar manutenções
POST   /api/manutencoes/{maquinaId} → Registrar manutenção

GET    /api/pecas                   → Listar peças
POST   /api/pecas                   → Cadastrar peça
PUT    /api/pecas/{id}              → Atualizar peça
DELETE /api/pecas/{id}              → Remover peça
```

---

## 🚀 Como Rodar Localmente

### Pré-requisitos
- Java 17+
- Node.js 18+
- Maven

### Backend
```bash
cd cmms-backend
./mvnw spring-boot:run
# Disponível em: http://localhost:8080
# H2 Console: http://localhost:8080/h2-console
# Swagger: http://localhost:8080/swagger-ui.html
```

### Frontend
```bash
cd cmms-frontend
npm install --legacy-peer-deps
npx ng serve
# Disponível em: http://localhost:4200
```

---

## ⚙️ Variáveis de Ambiente (Produção)

```properties
SERVER_PORT=10000
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
jwt.secret=...
```

---

## 🔐 Segurança

- Tokens JWT com expiração de 24h
- Refresh Token para renovação automática
- Spring Security com filtro JWT em todas as rotas protegidas
- CORS configurado para domínios autorizados
- Senhas armazenadas com BCrypt
- Variáveis sensíveis via environment variables (nunca hardcoded em produção)

---

## 📌 Roadmap

- [ ] Refresh automático do token no frontend
- [ ] Paginação nas listagens
- [ ] Relatórios e gráficos no dashboard
- [ ] Notificações de manutenção preventiva
- [ ] Exportação de relatórios em PDF
- [ ] App mobile (Angular + Capacitor)

---

## 👨‍💻 Autor

**Ezequiel Macedo**
- GitHub: [@ezequielmacedo9](https://github.com/ezequielmacedo9)

---

> CMMS Industrial Suite v1.0.0 · Desenvolvido com ☕ Java + ⚡ Angular

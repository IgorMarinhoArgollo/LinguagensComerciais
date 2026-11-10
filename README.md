# API de Chamados de Suporte Técnico

API REST em Spring Boot para registrar, consultar e priorizar chamados de suporte técnico (computadores, impressoras, redes e contas de acesso). Os dados ficam salvos em memória (sem persistência), em uma coleção Java, sem banco de dados. O projeto aplica o DDD

## Tecnologias

- Java 23
- Spring Boot 3.5.3
- Maven

## Como executar

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / macOS / Git Bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

O arquivo [requisicoes.http](requisicoes.http) traz todas as requisições da demonstração prontas para executar.

## Arquitetura em camadas

```
src/main/java/br/com/suporte/chamados/
├── ChamadosApplication.java                # Classe principal (inicia o Spring Boot)
├── controllers/                            # Camada HTTP
│   ├── ChamadoController.java              # Endpoints REST (@RestController)
│   ├── ChamadoRequest.java                 # JSON de entrada do POST
│   ├── ChamadoResponse.java                # JSON de saída de um chamado
│   └── TratadorDeErros.java                # Exceções -> 400/404 (@RestControllerAdvice)
├── application/                            # Casos de uso e regras de negócio
│   ├── ChamadoService.java                 # @Service
│   ├── ResumoChamados.java                 # Resultado do /resumo
│   ├── RegraDeNegocioException.java
│   └── ChamadoNaoEncontradoException.java
├── domain/                                 # Modelo do negócio (sem dependência de Spring)
│   ├── Chamado.java                        # Entidade
│   ├── Prioridade.java                     # BAIXA, MEDIA, ALTA
│   ├── Status.java                         # ABERTO, EM_ANDAMENTO, RESOLVIDO
│   └── ChamadoRepository.java              # Interface (contrato) do repositório
└── infrastructure/                         # Detalhes técnicos de armazenamento
    └── ChamadoRepositoryEmMemoria.java     # @Repository: coleção Java + 12 chamados iniciais
```

| Camada | Responsabilidade | Depende de |
|---|---|---|
| `controllers` | Receber a requisição HTTP, converter JSON em DTO, chamar o service e montar a resposta (status, headers, corpo) | `application`, `domain` |
| `application` | Regras de negócio: validações, filtros, critério de chamado crítico, resumo, abertura de chamado | `domain` |
| `domain` | Entidade `Chamado`, enums e o **contrato** `ChamadoRepository` | nada |
| `infrastructure` | **Implementação** do repositório (aqui, em memória) | `domain` |


## Modelo

| Campo         | Tipo         | Descrição                                   |
|---------------|--------------|---------------------------------------------|
| `id`          | `Long`       | Identificador único, gerado automaticamente |
| `titulo`      | `String`     | Descrição resumida da solicitação           |
| `solicitante` | `String`     | Pessoa que abriu o chamado                  |
| `setor`       | `String`     | Departamento de origem                      |
| `prioridade`  | `Prioridade` | `BAIXA`, `MEDIA` ou `ALTA`                  |
| `status`      | `Status`     | `ABERTO`, `EM_ANDAMENTO` ou `RESOLVIDO`     |


## Endpoints

| Método | Rota                                 | Descrição                                            |
|--------|--------------------------------------|------------------------------------------------------|
| GET    | `/chamados`                          | Lista todos os chamados                              |
| GET    | `/chamados/{id}`                     | Consulta um chamado por ID (404 se não existir)      |
| GET    | `/chamados/setor?nome=financeiro`    | Filtra por setor (ignora maiúsculas/minúsculas)      |
| GET    | `/chamados/prioridade?nivel=alta`    | Filtra por prioridade (400 se o nível for inválido)  |
| GET    | `/chamados/busca?texto=impressora`   | Busca parcial no título (ignora maiúsculas/minúsculas) |
| GET    | `/chamados/criticos`                 | Prioridade `ALTA` **e** status diferente de `RESOLVIDO` |
| GET    | `/chamados/resumo`                   | Quantidade por status e total geral                  |
| POST   | `/chamados`                          | Cadastra um chamado e retorna **201 Created**        |

### Regras de negócio (todas no `ChamadoService`)

- **Buscas sem diferenciar maiúsculas/minúsculas**: usa o `equalsIgnoreCase`, e o título e o termo são convertidos para minúsculas antes do `contains`. A prioridade (`nivel` e campo do POST) aceita `alta`, `Alta` ou `ALTA`.
- **`/criticos`** combina dois filtros: `prioridade == ALTA` e `status != RESOLVIDO`.
- **`/resumo`** devolve `{ "abertos", "emAndamento", "resolvidos", "total" }`.
- **POST**:
  - recebe JSON com `titulo`, `solicitante`, `setor` e `prioridade`, todos obrigatórios (**400** se algum faltar);
  - todo chamado novo começa com status `ABERTO`;
  - o ID é gerado pelo sistema. `id` e `status` enviados no JSON são ignorados;
  - responde **201 Created** com o header `Location` apontando para o novo recurso.

---

## Testes e demonstração
### Teste 1: Listar todos os chamados

```bash
curl -i http://localhost:8080/chamados
```

```http
HTTP/1.1 200
Content-Type: application/json

[ {
  "id" : 1,
  "titulo" : "Impressora não imprime",
  "solicitante" : "Ana Souza",
  "setor" : "Financeiro",
  "prioridade" : "ALTA",
  "status" : "ABERTO"
}, {
  "id" : 2,
  "titulo" : "Computador lento ao iniciar",
  "solicitante" : "Bruno Lima",
  "setor" : "RH",
  "prioridade" : "MEDIA",
  "status" : "EM_ANDAMENTO"
},
  ...]
```

**Explicação:** retorna os 12 chamados carregados na inicialização. O Jackson converte a `List<Chamado>` em um array JSON.

### Teste 2: Consultar chamado por ID

```bash
curl -i http://localhost:8080/chamados/3
```

```http
HTTP/1.1 200
Content-Type: application/json

{
  "id" : 3,
  "titulo" : "Sem acesso à rede Wi-Fi",
  "solicitante" : "Carla Mendes",
  "setor" : "Comercial",
  "prioridade" : "ALTA",
  "status" : "EM_ANDAMENTO"
}
```

**Explicação:** o `{id}` da URL é ligado ao parâmetro do método por `@PathVariable`, e o chamado correspondente é retornado.

### Teste 3: ID inexistente (sem correspondência)

```bash
curl -i http://localhost:8080/chamados/999
```

```http
HTTP/1.1 404
Content-Type: application/problem+json

{
  "type" : "about:blank",
  "title" : "Chamado não encontrado",
  "status" : 404,
  "detail" : "Chamado 999 não encontrado.",
  "instance" : "/chamados/999"
}
```

**Explicação:** nenhum chamado tem ID 999. O service lança `ChamadoNaoEncontradoException`, e o `TratadorDeErros` a converte em **404 Not Found**.

### Teste 4: Filtrar por setor (maiúsculas/minúsculas ignoradas)

```bash
curl -i "http://localhost:8080/chamados/setor?nome=FINANCEIRO"
```

```http
HTTP/1.1 200
Content-Type: application/json

[ {
  "id" : 1,
  "titulo" : "Impressora não imprime",
  "solicitante" : "Ana Souza",
  "setor" : "Financeiro",
  "prioridade" : "ALTA",
  "status" : "ABERTO"
}, {
  "id" : 4,
  "titulo" : "Reset de senha do e-mail",
  "solicitante" : "Diego Rocha",
  "setor" : "Financeiro",
  "prioridade" : "BAIXA",
  "status" : "RESOLVIDO"
}, {
  "id" : 9,
  "titulo" : "Instalação de software contábil",
  "solicitante" : "Isabela Nunes",
  "setor" : "Financeiro",
  "prioridade" : "MEDIA",
  "status" : "ABERTO"
} ]
```

**Explicação:** `FINANCEIRO` (maiúsculo) encontrou os chamados do setor `Financeiro`. A comparação usa `equalsIgnoreCase`.

### Teste 5: Filtrar por setor sem correspondência

```bash
curl -i "http://localhost:8080/chamados/setor?nome=diretoria"
```

```http
HTTP/1.1 200
Content-Type: application/json

[ ]
```

**Explicação:** nenhum chamado é do setor "diretoria". O filtro retorna uma lista vazia com status 200, já que a busca foi válida e apenas não encontrou resultados.

### Teste 6: Filtrar por prioridade

```bash
curl -i "http://localhost:8080/chamados/prioridade?nivel=alta"
```

```http
HTTP/1.1 200
Content-Type: application/json

[ { "id" : 1,  "titulo" : "Impressora não imprime",           "setor" : "Financeiro", "prioridade" : "ALTA", "status" : "ABERTO", ... },
  { "id" : 3,  "titulo" : "Sem acesso à rede Wi-Fi",          "setor" : "Comercial",  "prioridade" : "ALTA", "status" : "EM_ANDAMENTO", ... },
  { "id" : 6,  "titulo" : "Conta bloqueada no sistema ERP",   "setor" : "Logística",  "prioridade" : "ALTA", "status" : "RESOLVIDO", ... },
  { "id" : 8,  "titulo" : "Servidor de arquivos inacessível", "setor" : "TI",         "prioridade" : "ALTA", "status" : "ABERTO", ... },
  { "id" : 12, "titulo" : "VPN não conecta em home office",   "setor" : "Comercial",  "prioridade" : "ALTA", "status" : "ABERTO", ... } ]
```

**Explicação:** `alta` é convertido para o enum `Prioridade.ALTA`, e são retornados os 5 chamados com essa prioridade, inclusive o resolvido (ID 6).

### Teste 7: Prioridade inválida

```bash
curl -i "http://localhost:8080/chamados/prioridade?nivel=urgente"
```

```http
HTTP/1.1 400
Content-Type: application/problem+json

{
  "type" : "about:blank",
  "title" : "Requisição inválida",
  "status" : 400,
  "detail" : "Prioridade inválida: 'urgente'. Use BAIXA, MEDIA ou ALTA.",
  "instance" : "/chamados/prioridade"
}
```

**Explicação:** "urgente" não é um valor do enum. O service lança `RegraDeNegocioException`, e o `TratadorDeErros` responde **400 Bad Request**.

### Teste 8: Buscar pelo título

```bash
curl -i "http://localhost:8080/chamados/busca?texto=Impressora"
```

```http
HTTP/1.1 200
Content-Type: application/json

[ {
  "id" : 1,
  "titulo" : "Impressora não imprime",
  "solicitante" : "Ana Souza",
  "setor" : "Financeiro",
  "prioridade" : "ALTA",
  "status" : "ABERTO"
}, {
  "id" : 5,
  "titulo" : "Impressora com papel atolado",
  "solicitante" : "Elisa Martins",
  "setor" : "Jurídico",
  "prioridade" : "MEDIA",
  "status" : "RESOLVIDO"
} ]
```

**Explicação:** a busca é parcial (`contains`) e ignora maiúsculas/minúsculas. Os dois títulos com "impressora" são retornados.

### Teste 9: Busca sem correspondência

```bash
curl -i "http://localhost:8080/chamados/busca?texto=teclado"
```

```http
HTTP/1.1 200
Content-Type: application/json

[ ]
```

**Explicação:** nenhum título contém "teclado", então o resultado é uma lista vazia. Esse mesmo termo é usado no Teste 14 para provar que o cadastro funcionou.

### Teste 10: Chamados críticos

```bash
curl -i http://localhost:8080/chamados/criticos
```

```http
HTTP/1.1 200
Content-Type: application/json

[ { "id" : 1,  "titulo" : "Impressora não imprime",           "prioridade" : "ALTA", "status" : "ABERTO", ... },
  { "id" : 3,  "titulo" : "Sem acesso à rede Wi-Fi",          "prioridade" : "ALTA", "status" : "EM_ANDAMENTO", ... },
  { "id" : 8,  "titulo" : "Servidor de arquivos inacessível", "prioridade" : "ALTA", "status" : "ABERTO", ... },
  { "id" : 12, "titulo" : "VPN não conecta em home office",   "prioridade" : "ALTA", "status" : "ABERTO", ... } ]
```

**Explicação:** dos 5 chamados de prioridade ALTA (Teste 6), o ID 6 está `RESOLVIDO` e fica de fora. Restam 4, o que mostra que os dois filtros (prioridade e status) são aplicados juntos.

### Teste 11: Resumo por status

```bash
curl -i http://localhost:8080/chamados/resumo
```

```http
HTTP/1.1 200
Content-Type: application/json

{
  "abertos" : 6,
  "emAndamento" : 3,
  "resolvidos" : 3,
  "total" : 12
}
```

**Explicação:** contagem de cada status. A soma (6 + 3 + 3) bate com o total de 12.

### Teste 12: Cadastrar novo chamado

```bash
curl -i -X POST http://localhost:8080/chamados -H "Content-Type: application/json" -d '{"titulo":"Teclado sem funcionar","solicitante":"Marina Gomes","setor":"Financeiro","prioridade":"alta"}'
```

```http
HTTP/1.1 201
Location: http://localhost:8080/chamados/13
Content-Type: application/json

{
  "id" : 13,
  "titulo" : "Teclado sem funcionar",
  "solicitante" : "Marina Gomes",
  "setor" : "Financeiro",
  "prioridade" : "ALTA",
  "status" : "ABERTO"
}
```

**Explicação:** o JSON foi convertido em `ChamadoRequest` por `@RequestBody`, e o service abriu o chamado. O ID 13 foi gerado pelo repositório, o status ficou `ABERTO` (regra do service) e `"alta"` virou `ALTA`. A resposta é **201 Created** com o header `Location`. Mesmo enviando `"id": 99` ou `"status": "RESOLVIDO"` no corpo, esses valores são ignorados.

### Teste 13: Verificar o chamado cadastrado por ID

```bash
curl -i http://localhost:8080/chamados/13
```

```http
HTTP/1.1 200
Content-Type: application/json

{
  "id" : 13,
  "titulo" : "Teclado sem funcionar",
  "solicitante" : "Marina Gomes",
  "setor" : "Financeiro",
  "prioridade" : "ALTA",
  "status" : "ABERTO"
}
```

**Explicação:** o chamado criado no Teste 12 está armazenado na lista em memória e pode ser consultado.

### Teste 14: A busca agora encontra o novo chamado

```bash
curl -i "http://localhost:8080/chamados/busca?texto=teclado"
```

```http
HTTP/1.1 200
Content-Type: application/json

[ {
  "id" : 13,
  "titulo" : "Teclado sem funcionar",
  "solicitante" : "Marina Gomes",
  "setor" : "Financeiro",
  "prioridade" : "ALTA",
  "status" : "ABERTO"
} ]
```

**Explicação:** a mesma busca que retornou `[ ]` no Teste 9 agora encontra o chamado cadastrado.

### Teste 15: Resumo atualizado

```bash
curl -i http://localhost:8080/chamados/resumo
```

```http
HTTP/1.1 200
Content-Type: application/json

{
  "abertos" : 7,
  "emAndamento" : 3,
  "resolvidos" : 3,
  "total" : 13
}
```

**Explicação:** `abertos` passou de 6 para 7 e `total` de 12 para 13, refletindo o cadastro.

### Teste 16: Cadastro inválido

```bash
curl -i -X POST http://localhost:8080/chamados -H "Content-Type: application/json" -d '{"titulo":"Sem setor"}'
```

```http
HTTP/1.1 400
Content-Type: application/problem+json

{
  "type" : "about:blank",
  "title" : "Requisição inválida",
  "status" : 400,
  "detail" : "Solicitante é obrigatório.",
  "instance" : "/chamados"
}
```

**Explicação:** faltam campos obrigatórios. O service valida e aponta o primeiro campo ausente (`solicitante`). A API rejeita a requisição com 400 e não cria o chamado.

---

## Análise técnica

### 1. Como o Spring Boot recebe e processa requisições HTTP

1. Ao iniciar, `SpringApplication.run(...)` sobe um servidor Tomcat, que fica escutando a porta 8080.
2. O Spring faz o *component scan* do pacote `br.com.suporte.chamados` e encontra as classes anotadas com `@Repository`, `@Service` e `@RestController`. Ele cria uma instância de cada (*beans* singleton gerenciados pelo container) e as liga por injeção de dependência pelo construtor: o repositório entra no service, e o service entra no controller.
3. O `RequestMappingHandlerMapping` lê as anotações `@RequestMapping`, `@GetMapping` e `@PostMapping` e monta uma tabela que associa cada combinação de método HTTP e caminho a um método Java.



### 2. Função do Controller

O `ChamadoController` é a porta de entrada HTTP da API e não contém regra de negócio. Ele:

- define as rotas (`@RequestMapping("/chamados")` mais os mapeamentos de cada método);
- extrai os dados da requisição (path, query string e corpo JSON);
- delega o processamento ao `ChamadoService`;
- converte as entidades do domínio em DTOs de resposta (`ChamadoResponse`);
- define a resposta HTTP, como o **201 Created** com header `Location` no cadastro.

`@RestController` equivale a `@Controller` + `@ResponseBody`: o valor retornado por cada método é escrito diretamente no corpo da resposta (como JSON).



### 3. Conversão entre objetos Java e JSON

A conversão é feita pela biblioteca Jackson:

- **JSON → Java (entrada):** no POST, o `@RequestBody` faz o Spring ler o corpo com `Content-Type: application/json`. O Jackson cria um `ChamadoRequest` (um `record`) chamando seu construtor e casando os nomes das chaves JSON com os componentes do record (`titulo`, `setor`...). Chaves desconhecidas, como `id` ou `status`, são ignoradas por padrão no Spring Boot.
- **Java → JSON (saída):** o objeto retornado pelo método (`ChamadoResponse`, `List<ChamadoResponse>` ou `ResumoChamados`) é serializado pelo Jackson, que lê os acessores do record (`id()` vira `"id"`, por exemplo). Enums viram strings (`"EM_ANDAMENTO"`) e listas viram arrays JSON.
- Os DTOs (`ChamadoRequest`/`ChamadoResponse`) separam o formato do JSON da entidade de domínio `Chamado`. Assim, a API decide o que entra e o que sai, e o domínio não fica preso ao formato HTTP.
- O Spring escolhe o conversor pela negociação de conteúdo, usando os headers `Content-Type` e `Accept`.



### 4. Por que os chamados cadastrados não permanecem após reiniciar

Os chamados ficam em um `Map<Long, Chamado>` (`ConcurrentSkipListMap`) que é um atributo do objeto `ChamadoRepositoryEmMemoria`, e esse objeto vive na memória RAM da JVM. Ao parar a aplicação, o processo Java termina e o sistema operacional libera toda a memória dele, junto com o mapa e o contador de IDs.

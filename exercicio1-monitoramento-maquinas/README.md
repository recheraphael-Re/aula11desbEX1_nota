# Exercício 1 — Monitoramento de máquinas

API Java 21 e Spring Boot para cadastrar máquinas e acompanhar o consumo de energia e a produção da CloudTech Solutions.

## 1. Executar

Na pasta `exercicio1-monitoramento-maquinas`, use JDK 21 e Maven 3.9:

```powershell
mvn -B verify
mvn spring-boot:run
```

A API responde em `http://localhost:8080`. Para executar o JAR empacotado, use `java -jar target/monitoramento-1.0.0.jar`.

## 2. Endpoints

| Método | Caminho | Resultado esperado |
| --- | --- | --- |
| GET | `/` | 200, mensagem de boas-vindas |
| GET | `/status` | 200, `"status": "online"` |
| GET | `/versao` | 200, `"versao": "1.0.0"` |
| GET | `/maquinas` | 200, lista de máquinas cadastradas |
| POST | `/maquinas/cadastro` | 201, máquina cadastrada com o ID gerado |
| PUT | `/maquinas/{id}` | 200, máquina atualizada com o mesmo ID |
| DELETE | `/maquinas/{id}` | 204, sem corpo |
| POST | `/producao` | 201, registro com consumo, horário e peças produzidas |
| GET | `/producao` | 200, histórico completo |
| GET | `/producao?maquinaId={id}` | 200, histórico filtrado |

O cadastro e o PUT recebem `nome`, `tipo`, `setor` e `status` em JSON. Campos inválidos retornam 400. Alteração, exclusão e registro de produção exigem uma máquina cadastrada e retornam 404 para ID inexistente. Ao excluir uma máquina, ela sai da listagem e seus registros continuam disponíveis em `/producao`, inclusive com filtro pelo ID original.

Modelos ficam em `models`, rotas em `controller` e as regras de cadastro e produção em `service`. A aplicação lê o arquivo `data/monitoramento.json` na inicialização e o grava após operações de escrita. Os dados e os IDs são preservados ao reiniciar com o mesmo arquivo. Configure `APP_DATA_FILE` para escolher outro caminho.

## 3. Testes

`mvn -B verify` executa o JUnit 5 com MockMvc e gera o JAR. O relatório dos testes fica em `target/surefire-reports/`. Os testes usam um arquivo temporário próprio.

Para reproduzir cada requisição no Thunder Client, siga [tests/requisicoes.http](tests/requisicoes.http). Copie o corpo dos POST e do PUT em **Body → JSON** e substitua o ID de exemplo pelo ID recebido no cadastro. Registre método, URL, código HTTP e corpo da resposta. No DELETE 204, capture o status e a ausência de corpo.

## 4. GitHub Actions e Railway

O [workflow](../.github/workflows/pipeline.yml) roda no GitHub em push e pull request. Ele prepara o Java 21, executa `mvn -B verify` na pasta do exercício e disponibiliza o JAR e os XMLs do Surefire como artefatos.

Para testar o contêiner localmente, execute a partir desta pasta:

```powershell
docker build -t cloudtech-monitoramento .
docker run --rm -p 8080:8080 cloudtech-monitoramento
```

Na Railway, conecte este repositório à branch `main`, defina o **Root Directory** como `/exercicio1-monitoramento-maquinas` e ative **Wait for CI** para o deploy automático aguardar os testes. A API usa a variável `PORT` e assume `8080` em execução local. Gere um domínio público e valide `/`, `/status` e os demais endpoints. Se precisar preservar dados entre redeploys, use uma única instância com um volume em `/app/data`.

Use o [relatório](RELATORIO.md) para registrar as evidências da entrega.

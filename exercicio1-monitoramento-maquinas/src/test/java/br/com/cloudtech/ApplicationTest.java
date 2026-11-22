package br.com.cloudtech;

import br.com.cloudtech.service.MonitoramentoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationTest {
    private static final long ID_INEXISTENTE = 999999;
    private static final String MAQUINA = """
            {"nome":"Máquina CNC 01","tipo":"CNC","setor":"Usinagem","status":"em operação"}
            """;
    private static final String ATUALIZADA = """
            {"nome":"Máquina CNC 01","tipo":"CNC","setor":"Usinagem","status":"em manutenção"}
            """;
    private static final String PRODUCAO = """
            {"maquinaId":%d,"consumoKw":12.5,"horarioAcionamento":"08:30","pecasProduzidas":150}
            """;

    @TempDir
    static Path pastaDeDados;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @DynamicPropertySource
    static void configurarArquivo(DynamicPropertyRegistry propriedades) {
        propriedades.add("app.data-file", () -> arquivoDeDados().toString());
    }

    @Test
    void exibeInicioStatusEVersao() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(jsonPath("$.mensagem").isString());
        mvc.perform(get("/status")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("online"));
        mvc.perform(get("/versao")).andExpect(status().isOk()).andExpect(jsonPath("$.versao").value("1.0.0"));
    }

    @Test
    void cadastraEListaMaquinas() throws Exception {
        long id = cadastrarMaquina();
        mvc.perform(get("/maquinas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].tipo").value(contains("CNC")));
    }

    @Test
    void registraEConsultaHistoricoDeProducao() throws Exception {
        long id = cadastrarMaquina();
        registrar(id).andExpect(status().isCreated())
                .andExpect(jsonPath("$.consumoKw").value(12.5))
                .andExpect(jsonPath("$.horarioAcionamento").value("08:30:00"))
                .andExpect(jsonPath("$.pecasProduzidas").value(150));
        registrar(id).andExpect(status().isCreated());
        mvc.perform(get("/producao").param("maquinaId", Long.toString(id)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/producao")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    void alteraExcluiEPreservaHistorico() throws Exception {
        long id = cadastrarMaquina();
        registrar(id).andExpect(status().isCreated());

        alterar(id, ATUALIZADA).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("em manutenção"));
        String filtro = "$[?(@.id == " + id + ")]";
        mvc.perform(get("/maquinas")).andExpect(status().isOk())
                .andExpect(jsonPath(filtro + ".status").value(contains("em manutenção")));

        mvc.perform(delete("/maquinas/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/maquinas")).andExpect(status().isOk())
                .andExpect(jsonPath(filtro).isEmpty());
        registrar(id).andExpect(status().isNotFound());
        mvc.perform(get("/producao").param("maquinaId", Long.toString(id)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].pecasProduzidas").value(150));
    }

    @Test
    void rejeitaDadosInvalidosEIdsInexistentes() throws Exception {
        mvc.perform(post("/maquinas/cadastro").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        alterar(cadastrarMaquina(), "{}").andExpect(status().isBadRequest());
        alterar(ID_INEXISTENTE, ATUALIZADA).andExpect(status().isNotFound());
        mvc.perform(delete("/maquinas/{id}", ID_INEXISTENTE)).andExpect(status().isNotFound());
        mvc.perform(post("/producao").contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCAO.formatted(ID_INEXISTENTE).replace("12.5", "-1")))
                .andExpect(status().isBadRequest());
        registrar(ID_INEXISTENTE).andExpect(status().isNotFound());
        mvc.perform(get("/producao").param("maquinaId", Long.toString(ID_INEXISTENTE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void recuperaMaquinaEHistoricoDoArquivo() throws Exception {
        long id = cadastrarMaquina();
        alterar(id, ATUALIZADA).andExpect(status().isOk());
        registrar(id).andExpect(status().isCreated());

        var reaberto = new MonitoramentoService(json, arquivoDeDados().toString());
        var maquina = reaberto.listar().stream().filter(item -> item.id() == id).findFirst().orElseThrow();

        assertEquals("em manutenção", maquina.status());
        assertEquals(150, reaberto.consultar(id).getFirst().pecasProduzidas());
    }

    private long cadastrarMaquina() throws Exception {
        byte[] resposta = mvc.perform(post("/maquinas/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(MAQUINA.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.tipo").value("CNC"))
                .andReturn().getResponse().getContentAsByteArray();
        return json.readTree(resposta).get("id").asLong();
    }

    private ResultActions alterar(long id, String corpo) throws Exception {
        return mvc.perform(put("/maquinas/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(corpo.getBytes(StandardCharsets.UTF_8)));
    }

    private ResultActions registrar(long id) throws Exception {
        return mvc.perform(post("/producao").contentType(MediaType.APPLICATION_JSON)
                .content(PRODUCAO.formatted(id)));
    }

    private static Path arquivoDeDados() {
        return pastaDeDados.resolve("monitoramento.json");
    }
}

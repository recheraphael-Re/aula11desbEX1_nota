package br.com.cloudtech.controller;

import br.com.cloudtech.models.Maquina;
import br.com.cloudtech.models.Producao;
import br.com.cloudtech.service.MonitoramentoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MaquinaController {
    private final MonitoramentoService monitoramento;

    public MaquinaController(MonitoramentoService monitoramento) {
        this.monitoramento = monitoramento;
    }

    @GetMapping("/")
    public Map<String, String> inicio() {
        return Map.of("mensagem", "Bem-vindo ao monitoramento de máquinas da CloudTech Solutions");
    }

    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of("status", "online", "mensagem", "Aplicação de monitoramento em execução");
    }

    @GetMapping("/versao")
    public Map<String, String> versao() {
        return Map.of("versao", "1.0.0");
    }

    @GetMapping("/maquinas")
    public List<Maquina> listar() {
        return monitoramento.listar();
    }

    @PostMapping("/maquinas/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public Maquina cadastrar(@Valid @RequestBody Maquina.Dados dados) {
        return monitoramento.cadastrar(dados);
    }

    @PutMapping("/maquinas/{id}")
    public Maquina alterar(@PathVariable long id, @Valid @RequestBody Maquina.Dados dados) {
        return monitoramento.alterar(id, dados);
    }

    @DeleteMapping("/maquinas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable long id) {
        monitoramento.excluir(id);
    }

    @PostMapping("/producao")
    @ResponseStatus(HttpStatus.CREATED)
    public Producao registrar(@Valid @RequestBody Producao.Dados dados) {
        return monitoramento.registrar(dados);
    }

    @GetMapping("/producao")
    public List<Producao> consultar(@RequestParam(required = false) Long maquinaId) {
        return monitoramento.consultar(maquinaId);
    }
}

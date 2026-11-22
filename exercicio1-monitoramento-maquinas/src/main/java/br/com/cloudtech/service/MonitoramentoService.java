package br.com.cloudtech.service;

import br.com.cloudtech.models.Maquina;
import br.com.cloudtech.models.Producao;
import br.com.cloudtech.storage.ArquivoJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MonitoramentoService {
    private final ArquivoJson<DadosMonitoramento> arquivo;
    private DadosMonitoramento dados;

    public MonitoramentoService(ObjectMapper json, @Value("${app.data-file}") String caminho) {
        arquivo = new ArquivoJson<>(json, caminho, DadosMonitoramento.class);
        dados = arquivo.ler(new DadosMonitoramento(0, 0, List.of(), List.of()));
    }

    public synchronized List<Maquina> listar() {
        return dados.maquinas();
    }

    public synchronized Maquina cadastrar(Maquina.Dados entrada) {
        Maquina maquina = entrada.comId(dados.ultimoIdMaquina() + 1);
        var maquinas = new ArrayList<>(dados.maquinas());
        maquinas.add(maquina);
        salvar(new DadosMonitoramento(maquina.id(), dados.ultimoIdProducao(), maquinas, dados.historico()));
        return maquina;
    }

    public synchronized Maquina alterar(long id, Maquina.Dados entrada) {
        var maquinas = new ArrayList<>(dados.maquinas());
        Maquina atualizada = entrada.comId(id);
        maquinas.set(indiceMaquina(id), atualizada);
        salvar(new DadosMonitoramento(dados.ultimoIdMaquina(), dados.ultimoIdProducao(),
                maquinas, dados.historico()));
        return atualizada;
    }

    public synchronized void excluir(long id) {
        var maquinas = new ArrayList<>(dados.maquinas());
        maquinas.remove(indiceMaquina(id));
        salvar(new DadosMonitoramento(dados.ultimoIdMaquina(), dados.ultimoIdProducao(),
                maquinas, dados.historico()));
    }

    public synchronized Producao registrar(Producao.Dados entrada) {
        indiceMaquina(entrada.maquinaId());
        Producao producao = new Producao(dados.ultimoIdProducao() + 1, entrada.maquinaId(),
                entrada.consumoKw(), entrada.horarioAcionamento(), entrada.pecasProduzidas(), LocalDateTime.now());
        var historico = new ArrayList<>(dados.historico());
        historico.add(producao);
        salvar(new DadosMonitoramento(dados.ultimoIdMaquina(), producao.id(), dados.maquinas(), historico));
        return producao;
    }

    public synchronized List<Producao> consultar(Long maquinaId) {
        if (maquinaId == null) {
            return dados.historico();
        }
        List<Producao> historico = dados.historico().stream()
                .filter(producao -> producao.maquinaId() == maquinaId)
                .toList();
        if (historico.isEmpty()) {
            indiceMaquina(maquinaId);
        }
        return historico;
    }

    private int indiceMaquina(long id) {
        List<Maquina> maquinas = dados.maquinas();
        for (int i = 0; i < maquinas.size(); i++) {
            if (maquinas.get(i).id() == id) {
                return i;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Máquina não encontrada");
    }

    private void salvar(DadosMonitoramento atualizados) {
        arquivo.gravar(atualizados);
        dados = atualizados;
    }

    public record DadosMonitoramento(long ultimoIdMaquina, long ultimoIdProducao,
                                     List<Maquina> maquinas, List<Producao> historico) {
        public DadosMonitoramento {
            maquinas = List.copyOf(maquinas);
            historico = List.copyOf(historico);
        }
    }
}

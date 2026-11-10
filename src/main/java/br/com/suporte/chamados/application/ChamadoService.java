package br.com.suporte.chamados.application;

import br.com.suporte.chamados.domain.Chamado;
import br.com.suporte.chamados.domain.ChamadoRepository;
import br.com.suporte.chamados.domain.Prioridade;
import br.com.suporte.chamados.domain.Status;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ChamadoService {

    private final ChamadoRepository repository;

    public ChamadoService(ChamadoRepository repository) {
        this.repository = repository;
    }

    public List<Chamado> listarTodos() {
        return repository.listarTodos();
    }

    public Chamado buscarPorId(Long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new ChamadoNaoEncontradoException(id));
    }

    public List<Chamado> filtrarPorSetor(String nome) {
        String setor = obrigatorio(nome, "Informe o nome do setor.");
        return repository.listarTodos().stream()
                .filter(c -> c.getSetor().equalsIgnoreCase(setor))
                .toList();
    }

    public List<Chamado> filtrarPorPrioridade(String nivel) {
        Prioridade prioridade = converterPrioridade(nivel);
        return repository.listarTodos().stream()
                .filter(c -> c.getPrioridade() == prioridade)
                .toList();
    }

    public List<Chamado> buscarPorTitulo(String texto) {
        String termo = obrigatorio(texto, "Informe o texto da busca.").toLowerCase(Locale.ROOT);
        return repository.listarTodos().stream()
                .filter(c -> c.getTitulo().toLowerCase(Locale.ROOT).contains(termo))
                .toList();
    }

    public List<Chamado> listarCriticos() {
        return repository.listarTodos().stream()
                .filter(c -> c.getPrioridade() == Prioridade.ALTA)
                .filter(c -> c.getStatus() != Status.RESOLVIDO)
                .toList();
    }

    public ResumoChamados resumo() {
        List<Chamado> todos = repository.listarTodos();
        return new ResumoChamados(
                contar(todos, Status.ABERTO),
                contar(todos, Status.EM_ANDAMENTO),
                contar(todos, Status.RESOLVIDO),
                todos.size());
    }

    public Chamado abrir(String titulo, String solicitante, String setor, String prioridade) {
        Chamado chamado = new Chamado(
                null,
                obrigatorio(titulo, "Título é obrigatório."),
                obrigatorio(solicitante, "Solicitante é obrigatório."),
                obrigatorio(setor, "Setor é obrigatório."),
                converterPrioridade(prioridade),
                Status.ABERTO);
        return repository.salvar(chamado);
    }

    private long contar(List<Chamado> chamados, Status status) {
        return chamados.stream().filter(c -> c.getStatus() == status).count();
    }

    private Prioridade converterPrioridade(String nivel) {
        String valor = obrigatorio(nivel, "Prioridade é obrigatória. Use BAIXA, MEDIA ou ALTA.");
        try {
            return Prioridade.valueOf(valor.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RegraDeNegocioException(
                    "Prioridade inválida: '" + nivel + "'. Use BAIXA, MEDIA ou ALTA.");
        }
    }

    private String obrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new RegraDeNegocioException(mensagem);
        }
        return valor.trim();
    }
}

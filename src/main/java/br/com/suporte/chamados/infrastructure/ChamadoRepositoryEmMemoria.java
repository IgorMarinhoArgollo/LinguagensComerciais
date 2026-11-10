package br.com.suporte.chamados.infrastructure;

import br.com.suporte.chamados.domain.Chamado;
import br.com.suporte.chamados.domain.ChamadoRepository;
import br.com.suporte.chamados.domain.Prioridade;
import br.com.suporte.chamados.domain.Status;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class ChamadoRepositoryEmMemoria implements ChamadoRepository {

    private final Map<Long, Chamado> chamados = new ConcurrentSkipListMap<>();

    private final AtomicLong sequenciaId = new AtomicLong(0);

    public ChamadoRepositoryEmMemoria() {
        carregarDadosIniciais();
    }

    @Override
    public List<Chamado> listarTodos() {
        return List.copyOf(chamados.values());
    }

    @Override
    public Optional<Chamado> buscarPorId(Long id) {
        return Optional.ofNullable(chamados.get(id));
    }

    @Override
    public Chamado salvar(Chamado chamado) {
        if (chamado.getId() == null) {
            chamado.setId(sequenciaId.incrementAndGet());
        }
        chamados.put(chamado.getId(), chamado);
        return chamado;
    }

    private void carregarDadosIniciais() {
        inserir("Impressora não imprime", "Ana Souza", "Financeiro", Prioridade.ALTA, Status.ABERTO);
        inserir("Computador lento ao iniciar", "Bruno Lima", "RH", Prioridade.MEDIA, Status.EM_ANDAMENTO);
        inserir("Sem acesso à rede Wi-Fi", "Carla Mendes", "Comercial", Prioridade.ALTA, Status.EM_ANDAMENTO);
        inserir("Reset de senha do e-mail", "Diego Rocha", "Financeiro", Prioridade.BAIXA, Status.RESOLVIDO);
        inserir("Impressora com papel atolado", "Elisa Martins", "Jurídico", Prioridade.MEDIA, Status.RESOLVIDO);
        inserir("Conta bloqueada no sistema ERP", "Fábio Costa", "Logística", Prioridade.ALTA, Status.RESOLVIDO);
        inserir("Monitor piscando", "Gabriela Alves", "Marketing", Prioridade.BAIXA, Status.ABERTO);
        inserir("Servidor de arquivos inacessível", "Henrique Dias", "TI", Prioridade.ALTA, Status.ABERTO);
        inserir("Instalação de software contábil", "Isabela Nunes", "Financeiro", Prioridade.MEDIA, Status.ABERTO);
        inserir("Cabo de rede danificado", "João Pereira", "Logística", Prioridade.BAIXA, Status.EM_ANDAMENTO);
        inserir("Criação de conta para novo colaborador", "Karina Ribeiro", "RH", Prioridade.MEDIA, Status.ABERTO);
        inserir("VPN não conecta em home office", "Lucas Ferreira", "Comercial", Prioridade.ALTA, Status.ABERTO);
    }

    private void inserir(String titulo, String solicitante, String setor,
                         Prioridade prioridade, Status status) {
        salvar(new Chamado(null, titulo, solicitante, setor, prioridade, status));
    }
}

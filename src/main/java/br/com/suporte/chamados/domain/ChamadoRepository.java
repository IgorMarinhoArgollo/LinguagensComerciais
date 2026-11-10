package br.com.suporte.chamados.domain;

import java.util.List;
import java.util.Optional;

public interface ChamadoRepository {

    List<Chamado> listarTodos();

    Optional<Chamado> buscarPorId(Long id);

    Chamado salvar(Chamado chamado);
}

package br.com.suporte.chamados.controllers;

import br.com.suporte.chamados.domain.Chamado;
import br.com.suporte.chamados.domain.Prioridade;
import br.com.suporte.chamados.domain.Status;

import java.util.List;

public record ChamadoResponse(Long id, String titulo, String solicitante, String setor,
                              Prioridade prioridade, Status status) {

    public static ChamadoResponse de(Chamado chamado) {
        return new ChamadoResponse(chamado.getId(), chamado.getTitulo(), chamado.getSolicitante(),
                chamado.getSetor(), chamado.getPrioridade(), chamado.getStatus());
    }

    public static List<ChamadoResponse> de(List<Chamado> chamados) {
        return chamados.stream().map(ChamadoResponse::de).toList();
    }
}

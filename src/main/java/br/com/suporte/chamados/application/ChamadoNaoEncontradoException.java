package br.com.suporte.chamados.application;

public class ChamadoNaoEncontradoException extends RuntimeException {

    public ChamadoNaoEncontradoException(Long id) {
        super("Chamado " + id + " não encontrado.");
    }
}

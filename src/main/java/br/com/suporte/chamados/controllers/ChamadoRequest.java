package br.com.suporte.chamados.controllers;

public record ChamadoRequest(String titulo, String solicitante, String setor, String prioridade) {
}

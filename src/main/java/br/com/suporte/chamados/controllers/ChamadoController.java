package br.com.suporte.chamados.controllers;

import br.com.suporte.chamados.application.ChamadoService;
import br.com.suporte.chamados.application.ResumoChamados;
import br.com.suporte.chamados.domain.Chamado;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {

    private final ChamadoService service;

    public ChamadoController(ChamadoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ChamadoResponse> listarTodos() {
        return ChamadoResponse.de(service.listarTodos());
    }

    @GetMapping("/{id:\\d+}")
    public ChamadoResponse buscarPorId(@PathVariable Long id) {
        return ChamadoResponse.de(service.buscarPorId(id));
    }

    @GetMapping("/setor")
    public List<ChamadoResponse> filtrarPorSetor(@RequestParam String nome) {
        return ChamadoResponse.de(service.filtrarPorSetor(nome));
    }

    @GetMapping("/prioridade")
    public List<ChamadoResponse> filtrarPorPrioridade(@RequestParam String nivel) {
        return ChamadoResponse.de(service.filtrarPorPrioridade(nivel));
    }

    @GetMapping("/busca")
    public List<ChamadoResponse> buscarPorTitulo(@RequestParam String texto) {
        return ChamadoResponse.de(service.buscarPorTitulo(texto));
    }

    @GetMapping("/criticos")
    public List<ChamadoResponse> listarCriticos() {
        return ChamadoResponse.de(service.listarCriticos());
    }

    @GetMapping("/resumo")
    public ResumoChamados resumo() {
        return service.resumo();
    }

    @PostMapping
    public ResponseEntity<ChamadoResponse> cadastrar(@RequestBody ChamadoRequest request) {
        Chamado criado = service.abrir(request.titulo(), request.solicitante(),
                request.setor(), request.prioridade());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.getId())
                .toUri();

        return ResponseEntity.created(location).body(ChamadoResponse.de(criado));
    }
}

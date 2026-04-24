package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.model.Manutencao;
import br.com.cmms.cmms.service.ManutencaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manutencoes")
public class ManutencaoController {

    private final ManutencaoService manutencaoService;

    public ManutencaoController(ManutencaoService manutencaoService) {
        this.manutencaoService = manutencaoService;
    }

    @PostMapping("/{maquinaId}")
    public ResponseEntity<Manutencao> cadastrar(
            @PathVariable Long maquinaId,
            @RequestBody Manutencao manutencao
    ) {
        return ResponseEntity.ok(manutencaoService.cadastrar(manutencao, maquinaId));
    }

    @GetMapping
    public ResponseEntity<List<Manutencao>> listar() {
        return ResponseEntity.ok(manutencaoService.listar());
    }

    @GetMapping("/maquina/{maquinaId}")
    public ResponseEntity<List<Manutencao>> listarPorMaquina(@PathVariable Long maquinaId) {
        return ResponseEntity.ok(manutencaoService.listarPorMaquina(maquinaId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        manutencaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

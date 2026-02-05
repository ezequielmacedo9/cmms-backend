package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.model.Ferramenta;
import br.com.cmms.cmms.service.FerramentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ferramentas")
public class FerramentaController {

    @Autowired
    private FerramentaService ferramentaService;

    @PostMapping
    public ResponseEntity<Ferramenta> cadastrar(@RequestBody Ferramenta ferramenta) {
        return ResponseEntity.ok(ferramentaService.cadastrar(ferramenta));
    }

    @GetMapping
    public ResponseEntity<List<Ferramenta>> listar() {
        return ResponseEntity.ok(ferramentaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ferramenta> buscarPorId(@PathVariable Long id) {
        Ferramenta f = ferramentaService.buscarPorId(id);
        if (f == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(f);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ferramenta> atualizar(
            @PathVariable Long id,
            @RequestBody Ferramenta ferramenta
    ) {
        return ResponseEntity.ok(ferramentaService.atualizar(id, ferramenta));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        ferramentaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

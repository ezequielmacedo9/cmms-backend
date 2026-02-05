package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.service.MaquinaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maquinas")
public class MaquinaController {

    @Autowired
    private MaquinaService maquinaService;

    @PostMapping
    public ResponseEntity<Maquina> cadastrar(@RequestBody Maquina maquina) {
        return ResponseEntity.ok(maquinaService.cadastrar(maquina));
    }

    @GetMapping
    public ResponseEntity<List<Maquina>> listar() {
        return ResponseEntity.ok(maquinaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Maquina> buscarPorId(@PathVariable Long id) {
        Maquina m = maquinaService.buscarPorId(id);
        if (m == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(m);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Maquina> atualizar(
            @PathVariable Long id,
            @RequestBody Maquina maquina
    ) {
        return ResponseEntity.ok(maquinaService.atualizar(id, maquina));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        maquinaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

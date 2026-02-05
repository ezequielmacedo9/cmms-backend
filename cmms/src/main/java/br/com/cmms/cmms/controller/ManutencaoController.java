package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.model.Manutencao;
import br.com.cmms.cmms.service.ManutencaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manutencoes")
public class ManutencaoController {

    @Autowired
    private ManutencaoService manutencaoService;

    @PostMapping("/{maquinaId}")
    public ResponseEntity<Manutencao> registrar(
            @PathVariable Long maquinaId,
            @RequestBody Manutencao manutencao
    ) {
        return ResponseEntity.ok(manutencaoService.cadastrar(manutencao, maquinaId));
    }

    @GetMapping
    public ResponseEntity<List<Manutencao>> listar() {
        return ResponseEntity.ok(manutencaoService.listar());
    }
}

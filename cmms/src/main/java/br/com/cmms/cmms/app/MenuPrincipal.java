package br.com.cmms.cmms.app;

import br.com.cmms.cmms.dto.MaquinaRequestDTO;
import br.com.cmms.cmms.dto.MaquinaResponseDTO;
import br.com.cmms.cmms.dto.PecaRequestDTO;
import br.com.cmms.cmms.dto.PecaResponseDTO;
import br.com.cmms.cmms.service.FerramentaService;
import br.com.cmms.cmms.service.ManutencaoService;
import br.com.cmms.cmms.service.MaquinaService;
import br.com.cmms.cmms.service.PecaService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class MenuPrincipal {

    private final MaquinaService maquinaService;
    private final PecaService pecaService;
    private final FerramentaService ferramentaService;
    private final ManutencaoService manutencaoService;

    private final Scanner scanner = new Scanner(System.in);

    @Autowired
    public MenuPrincipal(
            MaquinaService maquinaService,
            PecaService pecaService,
            FerramentaService ferramentaService,
            ManutencaoService manutencaoService
    ) {
        this.maquinaService = maquinaService;
        this.pecaService = pecaService;
        this.ferramentaService = ferramentaService;
        this.manutencaoService = manutencaoService;
    }

    public void iniciar() {
        int opcao;

        do {
            System.out.println("""
            ===============================
            SISTEMA CMMS - MANUTENÇÃO
            ===============================

            1  - Cadastrar Máquina
            2  - Listar Máquinas
            3  - Atualizar Máquina
            4  - Deletar Máquina

            5  - Cadastrar Peça
            6  - Listar Peças
            7  - Atualizar Peça
            8  - Deletar Peça

            0  - Sair
            """);

            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1 -> cadastrarMaquina();
                case 2 -> listarMaquinas();
                case 3 -> atualizarMaquina();
                case 4 -> deletarMaquina();

                case 5 -> cadastrarPeca();
                case 6 -> listarPecas();
                case 7 -> atualizarPeca();
                case 8 -> deletarPeca();

                case 0 -> System.out.println("Encerrando sistema...");
                default -> System.out.println("Opção inválida!");
            }

        } while (opcao != 0);
    }

    // ===================== PEÇAS =====================

    private void cadastrarPeca() {
        PecaRequestDTO dto = new PecaRequestDTO();

        System.out.print("Nome da peça: ");
        dto.setNome(scanner.nextLine());

        System.out.print("Código: ");
        dto.setCodigo(scanner.nextLine());

        System.out.print("Quantidade em estoque: ");
        dto.setQuantidadeEmEstoque(Integer.valueOf(scanner.nextInt()));

        System.out.print("Custo unitário: ");
        dto.setCustoUnitario(Double.valueOf(scanner.nextDouble()));

        System.out.print("Vida útil (horas): ");
        dto.setVidaUtilHoras(Integer.valueOf(scanner.nextInt()));
        scanner.nextLine();

        PecaResponseDTO resposta = pecaService.cadastrar(dto);
        System.out.println("Peça cadastrada com sucesso! ID: " + resposta.getId());
    }

    private void listarPecas() {
        List<PecaResponseDTO> pecas = pecaService.listar();

        if (pecas.isEmpty()) {
            System.out.println("Nenhuma peça cadastrada.");
            return;
        }

        System.out.println("=== PEÇAS CADASTRADAS ===");
        pecas.forEach(p -> System.out.println(
                "ID: " + p.getId() +
                        " | Nome: " + p.getNome() +
                        " | Código: " + p.getCodigo() +
                        " | Quantidade: " + p.getQuantidadeEmEstoque() +
                        " | Custo: " + p.getCustoUnitario() +
                        " | Vida útil: " + p.getVidaUtilHoras()
        ));
    }

    private void atualizarPeca() {
        System.out.print("ID da peça: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        PecaRequestDTO dto = new PecaRequestDTO();

        System.out.print("Novo nome: ");
        dto.setNome(scanner.nextLine());

        System.out.print("Novo código: ");
        dto.setCodigo(scanner.nextLine());

        System.out.print("Nova quantidade: ");
        dto.setQuantidadeEmEstoque(Integer.valueOf(scanner.nextInt()));

        System.out.print("Novo custo unitário: ");
        dto.setCustoUnitario(Double.valueOf(scanner.nextDouble()));

        System.out.print("Nova vida útil (horas): ");
        dto.setVidaUtilHoras(Integer.valueOf(scanner.nextInt()));
        scanner.nextLine();

        PecaResponseDTO atualizada = pecaService.atualizar(Long.valueOf(id), dto);
        System.out.println("Peça atualizada com sucesso! ID: " + atualizada.getId());
    }

    private void deletarPeca() {
        System.out.print("ID da peça: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        pecaService.deletar(Long.valueOf(id));
        System.out.println("Peça deletada com sucesso!");
    }

    // ===================== MÁQUINAS =====================

    private void cadastrarMaquina() {
        System.out.print("Nome: ");
        String nome = scanner.nextLine();
        System.out.print("Setor: ");
        String setor = scanner.nextLine();
        System.out.print("Status (ATIVO/INATIVO): ");
        String status = scanner.nextLine();
        System.out.print("Intervalo preventiva (dias): ");
        int intervalo = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Data última manutenção (yyyy-MM-dd): ");
        java.time.LocalDate data = java.time.LocalDate.parse(scanner.nextLine());

        maquinaService.cadastrar(new MaquinaRequestDTO(nome, setor, status, "MEDIA", intervalo, data));
        System.out.println("Máquina cadastrada!");
    }

    private void listarMaquinas() {
        java.util.List<MaquinaResponseDTO> maquinas = maquinaService.listar();

        if (maquinas.isEmpty()) {
            System.out.println("Nenhuma máquina cadastrada.");
            return;
        }

        maquinas.forEach(m -> System.out.println(
                "ID: " + m.id() + " | Nome: " + m.nome() + " | Setor: " + m.setor()
        ));
    }

    private void atualizarMaquina() {
        System.out.print("ID da máquina: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        MaquinaResponseDTO maquina = maquinaService.buscarPorId(id);
        if (maquina == null) {
            System.out.println("Máquina não encontrada.");
            return;
        }

        System.out.print("Novo nome: ");
        String novoNome = scanner.nextLine();
        maquinaService.atualizar(id, new MaquinaRequestDTO(novoNome, maquina.setor(), maquina.status(), maquina.prioridade(), maquina.intervaloPreventivaDias(), maquina.dataUltimaManutencao()));
        System.out.println("Máquina atualizada!");
    }

    private void deletarMaquina() {
        System.out.print("ID da máquina: ");
        long id = scanner.nextLong();
        scanner.nextLine();

        maquinaService.deletar(Long.valueOf(id));
        System.out.println("Máquina deletada!");
    }
}
package clientes;

import classes.AvisoDeErro;
import classes.Mensagem;
import classes.Turma;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class Cliente {
    /*variaveis globais*/
    private ConexaoDoCliente conexaoDoCliente;
    private String idCliente;
    private String tipoDeCliente;
    private Object idAdicional;
    private String TEXTO_VERMELHO = "\u001B[31m"; /*sempre que mudar a cor, resetar para a parão depois*/
    private String TEXT_PADRAO = "\u001B[0m";

    /*construtores*/
    public Cliente(String idCliente, String tipoDeCliente, Object idAdicional){
        this.idCliente = idCliente;
        this.tipoDeCliente = tipoDeCliente;

        if (!Objects.isNull(idAdicional)){
            /*nulo caso seja um professor, matricula caso seja um aluno*/
            this.idAdicional = idAdicional;
        }

        /*estabelecer conexão com servidor*/
        try {
            System.out.println("\nEstabelecendo conexão...\n");
            Socket socket = new Socket("localhost", 5555);
            conexaoDoCliente = new ConexaoDoCliente(socket, this);
            conexaoDoCliente.start();

            /*iniciar interface (loop)*/
            executarInterface();
        } catch (IOException exception){
            /*exceção*/
            AvisoDeErro erro = new AvisoDeErro(exception.getMessage(), null);
            System.out.println(erro.getAviso());
        }
    }

    /*principais*/

    /*metodos*/
    public void executarInterface(){
        /*resetar mensagem do servidor a cada iteração*/
        conexaoDoCliente.setMensagemDoServidor(null);

        /*executar*/
        boolean executando = true;
        while(executando){
            /*menu, escolher uma solicitação*/
            String solicitacao = selecionarSolicitacao(tipoDeCliente);
            if (Objects.equals(solicitacao, "SAIR")){
                conexaoDoCliente.enviarSolicitacao("ENCERRAR_CONEXAO");
                while(true){
                    if (Objects.isNull(conexaoDoCliente.getMensagemDoServidor())){
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        Mensagem mensagemDoServidor = conexaoDoCliente.getMensagemDoServidor();
                        if(Objects.equals(mensagemDoServidor.getParam("ADICIONAL"), "ENCERRAR_CONEXAO")){
                            break;
                        }
                    }
                }
                executando = false;
                conexaoDoCliente.setMensagemDoServidor(null);
                continue;
            } else if(Objects.isNull(solicitacao)){
                executando = false;
                continue;
            }

            /*excolher turma e solicitar para o servidor*/
            Mensagem mensagemDoServidor = gerarSolicitacao(solicitacao, null);
            if (!conexaoDoCliente.getAtivo()){
                /*sair do loop de interface se a conexão estivar desativada*/
                break;
            } else if(Objects.equals(mensagemDoServidor.getSolicitacao(), "CANCELAR_SOLICITACAO")){
                System.out.println("Solicitação Cancelada.");
                continue;
            }

            /*informações importantes*/
            String idTurma = (String) mensagemDoServidor.getParam("TURMA_REFERENCIA");
            int numeroTurma = (Integer) mensagemDoServidor.getParam("NUMERO_TURMA");

            /*continuar interface (executar solicitação; limpar mensagem do servidor após exibir as mensagens)*/
            String informacaoAdicional = (String) mensagemDoServidor.getParam("ADICIONAL");
            switch (informacaoAdicional) {
                /*a informação adicional é lida como: reposta referente à solicitação tal.
                a ação ja foi executada no servidor, agora é só uma exibição visual relacionada à essa ação*/
                case "INICIO_DE_CHAMADA" -> {
                    int tamanhoDaTurma = (Integer) mensagemDoServidor.getParam("TAMANHO_DA_TURMA");
                    int quantidadeDeAlunosPresentes = (Integer) mensagemDoServidor.getParam("QUANTIDADE_DE_ALUNOS_PRESENTES");

                    /*esperar alunos*/
                    System.out.println("Aguardando alunos...\n\n" + "Alunos Presentes: " + quantidadeDeAlunosPresentes + "/" + tamanhoDaTurma + ".\n");
                    conexaoDoCliente.setMensagemDoServidor(null);
                    while (quantidadeDeAlunosPresentes < tamanhoDaTurma) {
                        if (Objects.isNull(conexaoDoCliente.getMensagemDoServidor())){
                            try {
                                if (!conexaoDoCliente.getAtivo()){
                                    /*previnir nova iteração da interface*/
                                    executando = false;

                                    /*sair do loop de espera*/
                                    break;
                                }
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            Mensagem mensagem = conexaoDoCliente.getMensagemDoServidor();
                            String adicional = (String) mensagem.getParam("ADICIONAL");

                            /*confirmação visual da presença registrada*/
                            if (Objects.equals(adicional, "REGISTRO_DE_PRESENCA")) {
                                String idProfessor = (String) mensagem.getParam("ID_PROFESSOR");
                                if (Objects.equals(idProfessor, idCliente)) {
                                    int matriculaDoAluno = (Integer) mensagem.getParam("MATRICULA_DO_ALUNO");
                                    StringBuilder alunoPresente = new StringBuilder();
                                    alunoPresente.append("----------Presença Registrada----------\n")
                                            .append("Aluno (Matrícula): " + matriculaDoAluno)
                                            .append("\n---------------------------------------\n")
                                            .append("\nAlunos Presentes: " + (quantidadeDeAlunosPresentes+1) + "/" + tamanhoDaTurma + ".\n");
                                    System.out.println(alunoPresente);
                                    quantidadeDeAlunosPresentes++;
                                    conexaoDoCliente.setMensagemDoServidor(null);
                                }
                            }
                        }
                    }

                    if (quantidadeDeAlunosPresentes >= tamanhoDaTurma){
                        System.out.println("Todos os alunos confirmaram presença.");
                    }

                }
                case "ENCERRAMENTO_DE_CHAMADA" ->{
                    if (conexaoDoCliente.getAtivo()){
                        /*exibir confirmação de encerramento*/
                        System.out.println("A chamada para a turma " + numeroTurma + " foi encerrada.");

                        /*limpar mensagem do servidor*/
                        conexaoDoCliente.setMensagemDoServidor(null);
                    }
                }
                case "REGISTRO_DE_PRESENCA" -> {
                    if(conexaoDoCliente.getAtivo()){
                        /*verificar presença*/
                        boolean presencaRegistrada = (Boolean) mensagemDoServidor.getParam("PRESENCA_REGISTRADA");

                        /*exibir mensagem*/
                        StringBuilder alunoPresente = new StringBuilder();
                        alunoPresente.append("Sua presença ").append(presencaRegistrada ? "foi " : "não foi ").append("registrada").append(presencaRegistrada ? "." : ": ");
                        String motivo = null;
                        if (!Objects.isNull(mensagemDoServidor.getParam("MOTIVO"))){
                            motivo = (String) mensagemDoServidor.getParam("MOTIVO");
                            alunoPresente.append(motivo);
                        }
                        System.out.println(alunoPresente);

                        /*limpar mensagem do servidor*/
                        conexaoDoCliente.setMensagemDoServidor(null);
                    }
                }
                default -> {
                }
            }
        }
    }
    public String selecionarSolicitacao(String tipoDeCliente){
        Scanner console = new Scanner(System.in);
        String solicitacao = null;
        StringBuilder selecao = new StringBuilder();

        /*exibir interface para escolher solicitação (loop)*/
        switch (tipoDeCliente){
            case "PROFESSOR" -> {
                while (Objects.isNull(solicitacao)) {
                    selecao.setLength(0);
                    selecao.append("\n----------Selecione uma Solicitação----------\n")
                            .append("\t1 - Iniciar Chamada")
                            .append("\n\t2 - Finalizar Chamada")
                            .append("\n\t3 - Sair")
                            .append("\n---------------------------------------------\n");
                    System.out.println(selecao);

                    try {
                        int opcao = console.nextInt();
                        switch (opcao) {
                            case 1 -> {
                                solicitacao = "INICIO_DE_CHAMADA";
                            }
                            case 2 -> {
                                solicitacao = "ENCERRAMENTO_DE_CHAMADA";
                            }
                            case 3 -> {
                                solicitacao = "SAIR";
                            }
                            default -> {
                                console.nextLine();
                                System.out.println("\nOpção Inválida, tente novamente.");
                            }
                        }
                    } catch (InputMismatchException e) {
                        console.nextLine();
                        System.out.println("\nInserção inválida, tente novamente.");
                    } catch (NoSuchElementException | IllegalStateException exception) {
                        try {
                            console.nextLine();
                        } catch (NoSuchElementException e) {

                        }
                        break;
                    }
                }
            }
            case "ALUNO" -> {
                while (Objects.isNull(solicitacao)) {
                    selecao.setLength(0);
                    selecao.append("\n----------Selecione uma Solicitação----------\n")
                            .append("\t1 - Registrar Presença")
                            .append("\n\t2 - Sair")
                            .append("\n---------------------------------------------\n");
                    System.out.println(selecao);
                    try {
                        int opcao = console.nextInt();
                        switch (opcao) {
                            case 1 -> {
                                solicitacao = "REGISTRO_DE_PRESENCA";
                            }
                            case 2 -> {
                                solicitacao = "SAIR";
                            }
                            default -> {
                                console.nextLine();
                                System.out.println("Opção Inválida.");
                            }
                        }
                    } catch (InputMismatchException e) {
                        console.nextLine();
                        System.out.println("\nInserção inválida, tente novamente.");
                    } catch (NoSuchElementException | IllegalStateException exception) {
                        try {
                            console.nextLine();
                        } catch (NoSuchElementException e) {

                        }
                        break;
                    }
                }
            }
        }

        /*retornar solicitção selecionada*/
        return solicitacao;
    }
    public Map<String, Object> exibirTurmas(String solicitacao){
        /*enviar solicitação de exibição de turmas*/
        Map<String, Object> adicionais = new HashMap<>();
        adicionais.put("ADICIONAL", solicitacao);
        conexaoDoCliente.enviarSolicitacao("EXIBIR_TURMAS", adicionais);

        /*aguardar resposta do servidor*/
        while(true){/*loop de espera / loop exibir turmas*/
            if (Objects.isNull(conexaoDoCliente.getMensagemDoServidor())){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else {
                /*interface para selecionar uma turma (limpar mensagem do após exibir cada mensagem)*/
                Mensagem mensagemDoServidor = conexaoDoCliente.getMensagemDoServidor();
                String clienteReferencia = (String) mensagemDoServidor.getParam("CLIENTE_REFERENCIA");
                if (Objects.equals(clienteReferencia, idCliente)){
                    /*exibir turmas*/
                    int quantidadeDeTurmasDisponiveis = (Integer) mensagemDoServidor.getParam("QUANTIDADE_DE_TURMAS_DISPONIVEIS");
                    ArrayList<Turma> arrayTurmasDisponiveis = (ArrayList<Turma>) mensagemDoServidor.getParam("TURMAS_DISPONIVEIS");

                    /*nao exbir mensagens de turma para alunos (ja será exibida outra mensagem em outro padrao)*/
                    boolean condicao1 = (Objects.equals(mensagemDoServidor.getParam("ADICIONAL"), "EXIBIR_TURMAS"));
                    boolean condicao2 = (Objects.equals(tipoDeCliente, "ALUNO"));
                    boolean condicoes = (condicao1 && condicao2);
                    if (!condicoes){
                        StringBuilder soutTurmasDisponiveis = new StringBuilder();
                        soutTurmasDisponiveis.append("\n----------Turmas Disponíveis----------\n")
                        .append("Quantidade de turmas disponíveis: " + quantidadeDeTurmasDisponiveis + "\n");

                        /*verifique se a quantidade é menor que zero*/
                        if (quantidadeDeTurmasDisponiveis <= 0){
                            /*menor igual, caso por algum motivo venha um numero menor qeu zero*/
                            if (tipoDeCliente.equals("PROFESSOR")){
                                soutTurmasDisponiveis.append("Só é possível encerrar chamadas de turmas que você abriu.\n");
                            }
                            soutTurmasDisponiveis.append("--------------------------------------\n");
                            System.out.println(soutTurmasDisponiveis);

                            /*sair do loop de espera*/
                            break;
                        }
                        else{
                            soutTurmasDisponiveis.append("Turmas disponíveis:\n")
                            .append("[\n"); /*abrir colchete*/
                            for (int index = 0; index<quantidadeDeTurmasDisponiveis; index++){
                                Turma turma = arrayTurmasDisponiveis.get(index);
                                soutTurmasDisponiveis.append("\tTurma ").append(turma.getNumero()).append(" -> ID: ").append(turma.getId());
                                /*ainda não é ultimo? ponha ; e um espaço. é o ultimo? então feche o colchete*/
                                soutTurmasDisponiveis.append(index+1 != quantidadeDeTurmasDisponiveis ? ",\n" : "\n]");
                            }
                            soutTurmasDisponiveis
                            .append("\n--------------------------------------\n\n")
                            .append("Insira o número da turma (ex.: Turma 1 -> 1), ou 0 para cancelar.\n");
                        }

                        System.out.println(soutTurmasDisponiveis);

                        /*limpar mensagem do servidor*/
                        conexaoDoCliente.setMensagemDoServidor(null);
                    }
                    else {
                        System.out.println("Insira o número da turma (ex.: Turma 1 -> 1), ou 0 para cancelar.\n");

                        /*limpar mensagem do servidor*/
                        conexaoDoCliente.setMensagemDoServidor(null);
                    }

                    /*esperar cliente selecionar turma (loop seleção de turma)*/
                    Scanner console = new Scanner(System.in);
                    while(adicionais.size() == 1){
                        /*1 porque ja tem a informação ADICIONAL inserida*/
                        try{
                            int index = console.nextInt();
                            if (conexaoDoCliente.getAtivo()){
                                if (index == 0){
                                    /*sair do loop de seleção de turma*/
                                    break;
                                }
                                else if (index < 0 || index > quantidadeDeTurmasDisponiveis) {
                                    System.out.println("\nTurma inválida. Tente novamente, ou insira 0 para cancelar.\n");
                                }
                                else {
                                    /*adicionar informacoes adicionais*/
                                    adicionais.put("ID_TURMA", arrayTurmasDisponiveis.get(index-1).getId());
                                    adicionais.put("NUMERO_TURMA", index);
                                    if(tipoDeCliente.equals("ALUNO")){
                                        adicionais.put("MATRICULA_DO_ALUNO", idAdicional);
                                    }
                                    if (solicitacao.equals("ENCERRAMENTO_DE_CHAMADA")){
                                        String dataHoraInicioChamada = arrayTurmasDisponiveis.get(index-1).getDataHoraInicioChamada();
                                        adicionais.put("DATAHORA_INICIO_CHAMADA", dataHoraInicioChamada);
                                    }
                                }
                            }
                            else{
                                /*sair do loop de seleção de turma*/
                                break;
                            }
                        } catch (InputMismatchException exception){
                            //exception.getStackTrace();
                            console.nextLine();
                            if(conexaoDoCliente.getAtivo()){
                                System.out.println("\nInserção inválida. Tente novamente, ou insira 0 para cancelar.\n");
                            }
                            else {
                                break;
                            }
                        }  catch (NoSuchElementException | IllegalStateException exception){
                            try{
                                console.nextLine();
                            } catch (NoSuchElementException e){}

                            /*sair do loop de seleção de turma*/
                            break;
                        }
                    }
                }

                /*sair do loop de espera*/
                break;
            }
        }

        /*retornar informações adicionais*/
        return adicionais;
    }
    public Mensagem gerarSolicitacao(String solicitacao, Map<String, Object> informacoesAdicionais) {
        /*Dispara o início da chamada informando a identificação
        numérica da turma e recebe do servidor uma confirmação
        contendo a data e hora do início da chamada.*/

        Mensagem mensagemDeRetorno = new Mensagem();

        /*selecionar turma e enviar dados*/
        if (Objects.isNull(informacoesAdicionais)){
            /*selecionar turma*/
            Map<String, Object> turmaSelecionada = exibirTurmas(solicitacao);
            if (turmaSelecionada.size() == 1){
                mensagemDeRetorno.setSolicitacao("CANCELAR_SOLICITACAO");
                return mensagemDeRetorno;
            } else {
                /*exibir turma*/
                String idTurma = (String) turmaSelecionada.get("ID_TURMA");
                int numeroTurma = (Integer) turmaSelecionada.get("NUMERO_TURMA");
                System.out.println("\nTurma Selecionada:\t" + "{ Turma " + numeroTurma + ",\tID: " + idTurma + " }");

                /*exibir mensagem de encerramento de chamada, caso seja necessário*/
                if (Objects.equals(solicitacao, "ENCERRAMENTO_DE_CHAMADA")){
                    System.out.println("\n\nA chamada para a turma " + numeroTurma + " será encerrada...");
                }

                /*enviar dados*/
                conexaoDoCliente.enviarSolicitacao(solicitacao, turmaSelecionada);
            }

        }
        else{
            /*enviar dados*/
            conexaoDoCliente.enviarSolicitacao(solicitacao, informacoesAdicionais);
        }

        /*receber mensagem do servidor*/
        while(true){
            if (Objects.isNull(conexaoDoCliente.getMensagemDoServidor())){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else {
                String retorno = (String) conexaoDoCliente.getMensagemDoServidor().getParam("ADICIONAL");
                if (retorno.equals(solicitacao)){
                    mensagemDeRetorno = conexaoDoCliente.getMensagemDoServidor();
                    break;
                }
            }
        }

        /*retornar mensagem do servidor*/
        return mensagemDeRetorno;
    }

    /*sobrecargas*/

    /*getters settters*/
    public String getIdCliente() {
        return idCliente;
    }
    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }
    public String getTipoDeCliente() {
        return tipoDeCliente;
    }
    public void setTipoDeCliente(String tipoDeCliente) {
        this.tipoDeCliente = tipoDeCliente;
    }
    public Object getIdAdicional() {
        return idAdicional;
    }
    public void setIdAdicional(Object idAdicional) {
        this.idAdicional = idAdicional;
    }
}
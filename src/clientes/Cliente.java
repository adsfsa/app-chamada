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
    private boolean ligado;
    private boolean consoleAberto = false;

    /*construtores*/
    public Cliente(String idCliente, String tipoDeCliente, Object idAdicional){
        /*iniciar id, tipo de cliente, e id adicional caso exista*/
        this.idCliente = idCliente;
        this.tipoDeCliente = tipoDeCliente;

        if (!Objects.isNull(idAdicional)){
            /*nulo caso seja um professor, matricula caso seja um aluno*/
            this.idAdicional = idAdicional;
        }

        /*exibir mensagem inicial*/
        ligado = true;
        System.out.println("\nSistema ligado.\n");

        /*começar conexão. o sistema deve continuar ligado mesmo se o servidor cair. se o servidor cair, apenas a conexão é encerrada*/
        while(ligado){
            /*exibir mensagem de tentativa, apenas uma vez*/
            boolean mostrarConexao = true;
            while(mostrarConexao){
                System.out.println("Tentando conexão...\n");
                mostrarConexao = false;
            }

            /*estabelecer conexão com servidor*/
            while(true){
                try {
                    if (conexaoDoCliente == null) {
                        Socket socket = new Socket("localhost", 5555);
                        conexaoDoCliente = new ConexaoDoCliente(socket, this);
                        conexaoDoCliente.start();
                    }

                    /*iniciar interface (loop)*/
                    executarInterface();

                    /*resetar loop de conexão*/
                    conexaoDoCliente = null;
                    break;
                } catch (IOException exception){
                    conexaoDoCliente = null;
                }
            }
        }

        /*exibir mensagem final*/
        System.out.println("Sistema desligado.");

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
                try {
                    conexaoDoCliente.enviarSolicitacao("ENCERRAR_CONEXAO");
                } catch (IOException exception) {
                    String textoErro = exception.getMessage();
                    String textoPadrao = "Você será desconectado, e tentaremos uma reconexão.";
                    AvisoDeErro erro = new AvisoDeErro(textoErro, textoPadrao);
                    System.out.println("\n" + erro.getAviso());
                    conexaoDoCliente.setAtivo(false);
                    ligado = false;
                    break;
                }
                while(true){
                    if (Objects.isNull(conexaoDoCliente.getMensagemDoServidor())){
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException exception) {
                            AvisoDeErro erro = new AvisoDeErro((exception.getMessage()), null);
                            System.out.println("\n" + erro.getAviso());
                        }
                    }
                    else {
                        Mensagem mensagemDoServidor = conexaoDoCliente.getMensagemDoServidor();
                        if(Objects.equals(mensagemDoServidor.getParam("ADICIONAL"), "ENCERRAR_CONEXAO")){
                            conexaoDoCliente.setAtivo(false);
                            ligado = false;
                            break;
                        }
                    }
                }
                executando = false;
                conexaoDoCliente.setMensagemDoServidor(null);
                continue;
            }
            else if(Objects.isNull(solicitacao)){
                executando = false;
                continue;
            }

            /*excolher turma e solicitar para o servidor*/
            Mensagem mensagemDoServidor = gerarSolicitacao(solicitacao, null);
            if (!conexaoDoCliente.getAtivo() || Objects.isNull(mensagemDoServidor)){
                /*sair do loop de interface se a conexão estivar desativada ou se a mensagem for nula*/
                break;
            }
            else if(Objects.equals(mensagemDoServidor.getSolicitacao(), "CANCELAR_SOLICITACAO")){
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
                            } catch (InterruptedException exception) {
                                AvisoDeErro erro = new AvisoDeErro((exception.getMessage()), null);
                                System.out.println("\n" + erro.getAviso());
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
                                    String alunoPresente = "----------Presença Registrada----------\n"
                                            + "Aluno (Matrícula): " + matriculaDoAluno
                                            + "\n---------------------------------------\n"
                                            + "\nAlunos Presentes: " + (quantidadeDeAlunosPresentes + 1) + "/" + tamanhoDaTurma + ".\n";
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
                        consoleAberto = true;
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
                                if (conexaoDoCliente.getAtivo()){
                                    System.out.println("\nOpção Inválida, tente novamente.");
                                }
                            }
                        }
                        if (!Objects.isNull(solicitacao)){
                            consoleAberto = false;
                        }
                    } catch (InputMismatchException e) {
                        console.nextLine();
                        if (conexaoDoCliente.getAtivo()){
                            System.out.println("\nInserção inválida, tente novamente.");
                        }
                    } catch (NoSuchElementException | IllegalStateException exception) {
                        try {
                            console.nextLine();
                        } catch (NoSuchElementException e) {}
                        consoleAberto = false;
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
                        consoleAberto = true;
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
                                if (conexaoDoCliente.getAtivo()){
                                    System.out.println("\nOpção Inválida.");
                                }
                            }
                        }
                        if (!Objects.isNull(solicitacao)){
                            consoleAberto = false;
                        }
                    } catch (InputMismatchException e) {
                        console.nextLine();
                        if (conexaoDoCliente.getAtivo()){
                            System.out.println("\nInserção inválida, tente novamente.");
                        }
                    } catch (NoSuchElementException | IllegalStateException exception) {
                        try {
                            console.nextLine();
                        } catch (NoSuchElementException e) {}
                        consoleAberto = false;
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
        try {
            conexaoDoCliente.enviarSolicitacao("EXIBIR_TURMAS", adicionais);
        } catch (IOException exception) {
            String textoErro = exception.getMessage();
            String textoPadrao = "Você será desconectado, e tentaremos uma reconexão.";
            AvisoDeErro erro = new AvisoDeErro(textoErro, textoPadrao);
            System.out.println("\n" + erro.getAviso());
            return adicionais;
        }

        /*aguardar resposta do servidor*/
        while(true){/*loop de espera / loop exibir turmas*/
            if (Objects.isNull(conexaoDoCliente.getMensagemDoServidor())){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException exception) {
                    AvisoDeErro erro = new AvisoDeErro((exception.getMessage()), null);
                    System.out.println("\n" + erro.getAviso());
                }
            }
            else {
                /*interface para selecionar uma turma (limpar mensagem do após exibir cada mensagem)*/
                Mensagem mensagemDoServidor = conexaoDoCliente.getMensagemDoServidor();
                String clienteReferencia = (String) mensagemDoServidor.getParam("CLIENTE_REFERENCIA");
                if (Objects.equals(clienteReferencia, idCliente)){
                    /*exibir turmas*/
                    int quantidadeDeTurmasDisponiveis = (Integer) mensagemDoServidor.getParam("QUANTIDADE_DE_TURMAS_DISPONIVEIS");

                    List<Turma> listTurmas = Arrays.asList((Turma[]) mensagemDoServidor.getParam("TURMAS_DISPONIVEIS"));
                    ArrayList<Turma> arrayTurmasDisponiveis = new ArrayList<>(listTurmas);

                    ArrayList<Integer> arrayNumerosDasTurmas = new ArrayList<>(Arrays.asList(new Integer[quantidadeDeTurmasDisponiveis]));

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
                                arrayNumerosDasTurmas.set(index, turma.getNumero());
                                soutTurmasDisponiveis.append("\tTurma ").append(turma.getNumero()).append(" -> ID: ").append(turma.getId());
                                /*ainda não é ultimo? ponha ; e um espaço. é o ultimo? então feche o colchete*/
                                soutTurmasDisponiveis.append(index+1 != quantidadeDeTurmasDisponiveis ? ",\n" : "\n]");
                            }
                            soutTurmasDisponiveis
                            .append("\n--------------------------------------\n\n")
                            .append("Insira o número da turma (ex.: Turma 1 -> 1), ou 0 para cancelar.\n");
                        }

                        System.out.println(soutTurmasDisponiveis);
                    }
                    else {
                        for (int index = 0; index<quantidadeDeTurmasDisponiveis; index++){
                            Turma turma = arrayTurmasDisponiveis.get(index);
                            arrayNumerosDasTurmas.set(index, turma.getNumero());
                        }
                        System.out.println("Insira o número da turma (ex.: Turma 1 -> 1), ou 0 para cancelar.\n");
                    }
                    /*limpar mensagem do servidor*/
                    conexaoDoCliente.setMensagemDoServidor(null);

                    /*esperar cliente selecionar turma (loop seleção de turma)*/
                    Scanner console = new Scanner(System.in);
                    while(adicionais.size() == 1){
                        /*1 porque ja tem a informação ADICIONAL inserida*/
                        try{
                            consoleAberto = true;
                            int numeroSelecionado = console.nextInt();
                            if (conexaoDoCliente.getAtivo()){
                                if (numeroSelecionado == 0){
                                    /*sair do loop de seleção de turma*/
                                    consoleAberto = false;
                                    break;
                                }
                                else if (numeroSelecionado < 0 || !arrayNumerosDasTurmas.contains(numeroSelecionado)) {
                                    if (conexaoDoCliente.getAtivo()){
                                        System.out.println("\nTurma inválida. Tente novamente, ou insira 0 para cancelar.\n");
                                    }
                                }
                                else {
                                    /*adicionar informacoes adicionais*/
                                    int indexOf = arrayNumerosDasTurmas.indexOf(numeroSelecionado);
                                    adicionais.put("ID_TURMA", arrayTurmasDisponiveis.get(indexOf).getId());
                                    adicionais.put("NUMERO_TURMA", numeroSelecionado);
                                    if(tipoDeCliente.equals("ALUNO")){
                                        adicionais.put("MATRICULA_DO_ALUNO", idAdicional);
                                    }
                                    if (solicitacao.equals("ENCERRAMENTO_DE_CHAMADA")){
                                        String dataHoraInicioChamada = arrayTurmasDisponiveis.get(indexOf).getDataHoraInicioChamada();
                                        adicionais.put("DATAHORA_INICIO_CHAMADA", dataHoraInicioChamada);
                                    }
                                }
                            }
                            else{
                                /*sair do loop de seleção de turma*/
                                break;
                            }
                            consoleAberto = false;
                        } catch (InputMismatchException exception){
                            console.nextLine();
                            if(conexaoDoCliente.getAtivo()){
                                System.out.println("\nInserção inválida. Tente novamente, ou insira 0 para cancelar.\n");
                            }
                            else {
                                consoleAberto = false;
                                break;
                            }
                        }  catch (NoSuchElementException | IllegalStateException exception){
                            try{
                                console.nextLine();
                            } catch (NoSuchElementException e){}

                            /*sair do loop de seleção de turma*/
                            consoleAberto = false;
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
                try {
                    conexaoDoCliente.enviarSolicitacao(solicitacao, turmaSelecionada);
                } catch (IOException exception) {
                    String textoErro = exception.getMessage();
                    String textoPadrao = "Você será desconectado, e tentaremos uma reconexão.";
                    AvisoDeErro erro = new AvisoDeErro(textoErro, textoPadrao);
                    System.out.println("\n" + erro.getAviso());
                    return null;
                }
            }

        }
        else{
            /*enviar dados*/
            try {
                conexaoDoCliente.enviarSolicitacao(solicitacao, informacoesAdicionais);
            } catch (IOException exception) {
                String textoErro = exception.getMessage();
                String textoPadrao = "Você será desconectado, e tentaremos uma reconexão.";
                AvisoDeErro erro = new AvisoDeErro(textoErro, textoPadrao);
                System.out.println("\n" + erro.getAviso());
                return null;
            }
        }

        /*receber mensagem do servidor*/
        while(true){
            if (Objects.isNull(conexaoDoCliente.getMensagemDoServidor())){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException exception) {
                    AvisoDeErro erro = new AvisoDeErro((exception.getMessage()), null);
                    System.out.println("\n" + erro.getAviso());
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
    public boolean isConsoleAberto() {
        return consoleAberto;
    }
    public void setConsoleAberto(boolean consoleAberto) {
        this.consoleAberto = consoleAberto;
    }
}

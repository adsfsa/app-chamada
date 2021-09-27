package servidor;

import classes.AvisoDeErro;
import classes.Mensagem;
import classes.Turma;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class ConexaoDoServidor extends Thread{
    /*variaveis globais*/
    private Socket socket;
    private Servidor servidor;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private boolean ativo = true;

    /*construtores*/
    public ConexaoDoServidor (Socket socket, Servidor servidor){
        super("ThreadCliente");
        this.socket = socket;
        this.servidor = servidor;
    }

    /*principais*/
    public void run(){
        /*iniciar thread*/
        try{
            System.out.println("\nUm cliente se conectou - (" + gerarDataHora() + ")\n");
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            /*loop de comunicação*/
            while(ativo){
                try {
                    /*receber mensagem*/
                    Mensagem mensagemDoCliente = (Mensagem) inputStream.readObject();

                    /*exibir mensagem*/
                    StringBuilder mensagemRecebida = new StringBuilder();
                    mensagemRecebida.append("\n----------Mensagem do Cliente----------\n")
                            .append(mensagemDoCliente)
                            .append("\n---------------------------------------\n");
                    System.out.println(mensagemRecebida);

                    /*processar mensagem*/
                    Mensagem mensagemDoServidor = processarMensagem(mensagemDoCliente);
                    if (Objects.equals(mensagemDoServidor.getParam("ADICIONAL"), "ENCERRAR_CONEXAO")){
                        ativo = false;
                    }

                    /*enviar resposta*/
                    enviarMensagemParaTodosCliente(mensagemDoServidor);
                } catch (IOException | ClassNotFoundException exception) {
                    String textoErro = exception.getMessage();
                    String textoPadrao = "Uma desconexão será registrada...";
                    AvisoDeErro erro = new AvisoDeErro(textoErro, textoPadrao);
                    System.out.println(erro.getAviso());
                    ativo = false;
                }
            }

            /*encerrar conexao*/
            fechar();
        } catch (IOException exception) {
            String textoErro = exception.getMessage();
            AvisoDeErro erro = new AvisoDeErro(textoErro, null);
            System.out.println(erro.getAviso() + "\n");
            fechar();
        }
    }
    public void fechar(){
        /*desconectar*/
        try{
            inputStream.close();
            outputStream.close();
            socket.close();
            System.out.println("Um cliente se desconectou - (" + gerarDataHora() + ")");
        } catch (IOException exception) {
            String textoErro = exception.getMessage();
            AvisoDeErro erro = new AvisoDeErro(textoErro, null);
            System.out.println(erro.getAviso() + "\n");
        }
    }

    /*métodos*/
    private String gerarDataHora(){
        String pattern = "EEEE, dd 'de' MMMM 'de' yyyy, 'às' HH:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, new Locale("pt", "BR"));
        return simpleDateFormat.format(new Date());
    }
    private Mensagem processarMensagem(Mensagem mensagemDoCliente){
        /*indentificar informações*/
        String solicitacao = mensagemDoCliente.getSolicitacao();
        String clienteReferencia = (String) mensagemDoCliente.getParam("ID_CLIENTE");
        String cliente = (String) mensagemDoCliente.getParam("TIPO_DE_CLIENTE");

        /*gerar mensagem de retorno*/
        Mensagem mensagemDoServidor = new Mensagem("MENSAGEM_DO_SERVIDOR");
        mensagemDoServidor.setParam("CLIENTE_REFERENCIA", clienteReferencia);
        mensagemDoServidor.setParam("ADICIONAL", solicitacao);

        /*identificar solicitacao*/
        if (solicitacao.equals("ENCERRAR_CONEXAO")){
            /*desativar turma (apenas professores)*/
            if (cliente.equals("PROFESSOR")){
                /*deative a turma*/
                for(Turma turma : servidor.getListaDeTurmas()){
                    if(turma.getStatus().equals("ativa") && turma.getIdProfessor().equals(clienteReferencia)){
                        turma.desativar();
                    }
                }
            }

            /*retornar mensagem de retorno nula ou vazia*/
            return mensagemDoServidor;
        }

        /*processar solicitação*/
        if(solicitacao.equals("EXIBIR_TURMAS")){
            ArrayList<Turma> turmasDisponiveis = new ArrayList<>();
            String solicitacaoAdicional = (String) mensagemDoCliente.getParam("ADICIONAL");
            switch (cliente){
                case "PROFESSOR":
                    switch (solicitacaoAdicional){
                        case "INICIO_DE_CHAMADA":
                            /*carregar turmas inativas*/
                            for (int index = 0; index < servidor.getListaDeTurmas().size(); index++){
                                Turma turmaAtual = servidor.getListaDeTurmas().get(index);
                                if (turmaAtual.getStatus().equals("inativa")){
                                    turmasDisponiveis.add(turmaAtual);
                                }
                            }
                            break;
                        case "ENCERRAMENTO_DE_CHAMADA":
                            /*carregar turmas ativas relacionadas ao professor*/
                            String idProfessor = (String) mensagemDoCliente.getParam("ID_CLIENTE");
                            for (int index = 0; index<servidor.getListaDeTurmas().size(); index++){
                                Turma turmaAtual = servidor.getListaDeTurmas().get(index);
                                if (turmaAtual.getStatus().equals("ativa") && turmaAtual.getIdProfessor().equals(idProfessor)){
                                    turmasDisponiveis.add(turmaAtual);
                                }
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case "ALUNO":
                    if (solicitacaoAdicional.equals("REGISTRO_DE_PRESENCA")) {
                        /*carregar todas as turmas*/
                        turmasDisponiveis = servidor.getListaDeTurmas();
                    }
                    break;
                default:
                    break;
            }
            mensagemDoServidor.setParam("QUANTIDADE_DE_TURMAS_DISPONIVEIS", turmasDisponiveis.size());
            mensagemDoServidor.setParam("TURMAS_DISPONIVEIS", turmasDisponiveis);
        }
        else {
            /*setar parametros da mensagem de retorno*/
            String idTurma = (String) mensagemDoCliente.getParam("ID_TURMA");
            int numeroTurma = 0;
            if (!Objects.isNull(mensagemDoCliente.getParam("NUMERO_TURMA"))){
                numeroTurma = (Integer) mensagemDoCliente.getParam("NUMERO_TURMA");
                mensagemDoServidor.setParam("NUMERO_TURMA", numeroTurma);
            }
            mensagemDoServidor.setParam("TURMA_REFERENCIA", idTurma);

            /*outras informações importantes*/
            Turma turmaSelecionada = null;
            for(Turma turma: servidor.getListaDeTurmas()){
                if (Objects.equals(turma.getId(), idTurma)){
                    turmaSelecionada = turma;
                    break;
                }
            }
            /*executar solicitações*/
            switch (cliente) {
                case "PROFESSOR" -> {
                    String idProfessor = clienteReferencia;

                    /*pegar a lista dos alunos presentes na turma selecionada*/
                    ArrayList<Integer> matriculasAlunosPresentes = new ArrayList<>();
                    if (!servidor.getListaDeTurmas().isEmpty()) {
                        /*se a lista de turmas não estiver vazia: verifique cada turma dessa lista*/
                        for (Turma turma : servidor.getListaDeTurmas()) {
                            if (Objects.equals(turma.getId(), idTurma)) {
                                /*se a turma atual tiver o mesmo id da turma selecionada: verifique a lista de matriculas dessa turma*/
                                for(Integer matricula : turma.getListaDeMatricula()){
                                    Integer anterior = null;
                                    if (!Objects.isNull(matricula)){
                                        /*se a matricula não for nula: adicione na lista final de alunos presentes*/
                                        matriculasAlunosPresentes.add(matricula);
                                    }
                                }
                                break;
                            }
                        }
                    }

                    /*verificar solicitação do professor (iniciar ou encerrar chamada)*/
                    switch (solicitacao) {
                        case "INICIO_DE_CHAMADA" -> {
                            /*Se for um início de chamada se prepara para armazenar as
                            matrículas de alunos que responderão a chamada da turma
                            informada e retorna confirmação adequada (data e hora) ao
                            professor. Lembre-se: pode haver mais de um professor
                            fazendo chamada ao mesmo tempo para turmas diferentes. Cabe
                            à equipe implementar uma solução para isso.*/

                            /*setar parâmetros da mensagem*/
                            String dataHoraInicioChamada = gerarDataHora();
                            int tamanhoDaTurma = servidor.getListaDeTurmas().get(0).getListaDeMatricula().size(); /*todas as turmas tem capacidade maxima fixa, por isso o 0 direto*/
                            mensagemDoServidor.setParam("STATUS_DA_CHAMADA", "ativa");
                            mensagemDoServidor.setParam("TAMANHO_DA_TURMA", tamanhoDaTurma);
                            mensagemDoServidor.setParam("QUANTIDADE_DE_ALUNOS_PRESENTES", 0);
                            mensagemDoServidor.setParam("DATAHORA_INICIO_CHAMADA", dataHoraInicioChamada);

                            /*ativar turma selecionada*/
                            turmaSelecionada.ativar("ativa", idProfessor, dataHoraInicioChamada);

                            /*exibir mensagem*/
                            StringBuilder inicioChamada = new StringBuilder();
                            inicioChamada
                                    .append("\n----------Chamada Iniciada----------\n")
                                    .append(dataHoraInicioChamada + "\n")
                                    .append("ID do professor: " + idProfessor + "\n")
                                    .append("ID da Turma: " + idTurma)
                                    .append("\n-------------------------------------\n");
                            System.out.println(inicioChamada);
                        }
                        case "ENCERRAMENTO_DE_CHAMADA" -> {
                            /*Se for um encerramento de chamada retorna ao professor a
                            confirmação adequada (data, hora e vetor de matrículas) ao
                            professor e apaga as informações referentes a chamada da
                            turma que foi encerrada. Lembre-se que podem haver outras
                            turmas ainda fazendo chamada e isso não pode sofrer
                            interferência. Cabe à equipe implementar uma solução para
                            isso.*/

                            /*setar parâmetros da mensagem*/
                            String dataHoraFimChamada = gerarDataHora();
                            mensagemDoServidor.setParam("STATUS_DA_CHAMADA", "inativa");
                            mensagemDoServidor.setParam("QUANTIDADE_DE_ALUNOS_PRESENTES", matriculasAlunosPresentes.size());
                            mensagemDoServidor.setParam("ALUNOS_PRESENTES", matriculasAlunosPresentes);
                            mensagemDoServidor.setParam("DATAHORA_FIM_CHAMADA", dataHoraFimChamada);
                            int indexOf = servidor.getListaDeTurmas().indexOf(turmaSelecionada);
                            mensagemDoServidor.setParam("DATAHORA_INICIO_CHAMADA", servidor.getListaDeTurmas().get(indexOf).getDataHoraInicioChamada());

                            /*deasativar turma selecionada*/
                            turmaSelecionada.desativar();

                            /*exibir mensagem*/
                            StringBuilder fimDeChamada = new StringBuilder();
                            fimDeChamada
                                    .append("\n----------Chamada Finalizada----------\n")
                                    .append(dataHoraFimChamada + "\n")
                                    .append("ID do professor: " + idProfessor + "\n")
                                    .append("ID da Turma: " + idTurma)
                                    .append("\n-------------------------------------\n");
                            System.out.println(fimDeChamada);
                        }
                        default -> {
                        }
                    }
                }
                case "ALUNO" -> {
                    String parametroDateTimePresenca = ""; /*presença registrada ou negada*/

                    /*verificar solicitação do aluno*/
                    if ("REGISTRO_DE_PRESENCA".equals(solicitacao)) {
                        /*Se for um registro de presença, verifica se existe uma
                        chamada ativa para a turma informada pelo aluno.*/

                        if (turmaSelecionada.getStatus().equals("ativa") && !turmaSelecionada.estaCheia()) {
                            /*Caso a chamada esteja ativa (professor já iniciou, mas
                            não encerrou), insere a matrícula do aluno no
                            armazenamento de alunos presentes e devolve a
                            confirmação adequada ao aluno (identificação da turma, a
                            data e a hora em que a presença foi registrada).*/

                            /*aluno não pode se registrar duas vezes*/
                            int matriculaDoAluno = (Integer) mensagemDoCliente.getParam("MATRICULA_DO_ALUNO");
                            int indexOfMatricula = turmaSelecionada.getListaDeMatricula().indexOf(matriculaDoAluno);
                            if (indexOfMatricula == -1) {
                                /*se aluno não esta na lista, adicione e confirme presença*/
                                int indexOfTurma = servidor.getListaDeTurmas().indexOf(turmaSelecionada);
                                servidor.getListaDeTurmas().get(indexOfTurma).addMatricula(matriculaDoAluno);

                                mensagemDoServidor.setParam("ID_PROFESSOR", turmaSelecionada.getIdProfessor());
                                mensagemDoServidor.setParam("MATRICULA_DO_ALUNO", matriculaDoAluno);
                                mensagemDoServidor.setParam("PRESENCA_REGISTRADA", true);
                                parametroDateTimePresenca = "DATAHORA_PRESENCA_REGISTRADA";
                            } else {
                                /*se aluno está na lista, negue a presença*/
                                mensagemDoServidor.setParam("PRESENCA_REGISTRADA", false);
                                parametroDateTimePresenca = "DATAHORA_PRESENCA_NEGADA";
                                mensagemDoServidor.setParam("MOTIVO", "Você já registrou presença para a turma selecionada.");
                            }
                        } else {
                            /*Caso a chamada não exista (professor não iniciou ou já
                            encerrou a chamada da turma) apenas devolve a resposta
                            adequada ao aluno (zero em lugar da identificação da
                            turma, a data e a hora em que a presença foi negada).*/

                            /*setar parâmetros da mensagem*/
                            mensagemDoServidor.setParam("PRESENCA_REGISTRADA", false);
                            parametroDateTimePresenca = "DATAHORA_PRESENCA_NEGADA";
                            String motivo = null;
                            if (turmaSelecionada.getStatus().equals("inativa")){
                                motivo = "A turma selecionada está inativa.";
                            } else if(turmaSelecionada.estaCheia()){
                                motivo = "A turma selecionada está cheia.";
                            }
                            mensagemDoServidor.setParam("MOTIVO", motivo);
                        }
                    }
                    if (!parametroDateTimePresenca.equals("")) {
                        String dataHora = gerarDataHora();
                        mensagemDoServidor.setParam(parametroDateTimePresenca, dataHora);
                    }
                }
                default -> {
                }
            }
        }

        /*finalizar mensagem de retorno*/
        return mensagemDoServidor;
    }
    public void enviarMensagemParaCliente(Mensagem mensagem){
        try {
            outputStream.reset();
            outputStream.writeObject(mensagem);
            outputStream.flush();
        } catch (IOException exception) {
            String textoErro = exception.getMessage();
            AvisoDeErro erro = new AvisoDeErro(textoErro, null);
            System.out.println(erro.getAviso() + "\n");
            fechar();
        }
    }
    public void enviarMensagemParaTodosCliente(Mensagem mensagem){
        for (int index = 0; index < servidor.getClientesConectados().size(); index++){
            ConexaoDoServidor threadCliente = servidor.getClientesConectados().get(index);
            threadCliente.enviarMensagemParaCliente(mensagem);
        }
    }

    /*sobrecargas*/

    /*getters settters*/
}

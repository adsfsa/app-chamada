package clientes;

import classes.AvisoDeErro;
import classes.Mensagem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;

public class ConexaoDoCliente extends Thread{
    /*variaveis globais*/
    public Socket socket;
    public Cliente cliente;
    public ObjectOutputStream outputStream;
    public ObjectInputStream inputStream;
    private boolean ativo = false;
    private Mensagem mensagemDoServidor;

    /*construtores*/
    public ConexaoDoCliente(Socket socket, Cliente cliente){
        this.socket = socket;
        this.cliente = cliente;
    }

    /*principais*/
    public void run(){
        try{
            System.out.println("Conectado!");
            ativo = true;
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            boolean mensagemFinalexibida = false;

            /*iniciar diálogo (loop)*/
            while(ativo){
                try {
                    Mensagem mensagemDoServidor = (Mensagem) inputStream.readObject();
                    String clienteReferencia = (String) mensagemDoServidor.getParam("CLIENTE_REFERENCIA");
                    if (Objects.equals(clienteReferencia, cliente.getIdCliente())){
                        /*exibir mensagem*/
                        boolean condicao1 = cliente.getTipoDeCliente().equals("PROFESSOR");
                        boolean condicao2 = Objects.equals(mensagemDoServidor.getParam("ADICIONAL"), "EXIBIR_TURMAS");
                        boolean condicao = (condicao1 && condicao2);
                        /*professores tem outro padrão de exibição de turmas (eles so precisam saber o id e o numero da turma)*/
                        if(!condicao){
                            /*se não for uma exibição de turmas para um professor, exiba*/
                            StringBuilder mensagemRecebida = new StringBuilder();
                            mensagemRecebida.append("\n----------Mensagem do Servidor----------\n")
                                    .append(mensagemDoServidor.toString(cliente.getIdCliente()))
                                    .append("\n----------------------------------------\n");
                            System.out.println(mensagemRecebida);
                        }

                        this.mensagemDoServidor = mensagemDoServidor;
                        if(Objects.equals(mensagemDoServidor.getParam("ADICIONAL"), "ENCERRAR_CONEXAO")){
                            ativo = false;
                        }
                    }
                    else{
                        /*professor captura mensagens para alunos que se registram em sua turma ativa*/
                        String adicional = (String) mensagemDoServidor.getParam("ADICIONAL");
                        if (Objects.equals(adicional, "REGISTRO_DE_PRESENCA")) {
                            String idProfessor = (String) mensagemDoServidor.getParam("ID_PROFESSOR");
                            if (Objects.equals(idProfessor, cliente.getIdCliente())) {
                                this.mensagemDoServidor = mensagemDoServidor;
                            }
                        }
                    }
                } catch (IOException | ClassNotFoundException exception) {
                    /*encerrar conexao*/
                    fechar();
                    String textoPadrao = "";
                    if (cliente.isConsoleAberto()){
                        System.out.println("Desconectado!\n");
                        textoPadrao = "Digite qualquer coisa para se reconectar...";
                        mensagemFinalexibida = true;
                    } else {
                        textoPadrao = "Você será desconectado, e tentaremos uma reconexão.";
                    }
                    AvisoDeErro erro = new AvisoDeErro(exception.getMessage(), textoPadrao);
                    System.out.println(erro.getAviso());
                }
            }

            if (!mensagemFinalexibida){
                System.out.println("Desconectado!\n");
            }

        } catch (IOException exception) {
            String textoErro = exception.getMessage();
            AvisoDeErro erro = new AvisoDeErro(textoErro, null);
            System.out.println(erro.getAviso() + "\n");
            fechar();
        }
    }
    public void fechar(){
        try{
            inputStream.close();
            outputStream.close();
            socket.close();
            mensagemDoServidor = null;
            ativo = false;
        } catch (IOException exception) {
            String textoErro = exception.getMessage();
            AvisoDeErro erro = new AvisoDeErro(textoErro, null);
            System.out.println(erro.getAviso() + "\n");
        }
    }

    /*métodos*/
    public void enviarSolicitacao(String solicitacao) throws IOException {
        /*limpar mensagem anterior*/
        mensagemDoServidor = null;

        /*criar nova mensagem*/
        Mensagem mensagemDoCliente = new Mensagem(solicitacao);
        mensagemDoCliente.setParam("TIPO_DE_CLIENTE", cliente.getTipoDeCliente());
        mensagemDoCliente.setParam("ID_CLIENTE", cliente.getIdCliente());

        /*enviar mensagem*/
        outputStream.reset();
        outputStream.writeObject(mensagemDoCliente);
        outputStream.flush();
    }
    public void enviarSolicitacao(String solicitacao, Map<String, Object> informacoesAdicionais) throws IOException {
        /*limpar mensagem anterior*/
        mensagemDoServidor = null;

        /*criar nova mensagem*/
        Mensagem mensagemDoCliente = new Mensagem(solicitacao);
        mensagemDoCliente.setParam("TIPO_DE_CLIENTE", cliente.getTipoDeCliente());
        mensagemDoCliente.setParam("ID_CLIENTE", cliente.getIdCliente());

        /*informacoes adicionais*/
        for (Map.Entry<String, Object> entry : informacoesAdicionais.entrySet()) {
            String chave = entry.getKey();
            Object valor = entry.getValue();
            setarParametros(mensagemDoCliente, chave, valor);
        }

        /*enviar mensagem*/
        outputStream.reset();
        outputStream.writeObject(mensagemDoCliente);
        outputStream.flush();
    }
    public void setarParametros(Mensagem mensagem , String chave, Object valor){
        mensagem.setParam(chave, valor);
    }

    /*sobrecargas*/

    /*getters settters*/

    public Socket getSocket() {
        return socket;
    }
    public void setSocket(Socket socket) {
        this.socket = socket;
    }
    public Mensagem getMensagemDoServidor(){
        return this.mensagemDoServidor;
    }
    public void setMensagemDoServidor(Mensagem mensagem){
        this.mensagemDoServidor = mensagem;
    }
    public boolean getAtivo() {
        return ativo;
    }
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}

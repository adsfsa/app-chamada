package servidor;

import classes.AvisoDeErro;
import classes.ListaDeTurmas;
import classes.Turma;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class Servidor {
    /*variaveis globais*/
    private ServerSocket serverSocket;
    private ArrayList<ConexaoDoServidor> clientesConectados = new ArrayList<>();
    private boolean ativo = true;
    private ListaDeTurmas listaDeTurmas = new ListaDeTurmas(10);

    /*construtores*/
    public Servidor(){
        try{
            /*criar servidor*/
            this.serverSocket = new ServerSocket(5555);
            System.out.println("\nServidor ativo.");

            /*rodar servidor*/
            while(ativo){
                /*esperar conexões*/
                Socket socket = serverSocket.accept();

                /*conectar*/
                ConexaoDoServidor conexaoDoServidor = new ConexaoDoServidor(socket, this);
                conexaoDoServidor.start();
                clientesConectados.add(conexaoDoServidor);
            }
        } catch (IOException exception) {
            AvisoDeErro erro = new AvisoDeErro((exception.getMessage()), null);
            System.out.println("\n" + erro.getAviso());
        }
    }

    /*principais*/
    public static void main(String[] args) {
        new Servidor();
    }

    /*métodos*/

    /*sobrecargas*/

    /*getters settters*/

    public boolean getAtivo() {
        return ativo;
    }
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
    public ArrayList<ConexaoDoServidor> getClientesConectados() {
        return clientesConectados;
    }
    public ListaDeTurmas getListaDeTurmas() {
        return listaDeTurmas;
    }
}
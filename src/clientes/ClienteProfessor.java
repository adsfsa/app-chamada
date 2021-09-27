package clientes;

import java.util.UUID;

public class ClienteProfessor extends Cliente{
    /*variaveis globais*/
    private String idProfessor;

    /*construtores*/
    public ClienteProfessor(String idProfessor){
        super(idProfessor,"PROFESSOR", null);
        this.idProfessor = idProfessor;
    }

    /*principais*/
    public static void main(String[] args) {
        String idProfessor = UUID.randomUUID().toString();
        new ClienteProfessor(idProfessor);
    }

    /*métodos*/

    /*sobrecargas*/

    /*getters settters*/
    public String getIdProfessor() {
        return idProfessor;
    }
    public void setIdProfessor(String idProfessor) {
        this.idProfessor = idProfessor;
    }
}

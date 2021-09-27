package clientes;

import java.util.Random;
import java.util.UUID;

public class ClienteAluno extends Cliente {
    /*variaveis globais*/
    private String idAluno;
    private int matriculaAluno;

    /*construtores*/
    public ClienteAluno(String idAluno, int matriculaAluno){
        super(idAluno, "ALUNO", matriculaAluno);
        this.idAluno = idAluno;
        this.matriculaAluno = matriculaAluno;
    }

    /*principais*/
    public static void main(String[] args) {
        String idAluno = UUID.randomUUID().toString();
        int matriculaAluno = new Random().nextInt(900000000) + 100000000;
        new ClienteAluno(idAluno, matriculaAluno);
    }

    /*métodos*/

    /*sobrecargas*/

    /*getters settters*/
    public String getIdAluno() {
        return idAluno;
    }
    public void setIdAluno(String idAluno) {
        this.idAluno = idAluno;
    }
    public int getMatriculaAluno() {
        return matriculaAluno;
    }
    public void setMatriculaAluno(int matriculaAluno) {
        this.matriculaAluno = matriculaAluno;
    }
}

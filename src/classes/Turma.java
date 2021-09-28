package classes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class Turma implements Serializable {
    /*variaveis globais*/
    private String id;
    private int numero;
    private String idProfessor;
    private String status;
    private ArrayList<Integer> listaDeMatricula = new ArrayList<>(Arrays.asList(new Integer[2])); /* max 40 alunos*/
    private String dataHoraInicioChamada;
    private String dataHoraFimChamada;

    /*construtores*/
    public Turma(int numero) {
        this.id = UUID.randomUUID().toString();
        this.numero = numero;
        this.idProfessor = null;
        this.status = "inativa";
        this.dataHoraInicioChamada = null;
        this.dataHoraFimChamada = null;
    }
    public Turma(String id) {
        this.id = id;
    }
    public Turma(String id, String status, String idProfessor) {
        this.id = id;
        this.status = status;
        this.idProfessor = idProfessor;
    }

    /*principais*/

    /*métodos*/
    public synchronized void addMatricula(int matricula){
        for(Integer m: listaDeMatricula){
            Integer verificador = null;
            if(Objects.isNull(m)){
                int indexOf = listaDeMatricula.indexOf(m);
                listaDeMatricula.set(indexOf, matricula);
                break;
            }
        }
    }
    public void ativar(String status, String idProfessor, String dataHoraInicioChamada){
        this.status = status;
        this.idProfessor = idProfessor;
        this.dataHoraInicioChamada = dataHoraInicioChamada;
    }
    public void desativar(){
        this.status = "inativa";
        this.idProfessor = null;
        this.listaDeMatricula.clear();
        this.dataHoraInicioChamada = null;
        this.dataHoraFimChamada = null;
    }
    public boolean estaCheia(){
        int total = 0;
        for (Integer matricula : listaDeMatricula){
            if (!Objects.isNull(matricula)){
                total++;
            }
        }
        return Objects.equals(total, listaDeMatricula.size());
    }

    /*sobrecargas*/
    @Override public String toString(){
        StringBuilder turma = new StringBuilder();
        turma.append("\tTurma " + numero+ ":");/*configurar o \n inicial no metodo que irá chamar o turma toString*/
        turma.append("\n\t{");
        turma.append(" ID: "+id);
        turma.append("    Professor Responsável (ID): "+ (!Objects.isNull(idProfessor) ? idProfessor : "nennhum"));
        turma.append("    Status: "+ status + " ");
        turma.append("}");/*configurar \n final no metodo que irá chamar o turma toString*/
        return turma.toString();
    }

    /*getters settters*/
    public ArrayList<Integer> getListaDeMatricula(){
        return this.listaDeMatricula;
    }
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public int getNumero() {
        return numero;
    }
    public void setNumero(int numero) {
        this.numero = numero;
    }
    public String getStatus(){
        return this.status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getIdProfessor(){
        return this.idProfessor;
    }
    public void setIdProfessor(String idProfessor){
        this.idProfessor = idProfessor;
    }
    public String getDataHoraInicioChamada() {
        return dataHoraInicioChamada;
    }
    public void setDataHoraInicioChamada(String dataHoraInicioChamada) {
        this.dataHoraInicioChamada = dataHoraInicioChamada;
    }
    public String getDataHoraFimChamada() {
        return dataHoraFimChamada;
    }
    public void setDataHoraFimChamada(String dataHoraFimChamada) {
        this.dataHoraFimChamada = dataHoraFimChamada;
    }
}

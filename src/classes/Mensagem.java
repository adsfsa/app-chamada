package classes;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Mensagem implements Serializable {
    /*variaveis globais*/
    private String solicitacao;
        /*Solicitações:
            * CRIAR_MENSAGEM -> (default) (exclusiva) criar nova mensagem;
            * CANCELAR_SOLICITACAO -> (exclusiva) cancelar solicitção;
            * MENSAGEM_DO_SERVIDOR -> (exclusiva) resposta do servidor;
            * EXIBIR_TURMAS -> solicitação para exibir turmas;
            * INICIO_DE_CHAMADA -> solicitação para iniciar chamada;
            * ENCERRAMENTO_DE_CHAMADA -> solicitação para encerrar chamada;
            * REGISTRO_DE_PRESENCA -> solicitação para registrar presença;
        */
    private Map<String, Object> params;
        /* "chave" : Object valor
            Parâmetros:
             * ID_TURMA -> (nível 1) (String) (cliente/servidor) id da turma;
             * ID_CLIENTE -> (nível 1) (String) (cliente) id do cliente;
             * TIPO_DE_CLIENTE -> (nível 1) (String) (cliente) tipo de cliente;
             * CLIENTE_REFERENCIA -> (nível 1) (String) (servidor) backup para o id do cliente;
             * ADICIONAL -> (nível 1) (String) (servidor) informação adicional (geralmente usada como backup da solicitação; solicitação em resposta à adicional);
             * MATRICULA_DO_ALUNO -> (nível 2) (Integer) (cliente) matrícula do aluno;
             * STATUS_DA_CHAMADA -> (nível 2) (String) (servidor) chamada ativa ou inativa;
             * TURMAS_DISPONIVEIS -> (nível 2) (ListaDeTurmas extends ArrayList<Turma>) (servidor) array de turmas ativas;
             * QUANTIDADE_DE_TURMAS_DISPONIVEIS -> (nível 2) (Integer) (servidor) quantidade de turmas ativas;
             * ALUNOS_PRESENTES -> (nível 2) (ArrayList<Integer>) (servidor) array com matriculas dos alunos que registraram presença;
             * QUANTIDADE_DE_ALUNOS_PRESENTES -> (nível 2) (Integer) (servidor) quantidade de alunos que registraram presença;
             * TURMA_REFERENCIA -> (nível 3) (String) (servidor) backup para o id da turma;
             * NUMERO_TURMA -> (nível 3) (Integer) (cliente/servidor) numero da turma selecionada (é mais importante para o cliente, para ele identificar a turma selecionada mais rapido);
             * ID_PROFESSOR -> (nível 3) (String) (cliente) backup para o id do professor (geralmente usado por alunos);
             * DATAHORA_INICIO_CHAMADA -> (nível 3) (String) (servidor) data e hora em que a chamada foi iniciada (formato: "EEEE, dd 'de' MMMM 'de' yyyy, 'às' HH:mm:ss" -> nome do dia, dia de mês de ano, às horas:minutos:segundos);
             * DATAHORA_FIM_CHAMADA -> (nível 3) (String) (servidor) data e hora em que a chamada foi encerrada;
             * PRESENCA_REGISTRADA -> (nível 3) (Boolean) (servidor) a chamada foi ou não foi registrada;
             * DATAHORA_PRESENCA_REGISTRADA -> (nível 3) (String) (servidor) data e hora em que a presença foi registrada;
             * DATAHORA_PRESENCA_NEGADA -> (nível 3) (String) (servidor) data e hora em que a presença foi registrada;
             * MOTIVO -> (nível 3) (String) (servidor) explicação para negações de chamada (geralemnte utilizada para explicar negações de presença registrada);
         */

    /*construtores*/
    public Mensagem() {
        this.solicitacao = "CRIAR_MENSAGEM";
        params = new HashMap<>();
    }
    public Mensagem(String solicitacao) {
        this.solicitacao = solicitacao;
        params = new HashMap<>();
    }

    /*principais*/

    /*métodos*/
    public String toString(String idCliente){
        StringBuilder mensagem = new StringBuilder();
        if (!Objects.equals(this.solicitacao, "MENSAGEM_DE_RESPOSTA")){
            mensagem = new StringBuilder("Solicitação: " + this.solicitacao + "\n");
        }
        mensagem.append("Parâmetros:");
        for(Object parametro : params.keySet()){
            if (parametro.equals("ID_CLIENTE")){
                mensagem.append("\n\t").append(parametro).append(" -> ").append(params.get(parametro) + " (Você)");
            }
            else{
                mensagem.append("\n\t").append(parametro).append(" -> ").append(params.get(parametro));
            }
        }
        return mensagem.toString();
    }

    /*sobrecargas*/
    @Override public String toString(){
        StringBuilder mensagem = new StringBuilder();
        if (!Objects.equals(this.solicitacao, "MENSAGEM_DE_RESPOSTA")){
            mensagem.append("Solicitação: ").append(this.solicitacao).append("\n");
        }
        mensagem.append("Parâmetros:");
        for(Object parametro : params.keySet()){
            mensagem.append("\n\t").append(parametro).append(" -> ").append(params.get(parametro));
        }
        return mensagem.toString();
    }

    /*getters settters*/
    public String getSolicitacao(){
        return this.solicitacao;
    }
    public void setSolicitacao(String solicitacao){
        this.solicitacao = solicitacao;
    }
    public Object getParam(String chave){
        return this.params.get(chave);
    }
    public void setParam(String chave, Object valor){
        this.params.put(chave, valor);
    }
}
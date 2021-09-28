package classes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class ListaDeTurmas extends ArrayList<Turma> implements Serializable {
    /*variaveis globais*/

    /*construtores*/
    public ListaDeTurmas(int capacidade) {
        super(new ArrayList<>(Arrays.asList(new Turma[capacidade])));
        for (int index = 0; index < super.size(); index++) {
            Turma turma = new Turma((index+1));
            super.set(index, turma);
        }
    }

    /*principais*/

    /*métodos*/

    /*sobrecargas*/
    @Override public String toString(){
        StringBuilder lista = new StringBuilder();

        lista.append("\n[\n");
        for(int index = 0; index < super.size(); index++) {
            lista.append("\tTurma " + (index+1) + ": ");
            lista.append(super.get(index));
            lista.append(index+1 == super.size() ? "\n]" : ",\n\n");
        }
        return lista.toString();
    }

    /*getters settters*/
}

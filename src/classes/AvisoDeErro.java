package classes;

import java.util.Objects;

public class AvisoDeErro {
    /*variaveis globais*/
    private String TEXTO_VERMELHO = "\u001b[31m"; /*sempre que mudar a cor, resetar para o padrão depois*/
    private String TEXT_PADRAO = " \u001b[0m ";
    private String aviso;
    /*construtores*/
    public AvisoDeErro(String textoErro, String textoPadrao) {
        StringBuilder mensagemDeErro = new StringBuilder();
        mensagemDeErro
                /*erro em vermelho; resetar para cor original*/
                /*.append(TEXTO_VERMELHO)*/.append("[ERRO] ").append(textoErro);
                /*.append(TEXT_PADRAO)*/
        if (!Objects.isNull(textoPadrao)){
            mensagemDeErro.append(" - ").append(textoPadrao).append("\n");
        }
        aviso = mensagemDeErro.toString();
    }

    /*métodos*/
    /*sobrecargas*/

    /*getters settters*/
    public String getAviso(){
        return this.aviso;
    }
}

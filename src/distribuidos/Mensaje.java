/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package distribuidos;

import java.io.Serializable;

/**
 *
 * @author david
 */
public class Mensaje implements Serializable{
    
    private int emisor;
    private int receptor;
    private String cabecera;
    private String contenido;
    private int aux1;
    private int aux2;
    private int aux3;
    private int aux4;
    private String str1;
    private String str2;
    
    public Mensaje(int emisor) {
        this.emisor = emisor;
        this.receptor = -1;
        this.cabecera = "";
        this.contenido = "";
    }

    public String getStr1() {
        return str1;
    }

    public void setStr1(String str1) {
        this.str1 = str1;
    }

    public String getStr2() {
        return str2;
    }

    public void setStr2(String str2) {
        this.str2 = str2;
    }

    public int getAux3() {
        return aux3;
    }

    public void setAux3(int aux3) {
        this.aux3 = aux3;
    }

    public int getAux4() {
        return aux4;
    }

    public void setAux4(int aux4) {
        this.aux4 = aux4;
    }

    public int getAux1() {
        return aux1;
    }

    public void setAux1(int aux1) {
        this.aux1 = aux1;
    }

    public int getAux2() {
        return aux2;
    }

    public void setAux2(int aux2) {
        this.aux2 = aux2;
    }

    public void reSetEmisor(int emisor){
        this.emisor = emisor;
    }
    
    public int getEmisor() {
        return emisor;
    }

    public void setReceptor(int receptor) {
        this.receptor = receptor;
    }
    
    public int getReceptor() {
        return receptor;
    }

    public String getCabecera() {
        return cabecera;
    }

    public void setCabecera(String cabecera) {
        this.cabecera = cabecera;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }
    
}

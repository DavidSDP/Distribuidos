
package distribuidos;

import java.util.ArrayList;
import java.util.Iterator;

public class Nodo{
    
    private final int id;
    private ArrayList<Integer> hijos;
    private ArrayList<Integer> padres;
    private int nhijos;
    private int npadres;
    
    public Nodo(int id){
        hijos = new ArrayList<>();
        padres = new ArrayList<>();
        this.id = id;
        this.nhijos = 0;
        this.npadres = 0;
    }
    
    public Iterator getHijos(){
        return this.hijos.iterator();
    }
    
    public Iterator getPadres(){
        return this.padres.iterator();
    }
    
    public int getIdentificador(){
        return this.id;
    }
    
    private boolean existeHijo(int idhijo){
        Iterator it = this.getHijos();
        while(it.hasNext()){
            if(((int)it.next()) == idhijo) return true;
        }
        return false;
    }
    
    private boolean existePadre(int idpadre){
        Iterator it = this.getPadres();
        while(it.hasNext()){
            if(((int)it.next()) == idpadre) return true;
        }
        return false;
    }
    
    public boolean addHijo(int hijo){
        if(this.existeHijo(hijo)) return false;
        this.hijos.add(hijo);
        this.nhijos++;
        return true;
    }
    
    public boolean addPadre(int padre){
        if(this.existePadre(padre)) return false;
        this.padres.add(padre);
        this.npadres++;
        return true;
    }

    public int getNhijos() {
        return nhijos;
    }

    public void setNhijos(int nhijos) {
        this.nhijos = nhijos;
    }

    public int getNpadres() {
        return npadres;
    }

    public void setNpadres(int npadres) {
        this.npadres = npadres;
    }
    
}

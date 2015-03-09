
package distribuidos;

import java.util.ArrayList;
import java.util.Iterator;

public class Grafo {
    
    private int nNodos;
    private ArrayList<Nodo> grafo;
    
    public Grafo(){
        this.grafo = new ArrayList<>();
        this.nNodos = 0;
    }
    
    private void putNodo(int idnodo){
        this.grafo.add(new Nodo(idnodo));
        this.nNodos++;
    }
    
    public Nodo getNodo(int idnodo){
        Iterator it = this.grafo.iterator();
        while(it.hasNext()){
            Nodo n = (Nodo)it.next();
            if(n.getIdentificador() == idnodo) return n;
        }
        return null;
    }
    
    public int getNNodos(){
        return this.nNodos;
    }
    
    public boolean existe(int idnodo){
        Iterator it = this.grafo.iterator();
        while(it.hasNext()){
            Nodo n = (Nodo)it.next();
            if(n.getIdentificador() == idnodo) return true;
        }
        return false;
    }
    
    public boolean addNodo(int idnodo){
        if(this.existe(idnodo)) return false;
        this.putNodo(idnodo);
        return true;
    }
    
    public boolean addHijo(int idpadre, int idhijo){
        if(!this.existe(idpadre)) return false;
        Nodo padre = this.getNodo(idpadre);
        if(padre == null) return false;
        return padre.addHijo(idhijo);
    }
    
    public boolean addPadre(int idhijo, int idpadre){
        if(!this.existe(idhijo)) return false;
        Nodo hijo = this.getNodo(idhijo);
        if(hijo == null) return false;
        return hijo.addPadre(idpadre);
    }
    
}

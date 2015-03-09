/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package distribuidos;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class DotToList {
    
    private Grafo grafo;
    
    public DotToList(String path) throws FileNotFoundException, IOException{
        grafo = new Grafo();
        FileInputStream fis = new FileInputStream(path);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        br.readLine(); //ignoramos primera linea
        String linea = br.readLine();
        while(linea.charAt(0) != '}'){
            if(linea.contains("->")){
                almacenarLinea(linea);
            }
            linea = br.readLine();
        }
        System.out.println("END");
    }

    private void almacenarLinea(String linea) {
        int i = 0;
        int f = 0;
        while((linea.charAt(i) == ' ')||(linea.charAt(i) == '\t')){
            i++;
        }
        f=i;
        f++;
        while((linea.charAt(f) != ' ')&&(linea.charAt(f) != '-')){
            f++;
        }
        int from = Integer.parseInt(linea.substring(i, f));
        i=f;
        while((linea.charAt(i)==' ')||(linea.charAt(i)=='-')||(linea.charAt(i)=='>')){
            i++;
        }
        int to = Integer.parseInt(linea.substring(i));
        grafo.addNodo(from);
        grafo.addNodo(to);
        grafo.addHijo(from, to);
        grafo.addPadre(to, from);
    }
    
    public Grafo getGrafo(){
        return this.grafo;
    }
    
}

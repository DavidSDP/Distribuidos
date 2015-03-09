
package distribuidos;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_BYTE_INDEXED;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class Main {

    public static String path_of_file = "graph.dot";
    public static Grafo grafo;
    public static final int contar = 1000000;
    public static final String pathfichero = "totales.txt";
    public static final String imagen = "original.png";
    public static final String newimagen = "nueva.png";
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        createNodes();
        createRoot();
        System.out.println("");
    }
    
    public static void createRoot(){
        MyThread root = new MyThread(grafo.getNodo(0), grafo);
        root.start();
    }
    
    public static void createNodes(){
        try {
            DotToList dtl = new DotToList(path_of_file);
            grafo = dtl.getGrafo();
        } catch (IOException ex) {
            System.out.println("Error: entrada de fichero");
        }
    }

}

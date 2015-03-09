/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package distribuidos;

import Beanstalkd.BeanstalkdClient;
import Beanstalkd.BeanstalkdClientImpl;
import Beanstalkd.BeanstalkdJob;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_BYTE_INDEXED;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import static java.lang.System.nanoTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.SerializationUtils;

/**
 *
 * @author david
 */
public class MyThread extends Thread{
    
    private final Nodo nodo;
    private Grafo grafo;
    private final long prioridadfija = 100;
    private final String host = "127.0.0.1";
    private final int port = 15000;
    private final String tubopropio;
    private String option;
    private final int idnode;
    private BeanstalkdClient cliente;
    private ArrayList<Mensaje> colaInterna = new ArrayList<>();
    private volatile Semaphore sem = new Semaphore(1);
    private long cputime;
    
    private String fichero;
    private final int acontar = Main.contar; // Solo Nodo Raiz
    private volatile Semaphore EM1 = new Semaphore(1);
    private volatile Semaphore semtoken = new Semaphore(0);
    private boolean holding;
    private int parent;
    private int deferred;

    private volatile int[] padres;
    private volatile int[] inDeficitArray;
    private volatile int inDeficit = 0;
    private volatile int outDeficit = 0;
    private volatile int parentDijkstra = -1;
    private boolean isTerminated = false;
    private volatile Semaphore signalDijkstra = new Semaphore(0);
    private volatile Semaphore EMDijkstra = new Semaphore(1);
    private volatile Semaphore EMenviarMensaje = new Semaphore(1);
    private volatile Semaphore semMenu = new Semaphore(1);
    private volatile Semaphore hayMensaje = new Semaphore(0);
    private String imagepath;
    private String newimagepath;
    
    

    public MyThread(Nodo nodo, Grafo grafo) { // Nodo Raiz
        this.nodo = nodo;
        this.idnode = nodo.getIdentificador();
        this.grafo = grafo;
        this.tubopropio = "t_" + this.idnode;
        cliente = new BeanstalkdClientImpl(host, port);
        this.holding = true;
        this.parent = this.idnode - 1;
        this.deferred = -1;
    }

    public MyThread(Nodo nodo) {
        this.nodo = nodo;
        this.idnode = nodo.getIdentificador();
        this.tubopropio = "t_" + this.idnode;
        cliente = new BeanstalkdClientImpl(host, port);
        this.holding = false;
        this.parent = this.idnode - 1;
        this.deferred = -1;
        this.inDeficitArray = new int[this.nodo.getNpadres()];
        this.padres = new int[this.nodo.getNpadres()];
        Iterator iter = this.nodo.getPadres();
        int i = 0;
        while (iter.hasNext()) {
            this.padres[i] = (int) iter.next();
            i++;
        }
    }

    public class sendSignal extends Thread {

        public sendSignal() {

        }

        @Override
        public void run() {
            while (true) {
                sndSignal();
            }
        }
        
    }

    public class MyListenThread extends Thread {

        private BeanstalkdClient clienteRecepcion;
        private final int idnode;
        private final String tubopropio;

        public MyListenThread(int idnode) {
            this.idnode = idnode;
            this.tubopropio = "t_" + this.idnode;
            this.clienteRecepcion = new BeanstalkdClientImpl(host, port);
            this.clienteRecepcion.useTube(this.tubopropio);
        }

        @Override
        public void run() {
            while (true) {
                Mensaje rcv = recibirMensaje();
                switch (rcv.getCabecera()) {
                    case "TOKEN":
                        //System.out.println("Soy "+idnode+" y recibo TOKEN de "+rcv.getEmisor());
                        semtoken.release(); //Adquiere TOKEN
                        break;
                    case "REQUEST":
                        //System.out.println("Soy "+idnode+" y recibo REQUEST de "+rcv.getEmisor());
                        this.treatRequest(rcv);
                        break;
                    case "SIGNAL":
                        //System.out.println(idnode + " recibe SIGNAL de " + rcv.getEmisor());
                        signalDijkstra.release();
                        try {
                            EMDijkstra.acquire();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        outDeficit--;
                        if((outDeficit==0)&&(idnode==0)){
                            System.out.println("Trabajo '"+option+"' terminado al 100% en "+(long)(((long)nanoTime() - (long)cputime))/(long)1000000000 + " segundos");
                            semMenu.release();
                        }
                        EMDijkstra.release();
                        break;
                    default:
                        queueMsg(rcv);
                        break;
                }
            }
        }

        private void treatRequest(Mensaje rcv) {
            try {
                EM1.acquire();
            } catch (InterruptedException ex) {
                Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            int source, originator;
            source = rcv.getEmisor();
            originator = Integer.parseInt(rcv.getContenido());
            if (parent == -1) {
                if (holding) {
                    this.enviarToken(originator);
                    holding = false;
                } else {
                    deferred = originator;
                }
            } else {
                this.enviarRequest(parent, originator);
            }
            parent = source;
            EM1.release();
        }

        private Mensaje recibirMensaje(){
            this.clienteRecepcion.useTube(this.tubopropio);
            this.clienteRecepcion.watch(this.tubopropio);
            BeanstalkdJob job = this.clienteRecepcion.reserve(null);
            byte[] obtiene = job.getData();
            this.clienteRecepcion.delete(job.getJobId());
            Mensaje rcv = (Mensaje) SerializationUtils.deserialize(obtiene);
            return rcv;
        }

        private void enviarMensaje(Mensaje mensaje) {
            try {
                EMenviarMensaje.acquire();
            } catch (InterruptedException ex) {
                Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            String tubo = "t_" + mensaje.getReceptor();
            this.clienteRecepcion.useTube(tubo);
            this.clienteRecepcion.put(prioridadfija, 0, 10, SerializationUtils.serialize(mensaje));
            this.clienteRecepcion.useTube("t_" + mensaje.getEmisor());
            EMenviarMensaje.release();
        }

        private void enviarToken(int originatorORdeferred) {
            Mensaje tok = new Mensaje(this.idnode);
            tok.setReceptor(originatorORdeferred);
            tok.setCabecera("TOKEN");
            //System.out.println("Soy " + idnode + " y envio TOKEN a " + tok.getReceptor());
            this.enviarMensaje(tok);
        }

        private void enviarRequest(int destinatario, int contenido) {
            Mensaje pet = new Mensaje(this.idnode);
            pet.setReceptor(destinatario);
            pet.setCabecera("REQUEST");
            pet.setContenido("" + contenido);
            this.enviarMensaje(pet);
        }

    }

    @Override
    public void run() {
        if (this.idnode == 0) { // Es Raiz
            inicializaHilos();
            MyListenThread mlt = new MyListenThread(this.idnode);
            mlt.start();
            tarea();
        } else {
            MyListenThread mlt = new MyListenThread(this.idnode);
            mlt.start();
            sendSignal ss = new sendSignal();
            ss.start();
            tarea();
        }
    }

    private String menu() {
            System.out.println("----------------------");
            System.out.println();
            System.out.println("Seleccione una opción:");
            System.out.println();
            System.out.println("1. Trabajo de contar");
            System.out.println("2. Trabajo de difuminar imagen");
            System.out.println();
            System.out.println();
            BufferedReader b = new BufferedReader(new InputStreamReader(System.in));
            int choice = 0;
            try {
                choice = Integer.parseInt(b.readLine());
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            switch (choice) {
                case 1:
                    return "CONTAR";
                case 2:
                    return "IMAGEN";
                default:
                    System.out.println("Introduce de nuevo:");
                    return null;
            }
    }
    
    private void tarea() {
        if (this.idnode == 0) {
            while(true) {
                try {
                    semMenu.acquire();
                } catch (InterruptedException ex) {
                    Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
                }
                String opt = menu();
                if(opt!=null){
                cputime = nanoTime();
                switch (opt) {
                    case "CONTAR":
                        crearFichero(Main.pathfichero);
                        this.fichero = Main.pathfichero;
                        contarRaiz();
                        break;
                    case "IMAGEN":
                        enviarImgRaiz();
                        break;
                    default:
                        break;
                }
                }else{
                    semMenu.release();
                }
            }
        } else {
            while (true) {
                this.isEmpty();
                procesarMensaje(this.retrieveMsg());
                isTerminated = true;
            }
        }
    }

    private void procesarMensaje(Mensaje msg) {
        try {
            EMDijkstra.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        // dijkstra ->
        if (parentDijkstra == -1) {
            parentDijkstra = msg.getEmisor();
            System.out.println("Soy " + idnode + " y mi padre es " + parentDijkstra);
        }
        int src = -1;
        int i = 0;
        while (src == -1) {
            if (padres[i] == msg.getEmisor()) {
                src = i;
            }
            i++;
        }
        inDeficitArray[src]++;
        inDeficit++;
        // <- dijkstra
        EMDijkstra.release();
        switch (msg.getCabecera()) {
            case "Contar":
                this.fichero = msg.getStr1();
                contarNodo(msg);
                break;
            case "Imagen":
                this.imagepath = msg.getStr1();
                this.newimagepath = msg.getStr2();
                procesarImgNodo(msg);
                break;
            default:
                System.out.println("Soy "+idnode+" WEIRD HEAD MESSAGE -> "+msg.getCabecera());
                break;
        }
    }
    
    private void inicializaHilos() {
        MyThread hilos[] = new MyThread[this.grafo.getNNodos()];
        hilos[0] = null;
        for (int i = 1; i < this.grafo.getNNodos(); i++) {
            hilos[i] = new MyThread(this.grafo.getNodo(i));
            hilos[i].start();
        }
    }
    
    
    // Tratamiento de la Cola de mensajes local
    
    private void queueMsg(Mensaje msg){
        try {
            this.sem.acquire();
        } catch (InterruptedException ex) {
            System.out.println("Interrupted Exception: wait() en queueMsg()");
        }
        this.colaInterna.add(msg);
        this.sem.release();
        this.hayMensaje.release();
    }
    
    private boolean isEmpty(){
        try {
            this.hayMensaje.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            this.sem.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        boolean rtn = this.colaInterna.isEmpty();
        if(rtn){
            this.sem.release();
        }
        return rtn;
    }
    
    private Mensaje retrieveMsg(){
        Mensaje rtn = this.colaInterna.get(0);
        this.colaInterna.remove(0);
        this.sem.release();
        return rtn;
    }

    
    // Terminacion distribuida Dijkstra-S
    
    private void sndSignal() {
        try {
            EMDijkstra.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (inDeficit > 1) {
            int i = 0;
            boolean found = false;
            while((!found)&&(i<this.padres.length)){
                if ((inDeficitArray[i] > 1) || ((inDeficitArray[i] == 1) && (padres[i] != parentDijkstra))) {
                    Mensaje signal = new Mensaje(this.idnode);
                    signal.setCabecera("SIGNAL");
                    signal.setReceptor(padres[i]);
                    this.enviarMensaje(signal);
                    inDeficitArray[i]--;
                    inDeficit--;
                    found = true;
                }
                i++;
            }
        } else if((inDeficit == 1) && (isTerminated) && (outDeficit == 0)) {
            System.out.println("Soy " + idnode + " y voy a acabar");
            Mensaje signal = new Mensaje(this.idnode);
            signal.setReceptor(parentDijkstra);
            signal.setCabecera("SIGNAL");
            this.enviarMensaje(signal);
            int idpadre = -1;
            for (int i = 0; i < padres.length; i++) {
                if (padres[i] == parentDijkstra) {
                    idpadre = i;
                }
            }
            inDeficitArray[idpadre] = 0;
            inDeficit = 0;
            parentDijkstra = -1;
            isTerminated = false;
            //signalDijkstra.release();
        }
        //endlist = true;
        EMDijkstra.release();
    }
    
    
    // Exclusión mutua Nielsen-Mizuno
    
    private void preSC(){
        try {
            EM1.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(!this.holding){
            this.enviarRequest(parent, this.idnode);
            this.parent = -1;
            try {
                EM1.release();
                semtoken.acquire();
                EM1.acquire();
            } catch (InterruptedException ex) {
                System.out.println("Error preSC");
            }
        }
        holding = false;
        EM1.release();
    }
    
    private void postSC(){
        try {
            EM1.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(deferred!=-1){
            this.enviarToken(deferred);
            deferred = -1;
        }else{
            holding = true;
        }
        EM1.release();
    }
    
    private void enviarMensaje(Mensaje mensaje) {
        String tubo = "t_" + mensaje.getReceptor();
        this.cliente.useTube(tubo);
        this.cliente.put(prioridadfija, 0, 10, SerializationUtils.serialize(mensaje));
        this.cliente.useTube("t_" + mensaje.getEmisor());
    }

    private void enviarToken(int originatorORdeferred) {
        Mensaje tok = new Mensaje(this.idnode);
        tok.setReceptor(originatorORdeferred);
        tok.setCabecera("TOKEN");
        //System.out.println("Soy " + idnode + " y envio TOKEN a " + tok.getReceptor());
        this.enviarMensaje(tok);
    }

    private void enviarRequest(int destinatario, int contenido) {
        Mensaje pet = new Mensaje(this.idnode);
        pet.setReceptor(destinatario);
        pet.setCabecera("REQUEST");
        pet.setContenido("" + contenido);
        this.enviarMensaje(pet);
    }
    
    
    // Trabajo Contar ->  
    
    private void contarRaiz() {
        Mensaje vamosacontar = new Mensaje(this.idnode);
        vamosacontar.setCabecera("Contar");
        vamosacontar.setContenido("" + this.acontar);
        vamosacontar.setStr1(this.fichero);
        int cnt = this.acontar / this.nodo.getNhijos();
        int rst = this.acontar % this.nodo.getNhijos();
        Iterator it = this.nodo.getHijos();
        while (it.hasNext()) {
            vamosacontar.setReceptor((int) it.next());
            vamosacontar.setContenido("" + (cnt + rst));
            rst = 0;
            //System.out.println("Soy la raiz y envio datos a " + vamosacontar.getReceptor());
            
            this.enviarMensaje(vamosacontar);
            try {
                EMDijkstra.acquire();
            } catch (InterruptedException ex) {
                Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            outDeficit++; //DIJKSTRA
            EMDijkstra.release();
        }
    }

    private void contarNodo(Mensaje msg){
        int peticion = Integer.parseInt(msg.getContenido());
        int partes = this.nodo.getNhijos() + 1;
        int cnt = peticion / partes;
        int resto = peticion % partes;
        int micnt = cnt + resto;
        
        Mensaje tochild = new Mensaje(this.idnode);
        tochild.setCabecera("Contar");
        tochild.setContenido("" + cnt);
        tochild.setStr1(this.fichero);
        Iterator it = this.nodo.getHijos();
        while (it.hasNext()) {
            tochild.setReceptor((int) it.next());
            if(parentDijkstra != -1) {
                this.enviarMensaje(tochild);
                try {
                    EMDijkstra.acquire();
                } catch (InterruptedException ex) {
                    Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
                }
                outDeficit++; //DIJKSTRA
                EMDijkstra.release();
            }
        }
        //System.out.println("Soy "+idnode+" y mi outDeficit es "+outDeficit);
        int resultado = this.contarHasta(micnt);
            //System.out.println(idnode+" trabajo terminado");
    }
    
    private int contarHasta(int cantidad){
        int decimaparte = cantidad/10;
        int resto = cantidad % 10;
        for (int i = 1; i <= 11; i++) {
            this.preSC();
            if (i <= 10) {
                aumentarFichero(decimaparte);
            } else {
                aumentarFichero(resto);
            }
            this.postSC();
        }
        return cantidad;
    }
    
    private static void crearFichero(String pathfichero) {
        File file = new File(pathfichero);
        try {
            if(file.exists()){
                file.delete();
            }
            file.createNewFile();
        } catch (IOException ex) {
            System.out.println("Error al crear fichero");
        }
    }
    
    private void aumentarFichero(int cantidad) {
        try {
            FileInputStream fis = new FileInputStream(this.fichero);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String linea;
            int numero = 0;
            if(br.ready()){
                linea = br.readLine();
                numero = Integer.parseInt(linea);
            }
            br.close();
            fis.close();
            
            numero += cantidad;
            
            FileOutputStream fos = new FileOutputStream(this.fichero, false);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write("" + numero);
            bw.close();
            fos.close();
        } catch (IOException ex) {
            System.out.println("Error de fichero");
        }
    }
    
    
    // Trabajo Imagen ->
    
    private void difuminarTrozo(int x, int y, int w, int h) {
        preSC();
        BufferedImage original = null;
        BufferedImage nueva = null;
        try {
            original = ImageIO.read(new File(imagepath));
            nueva = ImageIO.read(new File(newimagepath));
        } catch (IOException ex) {
            Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        BufferedImage copia = new BufferedImage(original.getWidth(),original.getHeight(),TYPE_BYTE_INDEXED);
        float[] floats = {
            0,0.125f,0,
            0.125f,0.5f,0.125f,
            0,0.125f,0,
        };
        BufferedImageOp op = new ConvolveOp(new Kernel(3,3,floats));
        op.filter(original,copia);
        
        copia = copia.getSubimage(x, y, w, h);
        Graphics2D g2d = nueva.createGraphics();
        g2d.drawImage(copia, x, y, null);
        //g2d.drawImage(copia, x, y, w, h, null);
        File output = new File(newimagepath);
        try {
            ImageIO.write(nueva, "png", output);
        } catch (IOException ex) {
            Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        postSC();
    }
    
    private void procesarImgNodo(Mensaje msg){
        int filacero = msg.getAux2();
        int partes = this.nodo.getNhijos() + 1;
        int filasporparte = msg.getAux4()/partes;
        int resto = msg.getAux4() % partes;
        int miparte = filasporparte + resto;
        Mensaje tochild = new Mensaje(this.idnode);
        tochild.setCabecera("Imagen");
        tochild.setContenido("");
        tochild.setStr1(msg.getStr1());
        tochild.setStr2(msg.getStr2());
        Iterator it = this.nodo.getHijos();
        int y = filacero;
        while (it.hasNext()) {
            tochild.setReceptor((int) it.next());
            tochild.setAux1(msg.getAux1());
            tochild.setAux2(y);
            tochild.setAux3(msg.getAux3());
            tochild.setAux4(filasporparte);
            if(parentDijkstra != -1) {
                this.enviarMensaje(tochild);
                try {
                    EMDijkstra.acquire();
                } catch (InterruptedException ex) {
                    Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
                }
                outDeficit++; //DIJKSTRA
                EMDijkstra.release();
            }
            y+=filasporparte;
        }
        int miposy=y;
        //System.out.println("Soy "+idnode+" y mi outDeficit es "+outDeficit);
        difuminarTrozo(msg.getAux1(),miposy,msg.getAux3(),miparte);
    }
    
    private void enviarImgRaiz(){
        String imagepath = Main.imagen;
        String newimagepath = Main.newimagen;
        BufferedImage original = null;
        try {
            original = ImageIO.read(new File(imagepath));
        } catch (IOException ex) {
            Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        Graphics2D g2d = original.createGraphics();
        g2d.setBackground(Color.WHITE);
        File f = new File(imagepath);
        try {
            ImageIO.write(original, "png", f);
        } catch (IOException ex) {
            Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        BufferedImage nueva = new BufferedImage(original.getWidth(),original.getHeight(),BufferedImage.TYPE_INT_ARGB);
        File fn = new File(newimagepath);
        try {
            ImageIO.write(nueva, "png", fn);
        } catch (IOException ex) {
            Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        int filacero = 0;
        int partes = this.nodo.getNhijos();
        int filasporparte = nueva.getHeight()/partes;
        int resto = nueva.getHeight() % partes;
        Mensaje tochild = new Mensaje(this.idnode);
        tochild.setCabecera("Imagen");
        tochild.setContenido("");
        tochild.setStr1(imagepath);
        tochild.setStr2(newimagepath);
        Iterator it = this.nodo.getHijos();
        int y = filacero;
        while (it.hasNext()) {
            tochild.setReceptor((int) it.next());
            tochild.setAux1(0);
            tochild.setAux2(y);
            tochild.setAux3(nueva.getWidth());
            tochild.setAux4(filasporparte + resto);
            System.out.println("Envio a hijo");
            this.enviarMensaje(tochild);
            try {
                EMDijkstra.acquire();
            } catch (InterruptedException ex) {
                Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            outDeficit++; //DIJKSTRA
            EMDijkstra.release();
            y +=filasporparte+resto;
            resto = 0;
        }
    }
    
}

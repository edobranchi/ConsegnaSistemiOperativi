import java.util.Random;
import java.util.concurrent.Semaphore;

import static java.lang.Thread.sleep;


//  EDOARDO BRANCHI
//  6163969
//  Compito 22/11/21

//TODO: le differenze con la consegna sono indicate con i "todo" per maggiore visibilità


public class Consegna {
    public static void main(String[] args) throws InterruptedException {

        int N = 15;    // N° di persone
        int M = 3;     // N° di posti in ospedale
        int K = 4;     // N° di persone inizialmente già contagiate
        int V = 2;     // N° di persone vaccinate

        Ospedale osp = new Ospedale(M);
        Persone[] pers = new Persone[N];
        Ambiente amb = new Ambiente(N);        //ambiente è allocato qui, ma con il vettore delle persone vuoto

        for (int i = 0; i < pers.length; i++) {
            if (i < K) {
                pers[i] = new Persone(i, 50, amb, osp, false);      //allocazione persone già contagiate
                System.out.println("Stanziata persona " + i + " carica 50 e non vaccinata");
            } else if (i > N - V) {
                pers[i] = new Persone(i, 0, amb, osp, true);        //allocazione persone vaccinate
                System.out.println("Stanziata persona " + i + " carica 0 e vaccinata");
            } else {
                pers[i] = new Persone(i, 0, amb, osp, false);       //allocazione persone non contagiate e non vaccinate
                System.out.println("Stanziata persona " + i + " carica 0 e non vaccinata");
            }
        }

        amb.LoadPersone(pers);          //inizializzazione vettore delle persone nella classe ambiente

        for (int i = 0; i < pers.length; i++) {
            pers[i].setName(String.valueOf(i));         //Start dei thread
            pers[i].start();
        }
//TODO:Stampa mancante nella consegna
        for (int i = 0; i < 30; i++) {              //ciclo per stampare ogni secondo per 30 secondi
            for (int j = 0; j < N; j++) {           //ciclo sul vettore delle persone
                if (pers[j].carica >= 10 && !amb.getOspedalizzati()[j]) {       //stampa solo persone con carica>=10 e che
                    System.out.println("---------------------------------------");      //attualmente non sono in ospedale
                    System.out.println(pers[j].getName() + " ha carica virale di : " + pers[j].carica);
                    System.out.println("Le persone in ospedale sono: " + (M - osp.pieno.availablePermits()));
                    System.out.println("Le persone in attesa dell'ospedale sono: " + osp.pieno.getQueueLength());
                    System.out.println("---------------------------------------");
                }
            }
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            Thread.sleep(1000);         //sleep di 1 sec * 30 volte di ciclo su i, circa 30 secondi
        }

        for (int i = 0; i < pers.length; i++) {                            //stampa finale prima della conclusione del programma
            System.out.println("---------------------------------------");
            pers[i].interrupt();            //terminazione dei thread
            System.out.println("Persona " + pers[i].getName() + ": ");
            System.out.println("Contagi Subiti " + pers[i].contagi);
            System.out.println("Contagi effettuati " + pers[i].contagiati);
            System.out.println("N° Ospedalizzazioni " + pers[i].ospnum);
            System.out.println("N° Movimenti " + pers[i].movimenti);


        }
    }
}




//Classe che si occupa di gestire le posizioni, movimento e distanza
class Position {

    double x, y;

    public Position(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void add(Position p) {
        x = x + p.x;
        y = x + p.y;
    }

    public double dist(Position p) {
        return Math.sqrt((x - p.x) * (y - p.y) + (y - p.y) * (y - p.y));
    }
}



//Classe Ospedale, il sempaforo "pieno" è allocato con M permessi disponibili
class Ospedale {

    Semaphore pieno;

    public Ospedale(int M) {
        this.pieno = new Semaphore(M);
    }

    public void GetInto(int T) throws InterruptedException {
        pieno.acquire();
        sleep(T);               //Una volta acquisito il semaforo, la persona rimane un tempo casuale T prima
        pieno.release();              //di essere dimesso e quindi rilasciare il semaforo
    }
}



//classe Ambiente, si occupa della generazione iniziale delle posizioni, del movimento e dell'infezione.
//L'i-esima persona in pers, sarà alla posizione i-esima del vetore pos, se alla posizione i-esima del
//vettore ospedalizzati = TRUE la persona sarà in ospedale.
class Ambiente {
    static Position[] pos;          //dichiaro un vettore delle posizioni
    static Persone[] pers;          //dichiaro un vettore delle persone
    static public boolean[] Ospedalizzati;          //dichiaro un vettore booleano per le Ospedalizzazioni

    Semaphore mutex = new Semaphore(1);

//TODO: LoadPersone e getOspedalizzati mancavano nella consegna, ma servono solo per un caricamento e una stampa
    public void LoadPersone(Persone[] pers) {           //carico le persone nel vettore pers
        this.pers = pers;
    }

    public static boolean[] getOspedalizzati() {         //get necessario per la stampa
        return Ospedalizzati;
    }


    public Ambiente(int N) {

        pos = new Position[N];          //allocazione del vettore delle posizioni (dimensione = n° di persone)

        //TODO:nella consegna non avevo allocato Ospedalizzati
        Ospedalizzati = new boolean[N];  //allocazione del vettore delle ospedalizzazioni (dimensione = n° di persone)

        for (int i = 0; i < N; i++) {       //generazione delle posizioni iniziali [0;20]
            pos[i] = new Position(new Random().nextDouble() * (20 - 0) + 0, new Random().nextDouble() * (20 - 0) + 0);
            Ospedalizzati[i] = false;    //all'inizio nessuna persona è in ospedale
        }
    }

    public void Move(int idpersona, Position p) throws InterruptedException {
        mutex.acquire();        //aquisisce il semaforo mutex per far muovere e infettare una sola persona alla volta
        pos[idpersona].add(p);      //muove fisicamente la persona
        Ambiente.Infect(idpersona);        //passa il controllo al metodo "Infect" che sarà comunque protetto dal mutex
        mutex.release();            //rilascia il mutex
    }

    public static void Infect(int idpersona) {

        Position checkpos = new Position(pos[idpersona].x, pos[idpersona].y);       //salvo temp la posizione della persona che si è appena mossa

        for (int i = 0; i < pos.length; i++) {          //ciclo su tutte le persone
            //TODO: nella consegna mancava il controllo che l'infettante non fosse in ospedale
            if (!Ospedalizzati[idpersona] && !Ospedalizzati[i] && pers[idpersona].carica > 10) {         //controlla che l'infettato e l'infettante non siano in ospedale e che la carica sia >=10
                if (i != idpersona && pos[i].dist(checkpos) < 2) {      //controlla che non possa infettare se stesso e che la distanza sia <2
                    if (!pers[i].vaccinato) {       //caso infettato non vaccinato
                        pers[i].carica += 5;        //aumenta la carica di 5
                        pers[i].contagi += 1;       //il contagiato aumenta di 1 il numero di contagi subiti
                        pers[idpersona].contagiati += 1;            //il contagiante aumenta di 1 il numero di contagi effettuati
                    } else {            //caso infettato sia vaccinato
                        int prob = (int) (Math.random() * 100);     //genera un numero casuale [0;0.1] e lo molt per 100 per ottenere [0;100]
                        if (prob <= 10) {    //probabilità <10% di infettare
                            pers[i].carica += 5;
                            pers[i].contagi += 1;
                            pers[idpersona].contagiati += 1;
                        }
                    }
                }
            }
        }
    }

    public void Ospedalizza(int idpersona) {        //ospedalizzare una persona e rimuoverla dalle altre
        Ospedalizzati[idpersona] = true;
    }

    public void Dimetti(int idpersona) {            //dimettere una persona e reinserirla fra le altre
        Ospedalizzati[idpersona] = false;
    }
}


class Persone extends Thread {
    int idpersona;
    int carica;         //carica virus
    boolean vaccinato;  //flag vaccino
    int contagi;        //N° contagi subiti
    int contagiati;     //N° di contagi effettuati
    int ospnum;         //N° di visite all'ospedale
    int movimenti;      //N° di volte che si è mosso
    Ambiente A;
    Ospedale H;

    //TODO:nella consegna mancava il costruttore di Persone, era indicato ma non completato
    public Persone(int idpersona, int carica, Ambiente a, Ospedale h, boolean vaccinato) {      //costruttore
        this.idpersona = idpersona;
        this.carica = carica;
        this.vaccinato = vaccinato;
        contagi = 0;
        contagiati = 0;
        ospnum = 0;
        movimenti=0;
        A = a;
        H = h;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Position p = new Position(new Random().nextDouble() * (1 - (-1)) + (-1), new Random().nextDouble() * (1 - (-1)) + (-1));
                A.Move(idpersona, p);       //Genero un movimento di [-1;1] e muovo la persona
                movimenti+=1;
                //TODO:nella consegna non controllava se la carica era 0 prima di decrementare
                if (carica > 0) {
                    carica = carica - 1;        //diminuisce la carica di 1 se è maggiore di 0
                }
                sleep(1000);         //attesa di 1000 perché con 100 il virus andava a scomparire
                //TODO: il controllo per l'ospedalizzazione è stato leggermente riarrangiato, mancava l'incremento del numero di ospedalizzazioni
                if (carica >= 100) {
                    A.Ospedalizza(idpersona);       //segno la persona come in ospedale e la rimuovo dalle altre
                    System.out.println("Persona " + getName() + ": Si ospedalizza ");
                    ospnum += 1;
                    H.GetInto((int) (Math.random() * ((10000 - 1000) + 1)) + 1000);     //la faccio entrare in ospedale con un tempo di attesa [1000;10000] ms
                    System.out.println("Persona " + getName() + ": uscita dall'ospedale: ");
                    A.Dimetti(idpersona); //segno la persona come dimessa e la riaggiungo alle altre
                    carica = 0;         //azzero la carica
                }
            }
        } catch (InterruptedException e) {
        }
        interrupt();
    }

}
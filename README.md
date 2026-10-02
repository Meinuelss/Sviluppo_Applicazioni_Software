# CatERing - Event & Kitchen Management System

Questo repository contiene il progetto realizzato per il corso di **Sviluppo Applicazioni Software** di Serranò Manuel e Riccioni Tommaso. Si tratta di un sistema software progettato e sviluppato per supportare un'azienda di catering nella gestione di eventi, menu, compiti in cucina e turni del personale.

L'obiettivo principale del progetto è stato l'applicazione rigorosa dei principi dell'Ingegneria del Software, partendo dall'analisi dei requisiti fino all'implementazione in Java, passando per la modellazione UML dettagliata.

## Funzionalità Principali (Core Features)

Il sistema implementa la logica di business per i seguenti moduli:
* **Gestione Eventi:** Creazione, approvazione e gestione dei dettagli degli eventi programmati.
* **Gestione Menu:** Creazione e modifica di menu, definizione di sezioni e associazione di ricette/preparazioni.
* **Gestione Cucina (Kitchen Tasks):** Generazione del foglio riepilogativo, assegnazione dei compiti ai cuochi, stima dei tempi e delle quantità.
* **Gestione Turni (Shifts):** Visualizzazione dei turni di cucina e del tabellone per l'assegnazione dello staff.
* **Gestione Ricettario:** Consultazione delle ricette e delle preparazioni disponibili.
* **Gestione Utenti:** Sistema basato su ruoli (Organizzatore, Chef, Cuoco, Personale di servizio, ecc.).

## Tecnologie Utilizzate

* **Linguaggio:** Java
* **Build Automation:** Maven
* **Database:** SQLite (tramite JDBC)
* **Testing:** JUnit (Unit testing e integrazione)
* **Progettazione:** StarUML / Draw.io (Modellazione UML)

## Documentazione di Progetto (Software Engineering)

Uno dei punti di forza di questo progetto è la documentazione. Nella cartella `Consegna_progetto` e `rev` è possibile trovare tutti gli artefatti prodotti durante il ciclo di vita del software:
* **Modello di Dominio**
* **Casi d'Uso (Brevi e Dettagliati)**
* **SSD (System Sequence Diagrams)** per gli scenari principali e le estensioni.
* **Contratti delle Operazioni** (Pre-condizioni e Post-condizioni).
* **DSD (Design Sequence Diagrams)** per la progettazione architetturale.
* **DCD (Design Class Diagram)** per la struttura finale delle classi.

## Installazione e Setup

1. **Clona il repository:**
   ```bash
   git clone https://github.com/TUO_USERNAME/Sviluppo_Applicazioni_Software.git
   ```
2. **Naviga nella cartella del progetto Java:**
   ```bash
   cd Sviluppo_Applicazioni_Software/catering
   ```
3. **Compila e installa le dipendenze con Maven:**
   ```bash
   mvn clean install
   ```
4. **Database:**
   Il database SQLite (`catering.db`) e lo script di inizializzazione (`catering_init_sqlite.sql`) sono presenti nella cartella `database`.

5. **Test ed esecuzione:**
   ```bash
   mvn test
   mvn exec:java
   ```
      
## Autore

* **Serranò Manuel e Riccioni Tommaso** - *Sviluppatore / Studente* *

---
*Progetto accademico realizzato a scopo didattico.*

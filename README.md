# NBG-PT

NBG-PT ist eine Webanwendung, mit der sich der offene Datenbestand der Stadt Nürnberg über einfache Textanfragen durchsuchen lässt.  
*(Quelle der Daten: [BayernData Nürnberg](https://nuernberg.bydata.de/?locale=de))*

## Aufbau des Projekts

Das Projekt besteht aus zwei Hauptteilen:

- **Datenvorbereitung:**  
  Python-Skripte, die Metadaten zu den verfügbaren Datensätzen herunterladen und so in einer Datenbank aufbereiten, dass sie durchsuchbar werden.

- **Webanwendung:**  
  Das Kernstück ist ein Chat-Interface (Frontend & Backend). Wenn ein Nutzer eine Frage stellt, sucht das System die passenden Informationen aus der Datenbank heraus. Ein KI-Modell liest diese Informationen und formuliert daraus eine direkte, verständliche Antwort für den Nutzer.

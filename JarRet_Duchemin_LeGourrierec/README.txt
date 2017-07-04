/************************** README by Duchemin & Le Gourrierec **************************/ 


* Les jars éxecutables ClientJarRet et ServerJarRet déjà créés sont situés dans  : 
./jarPreBuild


        une fois dans le dossier jarPreBuild les commandes à lancer sont les suivantes :
* Client :  java -jar ClientJarRet.jar  [-d|--debug] [-v|--verbose] clientId host port
* Server : java -jar ServerJarRet.jar ServerJarRet [-v|--verbose] pathConfig


* Les jars compilés par ant seront créé à la racine.


* Les sources sont placées dans le dossier :
./src


* Le fichier de configuration pour le lancement du serveur se actuellement à l’emplacement :
                ./input/JarRetConfig.json
        Il est à donner en paramètre au lancement du serveur en remplaçant pathConfig de la ligne de lancement. 
        Ce dernier se compose de différent champs a format JSON comme suit:
* "Port" : le port sur lequel doit se lancer le serveur
* "LogsPath" : le chemin où doivent être créés les fichiers de log du serveur
* "DefaultInputPath" : le chemin du répertoire où sont placer les jobs
* "FilesAnswerPath" : le chemin où vont être créés les répertoires/fichiers contenant les réponses des clients pour les jobs
* "MaxSizeFileAnswer" : la taille maximal que peut avoir un fichier contenant la réponse à un job
* "ComeBackTime" : le temps en seconde à envoyer au client avant qu’il ne redemande une nouvelle si le serveur n’en a plus à lui donner au moment de la demande du client


* Le(s) fichier(s) des jobs sont actuellement dans le dossier :
                ./input/jobs/


* Les logs des Clients seront placé dans un répertoire nommé :
./output/logsClient/ClientId/
Dans ce répertoire ce tiendra deux fichiers de logs par jours où le client est exécuté comme suit :
        ./output/logsClient/ClientId/aaaa-mm-jj Client(ClientId)Log.txt
                contenant les logs de bon fonctionnement du client.


        ./output/logsClient/ClientId/aaaa-mm-jj Client(ClientId)LogError.txt
        contenant les logs des erreurs intervenues pendant le fonctionnement du client.


* Les logs du Serveur seront placé dans un répertoire nommé actuellement :
./output/logsServer/
Dans ce répertoire ce tiendra deux fichiers de logs par jours où le client est exécuté comme suit :
        ./output/logsServer/aaaa-mm-jj ServerLog.txt
                contenant les logs de bon fonctionnement du serveur.


        ./output/logsServer/aaaa-mm-jj ServerLogError.txt
        contenant les logs des erreurs intervenues pendant le fonctionnement du serveur.


* Les réponses reçues par le serveur sont actuellement dans le répertoire :
                ./output/answer/
Ce dossier contiendra les réponses classées dans des dossiers par jobId et aurons la forme suivante : 
        ./output/answer/jobId/jobId(taskNumber)ClientId.json
        Ces derniers fichiers contiendront toutes la partie json renvoyés par le client après le traitement d’une tâche.



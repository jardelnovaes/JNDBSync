@echo off 
cls
java -cp libs;target/JNDBSync-0.0.1-SNAPSHOT.jar;D:/libs/repository-api-0.0.1.jar;D:/libs/model-1.0.0.jar; com.jardelnovaes.utils.database.neodbsync.JNDBSync

pause

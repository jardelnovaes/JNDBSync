@echo off 
cls
java -cp libs;target/JNDBSync-0.0.1-SNAPSHOT.jar;D:/Neogrid/workspaces/tempWork/NeoDBSync/libs/ng-repository-api-0.9.2-nodeps.jar;D:/Neogrid/workspaces/tempWork/NeoDBSync/libs/sped-arch-1.60.0-SNAPSHOT.jar;D:/Neogrid/workspaces/tempWork/NeoDBSync/libs/sped-model-1.60.0-SNAPSHOT.jar; com.jardelnovaes.utils.database.neodbsync.JNDBSync

pause

package com.company;

import java.util.*;
import com.company.DepListItem;
import com.company.InstallFile;
import java.nio.file.*;


// Detector of not allowed dependencies

@SuppressWarnings("WeakerAccess")
public class Main {

    @SuppressWarnings("LoopStatementThatDoesntLoop")
    public static void main(String[] args) {

        if (args.length < 2)  {
            System.out.println("Usage java OverlapDetector.jar install.txt_file_path storage_files_path -s -e -ew -l [machine read log filename]");
            System.out.println(" -s storage update only, no intersection detector ");
            System.out.println(" -e log only errors");
            System.out.println(" -w log warnings");
            System.out.println(" -l generate machine read log");
            System.out.println("[machine read log filename] if -l parameter detected, machine read log write in  this file, if parameter not set, log write in file OverlapDetector.err in install.txt file directory");
            return;
        }

        String FILE_NAME = args[0];   // полное имя файла install.txt кандидата
        String STORAGE_PATH = args[1]; // путь к основному билду=путь к сводным файлам  зависимостей и объектов

        int NextArgIdx=2;

        boolean flagOnlyStorage = false;
        boolean flagOnlyError = false;
        boolean flagAlsoLogWarning = false;

        boolean flagLogToFile = false;
        boolean flagAppendLogFile =false;

        String LogFileName = new String();

         while (args.length > NextArgIdx) {
            switch (args[NextArgIdx]) {
                case "-s":
                     flagOnlyStorage=true;
                     break;
                case "-e":
                      flagOnlyError=true;
                      break;
                case "-w":
                      flagAlsoLogWarning=true;
                      break;
                case "-l":
                    flagLogToFile = true;
                    if ((NextArgIdx+1) < args.length ) {
                        LogFileName=args[NextArgIdx+1];
                        flagAppendLogFile=true;
                    } else {
                        LogFileName=Paths.get(Paths.get(FILE_NAME).getParent().toString(),"OverlapDetector.err").toString();
                    }
                    break;
            };
            NextArgIdx= NextArgIdx+1;
        }

        ReleaseObject ReleaseObjectsFile = new ReleaseObject(STORAGE_PATH);

        InstallFile CheckInstFile = new InstallFile(FILE_NAME); //  парсим входной install.txt
        if (CheckInstFile.HasError())
        {
            System.out.println();
            System.out.println(CheckInstFile.getHasErrorString());
            // Сохранить отчет об ошибке
            if (flagLogToFile) {
                ReleaseObjectsFile.SaveReport(LogFileName, CheckInstFile,flagAppendLogFile,flagOnlyError,flagAlsoLogWarning);
            }
            if (!flagOnlyStorage) {
                // Установить ERRORLEVEL как ошибка
                System.exit(-1);
            }
        }

        if (flagOnlyStorage) {
            // Обновить объекты PCK
            ReleaseObjectsFile.ChangeReleaseItemList(CheckInstFile);
            // Обновить список ЗНИ
            ReleaseObjectsFile.ChangeReleaseZNIList(CheckInstFile);
            // Сохранить полный список ЗНИ
            ReleaseObjectsFile.SaveZNIList();
            // Сохранить полный список объектов PCK
            ReleaseObjectsFile.SaveItemList();
            // Установить ERRORLEVEL как без ошибок
            System.exit(0);
        }
        else
        {
            ReleaseObjectsFile.OverlapDetector(CheckInstFile);

            System.out.println(ReleaseObjectsFile.GenerateReportText(CheckInstFile, false, flagOnlyError, flagAlsoLogWarning));
            if (ReleaseObjectsFile.isErrorDetected())
            {
                // Сохранить отчет об ошибке
                if (flagLogToFile) {
                    ReleaseObjectsFile.SaveReport(LogFileName, CheckInstFile, flagAppendLogFile, flagOnlyError, flagAlsoLogWarning);
                }
                // Установить ERRORLEVEL как ошибка
                System.exit(-1);
            }
            else
            {
                // Установить ERRORLEVEL как ошибка
                System.exit(0);
            }
        }
    }
}
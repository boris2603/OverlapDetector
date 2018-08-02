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
            System.out.println("Usage java OverlapDetector.jar install.txt_file_path storage_files_path -s -l [machine read log filename]");
            System.out.println(" -s storage update only, no intersection detector ");
            System.out.println(" -l generate machine read log");
            System.out.println("[machine read log filename] if -l parameter detected, machine read log write in it file, if parameter not set, log write in file 'install directory'.err ");
            return;
        }

        String FILE_NAME = args[0];   // полное имя файла install.txt кандидата
        String STORAGE_PATH = args[1]; // путь к основному билду=путь к сводным файлам  зависимостей и объектов

        int NextArgIdx=2;

        boolean OnlyStorage = false;
        if (args.length >= (NextArgIdx+1)) {
            OnlyStorage = args[NextArgIdx].equals("-s");
            NextArgIdx= OnlyStorage ? NextArgIdx+1 : NextArgIdx;
        }
        boolean LogToFile = false;
        boolean AppendLogFile =false;

        String LogFileName = new String();

        if ((args.length >= (NextArgIdx+1)) && !OnlyStorage) {
            LogToFile = args[NextArgIdx].equals("-l");

            if ((args.length == (NextArgIdx+2)) && LogToFile) {
                LogFileName=args[NextArgIdx+1];
                AppendLogFile=true;
            } else {
                LogFileName=Paths.get(FILE_NAME).getParent().toString().concat("err");
            }
        }

        ReleaseObject ReleaseObjectsFile = new ReleaseObject(STORAGE_PATH);

        InstallFile CheckInstFile = new InstallFile(FILE_NAME); //  парсим входной install.txt
        if (CheckInstFile.HasError())
        {
            System.out.println();
            System.out.println(CheckInstFile.getHasErrorString());
            // Сохранить отчет об ошибке
            if (LogToFile) {
                ReleaseObjectsFile.SaveReport(LogFileName, CheckInstFile,AppendLogFile);
            }
            // Установить ERRORLEVEL как ошибка
            System.exit(-1);
        }

        if (OnlyStorage) {
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


            System.out.println(ReleaseObjectsFile.GenerateReportText(CheckInstFile, false));
            if (ReleaseObjectsFile.isErrorDetected())
            {
                // Сохранить отчет об ошибке
                if (LogToFile) {
                    ReleaseObjectsFile.SaveReport(LogFileName, CheckInstFile, AppendLogFile);
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
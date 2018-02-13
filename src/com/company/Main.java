package com.company;

import java.util.*;
import com.company.DepListItem;
import com.company.InstallFile;


// Detector of not allowed dependencies
@SuppressWarnings("WeakerAccess")
public class Main {

    @SuppressWarnings("LoopStatementThatDoesntLoop")
    public static void main(String[] args) {
        if (args.length < 2)  {
            System.out.println("Usage java OverlapDetector.jar install.txt_file_path storage_files_path -s");
            System.out.println(" -s storage update only, no intersection detector ");
            return;
        }
        String FILE_NAME=args[0];
        String STORAGE_PATH=args[1];
        boolean OnlyStorage=false;
        if (args.length==3) {
            OnlyStorage=args[2].equals("-s");
        }

        ReleaseObject ReleaseObjectsFile=new ReleaseObject(STORAGE_PATH);
        ReleaseObjectsFile.LoadItemList();
        ReleaseObjectsFile.LoadZNIList();
        
        InstallFile CheckInstFile=new InstallFile(FILE_NAME);
        if (CheckInstFile.HasError())
        {
            System.out.println();
            System.out.println(CheckInstFile.getHasErrorString());
            System.exit(-1);
        }

        if (OnlyStorage) {
            // Обновить объекты PCK
            ReleaseObjectsFile.ChangeReleaseItemList(CheckInstFile.sZNI, CheckInstFile.FullPCKItemsList);
            // Обновить список ЗНИ
            ReleaseObjectsFile.ChangeReleaseZNIList(CheckInstFile.sZNI, CheckInstFile.DepZNIList);

            // Сохранить полный список ЗНИ
            ReleaseObjectsFile.SaveZNIList();

            // Сохранить полный список объектов PCK
            ReleaseObjectsFile.SaveItemList();

            // Установить ERRORLEVEL как без ошибок
            System.exit(0);
        }
        else
        {
            ArrayList<OverlapItem> ZNIDepend=ReleaseObjectsFile.OverlapDetector(CheckInstFile);
            if (!ZNIDepend.isEmpty())
            {
                System.out.println();
                System.out.println("Not allowed intersections by RFC " + CheckInstFile.sZNI + ":");
                for (OverlapItem item : ZNIDepend) {
                    System.out.print(item.mainZNI + " ");
                    if (item.depListItems.isEmpty()) {
                        System.out.print(" not installed");
                    } else {
                        for (DepListItem depListItem : item.depListItems) {
                            System.out.print(depListItem.Type + " " + depListItem.TBP + " " + depListItem.Object);
                        }


                    }
                    System.out.println();
                }
                // Установить ERRORLEVEL как ошибка
                System.exit(-1);
            }
            else
            {
                System.out.println();
                System.out.println(CheckInstFile.sZNI+" intersection check passed successfully");
            }
        }

    }
}


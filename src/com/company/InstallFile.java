package com.company;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.*;
import java.util.*;


class InstallFile {

    // результаты разбора
    public String sZNI ="";
    public String Developer;
    public final ArrayList<DepListItem> FullPCKItemsList = new ArrayList<>();
    public final ArrayList<String> DepZNIList= new ArrayList<>();
    public final ArrayList<String> EmailList= new ArrayList<>();


    // Путь к файлам где лежит дистрибутив
    private final String InstallFileMasterPath;

    // Список ошибок
    private String HasErrorString = "";

    // Список PCK файлов
    private final ArrayList<String> pckList= new ArrayList<>();
    // Список MDB файлов
    private final ArrayList<String> mdbList= new ArrayList<>();

    // Список паттернов для разбора файла Install.txt
    // ЗНИ и почта разработки
    private final Pattern pZNI =Pattern.compile("ЗНИ\\s*[0-9]{6}");
    private final Pattern pEMail=Pattern.compile("<[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})>");
    // PCK файлы для установки
    private final Pattern pPCKListIndicator=Pattern.compile("\\.*Установить\\.*");
    private final Pattern pPCK =Pattern.compile("([\\w-]+\\\\)*\\w*\\.pck");
    // Список ЗНИ зависимостей
    private final Pattern pDepListIndicator=Pattern.compile("\\.*Установка\\s*после\\.*");
    private final Pattern pDependZNI=Pattern.compile("[0-9]{6}");
    // Индикатор что есть Mdb
    private final Pattern pMDB =Pattern.compile("([\\w-]+\\\\)*\\w*\\.mdb");
    // Разработчик
    private final Pattern pDeveloper=Pattern.compile("Разработчик:\\s*\\w*");


    // Индикатор, что у нас есть ошибки парсинга файлов
    boolean HasError()
    {
        return (!HasErrorString.isEmpty());
    }

    // Вернуть список ошибок парсинга
    public String getHasErrorString() {
        return HasErrorString;
    }

    InstallFile(String InstallTxtFileName)
    {
        Developer="";
        InstallFileMasterPath = Paths.get(InstallTxtFileName).getParent().toString();
        InstallFileParce(LoadFile(InstallTxtFileName));
        if (sZNI.isEmpty())
        {
            HasErrorString=HasErrorString+"Invalid install.txt format: unresolve ЗНИ parametr"+System.lineSeparator();
        }
        if (pckList.isEmpty() && (!mdbList.isEmpty()))
        {
            HasErrorString=HasErrorString+"Invalid install.txt format: found mdm but no pck files"+System.lineSeparator();
        }
        else {
            LoadDeployObjects(pckList);
        }

    }

    // Загрузить файл с диска с обработкой ошибок
    private List<String> LoadFile(String FileName)
    {
        List<String> lines = new ArrayList<>();

        try {
            lines = Files.readAllLines(Paths.get(FileName), Charset.forName("windows-1251"));
        }
        catch (IOException e)
        {
            System.out.println("IO Error reading Install File "+FileName);
            System.out.println(e.getMessage());
            HasErrorString=HasErrorString+"IO error reading Install File "+FileName+System.lineSeparator();
        }
        return lines;
    }

    // Получить список подстрок по pPattern c учетом сдвига CorrectIndex
    private ArrayList<String> GetMatchList(String line,Pattern pPattern,int CorrectIndex)
    {
        ArrayList<String> ResultList = new ArrayList<>();

        Matcher m = pPattern.matcher(line);
        while (m.find()) {
            ResultList.add(line.substring(m.start()+CorrectIndex, m.end()-CorrectIndex).trim());
        }
        return ResultList;
    }

    // Получить маасив подстрок по pPattern c учетом сдвига CorrectIndex, если найден pPatternIndicator
    private ArrayList<String> GetMatchListByIndicator(String line, Pattern pPattern, Pattern pPatternIndicator)
    {
        ArrayList<String> ResultList = new ArrayList<>();

        Matcher m = pPatternIndicator.matcher(line);
        if (m.find()) {
            ResultList = GetMatchList(line,pPattern, 0);
        }
        return ResultList;
    }

    // Получить подсктроку по маске с учетом корректировочных индексов
    private String GetMatchParam(String line, Pattern pPattern, @SuppressWarnings("SameParameterValue") int CorrectIndexLeft, @SuppressWarnings("SameParameterValue") int CorrectIndexRight) {
        String Result="";

        Matcher m = pPattern.matcher(line);
        if (m.find())  {
            Result=line.substring(m.start()+ CorrectIndexLeft, m.end()+CorrectIndexRight).trim();
        }
        return Result;
    }

    // Получить подсктроку по маске с учетом корректировочных индексов
    private String GetMatchParamByStart(String line, Pattern pPattern, @SuppressWarnings("SameParameterValue") int CorrectIndexLeft) {
        String Result="";

        Matcher m = pPattern.matcher(line);
        if (m.find())  {
            Result=line.substring(m.start()+ CorrectIndexLeft).trim();
        }
        return Result;
    }

    // Распарсить строку Install
    private void InstallFileParce(List<String> lines) {

        for(String line: lines){
            
            // Получаем номер ЗНИ
            if (sZNI.isEmpty())
            {
                sZNI=GetMatchParam(line,pZNI,4,0);
            }
            // Получаем ФИО разработчика
            if (Developer.isEmpty())
            {
                Developer=GetMatchParamByStart(line,pDeveloper,12);
            }

            // Создаем список PCK
            pckList.addAll(GetMatchListByIndicator(line,pPCK,pPCKListIndicator));
            mdbList.addAll(GetMatchList(line,pMDB,0));

            //Обрабатываем Установить после
            DepZNIList.addAll(GetMatchListByIndicator(line,pDependZNI,pDepListIndicator));

            //Получаем список почты
            EmailList.addAll(GetMatchList(line,pEMail,1));
        }

    }

    // Распарсить найденный PCK
    private ArrayList<DepListItem> LoadPCKFile(String ZNI, String pckFileName)
    {
        ArrayList<DepListItem> DepList = new ArrayList<>();
        List<String> PCKlines = LoadFile(pckFileName);

        for (String line : PCKlines) {
            String[] items = line.split(" ");
            if (items.length> 0) {
                switch (items[0]) {
                    case "METH":
                    case "CRIT":
                    case "TRANS":
                    case "E-METH":
                    case "IDX":
                        DepListItem dplItem = new DepListItem();
                        dplItem.ZNI = ZNI;
                        dplItem.Object = items[2];
                        dplItem.TBP = items[1];
                        dplItem.Type = items[0];
                        if (!ObjectAlreadyInList(dplItem)) {
                            DepList.add(dplItem);
                        }
                        break;
                }
            }
        }
        return DepList;
    }

    // Проверить что объект есть в списке объектов ЗНИ
    @SuppressWarnings("WeakerAccess")
    boolean ObjectAlreadyInList(DepListItem checkItem)
    {
        boolean retVal=false;
        if (checkItem !=null)
        {
            for (DepListItem item : FullPCKItemsList) {
                if (item.Type.equals(checkItem.Type) &&
                        item.TBP.equals(checkItem.TBP) && item.Object.equals(checkItem.Object))
                {
                    retVal=true;
                    break;
                }
            }
        }
        return retVal;
    }

    // Загрузить список из всех PCK файлов
    private void LoadDeployObjects(List<String> pckFileList)
    {
        for (String pckFile: pckFileList) {
        File file = new File(InstallFileMasterPath, pckFile.replace('\\',File.separatorChar));

        ArrayList<DepListItem> PCKListItem = LoadPCKFile(sZNI,file.getPath());
        FullPCKItemsList.addAll(PCKListItem);
    }

    }


}

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
    private final ArrayList<String> pckList = new ArrayList<>();
    // Список MDB файлов
    private final ArrayList<String> mdbList = new ArrayList<>();

    // Список паттернов для разбора файла Install.txt
    // ЗНИ и почта разработки
    private final Pattern pZNI = Pattern.compile("(ЗНИ|RFC).*([0-9]{6})", Pattern.CASE_INSENSITIVE);
    private final Pattern pEMail = Pattern.compile("<[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})>");
    // PCK файлы для установки
    private final Pattern pPCKListIndicator = Pattern.compile("Установить", Pattern.CASE_INSENSITIVE);
    private final Pattern pPCK = Pattern.compile("([\\w-]+\\\\)*((ЗНО|C0|CI|SD|IM))*\\w*\\.pck", Pattern.CASE_INSENSITIVE);
    // Список ЗНИ зависимостей
    private final Pattern pDepListIndicator = Pattern.compile("(Установка|Установить|Ставить|Устанавливать)\\s*после", Pattern.CASE_INSENSITIVE);
    private final Pattern pDependZNI = Pattern.compile("[0-9]{6}");
    // Индикатор что есть Mdb
    private final Pattern pMDB = Pattern.compile("([\\w-]+\\\\)*\\w*\\.mdb", Pattern.CASE_INSENSITIVE);
    // Разработчик
    private final Pattern pDeveloper = Pattern.compile("(Разработчик|Разработчики)\\s*:*\\s*(.{1,})",Pattern.CASE_INSENSITIVE );
    // ЗНО/CI/C0/SD и прочая светотень
    private final Pattern pZNO = Pattern.compile("(ЗНО|C0|CI|SD|IM).*[0-9]{6,8}", Pattern.CASE_INSENSITIVE);
    // UNITY
    private final Pattern pUnity = Pattern.compile("(UNITY)(.*|_)[0-9]{6}", Pattern.CASE_INSENSITIVE);

    // Индикатор, что у нас есть ошибки парсинга файлов
    boolean HasError()
    {
        return (!HasErrorString.isEmpty());
    }

    // Вернуть список ошибок парсинга
    public String getHasErrorString() {
        return ("ERROR "+ InstallFileMasterPath + System.lineSeparator() + HasErrorString);
    }

    InstallFile(String InstallTxtFileName)
    {
        Developer = "";
        InstallFileMasterPath = Paths.get(InstallTxtFileName).getParent().toString();

        InstallFileParce(LoadFile(InstallTxtFileName));

        if (sZNI.isEmpty())
        {
            HasErrorString = HasErrorString + "Invalid install.txt format: unresolve ЗНИ parametr" + System.lineSeparator();
        }
        else if (pckList.isEmpty() && (!mdbList.isEmpty()))
        {
            HasErrorString = HasErrorString + "Invalid install.txt format: found mdm but no pck files" + System.lineSeparator();
        }
        else
        {
            LoadDeployObjects(pckList); // заполняем структуру с перечнем объектов из всех pck этого install
        }

    }

    // Распарсить строку Install
    private void InstallFileParce(List<String> lines) {

        for(String line: lines){

            // Получаем номер ЗНИ
            if (sZNI.isEmpty())
            {
                sZNI = GetMatchParam(line, pZNI,2);
                if (sZNI.isEmpty())
                {
                    sZNI = GetMatchParam(line, pZNO,0);
                }
                if (sZNI.isEmpty())
                {
                    sZNI = GetMatchParam(line, pUnity,0);
                }
            }
            // Получаем ФИО разработчика
            if (Developer.isEmpty())
            {
                Developer = GetMatchParam(line, pDeveloper, 2);
                Developer = Developer.replace(',',' ');
            }

            // Создаем список PCK
            pckList.addAll(GetMatchListByIndicator(line, pPCK, pPCKListIndicator));
            mdbList.addAll(GetMatchList(line, pMDB));

            //Обрабатываем Установить после
            DepZNIList.addAll(GetMatchListByIndicator(line, pDependZNI, pDepListIndicator));

            //Получаем список почты
            EmailList.addAll(GetMatchList(line, pEMail));
        }

    }

    private ArrayList<String> GetMatchList(String line, Pattern pPattern)
    {
        ArrayList<String> ResultList = new ArrayList<>();

        Matcher m = pPattern.matcher(line);
        while (m.find()) {
            ResultList.add(m.group());
        }
        return ResultList;
    }

    // Получить маасив подстрок по pPattern c учетом сдвига CorrectIndex, если найден pPatternIndicator
    private ArrayList<String> GetMatchListByIndicator(String line, Pattern pPattern, Pattern pPatternIndicator)
    {
        ArrayList<String> ResultList = new ArrayList<>();

        Matcher m = pPatternIndicator.matcher(line);
        if (m.find()) {
            ResultList = GetMatchList(line,pPattern);
        }
        return ResultList;
    }

    private String GetMatchParam(String line, Pattern pPattern, int MatchGroupIdx) {
        String Result = "";

        Matcher m = pPattern.matcher(line);
        if (m.find())  {
            Result = m.group(MatchGroupIdx);
        }
        return Result;
    }

    // Загрузить в переменную список объектов из списка PCK файлов
//!!!SAM проверить обработку  РСК-файлов в вложенных папках
    private void LoadDeployObjects(List<String> pckFileList)
    {
        for (String pckFile : pckFileList) {
            File file = new File(InstallFileMasterPath, pckFile.replace('\\', File.separatorChar));
            FullPCKItemsList.addAll(LoadPCKFile(file.getPath()));
        }
    }

    // Распарсить один из найденных PCK
    private ArrayList<DepListItem> LoadPCKFile(String pckFileName)
    {
        ArrayList<DepListItem> DepList = new ArrayList<>();
        List<String> PCKlines = LoadFile(pckFileName);

        for (String line : PCKlines) {
            String[] items = line.split(" ");
            if (items.length > 0) {
                switch (items[0]) {
                    case "METH":
                    case "CRIT":
                    case "TRANS":
                    case "E-METH":
                    case "IDX":
                        DepListItem dplItem = new DepListItem();
                        dplItem.ZNI = sZNI;
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

    // Загрузить файл с диска с обработкой ошибок
    private List<String> LoadFile(String FileName)
    {
        List<String> lines = new ArrayList<>();

        if (!Files.exists(Paths.get(FileName)))
        {
            System.out.println("File not found " + FileName);
            HasErrorString = HasErrorString + "File not found " + FileName + System.lineSeparator();
        }
        else {
            try {
                lines = Files.readAllLines(Paths.get(FileName), Charset.forName("windows-1251"));
            } catch (IOException e) {
                System.out.println("IO Error reading file " + FileName);
                System.out.println(e.getMessage());
                HasErrorString = HasErrorString + "IO error reading File " + FileName + System.lineSeparator();
            }
        }
        return lines;
    }

    // Проверить наличие объекта в остальном списке объектов этой ЗНИ
    private boolean ObjectAlreadyInList(DepListItem checkItem)
    {
        boolean retVal = false;
        if (checkItem != null)
        {
            for (DepListItem item : FullPCKItemsList) {
                if (item.Type.equals(checkItem.Type) &&
                        item.TBP.equals(checkItem.TBP) && item.Object.equals(checkItem.Object))
                {
                    retVal = true;
                    break;
                }
            }
        }
        return retVal;
    }
}